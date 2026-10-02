package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.PackageItem
import com.example.data.ShippingCompany
import com.example.data.ShippingCompanyContact

data class TrackingOption(
    val title: String,
    val trackingNumber: String,
    val companyName: String,
    val url: String
)

object TrackingUtils {
    fun buildTrackingUrl(baseUrl: String, trackingNumber: String): String {
        val trimmedUrl = baseUrl.trim()
        val trimmedTrack = trackingNumber.trim()
        return when {
            trimmedUrl.contains("{tracking}", ignoreCase = true) ->
                trimmedUrl.replace("{tracking}", trimmedTrack, ignoreCase = true)
            trimmedUrl.contains("{tracking_number}", ignoreCase = true) ->
                trimmedUrl.replace("{tracking_number}", trimmedTrack, ignoreCase = true)
            trimmedUrl.contains("{0}") ->
                trimmedUrl.replace("{0}", trimmedTrack)
            trimmedUrl.endsWith("=") || trimmedUrl.endsWith("/") ->
                "$trimmedUrl$trimmedTrack"
            trimmedUrl.contains("dhl.com", ignoreCase = true) && !trimmedUrl.contains("AWB=", ignoreCase = true) -> {
                if (trimmedUrl.contains("?")) "$trimmedUrl&AWB=$trimmedTrack" else "$trimmedUrl?AWB=$trimmedTrack"
            }
            trimmedUrl.contains("fedex.com", ignoreCase = true) && !trimmedUrl.contains("trknbr=", ignoreCase = true) -> {
                if (trimmedUrl.contains("?")) "$trimmedUrl&trknbr=$trimmedTrack" else "$trimmedUrl?trknbr=$trimmedTrack"
            }
            trimmedUrl.contains("ups.com", ignoreCase = true) && !trimmedUrl.contains("tracknum=", ignoreCase = true) -> {
                if (trimmedUrl.contains("?")) "$trimmedUrl&tracknum=$trimmedTrack" else "$trimmedUrl?tracknum=$trimmedTrack"
            }
            trimmedUrl.endsWith("?") || trimmedUrl.endsWith("&") ->
                "$trimmedUrl$trimmedTrack"
            else ->
                "$trimmedUrl$trimmedTrack"
        }
    }

    fun getTrackingOptions(
        pkg: PackageItem?,
        companies: List<ShippingCompany>,
        contacts: List<ShippingCompanyContact>,
        defaultTrackingUrl: String? = null
    ): List<TrackingOption> {
        if (pkg == null) return emptyList()
        val options = mutableListOf<TrackingOption>()
        val defaultUrl = defaultTrackingUrl?.trim()?.takeIf { it.isNotBlank() }

        // 1. Store Tracking
        val storeTrack = pkg.storeTrackingNumber?.trim()
        if (!storeTrack.isNullOrEmpty()) {
            val companyName = pkg.storeShippingCompany?.trim() ?: ""
            val matchedCompany = companies.find { it.name.equals(companyName, ignoreCase = true) }
            val trackingBaseUrl = matchedCompany?.let { c ->
                contacts.find { it.shippingCompanyId == c.id && it.contactType.equals("Tracking URL", ignoreCase = true) }?.contactValue
            }?.trim()?.takeIf { it.isNotBlank() }

            val effectiveUrl = trackingBaseUrl ?: defaultUrl
            val finalUrl = if (!effectiveUrl.isNullOrEmpty()) {
                buildTrackingUrl(effectiveUrl, storeTrack)
            } else {
                "https://www.google.com/search?q=${Uri.encode(storeTrack)}"
            }

            val displayName = when {
                matchedCompany != null && trackingBaseUrl != null -> companyName
                matchedCompany != null -> "$companyName (Default)"
                defaultUrl != null -> "Default"
                else -> "Store Delivery"
            }

            options.add(
                TrackingOption(
                    title = "Store Tracking",
                    trackingNumber = storeTrack,
                    companyName = displayName,
                    url = finalUrl
                )
            )
        }

        // 2. Forwarder Tracking
        val fwdTrack = pkg.forwarderTrackingNumber?.trim()
        if (!fwdTrack.isNullOrEmpty()) {
            val companyName = pkg.forwarderShippingCompany?.trim() ?: ""
            val matchedCompany = companies.find { it.name.equals(companyName, ignoreCase = true) }
            val trackingBaseUrl = matchedCompany?.let { c ->
                contacts.find { it.shippingCompanyId == c.id && it.contactType.equals("Tracking URL", ignoreCase = true) }?.contactValue
            }?.trim()?.takeIf { it.isNotBlank() }

            val effectiveUrl = trackingBaseUrl ?: defaultUrl
            val finalUrl = if (!effectiveUrl.isNullOrEmpty()) {
                buildTrackingUrl(effectiveUrl, fwdTrack)
            } else {
                "https://www.google.com/search?q=${Uri.encode(fwdTrack)}"
            }

            val displayName = when {
                matchedCompany != null && trackingBaseUrl != null -> companyName
                matchedCompany != null -> "$companyName (Default)"
                defaultUrl != null -> "Default"
                else -> "Forwarder Delivery"
            }

            options.add(
                TrackingOption(
                    title = "Forwarder Tracking",
                    trackingNumber = fwdTrack,
                    companyName = displayName,
                    url = finalUrl
                )
            )
        }

        return options
    }

    fun openUrl(context: Context, url: String) {
        try {
            val fullUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
