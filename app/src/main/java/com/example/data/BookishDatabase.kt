package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        User::class,
        Bookstore::class,
        BookstoreContact::class,
        ForwardingService::class,
        ForwardingServiceContact::class,
        ShippingCompany::class,
        ShippingCompanyContact::class,
        UserAddress::class,
        SubscriptionType::class,
        SubscriptionSkipMethod::class,
        ScheduledSubscription::class,
        Preorder::class,
        SubscriptionSkip::class,
        PackageItem::class
    ],
    version = 26,
    exportSchema = false
)
abstract class BookishDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun bookstoreDao(): BookstoreDao
    abstract fun bookstoreContactDao(): BookstoreContactDao
    abstract fun forwardingServiceDao(): ForwardingServiceDao
    abstract fun forwardingServiceContactDao(): ForwardingServiceContactDao
    abstract fun shippingCompanyDao(): ShippingCompanyDao
    abstract fun shippingCompanyContactDao(): ShippingCompanyContactDao
    abstract fun userAddressDao(): UserAddressDao
    abstract fun subscriptionTypeDao(): SubscriptionTypeDao
    abstract fun subscriptionSkipMethodDao(): SubscriptionSkipMethodDao
    abstract fun scheduledSubscriptionDao(): ScheduledSubscriptionDao
    abstract fun preorderDao(): PreorderDao
    abstract fun subscriptionSkipDao(): SubscriptionSkipDao
    abstract fun packageDao(): PackageDao

    companion object {
        @Volatile
        private var INSTANCE: BookishDatabase? = null

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bookstore_contacts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookstoreId` INTEGER NOT NULL,
                        `contactType` TEXT NOT NULL,
                        `contactValue` TEXT NOT NULL,
                        FOREIGN KEY(`bookstoreId`) REFERENCES `bookstores`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subscription_skip_methods` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `subscriptionTypeId` INTEGER NOT NULL,
                        `skipMethodOrder` INTEGER NOT NULL,
                        `skipMethodType` TEXT NOT NULL,
                        `skipMethodValue` TEXT NOT NULL,
                        `skipMethodText` TEXT NOT NULL,
                        `consecutiveSkips` INTEGER NOT NULL,
                        FOREIGN KEY(`subscriptionTypeId`) REFERENCES `subscription_types`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `users` ADD COLUMN `country` TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `forwarding_services` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `website` TEXT NOT NULL,
                        `profilePic` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `forwarding_service_contacts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `forwardingServiceId` INTEGER NOT NULL,
                        `contactType` TEXT NOT NULL,
                        `contactValue` TEXT NOT NULL,
                        FOREIGN KEY(`forwardingServiceId`) REFERENCES `forwarding_services`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_addresses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` INTEGER NOT NULL,
                        `label` TEXT NOT NULL,
                        `recipientName` TEXT NOT NULL,
                        `streetAddress1` TEXT NOT NULL,
                        `streetAddress2` TEXT NOT NULL,
                        `city` TEXT NOT NULL,
                        `stateProvinceRegion` TEXT NOT NULL,
                        `postalCode` TEXT NOT NULL,
                        `country` TEXT NOT NULL,
                        `phoneNumber` TEXT NOT NULL,
                        `forwardingServiceId` INTEGER,
                        `isDefault` INTEGER NOT NULL,
                        FOREIGN KEY(`forwardingServiceId`) REFERENCES `forwarding_services`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_addresses_forwardingServiceId` ON `user_addresses` (`forwardingServiceId`)")
            }
        }

        val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `user_addresses`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_addresses` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `userId` INTEGER NOT NULL,
                        `streetAddress1` TEXT NOT NULL,
                        `streetAddress2` TEXT NOT NULL,
                        `city` TEXT NOT NULL,
                        `stateProvinceRegion` TEXT NOT NULL,
                        `postalCode` TEXT NOT NULL,
                        `country` TEXT NOT NULL,
                        `forwardingServiceId` INTEGER,
                        `isDefault` INTEGER NOT NULL,
                        FOREIGN KEY(`forwardingServiceId`) REFERENCES `forwarding_services`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_user_addresses_forwardingServiceId` ON `user_addresses` (`forwardingServiceId`)")
            }
        }

        val MIGRATION_19_20 = object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate subscription_types with new columns and foreign key to user_addresses
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subscription_types_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookstoreId` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `price` REAL NOT NULL,
                        `dueDate` INTEGER NOT NULL,
                        `startDate` INTEGER NOT NULL,
                        `finishDate` INTEGER NOT NULL,
                        `notificationAlertDays` INTEGER,
                        `frequency` TEXT NOT NULL,
                        `reminderEnabled` INTEGER NOT NULL,
                        `reminderDDayOffset` INTEGER NOT NULL,
                        `reminderHour` INTEGER NOT NULL,
                        `reminderMinute` INTEGER NOT NULL,
                        `skipType` TEXT,
                        `numberOfSkips` INTEGER,
                        `numberOfMonths` INTEGER,
                        `skipMethod` TEXT,
                        `skipLink` TEXT,
                        `skipText` TEXT,
                        `picturePath` TEXT,
                        `shippingAddressId` INTEGER,
                        `currency` TEXT,
                        `basePrice` REAL,
                        `shippingPrice` REAL,
                        `taxPrice` REAL,
                        `forwardShippingPrice` REAL,
                        `forwardTaxPrice` REAL,
                        FOREIGN KEY(`bookstoreId`) REFERENCES `bookstores`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`shippingAddressId`) REFERENCES `user_addresses`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `subscription_types_new` (
                        `id`, `bookstoreId`, `title`, `status`, `price`, `dueDate`, `startDate`, `finishDate`,
                        `notificationAlertDays`, `frequency`, `reminderEnabled`, `reminderDDayOffset`,
                        `reminderHour`, `reminderMinute`, `skipType`, `numberOfSkips`, `numberOfMonths`,
                        `skipMethod`, `skipLink`, `skipText`, `picturePath`
                    )
                    SELECT `id`, `bookstoreId`, `title`, `status`, `price`, `dueDate`, `startDate`, `finishDate`,
                           `notificationAlertDays`, `frequency`, `reminderEnabled`, `reminderDDayOffset`,
                           `reminderHour`, `reminderMinute`, `skipType`, `numberOfSkips`, `numberOfMonths`,
                           `skipMethod`, `skipLink`, `skipText`, `picturePath`
                    FROM `subscription_types`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `subscription_types`")
                db.execSQL("ALTER TABLE `subscription_types_new` RENAME TO `subscription_types`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscription_types_bookstoreId` ON `subscription_types` (`bookstoreId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscription_types_shippingAddressId` ON `subscription_types` (`shippingAddressId`)")

                // Recreate preorders with new columns and foreign key to user_addresses
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `preorders_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `bookstoreId` INTEGER NOT NULL,
                        `picturePath` TEXT,
                        `bookTitle` TEXT NOT NULL,
                        `bookAuthor` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `price` REAL NOT NULL,
                        `rangedSaleDateStart` INTEGER NOT NULL,
                        `rangedSaleDateEnd` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `reminderEnabled` INTEGER NOT NULL,
                        `reminderDDayOffset` INTEGER NOT NULL,
                        `reminderHour` INTEGER NOT NULL,
                        `reminderMinute` INTEGER NOT NULL,
                        `rating` REAL NOT NULL,
                        `shippingAddressId` INTEGER,
                        `currency` TEXT,
                        `basePrice` REAL,
                        `shippingPrice` REAL,
                        `taxPrice` REAL,
                        `forwardShippingPrice` REAL,
                        `forwardTaxPrice` REAL,
                        FOREIGN KEY(`bookstoreId`) REFERENCES `bookstores`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`shippingAddressId`) REFERENCES `user_addresses`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `preorders_new` (
                        `id`, `bookstoreId`, `picturePath`, `bookTitle`, `bookAuthor`, `description`,
                        `price`, `rangedSaleDateStart`, `rangedSaleDateEnd`, `status`,
                        `reminderEnabled`, `reminderDDayOffset`, `reminderHour`, `reminderMinute`, `rating`
                    )
                    SELECT `id`, `bookstoreId`, `picturePath`, `bookTitle`, `bookAuthor`, `description`,
                           `price`, `rangedSaleDateStart`, `rangedSaleDateEnd`, `status`,
                           `reminderEnabled`, `reminderDDayOffset`, `reminderHour`, `reminderMinute`, `rating`
                    FROM `preorders`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `preorders`")
                db.execSQL("ALTER TABLE `preorders_new` RENAME TO `preorders`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_preorders_bookstoreId` ON `preorders` (`bookstoreId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_preorders_shippingAddressId` ON `preorders` (`shippingAddressId`)")
            }
        }

        val MIGRATION_20_21 = object : Migration(20, 21) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `subscription_types` ADD COLUMN `discountedAmount` REAL")
                db.execSQL("ALTER TABLE `preorders` ADD COLUMN `discountedAmount` REAL")
            }
        }

        val MIGRATION_21_22 = object : Migration(21, 22) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `forwarding_services` ADD COLUMN `storageDays` INTEGER")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shipping_companies` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `website` TEXT NOT NULL,
                        `profilePic` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shipping_company_contacts` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `shippingCompanyId` INTEGER NOT NULL,
                        `contactType` TEXT NOT NULL,
                        `contactValue` TEXT NOT NULL,
                        FOREIGN KEY(`shippingCompanyId`) REFERENCES `shipping_companies`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_22_23 = object : Migration(22, 23) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `packages` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `originTable` TEXT NOT NULL,
                        `originId` INTEGER NOT NULL,
                        `storeShippingDate` INTEGER,
                        `storeShippingCompany` TEXT,
                        `storeTrackingNumber` TEXT,
                        `forwarderReceivedDate` INTEGER,
                        `forwarderShippingCompany` TEXT,
                        `forwarderTrackingNumber` TEXT,
                        `forwarderShippedDate` INTEGER,
                        `receivedDate` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_packages_originTable_originId` ON `packages` (`originTable`, `originId`)")
            }
        }

        val MIGRATION_23_24 = object : Migration(23, 24) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `packages` ADD COLUMN `forwarderShippingCompany` TEXT")
                db.execSQL("ALTER TABLE `packages` ADD COLUMN `forwarderTrackingNumber` TEXT")
            }
        }

        val MIGRATION_24_25 = object : Migration(24, 25) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `packages` ADD COLUMN `purchaseDate` INTEGER")
            }
        }

        val MIGRATION_25_26 = object : Migration(25, 26) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `forwarding_services` ADD COLUMN `reminderEnabled` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `forwarding_services` ADD COLUMN `reminderDDayOffset` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `forwarding_services` ADD COLUMN `reminderHour` INTEGER NOT NULL DEFAULT 8")
                db.execSQL("ALTER TABLE `forwarding_services` ADD COLUMN `reminderMinute` INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): BookishDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookishDatabase::class.java,
                    "bookish_database"
                )
                .addMigrations(MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        db.execSQL("PRAGMA foreign_keys=ON;")
                    }
                })
                .fallbackToDestructiveMigration(dropAllTables = true)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
