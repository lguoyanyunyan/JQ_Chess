# JQ Chess / 藏久棋

本仓库包含两个藏久棋实现：

- `JQ/`：原始 Windows 桌面版，使用 C# WinForms 界面和 C++ `jqai.dll` AI。
- `JQJava/`：JavaFX 迁移版，默认使用纯 Java AI，也可在 Windows x64 下调用随包携带的 native AI。

当前工程定位是“藏久棋三阶段基础规则已落地的工程化原型”。程序已覆盖布子、走子、跑吃、成方补吃、飞子基础移动、传统基础 / 竞技化双规则模式、基本阵型与枪/煞阵型吃、拉萨获胜阵型（含弱势方飞子临界保门胜）和 AI 对弈；剩余特殊阵型、八吉祥阵型与更完整传统胜负体系仍属于后续扩展。

## 快速开始

### JavaFX 版

```powershell
cd JQJava
mvn javafx:run
```

运行测试：

```powershell
cd JQJava
mvn test
```

Windows 打包：

```powershell
cd JQJava
powershell -ExecutionPolicy Bypass -File scripts\package-windows.ps1
```

打包脚本会先运行测试，再执行 Maven package 和 `jpackage`，默认输出到 `JQJava/target/dist/windows/ZangJiuQi-Java`。如需安装包，可追加 `-Installer`；安装包失败时会回退生成 app-image。

### WinForms 版

使用 Visual Studio 或 MSBuild 打开并构建：

```text
JQ/JQ.sln
```

建议配置为 `x64`。C# 主程序通过 P/Invoke 调用 `jqai.dll`，机机对战历史上还会使用 `jqai2.dll` 作为第二 AI 实例。

## 文档入口

- [文档索引](docs/README.md)
- [当前实现状态](docs/当前实现状态.md)
- [架构说明](docs/架构说明.md)
- [规则整理](docs/藏久棋规则整理.md)
- [规则覆盖表](docs/规则覆盖表.md)
- [AI 算法说明](docs/AI算法深度讲解.md)
- [后续路线图](docs/路线图.md)
- [发布检查清单](docs/发布检查清单.md)
- [变更记录](docs/变更记录.md)

## 版本管理

仓库已配置 `.gitignore` 和 `.gitattributes`，构建产物、IDE 缓存、日志、临时文件和压缩包不会进入版本库。`JQJava/src/main/resources/native/win-x64/jqai.dll` 是 Java 版可选 native AI 资源，已作为二进制资源保留。
