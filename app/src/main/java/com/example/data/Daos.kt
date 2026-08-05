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

    @Query("SELECT * FROM scheduled_subscriptions WHERE subscriptionTypeId = :subTypeId")
    suspend fun getScheduledSubscriptionsForType(subTypeId: Int): List<ScheduledSubscription>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(scheduledSubscription: ScheduledSubscription): Long

    @Update
    suspend fun update(scheduledSubscription: ScheduledSubscription)

    @Delete
    suspend fun delete(scheduledSubscription: ScheduledSubscription)

    @Query("SELECT * FROM scheduled_subscriptions WHERE id = :id LIMIT 1")
    suspend fun getScheduledSubscriptionById(id: Int): ScheduledSubscription?

    @Query("DELETE FROM scheduled_subscriptions WHERE subscriptionTypeId = :subTypeId AND dueDate > :date")
    suspend fun deleteScheduledSubsAfterDate(subTypeId: Int, date: Long)
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

@Dao
interface SubscriptionSkipDao {
    @Query("SELECT * FROM subscription_skips ORDER BY id ASC")
    fun getAllSubscriptionSkips(): Flow<List<SubscriptionSkip>>

    @Query("SELECT * FROM subscription_skips WHERE subscriptionTypeId = :subTypeId LIMIT 1")
    suspend fun getSkipForSubscriptionType(subTypeId: Int): SubscriptionSkip?

    @Query("SELECT * FROM subscription_skips WHERE subscriptionTypeId = :subTypeId ORDER BY id ASC")
    suspend fun getSkipsForSubscriptionType(subTypeId: Int): List<SubscriptionSkip>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscriptionSkip: SubscriptionSkip): Long

    @Update
    suspend fun update(subscriptionSkip: SubscriptionSkip)

    @Delete
    suspend fun delete(subscriptionSkip: SubscriptionSkip)

    @Query("DELETE FROM subscription_skips WHERE subscriptionTypeId = :subTypeId")
    suspend fun deleteBySubscriptionTypeId(subTypeId: Int)
}
