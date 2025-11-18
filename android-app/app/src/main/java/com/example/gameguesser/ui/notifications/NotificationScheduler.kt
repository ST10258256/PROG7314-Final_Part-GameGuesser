package com.example.gameguesser.ui.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat.getSystemService
import com.example.gameguesser.ReminderReceiver
import java.util.*

object NotificationScheduler {

    fun scheduleDailyReminders(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val reminderTimes = listOf(
            Pair(9, 0),
            Pair(12, 0),
            Pair(18, 0)
        )

        for ((hour, minute) in reminderTimes) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (before(Calendar.getInstance())) add(Calendar.DAY_OF_MONTH, 1)
            }

            val intent = Intent(context, ReminderReceiver::class.java).apply{
                putExtra("reminder_hour", hour)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                hour * 100 + minute, // unique requestCode per time
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    @RequiresPermission(android.Manifest.permission.SCHEDULE_EXACT_ALARM)
    fun scheduleTestNotification(context: Context, delayMillis: Long = 10_000L) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + delayMillis

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            triggerTime,
            pendingIntent
        )
    }
}
