# 藏久棋AI算法深度讲解

> 源文件：`JQ/jqai/ai.cpp`，`JQ/jqai/ai.h`
> 规则基线： [藏族久棋最新规则.pdf](藏族久棋最新规则.pdf)
> 维护说明：本文主要讲解 C++ native AI。最新接口、棋盘尺寸、JavaFX AI 后端和非法着法修复状态，以 [当前实现状态](当前实现状态.md) 与 [变更记录](变更记录.md) 为准。

---

## 目录

1. [整体框架](#1-整体框架)
2. [数据结构](#2-数据结构)
3. [预计算查找表](#3-预计算查找表)
4. [Zobrist哈希与置换表](#4-zobrist哈希与置换表)
5. [布局阶段AI](#5-布局阶段ai)
6. [行棋阶段AI](#6-行棋阶段ai)
7. [评估函数](#7-评估函数)
8. [走法生成](#8-走法生成)
9. [Alpha-Beta搜索](#9-alpha-beta搜索)
10. [迭代加深](#10-迭代加深)
11. [DLL接口与调用流程](#11-dll接口与调用流程)
12. [常量与调参说明](#12-常量与调参说明)
13. [当前覆盖范围与后续扩展](#13-当前覆盖范围与后续扩展)
14. [实现同步摘要](#14-实现同步摘要)

---

## 1. 整体框架

AI模块以C++ DLL形式存在，供C#主程序通过P/Invoke调用。整体分为两个相对独立的搜索系统：

```
getAIMove()  ←── 统一入口
    │
    ├─── state == 1（布局阶段）
    │        └─── neg_alpha_beta_embattle()   迭代加深α-β搜索
    │                 ├─── make_embattle_move()    走法生成
    │                 ├─── eval_embattle()          增量评估
    │                 └─── ProbeHash/RecordHash     置换表
    │
    └─── state == 2/3/4/5（行棋与飞子阶段）
             └─── neg_alpha_beta()            迭代加深α-β搜索
                      ├─── make_move()            走法生成
                      ├─── make_final_move()       走法筛选
                      ├─── eval_move()             局面评估
                      └─── ProbeHash/RecordHash    置换表
```

**布局与行棋/飞子使用不同的评估逻辑**，原因是布局阶段没有移动只有落子，而行棋和飞子阶段有复杂的移动、跳跃吃子链。

当前 native AI 已经支持飞子基础走法、竞技化走法过滤和尺寸感知入口。JavaFX 版另有纯 Java AI 与 native AI 校验层。按照最新规则 PDF，后续还应补充成阵型吃、吉祥阵型，以及传统固定棋形胜负规则。

---

## 2. 数据结构

### 2.1 棋盘表示

```cpp
unsigned char qp[14][14];  // 全局棋盘状态
// 0 = 空位，1 = 白棋，2 = 黑棋
```

注意：DLL内部用 1/2 表示棋子，C#层用奇偶性区分，传入时会做转换：
```csharp
// MyPanel.cs 中的转换逻辑
tcb[i*14+j] = cb[i,j] > 0 ? (byte)(2 - cb[i,j] % 2) : (byte)0;
// 奇数(白) → 1，偶数(黑) → 2
```

### 2.2 走法结构（行棋阶段）

```cpp
typedef struct _Amove {
    Point path[CB_WIDTH*CB_WIDTH/4 + CB_WIDTH/4]; // 移动路径
    Point fc[2];   // 成方吃子位置（最多2个）
    int length;    // 路径长度（含起点）
    int fccount;   // 成方吃子数量
    int cmpvalue;  // 用于排序的比较值（越大越优先搜索）
} Amove;
```

- `path[0]` 是起点，`path[length-1]` 是终点
- 相邻两点距离为2时表示跳跃吃子，被吃棋子在中间格
- `fc[]` 记录成方后需要吃掉的敌方棋子坐标

### 2.3 合法走法集合

```cpp
typedef struct _Legal_moves {
    int count;
    Amove moves[MOVE_MAKE_PRE_MOVE_COUNT * MOVE_MAKE_FINAL_MOVE_FC2_BRANCH_UP_TH
                * (MOVE_MAKE_FINAL_MOVE_FC2_BRANCH_UP_TH - 1) / 2];
} Legal_moves;
// 最大容纳 20 * 10 * 9 / 2 = 900 个走法
```

### 2.4 跳跃搜索栈（JumpStack）

```cpp
typedef struct _JumpStackNode {
    int sdd;   // 当前搜索方向（0~4）
    int frd;   // 禁止方向掩码（防止走回头路）
    Point pts; // 当前位置
} JumpStackNode;

typedef struct _JumpStack {
    int top;
    JumpStackNode stack[CB_WIDTH*CB_WIDTH/4 + CB_WIDTH/4];
} JumpStack;
```

用**显式栈模拟递归**来枚举所有跳跃链，避免真正递归的开销和栈溢出风险。`frd`字段用位掩码记录禁止返回的方向，防止走回头路形成死循环（但允许在更长路径中绕回来）。

---

## 3. 预计算查找表

### 3.1 Embattle_Connect_Two（相邻格表）

```cpp
Embattle_Connect_Two ect[14][14];
// ect[y][x] 存储位置(x,y)的所有水平/垂直相邻位置
```

用于快速查询：某位置周围是否有同色棋子相连（用于评估布局时的"两连"价值）。

### 3.2 Embattle_Connect_Door（成方关联格表）

```cpp
Embattle_Connect_Door ecd[14][14];
// ecd[y][x] 存储与位置(x,y)能构成正方形的另外三个角的所有组合
```

这是最关键的预计算表。对于14×14棋盘上的每个位置，预先枚举了所有能与之构成**1×1正方形**的三元组。

**举例**：对于位置(2,2)，ecd[2][2]包含：
- `{(3,2), (3,3), (2,3)}` —— 右下正方形的其他三角
- `{(1,2), (1,3), (2,3)}` —— 左下正方形的其他三角
- `{(3,2), (3,1), (2,1)}` —— 右上正方形的其他三角
- `{(1,2), (1,1), (2,1)}` —— 左上正方形的其他三角

通过这个表，判断某位置是否形成正方形只需 O(4) 次查询，而不是O(n²)遍历。

---

## 4. Zobrist哈希与置换表

### 4.1 Zobrist键的生成

```cpp
U64 zobrist[3][14][14];  // 3种状态(空/白/黑) × 14 × 14

void init_zobrist() {
    // 为每个(状态, 位置)生成随机64位数
    zobrist[k][i][j] = rand64();
}

// 当前局面的Zobrist键 = 所有非空位置的zobrist值的XOR
U64 get_qp_zobrist() {
    U64 r = 0;
    for 每个位置(i,j):
        r ^= zobrist[qp[i][j]][i][j];
    return r;
}
```

**增量更新**：每次落子/移动只需XOR两个值，无需重新计算整个键：
```cpp
// 落一个棋子（将空位(x,y)变为side方）
zobrist_key ^= zobrist[0][y][x];    // 取消"空"的贡献
zobrist_key ^= zobrist[side][y][x]; // 加上"side"的贡献
```

### 4.2 置换表（哈希表）

```cpp
#define HASHTABLESIZE (1024 * 1024 * 128)  // 128MB，约800万条目

typedef struct _HashNode {
    U64 key;    // Zobrist键（用于验证碰撞）
    int depth;  // 搜索深度
    int flags;  // HASHFEXACT/HASHFALPHA/HASHFBETA
    int value;  // 评估值
} HashNode;
```

**三种标志类型**：

| 标志 | 含义 | 使用场景 |
|------|------|----------|
| `HASHFEXACT` | 精确值 | 正常剪枝内完整搜索 |
| `HASHFALPHA` | 上界（未超过alpha） | 全部走法均未改善alpha |
| `HASHFBETA` | 下界（超过beta）| 发生beta剪枝 |

**查表逻辑**（`ProbeHash`）：
```cpp
int ProbeHash(int depth, int alpha, int beta) {
    HashNode* node = &hashtable[zobrist_key & (HASHTABLESIZE-1)];
    if (node->key == zobrist_key && node->depth >= depth) {
        if (flags == EXACT)  return node->value;
        if (flags == ALPHA && value <= alpha)  return alpha;
        if (flags == BETA  && value >= beta)   return beta;
    }
    return HASHUNKNOWNVALUE;
}
```

只有当存储的深度 ≥ 当前需要的深度时，才使用缓存结果（浅层搜索结果不能代替深层）。

---

## 5. 布局阶段AI

### 5.1 走法生成（`make_embattle_move`）

布局阶段走法就是选择空位落子。但并非随机选择，而是**对每个空位评分后按分排序**：

```
位置得分 = 位置中心性 + 相邻棋子数 × 100 + 三连情况 × 200 + 成方潜力 × 1000
```

**位置中心性**：越靠近棋盘中心分越高（通过计算与边缘的最短距离）。

**三连情况**：检查该位置落子后是否形成横向或纵向的三子连线，且至少一侧开放。

**成方潜力（CONNECT_DOOR）**：检查落子后另外三个角是否已有同色棋子，预示可能形成正方形。

**`ect[y][x]` 的使用**：快速枚举相邻位置，判断是否同色。

**迭代加深时的优先搜索**：当`iterdeep==1`时，将上一次迭代的最佳落点 `embattle_best_move` 排到队列最前面，使alpha-beta搜索更早产生剪枝。

### 5.2 布局评估（`eval_embattle` + `black_first_step_pre_door_score`）

`eval_embattle(x, y, side)` 是**增量评估**函数，每落一个子就增量更新总分：

```
单子得分（己方） = +中心性 + 准成方数×200 + 两连数×100 + 开放度×10 + 三连开放度×200 + 成方×1000 + 六方×300
单子得分（敌方） = -(以上各项，且部分还有额外惩罚×RIVAL_EXT)
```

`black_first_step_pre_door_score()` 是特殊的开局评估：
- 专门检测黑方是否在开局就在中心区域（(6,6)和(7,7)附近）形成了准成方
- 白方开局时用这个函数惩罚让黑方过早成方的局面

### 5.3 布局Alpha-Beta搜索（`neg_alpha_beta_embattle`）

```cpp
int neg_alpha_beta_embattle(int depth, int alpha, int beta, int side, int eval) {
    // 1. 查置换表
    if (ProbeHash(depth, alpha, beta) != UNKNOWN) return cached_value;

    // 2. 到达叶节点，返回评估值
    if (depth == 0) {
        score = eval;  // eval是从根节点增量累加的总分
        if (myside==1) score -= black_first_step_pre_door_score();
        return score * (奇偶符号);
    }

    // 3. 生成走法并搜索
    moves = make_embattle_move();
    for (each move m) {
        qp[m.y][m.x] = side;           // 落子
        zobrist_key ^= ...;              // 更新哈希
        score = -neg_alpha_beta_embattle(
            depth-1, -beta, -alpha,
            3-side,
            eval + eval_embattle(m.x, m.y, side)  // 增量评估
        );
        qp[m.y][m.x] = 0;              // 撤销
        zobrist_key ^= ...;

        if (score > alpha) { alpha = score; 记录最佳走法 }
        if (score >= beta) return beta;  // beta剪枝
    }
    return alpha;
}
```

**负极大值形式（Negamax）**：通过取负转换视角，无需区分MAX/MIN节点，代码更简洁。

---

## 6. 行棋阶段AI

### 6.1 行棋走法生成（`make_move`）

行棋走法比布局复杂得多，分两步：

**第一步：枚举所有移动路径**

对每个己方棋子，使用显式栈（JumpStack）枚举所有可能的：
1. **平移**：移到相邻空格
2. **跳跃链**：连续跳过敌方棋子（每跳一次吃掉一个，可连跳）

跳跃枚举的关键：
- `frd` 禁止方向掩码防止立即原路返回（避免A→B→A死循环）
- 允许绕圈回到已经过的位置（但要检测，代码中有处理）
- 每个跳跃终点都记录为一个独立走法

**第二步：对每个走法计算比较值（cmpvalue）**

```
走法得分 = 路径长度×1000 + 终点接近成方数×1300 + 两连数×100 + 位置中心性提升
         + 成方数×2000 + 经过己方成方棋子的被封堵棋子数×700
```

成方（FC）相关的走法优先级最高（`MOVE_MAKE_MOVE_VALUE_FC = 2000`）。

**`rival=1` 时的特殊处理**：生成敌方走法时，按起点合并走法（同一棋子的所有走法合并为一个代表），减少搜索宽度。

### 6.2 最终走法筛选（`make_final_move`）

在`make_move`生成的走法基础上，进一步：
1. 将上次迭代的最佳走法排在第一位（迭代加深优化）
2. 参考对方走法来评估我方每个走法的威胁性
3. 根据成方数量和路径长度重新排序（`finalmovecmp`）
4. 限制搜索宽度：
   - 预筛选阶段最多 `MOVE_MAKE_PRE_MOVE_COUNT = 20` 个
   - 对含成方的走法（FC1）最多 `MOVE_MAKE_FINAL_MOVE_FC1_BRANCH_UP_TH = 20` 个
   - 对含2个成方的走法（FC2）最多 `MOVE_MAKE_FINAL_MOVE_FC2_BRANCH_UP_TH = 10` 个

### 6.3 行棋搜索（`neg_alpha_beta`）

```cpp
int neg_alpha_beta(int depth, int alpha, int beta, int side, int* ibt) {
    // 1. 查置换表
    // 2. 到达叶节点，调用 eval_move() 返回局面评估值
    // 3. 生成走法（make_move + make_final_move）
    // 4. 对每个走法：
    //    a. 执行走法（更新qp和zobrist_key）
    //    b. 递归搜索
    //    c. 撤销走法
    //    d. 更新alpha/beta
}
```

**执行一个走法**的状态更新：
1. 将起点清空（`qp[sy][sx] = 0`）
2. 将终点设为己方棋子（`qp[ey][ex] = side`）
3. 对路径中每个跳跃：将中间被吃棋子清空
4. 对成方吃子（fc[]）：将指定位置清空
5. 同步更新 `zobrist_key`

---

## 7. 评估函数

### 7.1 行棋阶段评估（`eval_move`）

这是最复杂的函数，全局扫描所有棋子，计算双方态势分：

```
最终分 = (己方棋子数 - 敌方棋子数) × 1000 + 己方态势 - 1.05 × 敌方态势
```

**态势分**由以下部分组成（对每个棋子遍历所有可达位置）：

| 项目 | 常量名 | 值 | 说明 |
|------|--------|----|------|
| 大连（可从成方格出发再成方） | MOVE_VALUE_DALIAN | 8000 | 最高奖励 |
| 准成方数 | MOVE_VALUE_PRE_FC | 1800 | 每个准成方奖励 |
| 成方数 | MOVE_VALUE_FC | 1300 | 当前已成方奖励 |
| 路径深度 | MOVE_VALUE_PIECE | 1000 | 跳跃链越长越好 |
| 两连数 | MOVE_VALUE_CONNECT_TWO | 100 | 棋子互相靠近 |

**特别注意**：敌方态势乘以1.05（`rivalscore * 1.05`），即AI防守时略微高估敌方威胁，使AI稍偏防守型。

### 7.2 准成方计算（`cacl_pre_door`）

检测某位置是否已经具备"再走一步就能成方"的潜力：

```cpp
// tinc 是一个4×4×2×4的偏移量表
// 遍历16种潜在的准成方模式（每个正方形有4个顶点，每个顶点有4种准成方位置）
for 每种准成方模式:
    if (三个相关位置都是己方棋子 && 目标位置为空):
        predoorcount++;
```

---

## 8. 走法生成

### 8.1 跳跃链完整枚举

使用显式栈实现深度优先搜索，枚举所有跳跃组合：

```
初始状态：起点入栈，将起点清空（为了判断跳跃目标格是否空）

循环：
  取栈顶节点(x, y, 已搜索方向d, 禁止方向frd)
  for d in (d..3):
    tx = x + xinc[d], ty = y + yinc[d]  // 相邻格
    if qp[ty][tx] == 敌方:
      ttx = tx+xinc[d], tty = ty+yinc[d]  // 跳跃目标格
      if qp[tty][ttx] == 空:
        记录当前跳跃路径为一个候选走法
        新节点(ttx, tty, 0, 禁止反向)入栈
        break  // 从当前方向继续探索
  if d == 4（所有方向都搜索完）:
    出栈

恢复起点棋子
```

**关键设计**：`qp[i][j] = 0`（起点清空）确保棋子不会跳回自己的原位，但其他己方棋子仍在原位可以阻挡。

---

## 9. Alpha-Beta搜索

### 9.1 核心剪枝原理

```
Alpha（下界）：当前节点保证能达到的最小得分
Beta（上界）：对手不会让我们超过的分数

Beta剪枝（α≥β时）：当前节点已经"太好了"，对手不会选择走到这里
Alpha剪枝：在对手节点实现，通过负极大值转换统一处理
```

### 9.2 走法排序的重要性

Alpha-Beta的效率高度依赖走法排序。最佳走法排在最前面时，剪枝效率从O(b^d)降至O(b^(d/2))。本代码通过：
1. **`cmpvalue`字段**：生成时即评分排序
2. **`movecmp/finalmovecmp`**：按成方数量、路径长度排序
3. **最佳走法优先**：迭代加深时上次最佳走法排第一

### 9.3 搜索深度与超时

```cpp
// 超时检测在根节点层
if (depth == ai_search_depth) {
    time(&et);
    if (et - st > ai_search_timeout) break;
}
```

超时只在根节点检测，保证每次迭代的结果完整（不会只搜索了一半深度就返回）。

---

## 10. 迭代加深

```
深度 = 1 → 搜索，记录最佳走法
深度 = 2 → 上次最佳走法排第一，继续搜索
深度 = 3 → 同上
...
直到超时
```

```cpp
// 在 getAIMove 中（伪代码）
for (int d = 1; d <= ai_search_depth; d++) {
    iterdeep = 1;  // 启用最佳走法优先
    neg_alpha_beta(d, MIN_VALUE, MAX_VALUE, myside, &ibt);
    if (超时) break;
}
```

`iterdeep` 标志控制是否将上次最佳走法排在第一位。为避免在非根层级误用，搜索进入子节点时会临时置0（`tid = iterdeep; iterdeep = 0; ... iterdeep = tid;`）。

---

## 11. DLL接口与调用流程

### 11.1 接口定义（ai.h）

```cpp
// 导出函数
void getAIMove(
    unsigned char* cb,    // 棋盘，196字节一维数组 cb[y*14+x]
    unsigned char state,  // 1=布局，2=传统走子，3=竞技走子，4=传统飞子，5=竞技飞子
    unsigned char side,   // AI执子方：1=白，0=黑
    int aisearchdepth,    // 搜索深度（来自UI滑块）
    int timeout,          // 超时秒数
    char* bestmove        // 输出：最佳走法字符串
);

void destroy_hashtable();  // 清空置换表
```

### 11.2 返回格式

**布局阶段**：`"G7"`（列字母+行数字，如G列第7行）

**行棋阶段**：`"A1,B1,C1 TC-D2,E3"`
- 移动路径：`A1,B1,C1`（从A1经B1到C1）
- 吃子列表（可选）：` TC-D2,E3`（TC=被吃子，D2和E3被吃）

### 11.3 C#调用流程（MyPanel.cs）

```
AIMove() → 新建线程 → AIThink()
             │
             ├── 将 cb[i,j] 转换为 byte[196]
             ├── 调用 getAIMove(tcb, state, turn, depth, timeout, bestmove)
             └── 解析 bestmove 字符串
                  ├── 布局：解析坐标 → 更新 cb[y,x] = ++no
                  └── 行棋：解析路径和吃子 → 更新 cb 数组 → ConfirmTempMove()
```

---

## 12. 常量与调参说明

### 布局阶段评估常量

| 常量 | 值 | 含义 |
|------|----|------|
| `EMBATTLE_VALUE_CONNECT_TWO` | 100 | 两子相连奖励 |
| `EMBATTLE_VALUE_CONNECT_THREE_ONE_SIDE_BLANK` | 200 | 三连一侧开放奖励 |
| `EMBATTLE_VALUE_CONNECT_DOOR` | 1000 | 准成方奖励 |
| `EMBATTLE_VALUE_CONNECT_SIX_DOOR` | 300 | 六方奖励 |
| `EMBATTLE_VALUE_PRE_DOOR` | 200 | 准成方位置奖励 |
| `EMBATTLE_VALUE_OPEN` | 10 | 开放度奖励 |
| `EMBATTLE_VALUE_RIVAL` | 10 | 对手额外惩罚 |
| `EMBATTLE_VALUE_RIVAL_EXT` | 200 | 对手成方额外惩罚 |
| `EMBATTLE_VALUE_BLACK_FIRST_STEP_PRE_DOOR` | 1500 | 黑方先手准成方惩罚 |

### 行棋阶段评估常量

| 常量 | 值 | 含义 |
|------|----|------|
| `MOVE_VALUE_DALIAN` | 8000 | 大连（双成方威胁）奖励 |
| `MOVE_VALUE_PRE_FC` | 1800 | 准成方奖励 |
| `MOVE_VALUE_FC` | 1300 | 成方奖励 |
| `MOVE_VALUE_PIECE` | 1000 | 棋子灵活度奖励 |
| `MOVE_VALUE_CONNECT_TWO` | 100 | 两连奖励 |
| `MOVE_MAKE_MOVE_VALUE_FC` | 2000 | 走法排序：成方走法权重 |
| `MOVE_MAKE_MOVE_VALUE_TC` | 600 | 走法排序：吃子走法权重 |

### 搜索宽度限制

| 常量 | 值 | 含义 |
|------|----|------|
| `MOVE_MAKE_PRE_MOVE_COUNT` | 20 | 每次搜索最多考虑20个走法 |
| `MOVE_MAKE_FINAL_MOVE_FC1_BRANCH_UP_TH` | 20 | 含1个成方的走法上限 |
| `MOVE_MAKE_FINAL_MOVE_FC2_BRANCH_UP_TH` | 10 | 含2个成方的走法上限 |

---

## 附：核心函数调用关系图

```
getAIMove()
├── state==1: 布局阶段
│   ├── init_hashtable() → init_zobrist()
│   ├── get_qp_zobrist()
│   └── neg_alpha_beta_embattle(depth, α, β, side, eval=0)
│       ├── ProbeHash()
│       ├── make_embattle_move() → qsort(ptscmp)
│       │   └── 使用 ect[][], ecd[][] 查找表
│       ├── eval_embattle(x, y, side)
│       │   └── cacl_zfc()
│       ├── black_first_step_pre_door_score()
│       └── RecordHash()
│
└── state==2/3/4/5: 行棋与飞子阶段
    ├── eval_move() → 初始局面评估
    └── neg_alpha_beta(depth, α, β, side, ibt)
        ├── ProbeHash()
        ├── eval_move() → 叶节点评估
        ├── make_move(side, rival, after_vals)
        │   ├── JumpStack 枚举跳跃链
        │   └── qsort(movecmp)
        ├── make_after_myfc_rival_legal_move_values()
        ├── make_final_move(depth, moves, rival_moves, after_vals)
        │   └── qsort(finalmovecmp)
        └── RecordHash()
```

## 13. 当前覆盖范围与后续扩展

### 13.1 当前 AI 已覆盖的规则范围

从现有 `ai.cpp` 和 `ai.h` 看，当前 AI 覆盖以下部分：

- 布局阶段搜索
- 行棋阶段搜索
- 飞子阶段基础走法生成

当前 AI 已经支持的重点规则包括：

- 布局阶段落子选择
- 行棋阶段相邻移动
- 跳吃与连跳
- 成方收益评估
- 成方后提子结果的搜索建模
- 飞子阶段任意己子移动到任意空点
- 竞技化模式下的飞子阶段强势方不能单吃过滤

当前 AI 状态编码为：

| 状态值 | 含义 |
|--------|------|
| `1` | 布局阶段 |
| `2` | 传统基础走子 |
| `3` | 竞技化走子 |
| `4` | 传统基础飞子 |
| `5` | 竞技化飞子 |

JavaFX 版纯 Java AI 与 native AI 共用着法字符串协议，但实现路线不同。纯 Java AI 现在保留 `negamax + alpha-beta + transposition table` 搜索框架，不使用蒙特卡洛树搜索；搜索层通过 `AiStrategyProfile` 分流评估和走法排序。竞技化 profile 延续材料、吃子、成方和即时战术权重；传统基础 profile 增加目标阵型进度、破坏对方棋门和飞子临界风险权重。传统目标阵型通过 `TraditionalPatternHeuristic` 扩展，`PatternShape` / `PatternShapeScanner` 负责模板扫描；当前会按前端选择的传统获胜阵型启用拉萨或金鱼目标启发。八吉祥其他阵型、让棋目标和飞子阵型尚缺可复核坐标模板，未进入规则判定或默认可选目标。

### 13.2 当前 AI 未覆盖的规则范围

当前 native AI 尚未把以下规则正式纳入评估；纯 Java AI 已覆盖一部分阵型补吃，以及按所选获胜阵型启用的拉萨/金鱼目标启发，但仍不是完整传统规则 AI：

- 飞子阶段专项评估函数
- 更完整的传统阵型长期目标建模
- 吉祥阵型识别与胜负建模
- 传统“固定棋形 + 吃子数量”的完整胜负判定
- 飞子阵型，例如“十字飞子久”和“九子五久飞子久”

因此，现有 AI 文档更准确的定位是“三阶段基础 AI 讲解”，而不是“完整传统规则 AI 讲解”。

### 13.3 后续扩展建议

若后续继续扩展 AI，建议按以下顺序推进：

1. 为飞子阶段增加专项评估项，优先考虑阻断对手成方和制造己方准成方。
2. 沿用纯 Java AI 的 profile 分流，把传统目标阵型继续挂到 `TraditionalPatternHeuristic`。
3. 再逐步把八吉祥、让棋目标、岚、三叉黑铁、恰、颂然，以及“十字飞子久”“九子五久飞子久”纳入收益评估。
4. 建立固定局面回归样例，验证飞子、连跳、成方补吃、阵型补吃和竞技化约束不会回退。

### 13.4 文档配套关系

如果要结合规则与实现一起阅读，建议同时参考：

- [藏久棋规则整理](藏久棋规则整理.md)
- [当前实现状态](当前实现状态.md)
- [架构说明](架构说明.md)
- [路线图](路线图.md)
- [变更记录](变更记录.md)

## 14. 实现同步摘要

- `getAIMove` 状态值从原来的布局/行棋二分扩展为 `1..5`。
- `getAIMoveEx(..., boardSize, ...)` 已作为尺寸感知入口保留，支持 `8x8` 与 `14x14`。
- `make_move()` 已接入飞子走法生成分支。
- 竞技化单吃限制在合法走法生成阶段过滤。
- `机机模式` 两侧均调用升级后的 `jqai.dll`，不再依赖旧 `jqai2.dll`。
- `destroy_hashtable()` 已修正为 `delete[]`。
- AI DLL 已在 VS Build Tools 2022 / MSVC `v143` 下完成 `Release|x64` 构建。
- JavaFX 版默认使用纯 Java AI，也可选择 native AI 或 native AI + Java 校验。
- 纯 Java AI 已按传统基础 / 竞技化规则模式拆分评估 profile，传统 profile 会按所选传统获胜阵型接入拉萨或金鱼目标启发，并叠加破门收益和飞子临界风险。
