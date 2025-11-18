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

// 1. Update version to 3
@Database(entities = [User::class, LocalUser::class], version = 3, exportSchema = false)
abstract class UserDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun localUserDao(): LocalUserDao

    companion object {
        private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // This migration uses "user_table" which seems to be from a previous version of your entity.
                // If your current entity is @Entity(tableName = "users"), this might need correction in a real scenario.
                // For now, leaving as is based on previous context.
                // **Correction based on new context**: The table for User is `users`.
                db.execSQL("ALTER TABLE users ADD COLUMN lastPlayedCG INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN lastPlayedKW INTEGER NOT NULL DEFAULT 0")

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

        // 2. Define the migration from version 2 to 3
        private val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // The User entity uses the table name "users"
                db.execSQL("ALTER TABLE users ADD COLUMN consecStreakCG INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE users ADD COLUMN consecStreakKW INTEGER NOT NULL DEFAULT 0")
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
                    // 3. Add the new migration to the builder
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
