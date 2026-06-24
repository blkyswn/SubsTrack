package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = 1 LIMIT 1")
    fun getUser(): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(user: User)
}

@Dao
interface BookstoreDao {
    @Query("SELECT * FROM bookstores ORDER BY name ASC")
    fun getAllBookstores(): Flow<List<Bookstore>>

    @Query("SELECT * FROM bookstores WHERE id = :id LIMIT 1")
    suspend fun getBookstoreById(id: Int): Bookstore?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookstore: Bookstore): Long

    @Update
    suspend fun update(bookstore: Bookstore)

    @Delete
    suspend fun delete(bookstore: Bookstore)
}

@Dao
interface SubscriptionTypeDao {
    @Query("SELECT * FROM subscription_types ORDER BY title ASC")
    fun getAllSubscriptionTypes(): Flow<List<SubscriptionType>>

    @Query("SELECT * FROM subscription_types WHERE id = :id LIMIT 1")
    suspend fun getSubscriptionTypeById(id: Int): SubscriptionType?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscriptionType: SubscriptionType): Long

    @Update
    suspend fun update(subscriptionType: SubscriptionType)

    @Delete
    suspend fun delete(subscriptionType: SubscriptionType)
}

@Dao
interface ScheduledSubscriptionDao {
    @Query("SELECT * FROM scheduled_subscriptions ORDER BY dueDate ASC")
    fun getAllScheduledSubscriptions(): Flow<List<ScheduledSubscription>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scheduledSubscription: ScheduledSubscription): Long

    @Update
    suspend fun update(scheduledSubscription: ScheduledSubscription)

    @Delete
    suspend fun delete(scheduledSubscription: ScheduledSubscription)
}

@Dao
interface PreorderDao {
    @Query("SELECT * FROM preorders ORDER BY rangedSaleDateStart ASC")
    fun getAllPreorders(): Flow<List<Preorder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preorder: Preorder): Long

    @Update
    suspend fun update(preorder: Preorder)

    @Delete
    suspend fun delete(preorder: Preorder)
}
