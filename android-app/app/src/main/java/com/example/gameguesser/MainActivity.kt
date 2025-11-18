package com.example.gameguesser

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.gameguesser.Database.AppDatabase
import com.example.gameguesser.data.RetrofitClient
import com.example.gameguesser.databinding.ActivityMainBinding
import com.example.gameguesser.repository.GameRepository
import com.example.gameguesser.ui.notifications.NotificationScheduler
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import android.app.AlarmManager
import androidx.lifecycle.lifecycleScope


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "daily_reminder_channel",
                "Daily Reminder Notifications",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Sends reminders to play the game to keep your streak"

            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ScheduleExactAlarm", "MissingPermission")
    private fun scheduleNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return
            }
        }

        NotificationScheduler.scheduleTestNotification(this)
        NotificationScheduler.scheduleDailyReminders(this)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createNotificationChannel()

        // android 13 and up
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            } else {
                scheduleNotifications()
            }
        } else {
            scheduleNotifications()
        }

        // --- Start background sync ---
        val dao = AppDatabase.getDatabase(this).gameDao()
        val repository = GameRepository(dao, RetrofitClient.api, this)

        CoroutineScope(Dispatchers.IO).launch {
            repository.syncFromApi()
        }



        // --- ViewBinding ---
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // --- Navigation setup ---
        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_encyclopedia,
                R.id.navigation_chatbot,
                R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // --- WindowInsets for navView and navHost ---
        ViewCompat.setOnApplyWindowInsetsListener(navView) { v, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.updatePadding(bottom = navBarInset)
            insets
        }
        val navHost: View = findViewById(R.id.nav_host_fragment_activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(navHost) { v, insets ->
            val navBarInset = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            v.updatePadding(bottom = navBarInset)
            insets
        }
    }

    // permsison for notif
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            scheduleNotifications()
        }
    }
}
