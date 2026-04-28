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
mvn test
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

## AI 后端

默认 AI 后端是纯 Java AI，不依赖 native DLL。

界面中还可选择：

- 原生 AI：通过 JNA 调用 `src/main/resources/native/win-x64/jqai.dll`。
- 原生 AI + Java 校验：调用 native AI 后，由 Java 规则层校验返回着法再执行。

native AI 仅面向 Windows x64。

## 对局文件

Java 版完整存档使用 `.jqj`。该格式保存规则模式、传统胜负模式、AI 后端、阶段、当前方和完整棋盘状态。

旧版 `0/1/2` 文本棋盘仍可读取，但只包含棋盘矩阵，不包含阶段、轮到方、AI 设置等完整对局状态。

## 传统胜负

界面提供传统胜负模式选项：

- 关闭
- 固定棋形获胜
- 吉祥阵型获胜
- 让棋指定阵型

当前版本已经保存和恢复该配置，但完整阵型识别与对应胜负判定仍是后续迁移内容；开启后暂不改变实际胜负判定。
