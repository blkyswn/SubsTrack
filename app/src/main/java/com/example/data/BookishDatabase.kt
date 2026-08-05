package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        User::class,
        Bookstore::class,
        SubscriptionType::class,
        ScheduledSubscription::class,
        Preorder::class,
        SubscriptionSkip::class
    ],
    version = 10,
    exportSchema = false
)
abstract class BookishDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun bookstoreDao(): BookstoreDao
    abstract fun subscriptionTypeDao(): SubscriptionTypeDao
    abstract fun scheduledSubscriptionDao(): ScheduledSubscriptionDao
    abstract fun preorderDao(): PreorderDao
    abstract fun subscriptionSkipDao(): SubscriptionSkipDao

    companion object {
        @Volatile
        private var INSTANCE: BookishDatabase? = null

        fun getDatabase(context: Context): BookishDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookishDatabase::class.java,
                    "bookish_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
