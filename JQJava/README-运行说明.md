# 藏久棋 Java 版运行说明

## 环境要求

- JDK 17
- Maven 3.x
- Windows 打包需要 JDK 自带 `jpackage`

项目使用 JavaFX `17.0.10`、JNA `5.14.0` 和 JUnit 5。

## 开发运行

```powershell
cd JQJava
mvn javafx:run
```

## 测试

```powershell
cd JQJava
mvn -q test
```

发布前建议再运行一次 package：

```powershell
mvn -q -DskipTests package
```

## Windows 发布构建

生成 app-image：

```powershell
cd JQJava
powershell -ExecutionPolicy Bypass -File scripts\package-windows.ps1
```

默认输出目录：

```text
target\dist\windows\ZangJiuQi-Java
```

可直接运行：

```text
target\dist\windows\ZangJiuQi-Java\ZangJiuQi-Java.exe
```

尝试生成 Windows 安装包：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\package-windows.ps1 -Installer
```

如果本机缺少安装包依赖，脚本会回退生成 app-image。

打包产物位于 `target\dist\windows`，属于本地构建产物，不应提交到版本库。发布前检查项见 [发布检查清单](../docs/发布检查清单.md)。

## AI 后端

默认 AI 后端是纯 Java AI，不依赖 native DLL。

纯 Java AI 继续使用 `negamax + alpha-beta + transposition table` 搜索框架，没有引入蒙特卡洛树搜索。当前已按规则模式拆分策略 profile：竞技化模式偏重吃子、材料和即时战术；传统基础模式会提高目标阵型进度、破坏对方棋门和飞子临界风险的权重。传统阵型目标采用可扩展 heuristic 和模板扫描底座，并按当前选择的传统获胜阵型启用目标：选择 `拉萨` 时围绕双门/三门拉萨，选择 `金鱼` 时围绕金鱼；后续八吉祥其他阵型、让棋目标或飞子阵型可继续挂入。

界面中还可选择：

- 原生 AI：通过 JNA 调用 `src/main/resources/native/win-x64/jqai.dll`。
- 原生 AI + Java 校验：调用 native AI 后，由 Java 规则层校验返回着法再执行。

native AI 仅面向 Windows x64。

## 对局文件

Java 版完整存档使用 `.jqj`。该格式保存规则模式、传统获胜阵型、AI 后端、阶段、当前方和完整棋盘状态。

旧版 `0/1/2` 文本棋盘仍可读取，但只包含棋盘矩阵，不包含阶段、轮到方、AI 设置等完整对局状态。

## 传统获胜阵型

界面提供传统获胜阵型选项：

- 关闭
- 拉萨
- 金鱼

选择 `拉萨` 或 `金鱼` 后，传统基础模式下只围绕当前选择的获胜阵型结算胜负和引导传统 AI：

- `拉萨`：检查双门拉萨或三门拉萨；本手形成并完成补吃，或补吃结算后强势方已静态保持完整拉萨，都可进入获胜判断。
- `金鱼`：检查 PDF 图17 原图转写的金鱼模板，允许旋转与镜像；它作为单独获胜阵型接入，不代表八吉祥整套制度已实现。
- 强方获胜条件：强方棋子更多、弱方本手开始时尚未进入飞子阈值，且结算后弱方没有棋门。
- 弱方临界保门胜：弱势方本手首次被吃到飞子阈值内时，若强势方本手未形成且盘面未保持所选获胜阵型，并且弱势方仍有棋门，则判弱势方获胜。

竞技化规则下该选项会自动归一为关闭。旧 `.jqj` 中 `traditionalWinMode=OFF` 可兼容读取；旧非关闭传统胜负方式需要重新选择获胜阵型后再保存。
