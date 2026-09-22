package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

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
    val displayAmounts: Boolean = false,
    val country: String = ""
)

@Entity(tableName = "bookstores")
data class Bookstore(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val website: String,
    val profilePic: String?
)

@Entity(
    tableName = "bookstore_contacts",
    foreignKeys = [
        ForeignKey(
            entity = Bookstore::class,
            parentColumns = ["id"],
            childColumns = ["bookstoreId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class BookstoreContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookstoreId: Int,
    val contactType: String, // "Website", "Email", "Phone", or custom
    val contactValue: String
)

@Entity(tableName = "forwarding_services")
data class ForwardingService(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val website: String,
    val profilePic: String?
)

@Entity(
    tableName = "forwarding_service_contacts",
    foreignKeys = [
        ForeignKey(
            entity = ForwardingService::class,
            parentColumns = ["id"],
            childColumns = ["forwardingServiceId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ForwardingServiceContact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val forwardingServiceId: Int,
    val contactType: String, // "Website", "Email", "Phone", or custom
    val contactValue: String
)

@JsonClass(generateAdapter = true)
@Entity(
    tableName = "user_addresses",
    foreignKeys = [
        ForeignKey(
            entity = ForwardingService::class,
            parentColumns = ["id"],
            childColumns = ["forwardingServiceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["forwardingServiceId"])
    ]
)
data class UserAddress(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int = 1,
    val streetAddress1: String,
    val streetAddress2: String = "",
    val city: String,
    val stateProvinceRegion: String,
    val postalCode: String,
    val country: String,
    val forwardingServiceId: Int? = null,
    val isDefault: Boolean = false
) {
    fun toFormattedString(): String = buildString {
        append(streetAddress1)
        if (streetAddress2.isNotBlank()) append(", ").append(streetAddress2)
        append(", ").append(city)
        if (stateProvinceRegion.isNotBlank()) append(", ").append(stateProvinceRegion)
        if (postalCode.isNotBlank()) append(" ").append(postalCode)
        if (country.isNotBlank()) append(", ").append(country)
    }

    fun toShortDisplay(): String = buildString {
        append(streetAddress1)
        if (city.isNotBlank()) append(", ").append(city)
    }
}

/**
 * Serializer helper for the addresses table (UserAddress).
 */
object UserAddressSerializer {
    private val moshi by lazy {
        com.squareup.moshi.Moshi.Builder()
            .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
    }
    private val adapter by lazy { moshi.adapter(UserAddress::class.java) }

    fun toJson(address: UserAddress): String = adapter.toJson(address)
    fun fromJson(json: String): UserAddress? = try {
        adapter.fromJson(json)
    } catch (e: Exception) {
        null
    }

    fun toListJson(addresses: List<UserAddress>): String {
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, UserAddress::class.java)
        val listAdapter: com.squareup.moshi.JsonAdapter<List<UserAddress>> = moshi.adapter(type)
        return listAdapter.toJson(addresses)
    }

    fun fromListJson(json: String): List<UserAddress> = try {
        val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, UserAddress::class.java)
        val listAdapter: com.squareup.moshi.JsonAdapter<List<UserAddress>> = moshi.adapter(type)
        listAdapter.fromJson(json) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
}

@Entity(
    tableName = "subscription_types",
    foreignKeys = [
        ForeignKey(
            entity = Bookstore::class,
            parentColumns = ["id"],
            childColumns = ["bookstoreId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserAddress::class,
            parentColumns = ["id"],
            childColumns = ["shippingAddressId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["bookstoreId"]),
        Index(value = ["shippingAddressId"])
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
    val numberOfMonths: Int? = null,
    val skipMethod: String? = null, // e.g. "Website", "Email", "App", "Customer Support", etc.
    val skipLink: String? = null,
    val skipText: String? = null,
    val picturePath: String? = null,
    val shippingAddressId: Int? = null,
    val currency: String? = null,
    val basePrice: Double? = null,
    val shippingPrice: Double? = null,
    val taxPrice: Double? = null,
    val forwardShippingPrice: Double? = null,
    val forwardTaxPrice: Double? = null
)

@Entity(
    tableName = "subscription_skip_methods",
    foreignKeys = [
        ForeignKey(
            entity = SubscriptionType::class,
            parentColumns = ["id"],
            childColumns = ["subscriptionTypeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SubscriptionSkipMethod(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subscriptionTypeId: Int,
    val skipMethodOrder: Int = 1,
    val skipMethodType: String, // "Website", "Email", "Phone", or custom label
    val skipMethodValue: String, // Link URL, email address, phone number, etc.
    val skipMethodText: String = "", // Message text/template for Email, Phone, Custom
    val consecutiveSkips: Int = 0 // default 0
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
        ),
        ForeignKey(
            entity = UserAddress::class,
            parentColumns = ["id"],
            childColumns = ["shippingAddressId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["bookstoreId"]),
        Index(value = ["shippingAddressId"])
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
    val rating: Double = 0.0,
    val shippingAddressId: Int? = null,
    val currency: String? = null,
    val basePrice: Double? = null,
    val shippingPrice: Double? = null,
    val taxPrice: Double? = null,
    val forwardShippingPrice: Double? = null,
    val forwardTaxPrice: Double? = null
)
