package com.workalbum.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.workalbum.app.service.ScreenshotMonitorService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val pm = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
    val isIgnoringBattery = remember {
        pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置与帮助") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 电池优化
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.BatteryAlert, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("电池优化", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("华为/小米/OPPO/vivo 会自动杀后台，导致截图监控失效。", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "当前状态：" + if (isIgnoringBattery) "已免除电池优化" else "受电池优化限制",
                        color = if (isIgnoringBattery) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            })
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("前往设置关闭电池优化")
                    }
                }
            }

            // 服务控制
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Notifications, null)
                        Spacer(Modifier.width(8.dp))
                        Text("截图监控", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("监听新截图并发送通知", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { ScreenshotMonitorService.start(context) }) { Text("启动") }
                        OutlinedButton(onClick = { ScreenshotMonitorService.stop(context) }) { Text("停止") }
                    }
                }
            }

            // 测试通知
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.BugReport, null)
                        Spacer(Modifier.width(8.dp))
                        Text("调试", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("手动触发截图通知以测试功能", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        // 手动触发截图通知测试
                        val intent = android.content.Intent(context, com.workalbum.app.service.ScreenshotMonitorService::class.java)
                        intent.action = "com.workalbum.app.TEST_SCREENSHOT"
                        context.startService(intent)
                        android.widget.Toast.makeText(context, "已发送测试通知，请查看通知栏", android.widget.Toast.LENGTH_SHORT).show()
                    }) { Text("发送测试通知") }
                }
            }

            // 使用说明
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, null)
                        Spacer(Modifier.width(8.dp))
                        Text("使用说明", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("保存图片到工作相册：", fontWeight = FontWeight.Bold)
                    Text("在任何APP中长按图片 -> 点[分享]或[...] -> 在列表中找到[工作相册]")
                    Spacer(Modifier.height(8.dp))
                    Text("微信特别注意：", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Text("微信的[保存图片]按钮直接存系统相册，无法拦截。请用[分享]功能而非[保存]。")
                    Spacer(Modifier.height(8.dp))
                    Text("截图处理：", fontWeight = FontWeight.Bold)
                    Text("截图后通知栏会弹出提醒，点击[移入工作相册]即可。")
                }
            }
        }
    }
}