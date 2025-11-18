package com.example.gameguesser.Database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.gameguesser.Class.User
import com.example.gameguesser.DAOs.LocalUserDao
import com.example.gameguesser.Class.LocalUser
import com.example.gameguesser.DAOs.UserDao

@Database(entities = [User::class, LocalUser::class], version = 2, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun localUserDao(): LocalUserDao

    companion object {
        /**
         * Migration from version 1 to 2: Adds the `lastPlayedCG` column to the user_table.
         */
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add the new column to the existing user_table
                db.execSQL("ALTER TABLE user_table ADD COLUMN lastPlayedCG INTEGER NOT NULL DEFAULT 0")

                // 2. Create the new table for LocalUser
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS local_users (
                        email TEXT NOT NULL PRIMARY KEY,
                        userName TEXT NOT NULL,
                        passwordHash TEXT NOT NULL,
                        streak INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        @Volatile
        private var INSTANCE: UserDatabase? = null

        fun getDatabase(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_database"
                )
                    // Tell Room to use the single, consolidated migration
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
