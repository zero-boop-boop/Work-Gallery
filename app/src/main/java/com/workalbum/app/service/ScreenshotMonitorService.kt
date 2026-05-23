package com.workalbum.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.workalbum.app.MainActivity
import com.workalbum.app.R

/**
 * 截图监控服务
 *
 * 通过监听系统 MediaStore 的变化来检测新截图。
 * 当检测到新截图时，发送通知询问用户是否移入工作相册。
 *
 * 注意：这是一个前台服务（Foreground Service），
 * 需要显示持续通知才能保持运行，Android 8+ 强制要求。
 */
class ScreenshotMonitorService : Service() {

    private lateinit var contentObserver: ContentObserver
    private val handler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        // 前台服务必须显示通知
        startForeground(
            NOTIFICATION_ID_MONITOR,
            buildMonitorNotification("截图监控运行中")
        )

        // 注册 ContentObserver 监听媒体库变化
        contentObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                if (uri != null) {
                    checkNewScreenshot(uri)
                }
            }
        }

        // 监听外部存储中的图片变化
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
    }

    /**
     * 检查新文件是否为截图
     */
    private fun checkNewScreenshot(uri: Uri) {
        val cursor = contentResolver.query(
            uri,
            arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
            ),
            null, null, null
        ) ?: return

        if (cursor.moveToFirst()) {
            val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            val relCol = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
            val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)

            val imageId = if (idCol >= 0) cursor.getLong(idCol) else -1L
            val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
            val relPath = if (relCol >= 0) cursor.getString(relCol) ?: "" else ""
            val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""
            val dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else 0L

            // 多种方式判断截图
            val isScreenshot = path.lowercase().contains("screenshot") ||
                    relPath.lowercase().contains("screenshot") ||
                    name.lowercase().contains("screenshot") ||
                    relPath.lowercase().contains("pictures/screenshots") ||
                    relPath.lowercase().contains("dcim/screenshots")

            // 额外检查：最近 5 秒内添加的图片（截图刚产生）
            val nowSeconds = System.currentTimeMillis() / 1000
            val isRecent = (nowSeconds - dateAdded) < 5

            android.util.Log.d("WorkAlbum", "check: id=$imageId path=$path rel=$relPath name=$name screenshot=$isScreenshot recent=$isRecent")

            if (isScreenshot && isRecent && imageId > 0) {
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId
                )
                showScreenshotNotification(contentUri)
            }
        }
        cursor.close()
    }

    /**
     * 弹出截图通知，询问用户是否移入工作相册
     */
    private fun showScreenshotNotification(imageUri: Uri) {
        // 打开 APP 的 Intent
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // "移入工作相册" 按钮的 Intent
        val moveIntent = Intent(this, ScreenshotActionReceiver::class.java).apply {
            action = ACTION_MOVE_SCREENSHOT
            putExtra(EXTRA_IMAGE_URI, imageUri.toString())
        }
        val movePendingIntent = PendingIntent.getBroadcast(
            this, 1, moveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // "忽略" 按钮的 Intent
        val ignoreIntent = Intent(this, ScreenshotActionReceiver::class.java).apply {
            action = ACTION_IGNORE
        }
        val ignorePendingIntent = PendingIntent.getBroadcast(
            this, 2, ignoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_SCREENSHOT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📸 检测到新截图")
            .setContentText("是否移入工作相册？")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .addAction(0, "移入工作相册", movePendingIntent)
            .addAction(0, "忽略", ignorePendingIntent)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID_SCREENSHOT, notification)
    }

    private fun buildMonitorNotification(text: String) =
        NotificationCompat.Builder(this, CHANNEL_ID_MONITOR)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("工作相册")
            .setContentText(text)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun createNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java)

        // 监控服务通知渠道（低优先级）
        val monitorChannel = NotificationChannel(
            CHANNEL_ID_MONITOR,
            "截图监控",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(monitorChannel)

        // 截图通知渠道（高优先级）
        val screenshotChannel = NotificationChannel(
            CHANNEL_ID_SCREENSHOT,
            "截图提醒",
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(screenshotChannel)
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(contentObserver)
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID_MONITOR = "screenshot_monitor"
        private const val CHANNEL_ID_SCREENSHOT = "screenshot_alert"
        internal const val NOTIFICATION_ID_MONITOR = 1001
        internal const val NOTIFICATION_ID_SCREENSHOT = 1002

        const val ACTION_MOVE_SCREENSHOT = "com.workalbum.app.MOVE_SCREENSHOT"
        const val ACTION_IGNORE = "com.workalbum.app.IGNORE_SCREENSHOT"
        const val EXTRA_IMAGE_URI = "image_uri"

        fun start(context: Context) {
            val intent = Intent(context, ScreenshotMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ScreenshotMonitorService::class.java)
            context.stopService(intent)
        }
    }
}
