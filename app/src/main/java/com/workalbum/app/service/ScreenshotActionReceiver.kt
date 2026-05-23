package com.workalbum.app.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.*

class ScreenshotActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 用 goAsync() 保持 Receiver 存活直到异步操作完成
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val nm = context.getSystemService(NotificationManager::class.java)

                when (intent.action) {
                    ScreenshotMonitorService.ACTION_MOVE_SCREENSHOT -> {
                        val uriString = intent.getStringExtra(ScreenshotMonitorService.EXTRA_IMAGE_URI)
                        if (uriString != null) {
                            val app = context.applicationContext as? com.workalbum.app.WorkAlbumApplication
                            if (app != null) {
                                app.imageRepository.saveSharedImage(Uri.parse(uriString), "screenshot")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "截图已移入工作相册", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        nm.cancel(ScreenshotMonitorService.NOTIFICATION_ID_SCREENSHOT)
                    }
                    ScreenshotMonitorService.ACTION_IGNORE -> {
                        nm.cancel(ScreenshotMonitorService.NOTIFICATION_ID_SCREENSHOT)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "操作失败", Toast.LENGTH_SHORT).show()
                }
            } finally {
                // 通知系统 Receiver 工作完成
                pendingResult.finish()
            }
        }
    }
}