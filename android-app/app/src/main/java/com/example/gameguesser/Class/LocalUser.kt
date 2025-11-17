package com.example.gameguesser.Class

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_users")
data class LocalUser(
    @PrimaryKey val email: String,
    val userName: String,
    val passwordHash: String,
    val streak: Int = 0
)
