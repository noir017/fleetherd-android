# fleetherd-android

[fleetherd](https://github.com/noir017/fleetherd) WebUI 的轻量级原生安卓壳。

使用纯 Kotlin + Android System WebView 实现：服务端 WebUI 每次更新自动生效，无需发版；同时彻底去掉浏览器地址栏和多余窗口，补齐安卓原生体验。

[English](README.md)

## 核心特性

- **无浏览器窗口**：独立应用窗口、与 WebUI 一致的 `>_` 自适应图标、冷启动深色背景（`#131313`）防白屏。
- **状态栏与导航栏动态变色**：Edge-to-Edge 沉浸式适配，支持 160ms 平滑渐变及基于亮度的状态栏/导航栏图标自动反色（深底白字、浅底黑字）。无需改服务端即可自动跟随 `<meta name="theme-color">`、抽屉和全屏面板变色，同时提供 `window.FleetherdNative.setSystemBars(...)` 供前端主动控色。
- **软键盘避让**：精准响应 IME 高度，输入法弹出时不会遮挡底部输入框。
- **多实例管理（零硬编码）**：支持随时添加、切换、删除多个 `fleetherd` 服务器地址（例如 `https://fleetherd.example.com`）。随时可通过以下方式打开服务器管理：
  - 桌面长按应用图标 → **Manage Servers**
  - 三连击顶部状态栏区域
  - 连接失败页面的 **Switch Server** 按钮
  - 前端调用 `window.FleetherdNative.openServerManager()`
- **分层返回手势**：优先关闭页面内的弹窗/菜单/抽屉（调用 `window.__fleetherdBack()` 或自动派发 `Escape`），再回退历史记录，最后双击退出。
- **认证与 SSO 兼容**：持久化 `CookieManager`，同域及允许的 SSO 域名（默认放行常见 IdP，也可按实例自定义）保留在应用内完成登录，聊天气泡里的外部链接自动走 Custom Tabs 打开。
- **上传、下载与语音**：支持多选文件/图片上传、带会话 Cookie 的系统 `DownloadManager` 附件下载、以及录音按钮的麦克风权限动态申请。

## JS Bridge 协议

仅在当前页面与配置的服务器**严格同源**时生效：

```js
// 动态设置状态栏与导航栏颜色
window.FleetherdNative?.setSystemBars({
  statusBar: '#171717',
  navBar: '#131313',
  // 可选；省略时按背景亮度自动判断图标深浅
  lightStatusIcons: false,
});

// 唤起原生服务器切换弹窗
window.FleetherdNative?.openServerManager();

// 获取客户端版本信息
window.FleetherdNative?.getAppInfo();
```

## 构建与发布

本地构建需要 JDK 17、Android SDK (API 35) 与 Gradle 8.11+：

```bash
gradle testDebugUnitTest assembleDebug
```

仓库已配置 GitHub Actions：
- 推送 `main` 自动运行单元测试并产出可下载的 Debug APK；
- 推送 `v*` 标签自动发布 GitHub Release（若仓库配置了 `ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、`ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`，则产出签名 Release APK）。

## 后续规划（Phase 3）

- 接入原生离线推送（UnifiedPush / 自建 ntfy 或 FCM），点击通知直接跳转到对应话题（`/?t=<topic>`）。

## 许可证

MIT
