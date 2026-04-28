# 藏久棋 Java 版运行说明

## 开发启动

```powershell
cd JQJava
mvn javafx:run
```

## Windows 发布构建

```powershell
cd JQJava
powershell -ExecutionPolicy Bypass -File scripts\package-windows.ps1
```

默认生成 app-image 到：

```text
target\dist\windows\ZangJiuQi-Java
```

可直接双击运行：

```text
target\dist\windows\ZangJiuQi-Java\ZangJiuQi-Java.exe
```

如需尝试生成 Windows 安装包：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\package-windows.ps1 -Installer
```

如果本机缺少 jpackage 安装器依赖，脚本会退回生成 app-image。
如果 WiX 或 Windows Installer 服务导致安装器生成失败，也可以直接使用上面的 app-image 目录运行程序。

## AI 后端

默认后端是纯 Java AI，不依赖 native DLL。

界面中仍可选择原生 AI 或原生 AI + Java 校验。该后端仅面向 Windows x64，`jqai.dll` 已作为可选资源随应用打包。

## 对局文件

Java 版完整存档使用 `.jqj`。旧版 `0/1/2` 文本棋盘仍可读取，但只包含棋盘矩阵，不包含阶段、轮到方、AI 设置等完整对局状态。

## 传统胜负

界面提供“传统胜负”模式选择，默认关闭。当前版本已经保存和恢复该配置，但吉祥阵型、指定阵型和固定棋形胜负识别仍是后续迁移内容；开启后暂不改变实际胜负判定。
