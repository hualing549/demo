package com.examshuffler

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class ExamShufflerApp : Application() {

    companion object {
        const val CHANNEL_FLOATING = "floating_service"
        const val CHANNEL_CAPTURE = "capture_results"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            NotificationChannel(
                CHANNEL_FLOATING,
                "悬浮图标服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用于保持悬浮图标在桌面上运行"
                setShowBadge(false)
                manager.createNotificationChannel(this)
            }

            NotificationChannel(
                CHANNEL_CAPTURE,
                "截屏结果",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "显示截屏识别的结果"
                manager.createNotificationChannel(this)
            }
        }
    }
}
