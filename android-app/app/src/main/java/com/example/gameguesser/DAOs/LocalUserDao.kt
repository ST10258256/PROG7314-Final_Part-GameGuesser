package com.example.gameguesser.DAOs

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.gameguesser.Class.LocalUser

@Dao
interface LocalUserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun registerUser(user: LocalUser)

    @Query("SELECT * FROM local_users WHERE email = :email")
    suspend fun getUser(email: String): LocalUser?
}
