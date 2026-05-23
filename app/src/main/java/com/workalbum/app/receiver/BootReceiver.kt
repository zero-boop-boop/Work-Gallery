package com.workalbum.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.workalbum.app.service.ScreenshotMonitorService

/**
 * 开机启动接收器：重启后自动恢复截图监控服务
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ScreenshotMonitorService.start(context)
        }
    }
}
