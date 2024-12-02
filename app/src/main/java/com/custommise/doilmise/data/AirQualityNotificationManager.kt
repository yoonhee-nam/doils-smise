package com.custommise.doilmise.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.custommise.doilmise.R

class AirQualityNotificationManager(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "air_quality_alert"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "대기질 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "대기질이 나쁨 이상일 때 알림을 보냅니다"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showAirQualityAlert(grade: Grade, location: String) {
        val title = "대기질 경보"
        val message = when (grade) {
            Grade.BAD -> "현재 $location 의 대기질이 나쁨 수준입니다."
            Grade.VERY_BAD -> "현재 $location 의 대기질이 매우 나쁨 수준입니다."
            Grade.EXTREMELY_BAD -> "현재 $location 의 대기질이 극도로 나쁨 수준입니다."
            Grade.WORST -> "현재 $location 의 대기질이 최악 수준입니다."
            else -> return
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.normal)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(1, notification.build())
    }
}