package com.workalbum.app.receiver

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.workalbum.app.WorkAlbumApplication
import com.workalbum.app.ui.theme.WorkAlbumTheme
import kotlinx.coroutines.launch

class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取分享的图片
        val imageUris = when {
            intent.action == Intent.ACTION_SEND_MULTIPLE ->
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)
            intent.action == Intent.ACTION_SEND ->
                intent.getParcelableExtra(Intent.EXTRA_STREAM, android.net.Uri::class.java)?.let { listOf(it) }
            else -> null
        }

        if (imageUris.isNullOrEmpty()) {
            Toast.makeText(this, "\u672a\u63a5\u6536\u5230\u56fe\u7247", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val callingPackage = intent.`package` ?: ""
        val sourceApp = when {
            callingPackage.contains("tencent.mm") -> "wechat"
            callingPackage.contains("tencent.mobileqq") -> "qq"
            callingPackage.contains("com.alibaba.android.rimet") -> "dingtalk"
            callingPackage.contains("chrome") || callingPackage.contains("mozilla") -> "browser"
            else -> "other"
        }

        val sourceLabel = when (sourceApp) {
            "wechat" -> "\u5fae\u4fe1"
            "qq" -> "QQ"
            "dingtalk" -> "\u9489\u9489"
            "browser" -> "\u6d4f\u89c8\u5668"
            else -> sourceApp
        }

        setContent {
            WorkAlbumTheme {
                var showDialog by remember { mutableStateOf(true) }

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { /* 不做任何事 —— 禁止点外部关闭 */ },
                        title = { Text("\u4fdd\u5b58\u5230\u5de5\u4f5c\u76f8\u518c") },
                        text = {
                            Text(
                                "\u6765\u6e90\uff1a$sourceLabel\n\u5c06\u4fdd\u5b58 ${imageUris.size} \u5f20\u56fe\u7247\n\u4e0d\u4f1a\u51fa\u73b0\u5728\u7cfb\u7edf\u76f8\u518c\u4e2d"
                            )
                        },
                        confirmButton = {
                            Button(onClick = {
                                showDialog = false
                                saveImagesAndFinish(imageUris, sourceApp)
                            }) {
                                Text("\u4fdd\u5b58")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                finish()
                            }) {
                                Text("\u53d6\u6d88")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun saveImagesAndFinish(uris: List<android.net.Uri>, source: String) {
        val repo = (application as WorkAlbumApplication).imageRepository
        lifecycleScope.launch {
            var ok = 0
            for (uri in uris) {
                try { repo.saveSharedImage(uri, source); ok++ }
                catch (_: Exception) {}
            }
            Toast.makeText(this@ShareReceiverActivity, "\u5df2\u4fdd\u5b58 $ok \u5f20", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}