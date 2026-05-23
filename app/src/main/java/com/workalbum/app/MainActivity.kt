package com.workalbum.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.workalbum.app.model.WorkImage
import com.workalbum.app.service.ScreenshotMonitorService
import com.workalbum.app.ui.screens.GalleryScreen
import com.workalbum.app.ui.screens.ImageDetailScreen
import com.workalbum.app.ui.screens.SettingsScreen
import com.workalbum.app.ui.theme.WorkAlbumTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val repository by lazy {
        (application as WorkAlbumApplication).imageRepository
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            startScreenshotMonitor()
        } else {
            Toast.makeText(this, "通知权限被拒绝，将无法收到截图提醒", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WorkAlbumTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "gallery") {
                    composable("gallery") {
                        GalleryScreen(
                            repository = repository,
                            onImageClick = { image -> navController.navigate("detail/${image.id}") },
                            onSettingsClick = { navController.navigate("settings") }
                        )
                    }
                    composable("detail/{imageId}") { backStackEntry ->
                        val imageId = backStackEntry.arguments?.getString("imageId")?.toLongOrNull()
                        val image = imageId?.let {
                            var result by remember { mutableStateOf<WorkImage?>(null) }
                            LaunchedEffect(imageId) {
                                repository.getAllImages().collect { images ->
                                    result = images.find { img -> img.id == imageId }
                                }
                            }
                            result
                        }
                        if (image != null) {
                            ImageDetailScreen(image = image, repository = repository, onBack = { navController.popBackStack() })
                        }
                    }
                    composable("settings") {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }

        requestPermissionsAndStartMonitor()
        handleScreenshotIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleScreenshotIntent(intent)
    }

    private fun handleScreenshotIntent(intent: Intent?) {
        if (intent?.action == ScreenshotMonitorService.ACTION_MOVE_SCREENSHOT) {
            val uriString = intent.getStringExtra(ScreenshotMonitorService.EXTRA_IMAGE_URI)
            if (uriString != null) {
                lifecycleScope.launch {
                    try {
                        repository.saveSharedImage(Uri.parse(uriString), "screenshot")
                        Toast.makeText(this@MainActivity, "截图已移入工作相册", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "移入失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            } else if (intent?.action == ScreenshotMonitorService.ACTION_IGNORE) {
            val nm = getSystemService(android.app.NotificationManager::class.java)
            nm.cancel(ScreenshotMonitorService.NOTIFICATION_ID_SCREENSHOT)
        }
    }

    private fun requestPermissionsAndStartMonitor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            val mediaGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            if (!notificationGranted || !mediaGranted) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS, Manifest.permission.READ_MEDIA_IMAGES))
            } else {
                startScreenshotMonitor()
            }
        } else {
            startScreenshotMonitor()
        }
    }

    private fun startScreenshotMonitor() {
        ScreenshotMonitorService.start(this)
    }
}