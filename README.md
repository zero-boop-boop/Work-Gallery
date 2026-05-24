# 📁 工作相册 (Work Gallery)

> 一款 Android 工作图片管理工具，专为 ColorOS 优化。
> 将工作截图/微信图片与系统相册隔离存储，保持系统相册干净。

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material3-purple)](https://developer.android.com/compose)
[![MVP](https://img.shields.io/badge/status-MVP-green)]()
[![Platform](https://img.shields.io/badge/platform-Android_8%2B-orange)]()

---

## 🎯 解决什么问题

工作中经常从微信/钉钉/截图获取大量图片，这些图片混在系统相册里，忘记删就占用空间。
**工作相册**把它们单独存到 APP 私有目录，不进系统相册。

---

## ✨ 三大入口

| 入口 | 操作 | 状态 |
|------|------|:--:|
| 📸 截图自动 | 截图 → 通知栏点击 → 自动导入 + 删除系统原图 | ✅ |
| 👆 手动导入 | APP 内 ⊕ 按钮 → 从系统相册选图 | ✅ |
| 💬 微信 | 微信图片全屏 → 右上角 ··· → 用其他应用打开 → 工作相册 | ✅ |

> ✨ 导入后自动调用系统对话框删除系统相册原图（Android 11+）

---

## 🏗 技术架构

```
┌─────────────────────────────────────┐
│              Jetpack Compose          │
│  GalleryScreen ─ ImageDetailScreen  │
│        └── SettingsScreen            │
├─────────────────────────────────────┤
│         ViewModel + StateFlow        │
├─────────────────────────────────────┤
│          ImageRepository             │
│    ┌──────────┐  ┌───────────────┐   │
│    │ Room DB  │  │ 文件存储      │   │
│    │ (元数据) │  │ (私有目录)    │   │
│    └──────────┘  └───────────────┘   │
├─────────────────────────────────────┤
│         后台服务                      │
│  ScreenshotMonitorService           │
│  ├─ FileObserver    (秒级)          │
│  └─ ContentObserver (兜底)          │
├─────────────────────────────────────┤
│         接收入口                      │
│  ShareReceiverActivity              │
│  ├─ ACTION_SEND  (分享)            │
│  └─ ACTION_VIEW  (用其他应用打开)   │
└─────────────────────────────────────┘
```

| 层级 | 技术 |
|------|------|
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM (手动管理，未使用 AAC ViewModel) |
| 数据库 | Room (图片元数据 + 备注) |
| 异步 | Kotlin Coroutines + Flow |
| 图片加载 | Coil |
| 构建 | Gradle 8.9 + AGP 8.7 + Kotlin 2.0 |

---

## 📂 项目结构

```
app/src/main/java/com/workalbum/app/
├── MainActivity.kt              ← 核心：处理截图导入 + 系统删除
├── WorkAlbumApplication.kt      ← Application 初始化
├── model/
│   └── WorkImage.kt             ← Room Entity（数据模型）
├── data/
│   ├── ImageDao.kt              ← DAO 接口
│   ├── ImageDatabase.kt         ← Room 数据库
│   └── ImageRepository.kt       ← 数据仓库（文件+数据库）
├── receiver/
│   ├── ShareReceiverActivity.kt ← 分享/用其他应用打开 入口
│   └── BootReceiver.kt          ← 开机自启
├── service/
│   └── ScreenshotMonitorService.kt ← 截图监控（双通道）
└── ui/
    ├── theme/Theme.kt
    └── screens/
        ├── GalleryScreen.kt     ← 图片列表 + FAB 导入
        ├── ImageDetailScreen.kt ← 详情 + 备注
        └── SettingsScreen.kt    ← 设置 + 调试
```

---

## 🔧 本地开发

### 环境

- **JDK** 21 (`D:\Android Developers Tool\jbr`)
- **Android SDK** API 36 (`D:\Android\Sdk`)
- **Gradle** 8.9 (本地 `.gradle-local/`，不用 wrapper)
- **设备** ColorOS 16 / Android 14

### 编译 & 安装

```powershell
# 环境变量
$env:ANDROID_HOME = "D:\Android\Sdk"
$env:JAVA_HOME = "D:\Android Developers Tool\jbr"

# ⚠️ 编译前关闭 Clash Verge（否则 AGP 下载失败）

# 编译
.\gradlew.bat assembleDebug

# 安装 + 重启
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am force-stop com.workalbum.app.debug
adb shell am start -n com.workalbum.app.debug/com.workalbum.app.MainActivity
```

### GitHub CLI

```powershell
$env:HTTPS_PROXY = "http://127.0.0.1:37890"   # 推送需要代理
gh pr create
gh issue list
```

---

## 🚧 后续迭代方向

- [ ] **UI 美化** — 自定义图标、动画、深色模式优化
- [ ] **标签/分类** — 按项目/日期/来源给图片打标签
- [ ] **批量导出** — 导出到 ZIP/PDF
- [ ] **图片编辑** — 裁剪、标注、马赛克
- [ ] **云同步** — WebDAV / Google Drive 备份
- [ ] **OCR 搜索** — 识别图片文字内容
- [ ] **非 ColorOS 适配** — 小米/华为/三星
- [ ] **后台保活** — 引导用户锁定 APP 防被杀

---

## ⚠️ ColorOS 踩坑（备忘）

- 通知 `addAction` 按钮不显示 → 改用 `setContentIntent` + 点击通知体
- `contentResolver.delete()` 抛 SecurityException 而非返回0 → 用 `MediaStore.createDeleteRequest()`
- 微信长按无"分享"选项 → 用"用其他应用打开"（`ACTION_VIEW`）
- `gradle.properties` 不能用 UTF-8 BOM → 用 `[System.Text.UTF8Encoding]::new($false)`
- 项目路径含中文 → `android.overridePathCheck=true`

---

## 📄 License

MIT