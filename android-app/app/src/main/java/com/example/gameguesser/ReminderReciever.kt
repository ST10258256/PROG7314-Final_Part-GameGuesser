package com.example.gameguesser

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val hour = intent.getIntExtra("reminder_hour", -1)

        val (title, text) = when(hour) {
            9 -> "The Deadman rises!" to "Don’t let The Undertaker’s streak outlast yours."
            12 -> "Midday Choke-Slam!" to "Keep your streak alive "
            18 -> "The Reapers Bell!" to "Overcome the bell, like the Beast Incarnate did, and Roman ig!"
            else -> "Don’t Pull an Undertaker!" to "Play a game and keep your streak alive!"
        }

        val notification = NotificationCompat.Builder(context, "daily_reminder_channel")
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationId = System.currentTimeMillis().toInt()
        NotificationManagerCompat .from(context).notify(notificationId, notification)
    }
}