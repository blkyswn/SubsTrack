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
    val language: String
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
    val frequency: String // e.g., Weekly, Monthly, Bi-Monthly, Quarterly, Yearly
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
    val status: String, // Upcoming, Paid, Shipped, Received
    val isSkipped: Boolean = false
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
    val status: String // Upcoming, Released, Preordered, Shipped, Received
)
