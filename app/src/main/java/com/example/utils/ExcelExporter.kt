package com.example.utils

import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

object ExcelExporter {

    fun generateWorkbookXml(
        user: User,
        bookstores: List<Bookstore>,
        bookstoreContacts: List<BookstoreContact>,
        forwardingServices: List<ForwardingService>,
        forwardingServiceContacts: List<ForwardingServiceContact>,
        shippingCompanies: List<ShippingCompany>,
        shippingCompanyContacts: List<ShippingCompanyContact>,
        userAddresses: List<UserAddress>,
        subscriptions: List<SubscriptionWithBookstore>,
        subscriptionSkipMethods: List<SubscriptionSkipMethod>,
        subscriptionSkips: List<SubscriptionSkip>,
        scheduled: List<ScheduledWithDetails>,
        preorders: List<PreorderWithBookstore>,
        packages: List<PackageItem>
    ): String {
        val userDateFormatPattern = user.dateFormat.ifBlank { "yyyy-MM-dd" }
        val dateFormat = SimpleDateFormat(userDateFormatPattern, Locale.getDefault())

        val bookstoreMap = bookstores.associateBy { it.id }
        val forwardingMap = forwardingServices.associateBy { it.id }
        val shippingCompanyMap = shippingCompanies.associateBy { it.id }
        val addressMap = userAddresses.associateBy { it.id }
        val subscriptionMap = subscriptions.associateBy { it.subscription.id }
        val preorderMap = preorders.associateBy { it.preorder.id }
        val scheduledMap = scheduled.associateBy { it.scheduled.id }

        val xmlBuilder = StringBuilder()

        xmlBuilder.append("""<?xml version="1.0" encoding="UTF-8"?>
<?mso-application progid="Excel.Sheet"?>
<Workbook xmlns="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:o="urn:schemas-microsoft-com:office:office"
 xmlns:x="urn:schemas-microsoft-com:office:excel"
 xmlns:ss="urn:schemas-microsoft-com:office:spreadsheet"
 xmlns:html="http://www.w3.org/TR/REC-html40">
 <DocumentProperties xmlns="urn:schemas-microsoft-com:office:office">
  <Author>Bookish</Author>
  <Title>Bookish Export</Title>
 </DocumentProperties>
 <Styles>
  <Style ss:ID="Default" ss:Name="Normal">
   <Alignment ss:Vertical="Bottom"/>
   <Font ss:FontName="Calibri" x:Family="Swiss" ss:Size="11" ss:Color="#000000"/>
  </Style>
  <Style ss:ID="Header">
   <Font ss:FontName="Calibri" ss:Size="11" ss:Color="#1E293B" ss:Bold="1"/>
   <Interior ss:Color="#E2E8F0" ss:Pattern="Solid"/>
  </Style>
  <Style ss:ID="Title">
   <Font ss:FontName="Calibri" ss:Size="14" ss:Color="#0F172A" ss:Bold="1"/>
  </Style>
 </Styles>
""")

        // 1. Overview Sheet
        xmlBuilder.append(""" <Worksheet ss:Name="Overview">
  <Table>
   <Column ss:Width="200"/>
   <Column ss:Width="220"/>
   <Row>
    <Cell ss:StyleID="Title"><Data ss:Type="String">BOOKISH DATA EXPORT</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Export Date</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(dateFormat.format(Date()))}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">User Name</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(user.username)}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Profile Picture</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(user.profilePic ?: "")}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Preferred Currency</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(user.currency)}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Preferred Language</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(user.language)}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Theme Mode</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(user.themeMode)}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Theme Palette</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(user.themeCombo)}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Date Format</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(user.dateFormat)}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Display Amounts</Data></Cell>
    <Cell><Data ss:Type="String">${if (user.displayAmounts) "Yes" else "No"}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Country</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(user.country)}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Default Tracking URL</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(user.defaultTrackingUrl)}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Default Scheduled Sub Count</Data></Cell>
    <Cell><Data ss:Type="Number">${user.defaultScheduledSubCount}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Bookstores</Data></Cell>
    <Cell><Data ss:Type="Number">${bookstores.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Bookstore Contacts</Data></Cell>
    <Cell><Data ss:Type="Number">${bookstoreContacts.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Forwarding Services</Data></Cell>
    <Cell><Data ss:Type="Number">${forwardingServices.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Forwarding Service Contacts</Data></Cell>
    <Cell><Data ss:Type="Number">${forwardingServiceContacts.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Shipping Companies</Data></Cell>
    <Cell><Data ss:Type="Number">${shippingCompanies.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Shipping Company Contacts</Data></Cell>
    <Cell><Data ss:Type="Number">${shippingCompanyContacts.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total User Addresses</Data></Cell>
    <Cell><Data ss:Type="Number">${userAddresses.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Subscriptions</Data></Cell>
    <Cell><Data ss:Type="Number">${subscriptions.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Subscription Skip Methods</Data></Cell>
    <Cell><Data ss:Type="Number">${subscriptionSkipMethods.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Subscription Skips</Data></Cell>
    <Cell><Data ss:Type="Number">${subscriptionSkips.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Scheduled Deliveries</Data></Cell>
    <Cell><Data ss:Type="Number">${scheduled.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Preorders</Data></Cell>
    <Cell><Data ss:Type="Number">${preorders.size}</Data></Cell>
   </Row>
   <Row>
    <Cell><Data ss:Type="String">Total Packages</Data></Cell>
    <Cell><Data ss:Type="Number">${packages.size}</Data></Cell>
   </Row>
  </Table>
 </Worksheet>
""")

        // 2. Bookstores Sheet
        xmlBuilder.append(""" <Worksheet ss:Name="Bookstores">
  <Table>
   <Column ss:Width="80"/>
   <Column ss:Width="200"/>
   <Column ss:Width="260"/>
   <Column ss:Width="160"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Bookstore ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Name</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Website</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Profile Picture</Data></Cell>
   </Row>
""")
        for (b in bookstores) {
            xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${b.id}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(b.name)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(b.website)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(b.profilePic ?: "")}</Data></Cell>
   </Row>
""")
        }
        xmlBuilder.append("  </Table>\n </Worksheet>\n")

        // 3. Bookstore Contacts Sheet
        xmlBuilder.append(""" <Worksheet ss:Name="Bookstore Contacts">
  <Table>
   <Column ss:Width="80"/>
   <Column ss:Width="100"/>
   <Column ss:Width="180"/>
   <Column ss:Width="120"/>
   <Column ss:Width="250"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Contact ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Bookstore ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Bookstore Name</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Contact Type</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Contact Value</Data></Cell>
   </Row>
""")
        for (bc in bookstoreContacts) {
            val storeName = bookstoreMap[bc.bookstoreId]?.name ?: "Unknown"
            xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${bc.id}</Data></Cell>
    <Cell><Data ss:Type="Number">${bc.bookstoreId}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(storeName)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(bc.contactType)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(bc.contactValue)}</Data></Cell>
   </Row>
""")
        }
        xmlBuilder.append("  </Table>\n </Worksheet>\n")

        // 4. Forwarding Services Sheet
        xmlBuilder.append(""" <Worksheet ss:Name="Forwarding Services">
  <Table>
   <Column ss:Width="80"/>
   <Column ss:Width="200"/>
   <Column ss:Width="260"/>
   <Column ss:Width="160"/>
   <Column ss:Width="100"/>
   <Column ss:Width="120"/>
   <Column ss:Width="130"/>
   <Column ss:Width="100"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Service ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Name</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Website</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Profile Picture</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Storage Days</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Reminder Enabled</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Reminder D-Day Offset</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Reminder Time</Data></Cell>
   </Row>
""")
        for (fs in forwardingServices) {
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", fs.reminderHour, fs.reminderMinute)
            xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${fs.id}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(fs.name)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(fs.website)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(fs.profilePic ?: "")}</Data></Cell>
    <Cell><Data ss:Type="String">${fs.storageDays?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${if (fs.reminderEnabled) "Yes" else "No"}</Data></Cell>
    <Cell><Data ss:Type="Number">${fs.reminderDDayOffset}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(timeStr)}</Data></Cell>
   </Row>
""")
        }
        xmlBuilder.append("  </Table>\n </Worksheet>\n")

        // 5. Forwarding Service Contacts Sheet
        xmlBuilder.append(""" <Worksheet ss:Name="Forwarding Contacts">
  <Table>
   <Column ss:Width="80"/>
   <Column ss:Width="110"/>
   <Column ss:Width="180"/>
   <Column ss:Width="120"/>
   <Column ss:Width="250"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Contact ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Forwarding Service ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Forwarding Service Name</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Contact Type</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Contact Value</Data></Cell>
   </Row>
""")
        for (fsc in forwardingServiceContacts) {
            val fsName = forwardingMap[fsc.forwardingServiceId]?.name ?: "Unknown"
            xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${fsc.id}</Data></Cell>
    <Cell><Data ss:Type="Number">${fsc.forwardingServiceId}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(fsName)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(fsc.contactType)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(fsc.contactValue)}</Data></Cell>
   </Row>
""")
        }
        xmlBuilder.append("  </Table>\n </Worksheet>\n")

        // 6. Shipping Companies Sheet
        xmlBuilder.append(""" <Worksheet ss:Name="Shipping Companies">
  <Table>
   <Column ss:Width="80"/>
   <Column ss:Width="200"/>
   <Column ss:Width="260"/>
   <Column ss:Width="160"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Company ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Name</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Website</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Profile Picture</Data></Cell>
   </Row>
""")
        for (sc in shippingCompanies) {
            xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${sc.id}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sc.name)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sc.website)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sc.profilePic ?: "")}</Data></Cell>
   </Row>
""")
        }
        xmlBuilder.append("  </Table>\n </Worksheet>\n")

        // 7. Shipping Company Contacts Sheet
        xmlBuilder.append(""" <Worksheet ss:Name="Shipping Contacts">
  <Table>
   <Column ss:Width="80"/>
   <Column ss:Width="110"/>
   <Column ss:Width="180"/>
   <Column ss:Width="120"/>
   <Column ss:Width="250"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Contact ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Company ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Company Name</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Contact Type</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Contact Value</Data></Cell>
   </Row>
""")
        for (scc in shippingCompanyContacts) {
            val scName = shippingCompanyMap[scc.shippingCompanyId]?.name ?: "Unknown"
            xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${scc.id}</Data></Cell>
    <Cell><Data ss:Type="Number">${scc.shippingCompanyId}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(scName)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(scc.contactType)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(scc.contactValue)}</Data></Cell>
   </Row>
""")
        }
        xmlBuilder.append("  </Table>\n </Worksheet>\n")

        // 8. User Addresses Sheet
        xmlBuilder.append(""" <Worksheet ss:Name="Addresses">
  <Table>
   <Column ss:Width="80"/>
   <Column ss:Width="80"/>
   <Column ss:Width="180"/>
   <Column ss:Width="140"/>
   <Column ss:Width="120"/>
   <Column ss:Width="120"/>
   <Column ss:Width="90"/>
   <Column ss:Width="100"/>
   <Column ss:Width="120"/>
   <Column ss:Width="160"/>
   <Column ss:Width="80"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Address ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">User ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Street Address 1</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Street Address 2</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">City</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">State / Region</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Postal Code</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Country</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Forwarding Service ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Forwarding Service</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Is Default</Data></Cell>
   </Row>
""")
        for (addr in userAddresses) {
            val fsName = addr.forwardingServiceId?.let { forwardingMap[it]?.name } ?: ""
            xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${addr.id}</Data></Cell>
    <Cell><Data ss:Type="Number">${addr.userId}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(addr.streetAddress1)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(addr.streetAddress2)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(addr.city)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(addr.stateProvinceRegion)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(addr.postalCode)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(addr.country)}</Data></Cell>
    <Cell><Data ss:Type="String">${addr.forwardingServiceId?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(fsName)}</Data></Cell>
    <Cell><Data ss:Type="String">${if (addr.isDefault) "Yes" else "No"}</Data></Cell>
   </Row>
""")
        }
        xmlBuilder.append("  </Table>\n </Worksheet>\n")

        // 9. Subscriptions Sheet
        xmlBuilder.append(""" <Worksheet ss:Name="Subscriptions">
  <Table>
   <Column ss:Width="80"/>
   <Column ss:Width="160"/>
   <Column ss:Width="180"/>
   <Column ss:Width="90"/>
   <Column ss:Width="80"/>
   <Column ss:Width="60"/>
   <Column ss:Width="80"/>
   <Column ss:Width="80"/>
   <Column ss:Width="80"/>
   <Column ss:Width="80"/>
   <Column ss:Width="90"/>
   <Column ss:Width="90"/>
   <Column ss:Width="100"/>
   <Column ss:Width="100"/>
   <Column ss:Width="100"/>
   <Column ss:Width="80"/>
   <Column ss:Width="100"/>
   <Column ss:Width="100"/>
   <Column ss:Width="120"/>
   <Column ss:Width="100"/>
   <Column ss:Width="110"/>
   <Column ss:Width="90"/>
   <Column ss:Width="90"/>
   <Column ss:Width="220"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Subscription ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Bookstore ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Bookstore</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Title</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Status</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Price</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Currency</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Base Price</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Discount</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Shipping</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Tax</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Fwd Shipping</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Fwd Tax</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Due Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Start Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Finish Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Alert Days</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Frequency</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Reminder</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Reminder Offset</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Reminder Time</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Skip Type</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Max Skips</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Skip Months</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Skip Method</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Skip Link</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Skip Text</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Picture Path</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Shipping Address ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Shipping Address</Data></Cell>
   </Row>
""")
        for (s in subscriptions) {
            val sub = s.subscription
            val bookstoreName = s.bookstore?.name ?: "Unknown"
            val dueDateStr = if (sub.dueDate > 0) dateFormat.format(Date(sub.dueDate)) else ""
            val startDateStr = if (sub.startDate > 0) dateFormat.format(Date(sub.startDate)) else ""
            val finishDateStr = if (sub.finishDate > 0) dateFormat.format(Date(sub.finishDate)) else ""
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", sub.reminderHour, sub.reminderMinute)
            val shippingAddrStr = sub.shippingAddressId?.let { addressMap[it]?.toFormattedString() } ?: ""

            xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${sub.id}</Data></Cell>
    <Cell><Data ss:Type="Number">${sub.bookstoreId}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(bookstoreName)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sub.title)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sub.status)}</Data></Cell>
    <Cell><Data ss:Type="Number">${sub.price}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sub.currency ?: "")}</Data></Cell>
    <Cell><Data ss:Type="String">${sub.basePrice?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${sub.discountedAmount?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${sub.shippingPrice?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${sub.taxPrice?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${sub.forwardShippingPrice?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${sub.forwardTaxPrice?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(dueDateStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(startDateStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(finishDateStr)}</Data></Cell>
    <Cell><Data ss:Type="Number">${sub.notificationAlertDays ?: 0}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sub.frequency)}</Data></Cell>
    <Cell><Data ss:Type="String">${if (sub.reminderEnabled) "Yes" else "No"}</Data></Cell>
    <Cell><Data ss:Type="Number">${sub.reminderDDayOffset}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(timeStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sub.skipType ?: "None")}</Data></Cell>
    <Cell><Data ss:Type="String">${sub.numberOfSkips?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${sub.numberOfMonths?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sub.skipMethod ?: "")}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sub.skipLink ?: "")}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sub.skipText ?: "")}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sub.picturePath ?: "")}</Data></Cell>
    <Cell><Data ss:Type="String">${sub.shippingAddressId?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(shippingAddrStr)}</Data></Cell>
   </Row>
""")
        }
        xmlBuilder.append("  </Table>\n </Worksheet>\n")

        // 10. Subscription Skip Methods Sheet
        xmlBuilder.append(""" <Worksheet ss:Name="Skip Methods">
  <Table>
   <Column ss:Width="80"/>
   <Column ss:Width="100"/>
   <Column ss:Width="180"/>
   <Column ss:Width="60"/>
   <Column ss:Width="120"/>
   <Column ss:Width="220"/>
   <Column ss:Width="250"/>
   <Column ss:Width="120"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Method ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Subscription ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Subscription Title</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Order</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Method Type</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Method Value</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Message Text Template</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Consecutive Skips</Data></Cell>
   </Row>
""")
        for (sm in subscriptionSkipMethods) {
            val subTitle = subscriptionMap[sm.subscriptionTypeId]?.subscription?.title ?: "Unknown"
            xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${sm.id}</Data></Cell>
    <Cell><Data ss:Type="Number">${sm.subscriptionTypeId}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(subTitle)}</Data></Cell>
    <Cell><Data ss:Type="Number">${sm.skipMethodOrder}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sm.skipMethodType)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sm.skipMethodValue)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sm.skipMethodText)}</Data></Cell>
    <Cell><Data ss:Type="Number">${sm.consecutiveSkips}</Data></Cell>
   </Row>
""")
        }
        xmlBuilder.append("  </Table>\n </Worksheet>\n")

        // 11. Subscription Skips Sheet
        xmlBuilder.append(""" <Worksheet ss:Name="Subscription Skips">
  <Table>
   <Column ss:Width="80"/>
   <Column ss:Width="100"/>
   <Column ss:Width="180"/>
   <Column ss:Width="140"/>
   <Column ss:Width="90"/>
   <Column ss:Width="90"/>
   <Column ss:Width="100"/>
   <Column ss:Width="100"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Skip ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Subscription ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Subscription Title</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Skip Type</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Total Skips</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Skips Left</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Start Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">End Date</Data></Cell>
   </Row>
""")
        for (sk in subscriptionSkips) {
            val subTitle = subscriptionMap[sk.subscriptionTypeId]?.subscription?.title ?: "Unknown"
            val startStr = sk.skipStartDate?.let { if (it > 0) dateFormat.format(Date(it)) else "" } ?: ""
            val endStr = sk.skipEndDate?.let { if (it > 0) dateFormat.format(Date(it)) else "" } ?: ""
            xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${sk.id}</Data></Cell>
    <Cell><Data ss:Type="Number">${sk.subscriptionTypeId}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(subTitle)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sk.subscriptionSkipType)}</Data></Cell>
    <Cell><Data ss:Type="String">${sk.numberOfSkips?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${sk.skipsLeft?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(startStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(endStr)}</Data></Cell>
   </Row>
""")
        }
        xmlBuilder.append("  </Table>\n </Worksheet>\n")

        // 12. Scheduled Subscriptions Sheet
        xmlBuilder.append(""" <Worksheet ss:Name="Scheduled Subscriptions">
  <Table>
   <Column ss:Width="80"/>
   <Column ss:Width="160"/>
   <Column ss:Width="180"/>
   <Column ss:Width="160"/>
   <Column ss:Width="220"/>
   <Column ss:Width="100"/>
   <Column ss:Width="90"/>
   <Column ss:Width="80"/>
   <Column ss:Width="60"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Scheduled ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Subscription ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Subscription Title</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Book Title</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Author</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Description</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Due Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Status</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Is Skipped</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Rating</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Picture Path</Data></Cell>
   </Row>
""")
        for (sch in scheduled) {
            val sc = sch.scheduled
            val subTitle = sch.subscriptionType?.title ?: "Unknown"
            val dueDateStr = if (sc.dueDate > 0) dateFormat.format(Date(sc.dueDate)) else ""
            xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${sc.id}</Data></Cell>
    <Cell><Data ss:Type="Number">${sc.subscriptionTypeId}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(subTitle)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sc.bookTitle)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sc.bookAuthor)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sc.description)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(dueDateStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sc.status)}</Data></Cell>
    <Cell><Data ss:Type="String">${if (sc.isSkipped) "Yes" else "No"}</Data></Cell>
    <Cell><Data ss:Type="Number">${sc.rating}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(sc.picturePath ?: "")}</Data></Cell>
   </Row>
""")
        }
        xmlBuilder.append("  </Table>\n </Worksheet>\n")

        // 13. Preorders Sheet
        xmlBuilder.append(""" <Worksheet ss:Name="Preorders">
  <Table>
   <Column ss:Width="80"/>
   <Column ss:Width="80"/>
   <Column ss:Width="160"/>
   <Column ss:Width="180"/>
   <Column ss:Width="160"/>
   <Column ss:Width="220"/>
   <Column ss:Width="80"/>
   <Column ss:Width="60"/>
   <Column ss:Width="80"/>
   <Column ss:Width="80"/>
   <Column ss:Width="80"/>
   <Column ss:Width="80"/>
   <Column ss:Width="90"/>
   <Column ss:Width="90"/>
   <Column ss:Width="100"/>
   <Column ss:Width="100"/>
   <Column ss:Width="90"/>
   <Column ss:Width="90"/>
   <Column ss:Width="120"/>
   <Column ss:Width="100"/>
   <Column ss:Width="60"/>
   <Column ss:Width="100"/>
   <Column ss:Width="220"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Preorder ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Bookstore ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Bookstore</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Book Title</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Author</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Description</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Price</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Currency</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Base Price</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Discount</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Shipping</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Tax</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Fwd Shipping</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Fwd Tax</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Sale Start Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Sale End Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Status</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Reminder</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Reminder Offset</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Reminder Time</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Rating</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Picture Path</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Shipping Address ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Shipping Address</Data></Cell>
   </Row>
""")
        for (p in preorders) {
            val pr = p.preorder
            val storeName = p.bookstore?.name ?: "Unknown"
            val startDateStr = if (pr.rangedSaleDateStart > 0) dateFormat.format(Date(pr.rangedSaleDateStart)) else ""
            val endDateStr = if (pr.rangedSaleDateEnd > 0) dateFormat.format(Date(pr.rangedSaleDateEnd)) else ""
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", pr.reminderHour, pr.reminderMinute)
            val shippingAddrStr = pr.shippingAddressId?.let { addressMap[it]?.toFormattedString() } ?: ""

            xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${pr.id}</Data></Cell>
    <Cell><Data ss:Type="Number">${pr.bookstoreId}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(storeName)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pr.bookTitle)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pr.bookAuthor)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pr.description)}</Data></Cell>
    <Cell><Data ss:Type="Number">${pr.price}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pr.currency ?: "")}</Data></Cell>
    <Cell><Data ss:Type="String">${pr.basePrice?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${pr.discountedAmount?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${pr.shippingPrice?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${pr.taxPrice?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${pr.forwardShippingPrice?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${pr.forwardTaxPrice?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(startDateStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(endDateStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pr.status)}</Data></Cell>
    <Cell><Data ss:Type="String">${if (pr.reminderEnabled) "Yes" else "No"}</Data></Cell>
    <Cell><Data ss:Type="Number">${pr.reminderDDayOffset}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(timeStr)}</Data></Cell>
    <Cell><Data ss:Type="Number">${pr.rating}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pr.picturePath ?: "")}</Data></Cell>
    <Cell><Data ss:Type="String">${pr.shippingAddressId?.toString() ?: ""}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(shippingAddrStr)}</Data></Cell>
   </Row>
""")
        }
        xmlBuilder.append("  </Table>\n </Worksheet>\n")

        // 14. Packages Sheet
        xmlBuilder.append(""" <Worksheet ss:Name="Packages">
  <Table>
   <Column ss:Width="80"/>
   <Column ss:Width="110"/>
   <Column ss:Width="80"/>
   <Column ss:Width="180"/>
   <Column ss:Width="100"/>
   <Column ss:Width="110"/>
   <Column ss:Width="140"/>
   <Column ss:Width="150"/>
   <Column ss:Width="120"/>
   <Column ss:Width="140"/>
   <Column ss:Width="150"/>
   <Column ss:Width="120"/>
   <Column ss:Width="100"/>
   <Row>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Package ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Origin Table</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Origin ID</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Item Title</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Purchase Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Store Ship Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Store Shipping Co</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Store Tracking #</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Fwd Received Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Fwd Shipping Co</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Fwd Tracking #</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Fwd Shipped Date</Data></Cell>
    <Cell ss:StyleID="Header"><Data ss:Type="String">Received Date</Data></Cell>
   </Row>
""")
        for (pkg in packages) {
            val itemTitle = if (pkg.originTable == "preorders") {
                preorderMap[pkg.originId]?.preorder?.bookTitle ?: "Preorder #${pkg.originId}"
            } else {
                scheduledMap[pkg.originId]?.scheduled?.bookTitle
                    ?: scheduledMap[pkg.originId]?.subscriptionType?.title
                    ?: "Subscription Item #${pkg.originId}"
            }
            val purchaseDateStr = pkg.purchaseDate?.let { if (it > 0) dateFormat.format(Date(it)) else "" } ?: ""
            val storeShipDateStr = pkg.storeShippingDate?.let { if (it > 0) dateFormat.format(Date(it)) else "" } ?: ""
            val fwdReceivedDateStr = pkg.forwarderReceivedDate?.let { if (it > 0) dateFormat.format(Date(it)) else "" } ?: ""
            val fwdShippedDateStr = pkg.forwarderShippedDate?.let { if (it > 0) dateFormat.format(Date(it)) else "" } ?: ""
            val receivedDateStr = pkg.receivedDate?.let { if (it > 0) dateFormat.format(Date(it)) else "" } ?: ""

            xmlBuilder.append("""   <Row>
    <Cell><Data ss:Type="Number">${pkg.id}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pkg.originTable)}</Data></Cell>
    <Cell><Data ss:Type="Number">${pkg.originId}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(itemTitle)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(purchaseDateStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(storeShipDateStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pkg.storeShippingCompany ?: "")}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pkg.storeTrackingNumber ?: "")}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(fwdReceivedDateStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pkg.forwarderShippingCompany ?: "")}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(pkg.forwarderTrackingNumber ?: "")}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(fwdShippedDateStr)}</Data></Cell>
    <Cell><Data ss:Type="String">${escapeXml(receivedDateStr)}</Data></Cell>
   </Row>
""")
        }
        xmlBuilder.append("  </Table>\n </Worksheet>\n")
        xmlBuilder.append("</Workbook>")

        return xmlBuilder.toString()
    }

    private fun escapeXml(value: String?): String {
        if (value == null) return ""
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
