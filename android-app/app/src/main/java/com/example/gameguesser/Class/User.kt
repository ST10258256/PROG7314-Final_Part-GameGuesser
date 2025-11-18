package com.example.gameguesser.Class

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: String, // Google user ID
    var userName: String,           // Display name
    // streak for keyword game
    var streakKW: Int = 0,             // Initial streak
    //streak for compare game
    var streakCG: Int = 0,             // Initial streak
    // best streak for Keyword game
    var bestStreakKW: Int = 0,             // Initial streak
    // best streak for Compare game
    var bestStreakCG: Int = 0,             // Initial
    // Last played date for Compare Game streak
    var lastPlayedCG: Long = 0L,
    // Last played date for Compare Game streak
    var lastPlayedKW: Long = 0L
)
