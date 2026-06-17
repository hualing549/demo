package com.examshuffler

import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.examshuffler.service.FloatingIconService
import com.examshuffler.ui.MainScreen
import com.examshuffler.ui.PracticeScreen
import com.examshuffler.ui.SettingsScreen
import com.examshuffler.viewmodel.QuestionViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: QuestionViewModel by viewModel()

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val intent = Intent(this, FloatingIconService::class.java).apply {
                putExtra("RESULT_CODE", result.resultCode)
                putExtra("RESULT_DATA", result.data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Toast.makeText(this, "悬浮图标已启动", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "需要截屏权限才能使用", Toast.LENGTH_LONG).show()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            requestMediaProjection()
        } else {
            Toast.makeText(this, "需要悬浮窗权限", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onStartFloating = { checkAndStartFloating() },
                                onNavigateToPractice = { navController.navigate("practice") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("practice") {
                            PracticeScreen(
                                viewModel = viewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkAndStartFloating() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:" + packageName)
            )
            overlayPermissionLauncher.launch(intent)
            return
        }
        requestMediaProjection()
    }

    private fun requestMediaProjection() {
        val manager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(manager.createScreenCaptureIntent())
    }
}
