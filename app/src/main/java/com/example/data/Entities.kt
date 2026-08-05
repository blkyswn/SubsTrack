package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val id: Int = 1, // Only single user locally
    val username: String,
    val profilePic: String?,
    val currency: String,
    val language: String,
    val defaultScheduledSubCount: Int = 6,
    val themeMode: String = "system",
    val themeCombo: String = "default",
    val dateFormat: String = "yyyy-MM-dd",
    val displayAmounts: Boolean = false
)

@Entity(tableName = "bookstores")
data class Bookstore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val url: String,
    val profilePic: String?
)

@Entity(
    tableName = "subscription_types",
    foreignKeys = [
        ForeignKey(
            entity = Bookstore::class,
            parentColumns = ["id"],
            childColumns = ["bookstoreId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SubscriptionType(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookstoreId: Int,
    val title: String,
    val status: String, // Active, Waitlist, Paused, Canceled
    val price: Double,
    val dueDate: Long, // timestamp
    val startDate: Long, // timestamp
    val finishDate: Long, // timestamp
    val notificationAlertDays: Int?, // optional personalized notification alert days before due date
    val frequency: String, // e.g., Weekly, Monthly, Bi-Monthly, Quarterly, Yearly
    val reminderEnabled: Boolean = false,
    val reminderDDayOffset: Int = 0, // 0 to 5 (D-Day, D-1, ..., D-5)
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val skipType: String? = "None", // "None", "Each calendar year", "Every certain months", "Unlimited"
    val numberOfSkips: Int? = null,
    val numberOfMonths: Int? = null
)

@Entity(
    tableName = "subscription_skips",
    foreignKeys = [
        ForeignKey(
            entity = SubscriptionType::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SubscriptionSkip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subscriptionTypeId: Int,
    val subscriptionSkipType: String, // "Each calendar year", "Every certain months", "Unlimited"
    val numberOfSkips: Int? = null,
    val skipsLeft: Int? = null,
    val skipStartDate: Long? = null,
    val skipEndDate: Long? = null
)

@Entity(
    tableName = "scheduled_subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = SubscriptionType::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ScheduledSubscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subscriptionTypeId: Int,
    val bookTitle: String,
    val bookAuthor: String,
    val description: String,
    val dueDate: Long, // timestamp
    val status: String, // Upcoming, Skipped, Renewed, Shipped, Received
    val isSkipped: Boolean = false,
    val picturePath: String? = null,
    val rating: Double = 0.0
)

@Entity(
    tableName = "preorders",
    foreignKeys = [
        ForeignKey(
            entity = Bookstore::class,
            parentColumns = ["id"],
            childColumns = ["bookstoreId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Preorder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookstoreId: Int,
    val picturePath: String?,
    val bookTitle: String,
    val bookAuthor: String,
    val description: String,
    val price: Double,
    val rangedSaleDateStart: Long, // timestamp
    val rangedSaleDateEnd: Long, // timestamp
    val status: String, // Upcoming, Released, Preordered, Shipped, Received
    val reminderEnabled: Boolean = false,
    val reminderDDayOffset: Int = 0, // 0 to 5 (D-Day, D-1, ..., D-5)
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val rating: Double = 0.0
)
