# FancyL2D

为 FancyMenu 添加一个客户端 **Live2D 模型**元素。目标：Minecraft 26.1.2 / NeoForge 26.1.2.130 / Java 25。编辑器默认显示静态占位，可按元素开启实时预览。

## 安装与使用

1. 将 `fancyl2d-1.1.5.jar` 放进客户端的 `mods`；升级时先移除旧版 FancyL2D JAR。安装 FancyMenu 3.11.0 及其前置 Konkrete 和 Melody；实际渲染需要 Rinku 3.0.4。不装 Rinku 时，游戏仍可运行，元素会显示占位提示。
2. 第一次显示 Live2D 模型时，如果当前实例缺少 Cubism Core，模组会从 Live2D 官方地址后台下载并校验 SHA-256，然后保存到 `config/fancyl2d/live2dcubismcore.min.js`。以后直接使用本地文件，已有文件会保留。如果下载失败或官方文件发生变更，可手动放入 SDK 中的同名文件。[Live2D Cubism SDK for Web](https://www.live2d.com/en/sdk/download/web/)
3. 新建元素默认使用随模组分发的 **Dragon_Balanced**，无需复制模型。要换成其他模型，再将其完整目录放进 `config/fancyl2d/models/`，保留 `.model3.json`、`.moc3`、贴图及其他引用文件的相对结构。
4. 在 FancyMenu 布局编辑器添加 **Live2D 模型**，用 FancyMenu 自带的拖拽、尺寸和锚点布置元素。右击选「选择 Live2D 模型…」可搜索配置目录内的模型；也可以手填相对路径。内置选项的标识是 `builtin:dragon_balanced`；现有布局中的本地路径不会被覆盖。右击元素选择「开启实时预览」，可以在画布上观察位置、尺寸和模型设置。一个编辑器同时预览一个 Live2D 元素。退出编辑器会释放预览，重新进入时默认关闭。

| 选项 | 行为 |
| --- | --- |
| 模型缩放 | 以元素中点为中心，在适配元素尺寸的基础上缩放（0.1–5）。 |
| 鼠标跟随强度 | 0 关闭，1 为完整跟随；不发送点击或焦点。 |
| 摇摆幅度 | 0 关闭，1 为最大 ±4°；仅旋转角色容器，不改变模型大小或自身动作曲线。 |
| 待机动画 | 关闭后停用 `Idle` 动作组以及前端适配器的自动呼吸、眨眼；保留模型物理和鼠标跟随。 |
| 请求帧率 | 15–120，默认 60；同时传给 Pixi 和 Rinku。实际帧率受 Chromium、显示器和设备限制。 |

进世界前，同一个布局元素在菜单之间复用浏览器，离开画面时暂停模型动画，返回时恢复。切换语言和普通游戏资源重载不会主动清空模型。进入世界、退出世界或关闭客户端时清理会话；更改模型或渲染配置也会替换旧会话。返回标题界面后首次显示会重新加载。

页面和模型通过绑定在 `127.0.0.1` 上的临时端口加载，不需要开启 Chromium 的跨域文件访问。初次启用 Rinku 可能需要下载 Chromium 组件。加载失败可检查 `logs/latest.log`，更换模型或补齐 Core 后重新打开菜单。

## 构建与开发测试

需要完整 **JDK 25**、Node.js 24 和 pnpm 11（可通过 Corepack 安装）。把以下原始 JAR 放入项目的 `libs/`，不要解包或修改：

- `fancymenu_neoforge_3.11.0_MC_26.1.2.jar`
- `rinku_neoforge_3.0.4_MC_26.1.2.jar`
- `melody_neoforge_1.0.16_MC_26.1.1.jar`
- `konkrete_neoforge_1.10.1_MC_26.1.1.jar`

Windows：

```powershell
.\gradlew.bat build
.\gradlew.bat runClient
# 缺 Rinku 的独立降级测试（先结束上一次 runClient）
.\gradlew.bat runClient -PwithoutRinku
```

构建产物：`build/libs/fancyl2d-1.1.5.jar`。Gradle 自动构建锁定版本的 Web 前端；前置 JAR 不会打入产物。开发客户端的游戏目录是 `run/`，不读取正式客户端的布局或模型。

逐项操作、预期结果和故障排查见 [手动验收说明](TESTING.md)。
