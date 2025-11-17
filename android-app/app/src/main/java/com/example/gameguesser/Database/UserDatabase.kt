package com.example.gameguesser.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gameguesser.Class.LocalUser
import com.example.gameguesser.Class.User
import com.example.gameguesser.DAOs.LocalUserDao
import com.example.gameguesser.DAOs.UserDao

@Database(entities = [User::class, LocalUser::class], version = 2, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun localUserDao(): LocalUserDao

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null

        fun getDatabase(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {

                // Migration from version 1 -> 2
                val MIGRATION_1_2 = object : Migration(1, 2) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("""
                            CREATE TABLE IF NOT EXISTS local_users (
                                email TEXT NOT NULL PRIMARY KEY,
                                userName TEXT NOT NULL,
                                passwordHash TEXT NOT NULL,
                                streak INTEGER NOT NULL DEFAULT 0
                            )
                        """.trimIndent())
                    }
                }

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}