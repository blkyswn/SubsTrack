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

    @Query("SELECT * FROM bookstores")
    suspend fun getAllBookstoresList(): List<Bookstore>

    @Query("SELECT * FROM bookstores WHERE id = :id LIMIT 1")
    suspend fun getBookstoreById(id: Int): Bookstore?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookstore: Bookstore): Long

    @Update
    suspend fun update(bookstore: Bookstore)

    @Delete
    suspend fun delete(bookstore: Bookstore)

    @Query("DELETE FROM bookstores")
    suspend fun deleteAllBookstores()
}

@Dao
interface BookstoreContactDao {
    @Query("SELECT * FROM bookstore_contacts WHERE bookstoreId = :bookstoreId")
    fun getContactsForBookstore(bookstoreId: Int): Flow<List<BookstoreContact>>

    @Query("SELECT * FROM bookstore_contacts WHERE bookstoreId = :bookstoreId")
    suspend fun getContactsForBookstoreDirect(bookstoreId: Int): List<BookstoreContact>

    @Query("SELECT * FROM bookstore_contacts")
    fun getAllContacts(): Flow<List<BookstoreContact>>

    @Query("SELECT * FROM bookstore_contacts")
    suspend fun getAllContactsList(): List<BookstoreContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: BookstoreContact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<BookstoreContact>)

    @Update
    suspend fun update(contact: BookstoreContact)

    @Delete
    suspend fun delete(contact: BookstoreContact)

    @Query("DELETE FROM bookstore_contacts WHERE bookstoreId = :bookstoreId")
    suspend fun deleteContactsForBookstore(bookstoreId: Int)
}

@Dao
interface ForwardingServiceDao {
    @Query("SELECT * FROM forwarding_services ORDER BY name ASC")
    fun getAllForwardingServices(): Flow<List<ForwardingService>>

    @Query("SELECT * FROM forwarding_services")
    suspend fun getAllForwardingServicesList(): List<ForwardingService>

    @Query("SELECT * FROM forwarding_services WHERE id = :id LIMIT 1")
    suspend fun getForwardingServiceById(id: Int): ForwardingService?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(forwardingService: ForwardingService): Long

    @Update
    suspend fun update(forwardingService: ForwardingService)

    @Delete
    suspend fun delete(forwardingService: ForwardingService)

    @Query("DELETE FROM forwarding_services")
    suspend fun deleteAllForwardingServices()
}

@Dao
interface ForwardingServiceContactDao {
    @Query("SELECT * FROM forwarding_service_contacts WHERE forwardingServiceId = :forwardingServiceId")
    fun getContactsForForwardingService(forwardingServiceId: Int): Flow<List<ForwardingServiceContact>>

    @Query("SELECT * FROM forwarding_service_contacts WHERE forwardingServiceId = :forwardingServiceId")
    suspend fun getContactsForForwardingServiceDirect(forwardingServiceId: Int): List<ForwardingServiceContact>

    @Query("SELECT * FROM forwarding_service_contacts")
    fun getAllContacts(): Flow<List<ForwardingServiceContact>>

    @Query("SELECT * FROM forwarding_service_contacts")
    suspend fun getAllContactsList(): List<ForwardingServiceContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ForwardingServiceContact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ForwardingServiceContact>)

    @Update
    suspend fun update(contact: ForwardingServiceContact)

    @Delete
    suspend fun delete(contact: ForwardingServiceContact)

    @Query("DELETE FROM forwarding_service_contacts WHERE forwardingServiceId = :forwardingServiceId")
    suspend fun deleteContactsForForwardingService(forwardingServiceId: Int)
}

@Dao
interface SubscriptionTypeDao {
    @Query("SELECT * FROM subscription_types ORDER BY title ASC")
    fun getAllSubscriptionTypes(): Flow<List<SubscriptionType>>

    @Query("SELECT * FROM subscription_types")
    suspend fun getAllSubscriptionTypesList(): List<SubscriptionType>

    @Query("SELECT * FROM subscription_types WHERE id = :id LIMIT 1")
    suspend fun getSubscriptionTypeById(id: Int): SubscriptionType?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscriptionType: SubscriptionType): Long

    @Update
    suspend fun update(subscriptionType: SubscriptionType)

    @Delete
    suspend fun delete(subscriptionType: SubscriptionType)

    @Query("DELETE FROM subscription_types")
    suspend fun deleteAllSubscriptionTypes()
}

@Dao
interface ScheduledSubscriptionDao {
    @Query("SELECT * FROM scheduled_subscriptions ORDER BY dueDate ASC")
    fun getAllScheduledSubscriptions(): Flow<List<ScheduledSubscription>>

    @Query("SELECT * FROM scheduled_subscriptions")
    suspend fun getAllScheduledSubscriptionsList(): List<ScheduledSubscription>

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

    @Query("SELECT * FROM scheduled_subscriptions WHERE LOWER(status) = 'skipped' ORDER BY dueDate ASC")
    suspend fun getSkippedScheduledSubscriptions(): List<ScheduledSubscription>

    @Query("DELETE FROM scheduled_subscriptions WHERE subscriptionTypeId = :subTypeId AND dueDate > :date")
    suspend fun deleteScheduledSubsAfterDate(subTypeId: Int, date: Long)

    @Query("DELETE FROM scheduled_subscriptions WHERE subscriptionTypeId = :subTypeId")
    suspend fun deleteScheduledSubsForType(subTypeId: Int)

    @Query("DELETE FROM scheduled_subscriptions")
    suspend fun deleteAllScheduledSubscriptions()
}

@Dao
interface PreorderDao {
    @Query("SELECT * FROM preorders ORDER BY rangedSaleDateStart ASC")
    fun getAllPreorders(): Flow<List<Preorder>>

    @Query("SELECT * FROM preorders")
    suspend fun getAllPreordersList(): List<Preorder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preorder: Preorder): Long

    @Update
    suspend fun update(preorder: Preorder)

    @Query("SELECT * FROM preorders WHERE id = :id LIMIT 1")
    suspend fun getPreorderById(id: Int): Preorder?

    @Delete
    suspend fun delete(preorder: Preorder)

    @Query("DELETE FROM preorders")
    suspend fun deleteAllPreorders()
}

@Dao
interface SubscriptionSkipDao {
    @Query("SELECT * FROM subscription_skips ORDER BY id ASC")
    fun getAllSubscriptionSkips(): Flow<List<SubscriptionSkip>>

    @Query("SELECT * FROM subscription_skips")
    suspend fun getAllSubscriptionSkipsList(): List<SubscriptionSkip>

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

    @Query("DELETE FROM subscription_skips")
    suspend fun deleteAllSubscriptionSkips()
}

@Dao
interface SubscriptionSkipMethodDao {
    @Query("SELECT * FROM subscription_skip_methods ORDER BY skipMethodOrder ASC")
    fun getAllSubscriptionSkipMethods(): Flow<List<SubscriptionSkipMethod>>

    @Query("SELECT * FROM subscription_skip_methods")
    suspend fun getAllSubscriptionSkipMethodsList(): List<SubscriptionSkipMethod>

    @Query("SELECT * FROM subscription_skip_methods WHERE subscriptionTypeId = :subscriptionTypeId ORDER BY skipMethodOrder ASC")
    fun getSkipMethodsForSubscriptionType(subscriptionTypeId: Int): Flow<List<SubscriptionSkipMethod>>

    @Query("SELECT * FROM subscription_skip_methods WHERE subscriptionTypeId = :subscriptionTypeId ORDER BY skipMethodOrder ASC")
    suspend fun getSkipMethodsForSubscriptionTypeDirect(subscriptionTypeId: Int): List<SubscriptionSkipMethod>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscriptionSkipMethod: SubscriptionSkipMethod): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(methods: List<SubscriptionSkipMethod>)

    @Update
    suspend fun update(subscriptionSkipMethod: SubscriptionSkipMethod)

    @Delete
    suspend fun delete(subscriptionSkipMethod: SubscriptionSkipMethod)

    @Query("DELETE FROM subscription_skip_methods WHERE subscriptionTypeId = :subscriptionTypeId")
    suspend fun deleteBySubscriptionTypeId(subscriptionTypeId: Int)
}

@Dao
interface UserAddressDao {
    @Query("SELECT * FROM user_addresses ORDER BY isDefault DESC, id DESC")
    fun getAllUserAddresses(): Flow<List<UserAddress>>

    @Query("SELECT * FROM user_addresses")
    suspend fun getAllUserAddressesList(): List<UserAddress>

    @Query("SELECT * FROM user_addresses WHERE id = :id LIMIT 1")
    suspend fun getAddressById(id: Int): UserAddress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(address: UserAddress): Long

    @Update
    suspend fun update(address: UserAddress)

    @Delete
    suspend fun delete(address: UserAddress)

    @Query("DELETE FROM user_addresses")
    suspend fun deleteAllAddresses()

    @Query("UPDATE user_addresses SET isDefault = 0 WHERE id != :exceptId")
    suspend fun clearOtherDefaults(exceptId: Int)
}

@Dao
interface ShippingCompanyDao {
    @Query("SELECT * FROM shipping_companies ORDER BY name ASC")
    fun getAllShippingCompanies(): Flow<List<ShippingCompany>>

    @Query("SELECT * FROM shipping_companies")
    suspend fun getAllShippingCompaniesList(): List<ShippingCompany>

    @Query("SELECT * FROM shipping_companies WHERE id = :id LIMIT 1")
    suspend fun getShippingCompanyById(id: Int): ShippingCompany?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(shippingCompany: ShippingCompany): Long

    @Update
    suspend fun update(shippingCompany: ShippingCompany)

    @Delete
    suspend fun delete(shippingCompany: ShippingCompany)

    @Query("DELETE FROM shipping_companies")
    suspend fun deleteAllShippingCompanies()
}

@Dao
interface ShippingCompanyContactDao {
    @Query("SELECT * FROM shipping_company_contacts WHERE shippingCompanyId = :shippingCompanyId")
    fun getContactsForShippingCompany(shippingCompanyId: Int): Flow<List<ShippingCompanyContact>>

    @Query("SELECT * FROM shipping_company_contacts WHERE shippingCompanyId = :shippingCompanyId")
    suspend fun getContactsForShippingCompanyDirect(shippingCompanyId: Int): List<ShippingCompanyContact>

    @Query("SELECT * FROM shipping_company_contacts")
    fun getAllContacts(): Flow<List<ShippingCompanyContact>>

    @Query("SELECT * FROM shipping_company_contacts")
    suspend fun getAllContactsList(): List<ShippingCompanyContact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ShippingCompanyContact): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(contacts: List<ShippingCompanyContact>)

    @Update
    suspend fun update(contact: ShippingCompanyContact)

    @Delete
    suspend fun delete(contact: ShippingCompanyContact)

    @Query("DELETE FROM shipping_company_contacts WHERE shippingCompanyId = :shippingCompanyId")
    suspend fun deleteContactsForShippingCompany(shippingCompanyId: Int)
}

@Dao
interface PackageDao {
    @Query("SELECT * FROM packages WHERE originTable = :originTable AND originId = :originId LIMIT 1")
    fun getPackage(originTable: String, originId: Int): Flow<PackageItem?>

    @Query("SELECT * FROM packages WHERE originTable = :originTable AND originId = :originId LIMIT 1")
    suspend fun getPackageDirect(originTable: String, originId: Int): PackageItem?

    @Query("SELECT * FROM packages")
    fun getAllPackages(): Flow<List<PackageItem>>

    @Query("SELECT * FROM packages")
    suspend fun getAllPackagesList(): List<PackageItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(packageItem: PackageItem): Long

    @Update
    suspend fun update(packageItem: PackageItem)

    @Delete
    suspend fun delete(packageItem: PackageItem)

    @Query("DELETE FROM packages")
    suspend fun deleteAllPackages()

    @Query("DELETE FROM packages WHERE originTable = :originTable AND originId = :originId")
    suspend fun deleteByOrigin(originTable: String, originId: Int)
}


