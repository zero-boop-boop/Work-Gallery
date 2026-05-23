# 工作相册 (Work Album)

## 项目概述
Android 工作图片管理 APP，将工作图片与系统相册隔离存储。

## 技术栈
- 语言：Kotlin 2.0+
- UI：Jetpack Compose + Material 3
- 架构：MVVM (ViewModel + StateFlow)
- 数据库：Room (存储图片元数据和备注)
- 异步：Kotlin Coroutines + Flow
- 构建：Gradle 8.9 + AGP 8.7+
- 最低SDK：26 (Android 8.0)
- 目标SDK：36

## 项目结构
```
app/src/main/java/com/workalbum/app/
├── MainActivity.kt          # 主入口
├── WorkAlbumApplication.kt  # Application 类
├── ui/
│   ├── screens/
│   │   ├── GalleryScreen.kt      # 图片列表主页
│   │   └── ImageDetailScreen.kt  # 图片详情（查看+备注）
│   └── theme/
│       └── Theme.kt              # Material3 主题
├── receiver/
│   └── ShareReceiver.kt          # 接收外部分享的图片
├── service/
│   └── ScreenshotMonitorService.kt  # 截图监控服务
├── data/
│   ├── ImageRepository.kt        # 数据仓库
│   ├── ImageDatabase.kt          # Room 数据库
│   └── ImageDao.kt               # DAO 接口
└── model/
    └── WorkImage.kt              # 数据模型
```

## 编码偏好
- 使用中文注释说明关键逻辑
- 包名：com.workalbum.app
- 图片存储在 APP 私有目录：/sdcard/Android/data/com.workalbum.app/files/images/
- 权限：仅请求必要权限，遵循最小权限原则
- 所有文件操作使用 Android Scoped Storage API

## 踩坑记录（ColorOS 适配）
- ColorOS 禁用后台通知 action 按钮，BroadcastReceiver + Notification Action 方案不可行
- 微信的"保存图片"按钮走微信自有通道，无法拦截；需用"分享"入口
- gradlew.bat 绕过 wrapper jar：指向本地 Gradle 分发版
- gradle.properties 必须用 ASCII 编码，UTF-8 BOM 会导致 Android Gradle Plugin 无法识别
- 项目路径含中文需加 `android.overridePathCheck=true`
- 通知栏 action 按钮（addAction）在 ColorOS 无效，替代方案：点击通知体触发 PendingIntent.getActivity()
- `adb install -r` 后需 `am force-stop` + `am start` 才能加载新代码

## 技术决策
- 截图监控：从后台 ContentObserver 改为 APP 内手动导入（ColorOS 兼容）
- 通知方案：从 BroadcastReceiver 改为 PendingIntent → MainActivity 直接处理
- Gradle：本地 `.gradle-local/gradle-8.9` 而非标准 wrapper

## 下一步
- 砍掉 ScreenshotMonitorService，改为 APP 内"从系统相册导入"按钮
- 在非 OPPO 手机上验证分享入口
- 详见 TODO.md