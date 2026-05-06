# release-v1 截图目录

更新时间：2026-04-30

本目录用于保存 JavaFX 版发布材料 v1 的截图。构建产物仍放在 `JQJava/target/`，不放入本文档目录。

## 命名规范

- `01-main-competitive-8x8.png`：竞技化 `8x8` 主界面。
- `02-traditional-14x14.png`：传统基础 `14x14` 主界面。
- `03-formation-capture.png`：阵型触发后的补吃状态。
- `04-winning-pattern.png`：拉萨/金鱼可选获胜阵型相关界面或结果。
- `05-fly-threshold-adjudication.png`：弱势方飞子临界保门胜相关界面或结果。

## 截图步骤

1. 在 `JQJava/` 目录执行 `mvn javafx:run`。
2. 按 [演示脚本 v1](../../演示脚本-v1.md) 准备对应局面。
3. 使用系统截图工具保存为 PNG。
4. 将图片放入本目录，并在发布检查清单中勾选对应项。

## 当前状态

本次发布材料先提供截图目录、命名规范和采集步骤。若当前运行环境无法稳定打开 JavaFX GUI，可交付本 README 作为占位说明；不得使用伪造截图替代真实界面。
