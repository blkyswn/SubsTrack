package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlinx.coroutines.flow.first

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SubsTrack", appName)
  }

  @Test
  fun `launch MainActivity cleanly`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    val activity = controller.get()
    org.junit.Assert.assertNotNull(activity)
  }

  @Test
  fun `test excel export and import roundtrip`() {
    val sampleUser = com.example.data.User(
      id = 1,
      username = "TestReader",
      profilePic = "avatar_classic",
      currency = "£",
      language = "English",
      country = "United Kingdom",
      defaultTrackingUrl = "https://tracking.example.com",
      defaultScheduledSubCount = 4
    )
    val sampleBookstore = com.example.data.Bookstore(
      id = 1,
      name = "Illumicrate",
      website = "https://illumicrate.com",
      profilePic = "ic_book_1"
    )
    val sampleSub = com.example.data.SubscriptionWithBookstore(
      subscription = com.example.data.SubscriptionType(
        id = 1,
        bookstoreId = 1,
        title = "Illumicrate Monthly",
        status = "Active",
        price = 28.0,
        dueDate = 1727788800000L,
        startDate = 1725196800000L,
        finishDate = 0L,
        notificationAlertDays = 3,
        frequency = "Monthly",
        skipType = "Unlimited",
        numberOfSkips = 10,
        numberOfMonths = 12
      ),
      bookstore = sampleBookstore,
      skipInfo = null
    )

    val xml = com.example.utils.ExcelExporter.generateWorkbookXml(
      user = sampleUser,
      bookstores = listOf(sampleBookstore),
      bookstoreContacts = emptyList(),
      forwardingServices = emptyList(),
      forwardingServiceContacts = emptyList(),
      shippingCompanies = emptyList(),
      shippingCompanyContacts = emptyList(),
      userAddresses = emptyList(),
      subscriptions = listOf(sampleSub),
      subscriptionSkipMethods = emptyList(),
      subscriptionSkips = emptyList(),
      scheduled = emptyList(),
      preorders = emptyList(),
      packages = emptyList()
    )

    org.junit.Assert.assertTrue("Export should produce XML workbook", xml.contains("<Workbook"))
    org.junit.Assert.assertTrue("Export should include bookstore name", xml.contains("Illumicrate"))
    org.junit.Assert.assertTrue("Export should include subscription title", xml.contains("Illumicrate Monthly"))

    val parsed = com.example.utils.ExcelImporter.parseWorkbook(
      java.io.ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8)),
      "yyyy-MM-dd"
    )

    org.junit.Assert.assertEquals("TestReader", parsed.user?.username)
    org.junit.Assert.assertEquals("£", parsed.user?.currency)
    org.junit.Assert.assertEquals(1, parsed.bookstores.size)
    org.junit.Assert.assertEquals("Illumicrate", parsed.bookstores[0].name)
    org.junit.Assert.assertEquals(1, parsed.subscriptions.size)
    org.junit.Assert.assertEquals("Illumicrate Monthly", parsed.subscriptions[0].title)
    org.junit.Assert.assertEquals(28.0, parsed.subscriptions[0].price, 0.001)
  }

  @Test
  fun `test database snapshot creation and restoration`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)

    // Ensure database initialized
    val snapshotBytes = com.example.data.BookishDatabase.createSnapshotBytes(context)
    org.junit.Assert.assertNotNull(snapshotBytes)
    org.junit.Assert.assertTrue((snapshotBytes?.size ?: 0) > 0)

    // Restore snapshot from the created bytes
    val restored = com.example.data.BookishDatabase.restoreDatabaseSnapshot(
      context,
      java.io.ByteArrayInputStream(snapshotBytes!!)
    )
    org.junit.Assert.assertTrue("Snapshot restoration should succeed", restored)
    org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
  }

  @Test
  fun `test AppFolderManager initialization and setting`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.utils.AppFolderManager.init(context)

    // Setting null resets to default internal storage app folder
    com.example.utils.AppFolderManager.setAppFolderUri(context, null)
    val defaultPath = com.example.utils.AppFolderManager.getDefaultAppFolder(context)
    org.junit.Assert.assertEquals(defaultPath, com.example.utils.AppFolderManager.appFolderUri.value)
    org.junit.Assert.assertTrue(com.example.utils.AppFolderManager.appFolderDisplayPath.value?.contains("SubsTrack") == true)
  }

  @Test
  fun `test backup filename has datetime suffix format`() {
    val timestamp = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.US).format(java.util.Date())
    val dbFileName = "bookish_database_backup_${timestamp}.db"
    val excelFileName = "bookish_library_export_${timestamp}.xls"

    // Verify trailing 14-digit timestamp suffix before extension
    val regex = Regex(".+_[0-9]{14}\\.[a-zA-Z0-9]+")
    org.junit.Assert.assertTrue("Database backup file name should match _yyyyMMddHHmmss suffix pattern", regex.matches(dbFileName))
    org.junit.Assert.assertTrue("Excel export file name should match _yyyyMMddHHmmss suffix pattern", regex.matches(excelFileName))
  }

  @Test
  fun `test user table default app folder path route to internal storage`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val defaultPath = com.example.utils.AppFolderManager.getDefaultAppFolder(context)
    org.junit.Assert.assertTrue("Default path must point to internal storage", defaultPath.startsWith(context.filesDir.absolutePath))
    org.junit.Assert.assertTrue("Default path must include app name SubsTrack", defaultPath.endsWith("SubsTrack"))
  }

  @Test
  fun `test excel import deduplication without comparing ids`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = com.example.data.BookishDatabase.getDatabase(context)
    val repo = com.example.data.BookishRepository(db)

    // Prepopulate
    repo.checkAndPrepopulate(com.example.utils.AppFolderManager.getDefaultAppFolder(context))

    val existingBookstoresBefore = repo.allBookstoresFlow.first()
    val initialCount = existingBookstoresBefore.size
    org.junit.Assert.assertTrue(initialCount > 0)

    // Try importing an existing store with ID = 999 (should not create duplicate, ignoring ID)
    val duplicateStore = com.example.data.Bookstore(id = 999, name = "FairyLoot", website = "https://fairyloot.com", profilePic = null)
    val importData = com.example.utils.ExcelImportData(
      bookstores = listOf(duplicateStore)
    )

    val result = repo.importSpreadsheetData(importData, replaceExisting = false)
    org.junit.Assert.assertTrue(result.success)
    org.junit.Assert.assertEquals("Duplicate bookstore should not be added", 0, result.bookstoresCount)

    val bookstoresAfter = repo.allBookstoresFlow.first()
    org.junit.Assert.assertEquals("Bookstore count should remain the same", initialCount, bookstoresAfter.size)
  }
}
