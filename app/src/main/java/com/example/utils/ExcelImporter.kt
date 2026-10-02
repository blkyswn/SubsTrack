package com.example.utils

import android.util.Xml
import com.example.data.*
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

data class ExcelImportData(
    val user: User? = null,
    val bookstores: List<Bookstore> = emptyList(),
    val bookstoreContacts: List<BookstoreContact> = emptyList(),
    val forwardingServices: List<ForwardingService> = emptyList(),
    val forwardingServiceContacts: List<ForwardingServiceContact> = emptyList(),
    val shippingCompanies: List<ShippingCompany> = emptyList(),
    val shippingCompanyContacts: List<ShippingCompanyContact> = emptyList(),
    val userAddresses: List<UserAddress> = emptyList(),
    val subscriptions: List<SubscriptionType> = emptyList(),
    val subscriptionSkipMethods: List<SubscriptionSkipMethod> = emptyList(),
    val subscriptionSkips: List<SubscriptionSkip> = emptyList(),
    val scheduledSubscriptions: List<ScheduledSubscription> = emptyList(),
    val preorders: List<Preorder> = emptyList(),
    val packages: List<PackageItem> = emptyList()
) {
    val totalCount: Int
        get() = bookstores.size + bookstoreContacts.size + forwardingServices.size +
                forwardingServiceContacts.size + shippingCompanies.size + shippingCompanyContacts.size +
                userAddresses.size + subscriptions.size + subscriptionSkipMethods.size +
                subscriptionSkips.size + scheduledSubscriptions.size + preorders.size + packages.size
}

data class ImportResult(
    val success: Boolean,
    val message: String,
    val bookstoresCount: Int = 0,
    val subscriptionsCount: Int = 0,
    val preordersCount: Int = 0,
    val addressesCount: Int = 0,
    val packagesCount: Int = 0
)

object ExcelImporter {

    fun parseWorkbook(inputStream: InputStream, userDateFormat: String = "yyyy-MM-dd"): ExcelImportData {
        // Read stream into byte array to inspect format and strip BOM
        val rawBytes = inputStream.readBytes()
        val cleanBytes = stripBom(rawBytes)

        val isXml = isXmlStream(cleanBytes)
        val sheetsData: Map<String, List<List<String>>> = if (isXml) {
            parseXmlWorkbook(ByteArrayInputStream(cleanBytes))
        } else {
            parseCsvWorkbook(ByteArrayInputStream(cleanBytes))
        }

        return buildImportData(sheetsData, userDateFormat)
    }

    private fun stripBom(bytes: ByteArray): ByteArray {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return bytes.copyOfRange(3, bytes.size)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
            return bytes.copyOfRange(2, bytes.size)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return bytes.copyOfRange(2, bytes.size)
        }
        return bytes
    }

    private fun isXmlStream(bytes: ByteArray): Boolean {
        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c.isWhitespace()) continue
            return c == '<'
        }
        return false
    }

    private fun parseXmlWorkbook(inputStream: InputStream): Map<String, MutableList<List<String>>> {
        val parser = Xml.newPullParser()
        parser.setInput(inputStream, "UTF-8")

        val sheetsData = mutableMapOf<String, MutableList<List<String>>>()
        var currentSheetName: String? = null
        var currentRow: MutableList<String>? = null
        var currentCellIndex = 0
        var currentCellText: StringBuilder? = null
        var insideData = false

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name?.substringAfterLast(':') ?: ""
                    when {
                        tagName.equals("Worksheet", ignoreCase = true) -> {
                            var sheetName: String? = null
                            for (i in 0 until parser.attributeCount) {
                                val attrName = parser.getAttributeName(i)?.substringAfterLast(':') ?: ""
                                if (attrName.equals("Name", ignoreCase = true)) {
                                    sheetName = parser.getAttributeValue(i)
                                    break
                                }
                            }
                            currentSheetName = sheetName?.trim()?.lowercase(Locale.ROOT)
                            if (currentSheetName != null) {
                                sheetsData.getOrPut(currentSheetName) { mutableListOf() }
                            }
                        }
                        tagName.equals("Row", ignoreCase = true) -> {
                            currentRow = mutableListOf()
                            currentCellIndex = 0
                        }
                        tagName.equals("Cell", ignoreCase = true) -> {
                            var specifiedIndex: Int? = null
                            for (i in 0 until parser.attributeCount) {
                                val attrName = parser.getAttributeName(i)?.substringAfterLast(':') ?: ""
                                if (attrName.equals("Index", ignoreCase = true)) {
                                    specifiedIndex = parser.getAttributeValue(i).toIntOrNull()
                                    break
                                }
                            }
                            if (specifiedIndex != null && specifiedIndex > currentCellIndex + 1) {
                                while (currentRow != null && currentRow.size < specifiedIndex - 1) {
                                    currentRow.add("")
                                }
                                currentCellIndex = specifiedIndex - 1
                            }
                            currentCellText = StringBuilder()
                        }
                        tagName.equals("Data", ignoreCase = true) -> {
                            insideData = true
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (insideData && currentCellText != null) {
                        currentCellText.append(parser.text)
                    }
                }
                XmlPullParser.END_TAG -> {
                    val tagName = parser.name?.substringAfterLast(':') ?: ""
                    when {
                        tagName.equals("Data", ignoreCase = true) -> {
                            insideData = false
                        }
                        tagName.equals("Cell", ignoreCase = true) -> {
                            val text = currentCellText?.toString()?.trim() ?: ""
                            currentRow?.add(text)
                            currentCellIndex++
                            currentCellText = null
                        }
                        tagName.equals("Row", ignoreCase = true) -> {
                            if (currentRow != null && currentRow.any { it.isNotBlank() }) {
                                currentSheetName?.let { sName ->
                                    sheetsData[sName]?.add(currentRow)
                                }
                            }
                            currentRow = null
                        }
                        tagName.equals("Worksheet", ignoreCase = true) -> {
                            currentSheetName = null
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return sheetsData
    }

    private fun parseCsvWorkbook(inputStream: InputStream): Map<String, List<List<String>>> {
        val sheetsData = mutableMapOf<String, MutableList<List<String>>>()
        var currentSection = "overview"
        sheetsData[currentSection] = mutableListOf()

        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        reader.useLines { lines ->
            for (rawLine in lines) {
                val line = rawLine.trim()
                if (line.isBlank()) continue

                // Check section headers like [Bookstores] or === Bookstores ===
                if ((line.startsWith("[") && line.endsWith("]")) || (line.startsWith("===") && line.endsWith("==="))) {
                    val sectionName = line.trim('[', ']', '=', ' ').trim().lowercase(Locale.ROOT)
                    if (sectionName.isNotBlank()) {
                        currentSection = sectionName
                        sheetsData.getOrPut(currentSection) { mutableListOf() }
                        continue
                    }
                }

                val cells = parseCsvLine(rawLine)
                if (cells.any { it.isNotBlank() }) {
                    sheetsData.getOrPut(currentSection) { mutableListOf() }.add(cells)
                }
            }
        }
        return sheetsData
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        val delimiter = if (line.contains(";") && !line.contains(",")) ';' else ','

        for (i in 0 until line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    private enum class TargetSheet {
        OVERVIEW,
        BOOKSTORES,
        BOOKSTORE_CONTACTS,
        FORWARDING_SERVICES,
        FORWARDING_CONTACTS,
        SHIPPING_COMPANIES,
        SHIPPING_CONTACTS,
        ADDRESSES,
        SUBSCRIPTIONS,
        SKIP_METHODS,
        SUBSCRIPTION_SKIPS,
        SCHEDULED_SUBSCRIPTIONS,
        PREORDERS,
        PACKAGES
    }

    private fun findSheetRows(
        sheetsData: Map<String, List<List<String>>>,
        sheet: TargetSheet
    ): List<List<String>> {
        for ((key, rows) in sheetsData) {
            val k = key.lowercase(Locale.ROOT).trim()
            val matches = when (sheet) {
                TargetSheet.OVERVIEW -> k == "overview" || k.startsWith("profile") || k.startsWith("user")
                TargetSheet.BOOKSTORES -> (k == "bookstores" || k == "bookstore") && !k.contains("contact")
                TargetSheet.BOOKSTORE_CONTACTS -> k.contains("bookstore") && k.contains("contact")
                TargetSheet.FORWARDING_SERVICES -> (k == "forwarding services" || k == "forwarding service" || k == "forwarding") && !k.contains("contact")
                TargetSheet.FORWARDING_CONTACTS -> (k.contains("forwarding") || k.contains("forwarder")) && k.contains("contact")
                TargetSheet.SHIPPING_COMPANIES -> (k == "shipping companies" || k == "shipping company" || k == "carriers") && !k.contains("contact")
                TargetSheet.SHIPPING_CONTACTS -> (k.contains("shipping") || k.contains("carrier")) && k.contains("contact")
                TargetSheet.ADDRESSES -> k.contains("address")
                TargetSheet.SUBSCRIPTIONS -> (k == "subscriptions" || k == "subscription" || k == "subs") && !k.contains("skip") && !k.contains("method") && !k.contains("scheduled")
                TargetSheet.SKIP_METHODS -> k.contains("skip") && (k.contains("method") || k.contains("template"))
                TargetSheet.SUBSCRIPTION_SKIPS -> k.contains("skip") && !k.contains("method")
                TargetSheet.SCHEDULED_SUBSCRIPTIONS -> k.contains("scheduled") || k.contains("deliver")
                TargetSheet.PREORDERS -> k.contains("preorder")
                TargetSheet.PACKAGES -> k.contains("package") || k.contains("tracking")
            }
            if (matches) return rows
        }
        return emptyList()
    }

    private fun buildImportData(
        sheetsData: Map<String, List<List<String>>>,
        userDateFormat: String
    ): ExcelImportData {
        // 1. Overview sheet (User Profile)
        var parsedUser: User? = null
        val overviewRows = findSheetRows(sheetsData, TargetSheet.OVERVIEW)
        if (overviewRows.isNotEmpty()) {
            val overviewMap = mutableMapOf<String, String>()
            for (row in overviewRows) {
                if (row.size >= 2) {
                    val key = normalizeKey(row[0])
                    val value = row[1].trim()
                    if (key.isNotBlank()) overviewMap[key] = value
                }
            }
            val username = overviewMap["user name"] ?: overviewMap["username"] ?: "Reader"
            val currency = overviewMap["preferred currency"] ?: overviewMap["currency"] ?: "$"
            val language = overviewMap["preferred language"] ?: overviewMap["language"] ?: "English"
            val themeMode = overviewMap["theme mode"] ?: "system"
            val themeCombo = overviewMap["theme palette"] ?: overviewMap["theme combo"] ?: "default"
            val dateFormat = overviewMap["date format"] ?: userDateFormat
            val displayAmounts = parseBoolean(overviewMap["display amounts"])
            val country = overviewMap["country"] ?: ""
            val defaultTrackingUrl = overviewMap["default tracking url"] ?: overviewMap["tracking url"] ?: ""
            val profilePic = overviewMap["profile picture"] ?: overviewMap["profile pic"] ?: "avatar_classic"
            val scheduledCount = overviewMap["default scheduled sub count"]?.toIntOrNull() ?: 6

            parsedUser = User(
                id = 1,
                username = username,
                profilePic = profilePic.ifBlank { "avatar_classic" },
                currency = currency,
                language = language,
                defaultScheduledSubCount = scheduledCount,
                themeMode = themeMode,
                themeCombo = themeCombo,
                dateFormat = dateFormat,
                displayAmounts = displayAmounts,
                country = country,
                defaultTrackingUrl = defaultTrackingUrl
            )
        }

        fun mapRows(sheet: TargetSheet): List<Map<String, String>> {
            val rawRows = findSheetRows(sheetsData, sheet)
            if (rawRows.size < 2) return emptyList()

            // Header row is first row
            val headerRow = rawRows.first()
            val headers = headerRow.map { normalizeKey(it) }

            val result = mutableListOf<Map<String, String>>()
            for (i in 1 until rawRows.size) {
                val row = rawRows[i]
                val rowMap = mutableMapOf<String, String>()
                for (c in 0 until minOf(headers.size, row.size)) {
                    val h = headers[c]
                    if (h.isNotBlank()) {
                        rowMap[h] = row[c].trim()
                    }
                }
                if (rowMap.values.any { it.isNotBlank() }) {
                    result.add(rowMap)
                }
            }
            return result
        }

        // 2. Bookstores
        val bookstoreRows = mapRows(TargetSheet.BOOKSTORES)
        val bookstores = bookstoreRows.mapIndexed { index, row ->
            val id = getInt(row, "bookstore id", "id") ?: (index + 1)
            val name = getString(row, "name", "bookstore name", "title") ?: "Bookstore $id"
            val website = getString(row, "website", "url") ?: ""
            val profilePic = getString(row, "profile picture", "profile pic", "picture")
            Bookstore(id = id, name = name, website = website, profilePic = profilePic?.ifBlank { null })
        }
        val bookstoreNameToId = bookstores.associate { it.name.lowercase(Locale.ROOT) to it.id }

        // 3. Bookstore Contacts
        val bookstoreContactRows = mapRows(TargetSheet.BOOKSTORE_CONTACTS)
        val bookstoreContacts = bookstoreContactRows.mapIndexed { index, row ->
            val id = getInt(row, "contact id", "id") ?: (index + 1)
            val storeName = getString(row, "bookstore name", "bookstore")?.lowercase(Locale.ROOT)
            val bookstoreId = getInt(row, "bookstore id") ?: storeName?.let { bookstoreNameToId[it] } ?: 1
            val contactType = getString(row, "contact type", "type") ?: "Website"
            val contactValue = getString(row, "contact value", "value", "contact") ?: ""
            BookstoreContact(id = id, bookstoreId = bookstoreId, contactType = contactType, contactValue = contactValue)
        }

        // 4. Forwarding Services
        val forwardingRows = mapRows(TargetSheet.FORWARDING_SERVICES)
        val forwardingServices = forwardingRows.mapIndexed { index, row ->
            val id = getInt(row, "service id", "forwarding service id", "id") ?: (index + 1)
            val name = getString(row, "name", "service name") ?: "Forwarder $id"
            val website = getString(row, "website", "url") ?: ""
            val profilePic = getString(row, "profile picture", "profile pic")
            val storageDays = getInt(row, "storage days", "storage")
            val reminderEnabled = parseBoolean(getString(row, "reminder enabled", "reminder"))
            val reminderDDayOffset = getInt(row, "reminder d-day offset", "reminder offset") ?: 0
            val reminderTimeStr = getString(row, "reminder time")
            val (hour, minute) = parseHourMinute(reminderTimeStr) ?: (8 to 0)

            ForwardingService(
                id = id,
                name = name,
                website = website,
                profilePic = profilePic?.ifBlank { null },
                storageDays = storageDays,
                reminderEnabled = reminderEnabled,
                reminderDDayOffset = reminderDDayOffset,
                reminderHour = hour,
                reminderMinute = minute
            )
        }
        val forwardingNameToId = forwardingServices.associate { it.name.lowercase(Locale.ROOT) to it.id }

        // 5. Forwarding Service Contacts
        val forwardingContactRows = mapRows(TargetSheet.FORWARDING_CONTACTS)
        val forwardingServiceContacts = forwardingContactRows.mapIndexed { index, row ->
            val id = getInt(row, "contact id", "id") ?: (index + 1)
            val fsName = getString(row, "forwarding service name", "service name")?.lowercase(Locale.ROOT)
            val forwardingServiceId = getInt(row, "forwarding service id", "service id")
                ?: fsName?.let { forwardingNameToId[it] } ?: 1
            val contactType = getString(row, "contact type", "type") ?: "Website"
            val contactValue = getString(row, "contact value", "value") ?: ""
            ForwardingServiceContact(
                id = id,
                forwardingServiceId = forwardingServiceId,
                contactType = contactType,
                contactValue = contactValue
            )
        }

        // 6. Shipping Companies
        val shippingCompanyRows = mapRows(TargetSheet.SHIPPING_COMPANIES)
        val shippingCompanies = shippingCompanyRows.mapIndexed { index, row ->
            val id = getInt(row, "company id", "id") ?: (index + 1)
            val name = getString(row, "name", "company name") ?: "Carrier $id"
            val website = getString(row, "website", "url") ?: ""
            val profilePic = getString(row, "profile picture", "profile pic")
            ShippingCompany(id = id, name = name, website = website, profilePic = profilePic?.ifBlank { null })
        }
        val shippingNameToId = shippingCompanies.associate { it.name.lowercase(Locale.ROOT) to it.id }

        // 7. Shipping Company Contacts
        val shippingContactRows = mapRows(TargetSheet.SHIPPING_CONTACTS)
        val shippingCompanyContacts = shippingContactRows.mapIndexed { index, row ->
            val id = getInt(row, "contact id", "id") ?: (index + 1)
            val scName = getString(row, "company name")?.lowercase(Locale.ROOT)
            val shippingCompanyId = getInt(row, "company id", "shipping company id")
                ?: scName?.let { shippingNameToId[it] } ?: 1
            val contactType = getString(row, "contact type", "type") ?: "Website"
            val contactValue = getString(row, "contact value", "value") ?: ""
            ShippingCompanyContact(
                id = id,
                shippingCompanyId = shippingCompanyId,
                contactType = contactType,
                contactValue = contactValue
            )
        }

        // 8. User Addresses
        val addressRows = mapRows(TargetSheet.ADDRESSES)
        val userAddresses = addressRows.mapIndexed { index, row ->
            val id = getInt(row, "address id", "id") ?: (index + 1)
            val userId = getInt(row, "user id") ?: 1
            val street1 = getString(row, "street address 1", "street 1", "street") ?: ""
            val street2 = getString(row, "street address 2", "street 2") ?: ""
            val city = getString(row, "city") ?: ""
            val state = getString(row, "state / region", "state", "region") ?: ""
            val postal = getString(row, "postal code", "zip", "zip code") ?: ""
            val country = getString(row, "country") ?: ""
            val fsName = getString(row, "forwarding service")?.lowercase(Locale.ROOT)
            val forwardingServiceId = getInt(row, "forwarding service id")
                ?: fsName?.let { forwardingNameToId[it] }
            val isDefault = parseBoolean(getString(row, "is default", "default"))

            UserAddress(
                id = id,
                userId = userId,
                streetAddress1 = street1,
                streetAddress2 = street2,
                city = city,
                stateProvinceRegion = state,
                postalCode = postal,
                country = country,
                forwardingServiceId = forwardingServiceId,
                isDefault = isDefault
            )
        }
        val addressMatchToId = userAddresses.associate { it.toFormattedString().lowercase(Locale.ROOT) to it.id }

        // 9. Subscriptions
        val subscriptionRows = mapRows(TargetSheet.SUBSCRIPTIONS)
        val subscriptions = subscriptionRows.mapIndexed { index, row ->
            val id = getInt(row, "subscription id", "id") ?: (index + 1)
            val storeName = getString(row, "bookstore")?.lowercase(Locale.ROOT)
            val bookstoreId = getInt(row, "bookstore id") ?: storeName?.let { bookstoreNameToId[it] } ?: 1
            val title = getString(row, "title", "subscription title") ?: "Subscription $id"
            val status = getString(row, "status") ?: "Active"
            val price = getDouble(row, "price") ?: 0.0
            val currency = getString(row, "currency")
            val basePrice = getDouble(row, "base price")
            val discountedAmount = getDouble(row, "discount", "discounted amount")
            val shippingPrice = getDouble(row, "shipping", "shipping price")
            val taxPrice = getDouble(row, "tax", "tax price")
            val fwdShipping = getDouble(row, "fwd shipping", "forward shipping price")
            val fwdTax = getDouble(row, "fwd tax", "forward tax price")

            val dueDate = parseDate(getString(row, "due date"), userDateFormat) ?: System.currentTimeMillis()
            val startDate = parseDate(getString(row, "start date"), userDateFormat) ?: System.currentTimeMillis()
            val finishDate = parseDate(getString(row, "finish date"), userDateFormat) ?: 0L

            val alertDays = getInt(row, "alert days", "notification alert days")
            val frequency = getString(row, "frequency") ?: "Monthly"
            val reminderEnabled = parseBoolean(getString(row, "reminder", "reminder enabled"))
            val reminderDDayOffset = getInt(row, "reminder offset", "reminder d-day offset") ?: 0
            val reminderTimeStr = getString(row, "reminder time")
            val (hour, minute) = parseHourMinute(reminderTimeStr) ?: (8 to 0)

            val skipType = getString(row, "skip type") ?: "None"
            val maxSkips = getInt(row, "max skips", "number of skips")
            val skipMonths = getInt(row, "skip months", "number of months")
            val skipMethod = getString(row, "skip method")
            val skipLink = getString(row, "skip link")
            val skipText = getString(row, "skip text")
            val picturePath = getString(row, "picture path", "picture")

            val addrStr = getString(row, "shipping address")?.lowercase(Locale.ROOT)
            val shippingAddressId = getInt(row, "shipping address id")
                ?: addrStr?.let { addressMatchToId[it] }

            SubscriptionType(
                id = id,
                bookstoreId = bookstoreId,
                title = title,
                status = status,
                price = price,
                dueDate = dueDate,
                startDate = startDate,
                finishDate = finishDate,
                notificationAlertDays = alertDays,
                frequency = frequency,
                reminderEnabled = reminderEnabled,
                reminderDDayOffset = reminderDDayOffset,
                reminderHour = hour,
                reminderMinute = minute,
                skipType = skipType,
                numberOfSkips = maxSkips,
                numberOfMonths = skipMonths,
                skipMethod = skipMethod,
                skipLink = skipLink,
                skipText = skipText,
                picturePath = picturePath?.ifBlank { null },
                shippingAddressId = shippingAddressId,
                currency = currency,
                basePrice = basePrice,
                discountedAmount = discountedAmount,
                shippingPrice = shippingPrice,
                taxPrice = taxPrice,
                forwardShippingPrice = fwdShipping,
                forwardTaxPrice = fwdTax
            )
        }
        val subscriptionTitleToId = subscriptions.associate { it.title.lowercase(Locale.ROOT) to it.id }

        // 10. Subscription Skip Methods
        val skipMethodRows = mapRows(TargetSheet.SKIP_METHODS)
        val skipMethods = skipMethodRows.mapIndexed { index, row ->
            val id = getInt(row, "method id", "id") ?: (index + 1)
            val subTitle = getString(row, "subscription title", "subscription")?.lowercase(Locale.ROOT)
            val subId = getInt(row, "subscription id") ?: subTitle?.let { subscriptionTitleToId[it] } ?: 1
            val order = getInt(row, "order", "skip method order") ?: (index + 1)
            val methodType = getString(row, "method type", "type") ?: "Website"
            val methodValue = getString(row, "method value", "value") ?: ""
            val messageText = getString(row, "message text template", "message text", "template") ?: ""
            val consecutiveSkips = getInt(row, "consecutive skips") ?: 0

            SubscriptionSkipMethod(
                id = id,
                subscriptionTypeId = subId,
                skipMethodOrder = order,
                skipMethodType = methodType,
                skipMethodValue = methodValue,
                skipMethodText = messageText,
                consecutiveSkips = consecutiveSkips
            )
        }

        // 11. Subscription Skips
        val skipRows = mapRows(TargetSheet.SUBSCRIPTION_SKIPS)
        val subscriptionSkips = skipRows.mapIndexed { index, row ->
            val id = getInt(row, "skip id", "id") ?: (index + 1)
            val subTitle = getString(row, "subscription title", "subscription")?.lowercase(Locale.ROOT)
            val subId = getInt(row, "subscription id") ?: subTitle?.let { subscriptionTitleToId[it] } ?: 1
            val skipType = getString(row, "skip type") ?: "Unlimited"
            val totalSkips = getInt(row, "total skips", "number of skips")
            val skipsLeft = getInt(row, "skips left")
            val startDate = parseDate(getString(row, "start date", "skip start date"), userDateFormat)
            val endDate = parseDate(getString(row, "end date", "skip end date"), userDateFormat)

            SubscriptionSkip(
                id = id,
                subscriptionTypeId = subId,
                subscriptionSkipType = skipType,
                numberOfSkips = totalSkips,
                skipsLeft = skipsLeft,
                skipStartDate = startDate,
                skipEndDate = endDate
            )
        }

        // 12. Scheduled Subscriptions
        val scheduledRows = mapRows(TargetSheet.SCHEDULED_SUBSCRIPTIONS)
        val scheduledSubscriptions = scheduledRows.mapIndexed { index, row ->
            val id = getInt(row, "scheduled id", "id") ?: (index + 1)
            val subTitle = getString(row, "subscription title", "subscription")?.lowercase(Locale.ROOT)
            val subId = getInt(row, "subscription id") ?: subTitle?.let { subscriptionTitleToId[it] } ?: 1
            val bookTitle = getString(row, "book title", "title") ?: "Delivery #$id"
            val author = getString(row, "author", "book author") ?: ""
            val description = getString(row, "description") ?: ""
            val dueDate = parseDate(getString(row, "due date"), userDateFormat) ?: System.currentTimeMillis()
            val status = getString(row, "status") ?: "Upcoming"
            val isSkipped = parseBoolean(getString(row, "is skipped", "skipped"))
            val rating = getDouble(row, "rating") ?: 0.0
            val picturePath = getString(row, "picture path", "picture")

            ScheduledSubscription(
                id = id,
                subscriptionTypeId = subId,
                bookTitle = bookTitle,
                bookAuthor = author,
                description = description,
                dueDate = dueDate,
                status = status,
                isSkipped = isSkipped,
                picturePath = picturePath?.ifBlank { null },
                rating = rating
            )
        }
        val scheduledTitleToId = scheduledSubscriptions.associate { it.bookTitle.lowercase(Locale.ROOT) to it.id }

        // 13. Preorders
        val preorderRows = mapRows(TargetSheet.PREORDERS)
        val preorders = preorderRows.mapIndexed { index, row ->
            val id = getInt(row, "preorder id", "id") ?: (index + 1)
            val storeName = getString(row, "bookstore")?.lowercase(Locale.ROOT)
            val bookstoreId = getInt(row, "bookstore id") ?: storeName?.let { bookstoreNameToId[it] } ?: 1
            val title = getString(row, "book title", "title") ?: "Preorder #$id"
            val author = getString(row, "author", "book author") ?: ""
            val description = getString(row, "description") ?: ""
            val price = getDouble(row, "price") ?: 0.0
            val currency = getString(row, "currency")
            val basePrice = getDouble(row, "base price")
            val discount = getDouble(row, "discount")
            val shipping = getDouble(row, "shipping")
            val tax = getDouble(row, "tax")
            val fwdShipping = getDouble(row, "fwd shipping")
            val fwdTax = getDouble(row, "fwd tax")

            val saleStartDate = parseDate(getString(row, "sale start date", "start date"), userDateFormat) ?: System.currentTimeMillis()
            val saleEndDate = parseDate(getString(row, "sale end date", "end date"), userDateFormat) ?: saleStartDate
            val status = getString(row, "status") ?: "Upcoming"

            val reminderEnabled = parseBoolean(getString(row, "reminder", "reminder enabled"))
            val reminderDDayOffset = getInt(row, "reminder offset", "reminder d-day offset") ?: 0
            val reminderTimeStr = getString(row, "reminder time")
            val (hour, minute) = parseHourMinute(reminderTimeStr) ?: (8 to 0)

            val rating = getDouble(row, "rating") ?: 0.0
            val addrStr = getString(row, "shipping address")?.lowercase(Locale.ROOT)
            val shippingAddressId = getInt(row, "shipping address id")
                ?: addrStr?.let { addressMatchToId[it] }
            val picturePath = getString(row, "picture path", "picture")

            Preorder(
                id = id,
                bookstoreId = bookstoreId,
                picturePath = picturePath?.ifBlank { null },
                bookTitle = title,
                bookAuthor = author,
                description = description,
                price = price,
                rangedSaleDateStart = saleStartDate,
                rangedSaleDateEnd = saleEndDate,
                status = status,
                reminderEnabled = reminderEnabled,
                reminderDDayOffset = reminderDDayOffset,
                reminderHour = hour,
                reminderMinute = minute,
                rating = rating,
                shippingAddressId = shippingAddressId,
                currency = currency,
                basePrice = basePrice,
                discountedAmount = discount,
                shippingPrice = shipping,
                taxPrice = tax,
                forwardShippingPrice = fwdShipping,
                forwardTaxPrice = fwdTax
            )
        }
        val preorderTitleToId = preorders.associate { it.bookTitle.lowercase(Locale.ROOT) to it.id }

        // 14. Packages
        val packageRows = mapRows(TargetSheet.PACKAGES)
        val packages = packageRows.mapIndexed { index, row ->
            val id = getInt(row, "package id", "id") ?: (index + 1)
            val originTable = getString(row, "origin table", "type") ?: "scheduled_subs"
            val itemTitle = getString(row, "item title", "title")?.lowercase(Locale.ROOT)
            val originId = getInt(row, "origin id")
                ?: (if (originTable == "preorders") itemTitle?.let { preorderTitleToId[it] } else itemTitle?.let { scheduledTitleToId[it] })
                ?: 1
            val purchaseDate = parseDate(getString(row, "purchase date"), userDateFormat)
            val storeShipDate = parseDate(getString(row, "store ship date", "shipped date"), userDateFormat)
            val storeCompany = getString(row, "store shipping co", "shipping company")
            val storeTracking = getString(row, "store tracking #", "tracking number")
            val fwdReceivedDate = parseDate(getString(row, "fwd received date"), userDateFormat)
            val fwdCompany = getString(row, "fwd shipping co")
            val fwdTracking = getString(row, "fwd tracking #")
            val fwdShippedDate = parseDate(getString(row, "fwd shipped date"), userDateFormat)
            val receivedDate = parseDate(getString(row, "received date"), userDateFormat)

            PackageItem(
                id = id,
                originTable = originTable,
                originId = originId,
                purchaseDate = purchaseDate,
                storeShippingDate = storeShipDate,
                storeShippingCompany = storeCompany?.ifBlank { null },
                storeTrackingNumber = storeTracking?.ifBlank { null },
                forwarderReceivedDate = fwdReceivedDate,
                forwarderShippingCompany = fwdCompany?.ifBlank { null },
                forwarderTrackingNumber = fwdTracking?.ifBlank { null },
                forwarderShippedDate = fwdShippedDate,
                receivedDate = receivedDate
            )
        }

        return ExcelImportData(
            user = parsedUser,
            bookstores = bookstores,
            bookstoreContacts = bookstoreContacts,
            forwardingServices = forwardingServices,
            forwardingServiceContacts = forwardingServiceContacts,
            shippingCompanies = shippingCompanies,
            shippingCompanyContacts = shippingCompanyContacts,
            userAddresses = userAddresses,
            subscriptions = subscriptions,
            subscriptionSkipMethods = skipMethods,
            subscriptionSkips = subscriptionSkips,
            scheduledSubscriptions = scheduledSubscriptions,
            preorders = preorders,
            packages = packages
        )
    }

    private fun normalizeKey(raw: String): String {
        return raw.lowercase(Locale.ROOT)
            .replace("_", " ")
            .replace("-", " ")
            .trim()
    }

    private fun getString(row: Map<String, String>, vararg keys: String): String? {
        for (k in keys) {
            val v = row[k]
            if (!v.isNullOrBlank()) return v.trim()
        }
        return null
    }

    private fun getInt(row: Map<String, String>, vararg keys: String): Int? {
        val s = getString(row, *keys) ?: return null
        return s.toDoubleOrNull()?.toInt() ?: s.toIntOrNull()
    }

    private fun getDouble(row: Map<String, String>, vararg keys: String): Double? {
        val s = getString(row, *keys) ?: return null
        val cleaned = s.replace("$", "").replace("€", "").replace("£", "").replace("¥", "").replace("₹", "").trim()
        return cleaned.toDoubleOrNull()
    }

    private fun parseBoolean(raw: String?): Boolean {
        if (raw == null) return false
        val s = raw.trim().lowercase(Locale.ROOT)
        return s == "yes" || s == "true" || s == "1" || s == "y" || s == "t" || s == "active" || s == "enabled"
    }

    private fun parseHourMinute(timeStr: String?): Pair<Int, Int>? {
        if (timeStr.isNullOrBlank()) return null
        val parts = timeStr.trim().split(":")
        if (parts.size >= 2) {
            val h = parts[0].toIntOrNull() ?: return null
            val m = parts[1].toIntOrNull() ?: return null
            return h to m
        }
        return null
    }

    fun parseDate(raw: String?, userFormatPattern: String = "yyyy-MM-dd"): Long? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()

        // 1. Check numeric timestamp (ms or s)
        trimmed.toLongOrNull()?.let {
            if (it > 1000000000L) {
                return if (it < 100000000000L) it * 1000L else it
            }
        }

        // 2. Check Excel serial date number (e.g. 45000 to 55000 is years 2023 to 2050)
        trimmed.toDoubleOrNull()?.let { serial ->
            if (serial in 1000.0..90000.0) {
                val epochMs = ((serial - 25569.0) * 86400.0 * 1000.0).toLong()
                return epochMs
            }
        }

        // 3. String date formats
        val formats = linkedSetOf(
            userFormatPattern,
            "yyyy-MM-dd",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd",
            "MM/dd/yyyy",
            "dd/MM/yyyy",
            "dd.MM.yyyy",
            "yyyy.MM.dd",
            "MMM dd, yyyy",
            "MMMM dd, yyyy",
            "dd-MMM-yyyy",
            "dd MMM yyyy"
        )

        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault()).apply { isLenient = true }
                val d = sdf.parse(trimmed)
                if (d != null) return d.time
            } catch (_: Exception) {}
            try {
                val sdf = SimpleDateFormat(fmt, Locale.US).apply { isLenient = true }
                val d = sdf.parse(trimmed)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return null
    }
}
