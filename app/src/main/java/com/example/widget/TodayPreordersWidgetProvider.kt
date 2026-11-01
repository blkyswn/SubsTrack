package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.BookishDatabase
import com.example.data.Bookstore
import com.example.data.Preorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class TodayPreordersWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PREV_PREORDER = "com.example.ACTION_PREV_PREORDER"
        const val ACTION_NEXT_PREORDER = "com.example.ACTION_NEXT_PREORDER"
        private const val PREFS_NAME = "today_preorders_widget_prefs"
        private const val KEY_INDEX_PREFIX = "preorder_index_"

        private fun getPrefs(context: Context) =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun triggerUpdate(context: Context) {
            try {
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, TodayPreordersWidgetProvider::class.java)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
                if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                    val provider = TodayPreordersWidgetProvider()
                    provider.updateWidgetsInternal(context, appWidgetManager, appWidgetIds, null)
                }
            } catch (e: Exception) {
                android.util.Log.e("TodayPreordersWidget", "Error triggering widget update", e)
            }
        }

        fun calculateItemHeightDp(context: Context, options: Bundle?): Int {
            val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
            return if (minHeight > 0) minHeight else 40
        }

        private fun createRoundedBitmap(src: Bitmap, width: Int, height: Int, radius: Float): Bitmap {
            val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())

            val srcW = src.width
            val srcH = src.height
            val srcRect = if (srcW > srcH) {
                val diff = (srcW - srcH) / 2
                Rect(diff, 0, diff + srcH, srcH)
            } else {
                val diff = (srcH - srcW) / 2
                Rect(0, diff, srcW, diff + srcW)
            }

            val path = Path().apply {
                addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            canvas.clipPath(path)
            canvas.drawBitmap(src, srcRect, rect, paint)
            return output
        }

        private fun createSliderDotsBitmap(
            context: Context,
            totalCount: Int,
            selectedIndex: Int,
            primaryColor: Int,
            unselectedColor: Int
        ): Bitmap {
            val density = context.resources.displayMetrics.density
            val widthPx = (4 * density).toInt().coerceAtLeast(1)
            val dotSpacingPx = (4 * density).toInt().coerceAtLeast(1)
            val radiusPx = 2 * density

            val heights = (0 until totalCount).map { i ->
                if (i == selectedIndex) (18 * density).toInt() else (5 * density).toInt()
            }
            val totalHeightPx = heights.sum() + ((totalCount - 1) * dotSpacingPx)

            val bitmap = Bitmap.createBitmap(widthPx, totalHeightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            var currentY = 0f
            for (i in 0 until totalCount) {
                val h = heights[i].toFloat()
                val isSelected = i == selectedIndex
                paint.color = if (isSelected) primaryColor else unselectedColor
                val rect = RectF(0f, currentY, widthPx.toFloat(), currentY + h)
                canvas.drawRoundRect(rect, radiusPx, radiusPx, paint)
                currentY += h + dotSpacingPx
            }
            return bitmap
        }

        private fun decodeScaledBitmap(context: Context, path: String, maxDim: Int): Bitmap? {
            return try {
                val file = File(path)
                val stream = if (file.exists()) file.inputStream()
                else if (path.startsWith("content://") || path.startsWith("file://")) {
                    context.contentResolver.openInputStream(Uri.parse(path))
                } else null

                stream?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                    var sample = 1
                    while ((opts.outWidth / sample) > maxDim || (opts.outHeight / sample) > maxDim) {
                        sample *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                }
            } catch (_: Exception) {
                null
            }
        }

        fun populatePreorderCard(
            context: Context,
            views: RemoteViews,
            item: Preorder,
            bookstores: List<Bookstore>,
            currency: String,
            themeColors: WidgetThemeColors,
            position: Int = 0,
            totalCount: Int = 1,
            isOneRow: Boolean = false
        ) {
            val store = bookstores.find { it.id == item.bookstoreId }
            val isDark = WidgetThemeUtils.isDarkTheme(context, null)

            // Status accent color matching home screen today's preorders exactly
            val curStatus = item.status.lowercase(Locale.getDefault()).trim()
            val accentColor = when (curStatus) {
                "upcoming" -> Color.parseColor("#64748B")
                "released" -> Color.parseColor("#F97316")
                "wishlist" -> Color.parseColor("#EC4899")
                "preordered" -> Color.parseColor("#2563EB")
                "forwarded" -> Color.parseColor("#0EA5E9")
                "in suite" -> Color.parseColor("#D97706")
                "shipped" -> Color.parseColor("#8B5CF6")
                "received" -> Color.parseColor("#10B981")
                else -> Color.parseColor("#64748B")
            }

            val statusContainerColor = when (curStatus) {
                "upcoming" -> if (isDark) Color.parseColor("#334155") else Color.parseColor("#F1F5F9")
                "released" -> if (isDark) Color.parseColor("#7C2D12") else Color.parseColor("#FFEDD5")
                "wishlist" -> if (isDark) Color.parseColor("#831843") else Color.parseColor("#FCE7F3")
                "preordered" -> if (isDark) Color.parseColor("#1E3A8A") else Color.parseColor("#DBEAFE")
                "forwarded" -> if (isDark) Color.parseColor("#0C4A6E") else Color.parseColor("#E0F2FE")
                "in suite" -> if (isDark) Color.parseColor("#78350F") else Color.parseColor("#FEF3C7")
                "shipped" -> if (isDark) Color.parseColor("#4C1D95") else Color.parseColor("#F3E8FF")
                "received" -> if (isDark) Color.parseColor("#064E3B") else Color.parseColor("#D1FAE5")
                else -> themeColors.surfaceVariant
            }

            val statusOnContainerColor = when (curStatus) {
                "upcoming" -> if (isDark) Color.parseColor("#F1F5F9") else Color.parseColor("#334155")
                "released" -> if (isDark) Color.parseColor("#FFEDD5") else Color.parseColor("#9A3412")
                "wishlist" -> if (isDark) Color.parseColor("#FCE7F3") else Color.parseColor("#9D174D")
                "preordered" -> if (isDark) Color.parseColor("#DBEAFE") else Color.parseColor("#1E40AF")
                "forwarded" -> if (isDark) Color.parseColor("#E0F2FE") else Color.parseColor("#0369A1")
                "in suite" -> if (isDark) Color.parseColor("#FEF3C7") else Color.parseColor("#92400E")
                "shipped" -> if (isDark) Color.parseColor("#F3E8FF") else Color.parseColor("#5B21B6")
                "received" -> if (isDark) Color.parseColor("#D1FAE5") else Color.parseColor("#065F46")
                else -> themeColors.onSurfaceVariant
            }

            val statusIconRes = when (curStatus) {
                "upcoming", "preordered" -> R.drawable.ic_status_shopping_bag
                "forwarded" -> R.drawable.ic_status_alt_route
                "in suite" -> R.drawable.ic_status_warehouse
                "released" -> R.drawable.ic_status_new_releases
                "wishlist" -> R.drawable.ic_status_favorite
                "shipped" -> R.drawable.ic_status_local_shipping
                "received" -> R.drawable.ic_status_check_circle
                else -> R.drawable.ic_status_shopping_bag
            }

            // 1. Preorder Card Background with 20dp corners, status-tinted fill, and 1dp border
            val cardBgRes = when (curStatus) {
                "upcoming" -> R.drawable.widget_card_bg_upcoming
                "released" -> R.drawable.widget_card_bg_released
                "wishlist" -> R.drawable.widget_card_bg_wishlist
                "preordered" -> R.drawable.widget_card_bg_preordered
                "forwarded" -> R.drawable.widget_card_bg_forwarded
                "in suite" -> R.drawable.widget_card_bg_in_suite
                "shipped" -> R.drawable.widget_card_bg_shipped
                "received" -> R.drawable.widget_card_bg_received
                else -> R.drawable.widget_card_bg_preordered
            }
            views.setImageViewResource(R.id.widget_preorder_item_bg, cardBgRes)

            // 2. Left Slider Indicator Dots (Matching home screen slider dots outside card)
            if (totalCount > 1) {
                views.setViewVisibility(R.id.widget_preorder_slider_dots_img, View.VISIBLE)
                val selectedIndex = ((position % totalCount) + totalCount) % totalCount
                val unselectedColor = Color.argb(89, Color.red(themeColors.onSurfaceVariant), Color.green(themeColors.onSurfaceVariant), Color.blue(themeColors.onSurfaceVariant))
                val dotsBitmap = createSliderDotsBitmap(context, totalCount, selectedIndex, themeColors.primary, unselectedColor)
                views.setImageViewBitmap(R.id.widget_preorder_slider_dots_img, dotsBitmap)
            } else {
                views.setViewVisibility(R.id.widget_preorder_slider_dots_img, View.GONE)
            }

            // 3. Left Indicator Bar (Always visible inside card, matching home screen)
            views.setViewVisibility(R.id.widget_preorder_bar, View.VISIBLE)
            views.setInt(R.id.widget_preorder_bar, "setColorFilter", accentColor)

            // 4. Cover Thumbnail Box (Rounded 10dp with accent-tinted placeholder)
            views.setInt(R.id.widget_preorder_thumb_bg, "setColorFilter", accentColor)

            var hasBitmap = false
            val imageCandidate = item.picturePath?.takeIf { it.isNotBlank() }
                ?: store?.profilePic?.takeIf { it.isNotBlank() && it != "ic_launcher_foreground" }

            if (!imageCandidate.isNullOrBlank()) {
                val rawBitmap = decodeScaledBitmap(context, imageCandidate, 100)
                if (rawBitmap != null) {
                    val rounded = createRoundedBitmap(rawBitmap, 100, 100, 16f)
                    views.setImageViewBitmap(R.id.widget_preorder_thumb_img, rounded)
                    views.setViewVisibility(R.id.widget_preorder_thumb_img, View.VISIBLE)
                    views.setViewVisibility(R.id.widget_preorder_thumb_initial, View.GONE)
                    hasBitmap = true
                }
            }

            if (!hasBitmap) {
                views.setViewVisibility(R.id.widget_preorder_thumb_img, View.GONE)
                views.setViewVisibility(R.id.widget_preorder_thumb_initial, View.VISIBLE)
                val storeName = store?.name?.takeIf { it.isNotBlank() } ?: item.bookTitle
                val initial = (storeName.firstOrNull { it.isLetterOrDigit() } ?: 'P').uppercaseChar().toString()
                views.setTextViewText(R.id.widget_preorder_thumb_initial, initial)
                views.setTextColor(R.id.widget_preorder_thumb_initial, accentColor)
            }

            // 5. Middle Column Info: Always showcase book title, book author, and bookstore one underneath the other
            views.setTextViewText(R.id.widget_preorder_title, item.bookTitle)
            views.setTextColor(R.id.widget_preorder_title, themeColors.onSurface)

            val authorText = if (!item.bookAuthor.isNullOrBlank()) "By ${item.bookAuthor}" else "By Unknown Author"
            views.setTextViewText(R.id.widget_preorder_author, authorText)
            views.setTextColor(R.id.widget_preorder_author, themeColors.onSurfaceVariant)
            views.setViewVisibility(R.id.widget_preorder_author, View.VISIBLE)

            val storeName = if (store != null && store.name.isNotBlank()) store.name else "Unknown Bookstore"
            views.setTextViewText(R.id.widget_preorder_bookstore, storeName)
            views.setTextColor(R.id.widget_preorder_bookstore, themeColors.primary)
            views.setViewVisibility(R.id.widget_preorder_bookstore, View.VISIBLE)

            // Status Badge Pill
            views.setTextViewText(R.id.widget_preorder_status, item.status)
            views.setTextColor(R.id.widget_preorder_status, statusOnContainerColor)
            views.setImageViewResource(R.id.widget_preorder_status_icon, statusIconRes)
            views.setInt(R.id.widget_preorder_status_icon, "setColorFilter", statusOnContainerColor)
            views.setInt(R.id.widget_preorder_status_pill_bg, "setColorFilter", statusContainerColor)

            // Price & Dot
            val priceFormatted = "$currency${String.format(Locale.getDefault(), "%.2f", item.price)}"
            views.setTextViewText(R.id.widget_preorder_price, priceFormatted)
            views.setTextColor(R.id.widget_preorder_price, themeColors.onSurface)
            views.setTextColor(R.id.widget_preorder_dot_separator, themeColors.onSurfaceVariant)

            // 6. Right Column: Date Card Box (10dp rounded corners, 8% fill, 20% border matching Home screen)
            val dateBoxRes = when (curStatus) {
                "upcoming" -> R.drawable.widget_date_box_upcoming
                "released" -> R.drawable.widget_date_box_released
                "wishlist" -> R.drawable.widget_date_box_wishlist
                "preordered" -> R.drawable.widget_date_box_preordered
                "forwarded" -> R.drawable.widget_date_box_forwarded
                "in suite" -> R.drawable.widget_date_box_in_suite
                "shipped" -> R.drawable.widget_date_box_shipped
                "received" -> R.drawable.widget_date_box_received
                else -> R.drawable.widget_date_box_preordered
            }
            views.setImageViewResource(R.id.widget_preorder_date_card_bg, dateBoxRes)

            val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
            val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateObj = Date(item.rangedSaleDateStart)

            views.setTextViewText(R.id.widget_preorder_date_month, monthFormat.format(dateObj).uppercase(Locale.getDefault()))
            views.setTextColor(R.id.widget_preorder_date_month, accentColor)

            views.setTextViewText(R.id.widget_preorder_date_day, dayFormat.format(dateObj))
            views.setTextColor(R.id.widget_preorder_date_day, themeColors.onSurface)

            if (isOneRow) {
                views.setViewVisibility(R.id.widget_preorder_date_time, View.GONE)
            } else {
                views.setTextViewText(R.id.widget_preorder_date_time, timeFormat.format(dateObj))
                views.setTextColor(R.id.widget_preorder_date_time, accentColor)
                views.setViewVisibility(R.id.widget_preorder_date_time, View.VISIBLE)
            }

            // Ensure ample breathing room between borders of the gray box and the content inside it
            val density = context.resources.displayMetrics.density
            val hPad = (16 * density).toInt()
            val vPad = if (isOneRow) (6 * density).toInt() else (12 * density).toInt()
            views.setViewPadding(R.id.widget_preorder_card_content, hPad, vPad, hPad, vPad)

            val dateHPad = (6 * density).toInt()
            val dateVPad = if (isOneRow) (4 * density).toInt() else (6 * density).toInt()
            views.setViewPadding(R.id.widget_preorder_date_card_content, dateHPad, dateVPad, dateHPad, dateVPad)
        }

        fun buildPreorderItemViews(
            context: Context,
            item: Preorder,
            bookstores: List<Bookstore>,
            currency: String,
            themeColors: WidgetThemeColors,
            position: Int = 0,
            totalCount: Int = 1,
            itemHeightDp: Int = 110,
            isOneRow: Boolean = false
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.today_preorders_widget)
            views.setViewVisibility(R.id.today_preorders_empty_container, View.GONE)
            views.setViewVisibility(R.id.widget_preorder_item_root, View.VISIBLE)
            populatePreorderCard(context, views, item, bookstores, currency, themeColors, position, totalCount, isOneRow)
            return views
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        if (appWidgetIds.isEmpty()) return
        val pendingResult = try {
            goAsync()
        } catch (_: Exception) {
            null
        }
        updateWidgetsInternal(context, appWidgetManager, appWidgetIds, pendingResult)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        triggerUpdate(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle?
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        val pendingResult = try {
            goAsync()
        } catch (_: Exception) {
            null
        }
        updateWidgetsInternal(context, appWidgetManager, intArrayOf(appWidgetId), pendingResult)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val prefs = getPrefs(context)

        when (action) {
            ACTION_NEXT_PREORDER -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val curIdx = prefs.getInt(KEY_INDEX_PREFIX + appWidgetId, 0)
                    prefs.edit().putInt(KEY_INDEX_PREFIX + appWidgetId, curIdx + 1).apply()
                } else {
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    val componentName = ComponentName(context, TodayPreordersWidgetProvider::class.java)
                    val ids = appWidgetManager.getAppWidgetIds(componentName) ?: IntArray(0)
                    val editor = prefs.edit()
                    for (id in ids) {
                        val curIdx = prefs.getInt(KEY_INDEX_PREFIX + id, 0)
                        editor.putInt(KEY_INDEX_PREFIX + id, curIdx + 1)
                    }
                    editor.apply()
                }
                triggerUpdate(context)
            }
            ACTION_PREV_PREORDER -> {
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    val curIdx = prefs.getInt(KEY_INDEX_PREFIX + appWidgetId, 0)
                    prefs.edit().putInt(KEY_INDEX_PREFIX + appWidgetId, (curIdx - 1).coerceAtLeast(0)).apply()
                }
                triggerUpdate(context)
            }
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> {
                triggerUpdate(context)
            }
        }
    }

    fun updateWidgetsInternal(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
        pendingResult: PendingResult?
    ) {
        if (appWidgetIds.isEmpty()) {
            pendingResult?.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = BookishDatabase.getDatabase(context)
                val user = db.userDao().getUserDirect() ?: db.userDao().getUser().firstOrNull()
                val bookstores = db.bookstoreDao().getAllBookstoresList()
                val allPreorders = db.preorderDao().getAllPreordersList()

                val now = System.currentTimeMillis()
                val getDayStart = { ms: Long ->
                    Calendar.getInstance().apply {
                        timeInMillis = ms
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                }
                val todayStart = getDayStart(now)
                val dateFormatKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayKey = dateFormatKey.format(Date(now))

                val todayPreorders = allPreorders.filter { item ->
                    val s = getDayStart(item.rangedSaleDateStart)
                    val e = getDayStart(
                        if (item.rangedSaleDateEnd > item.rangedSaleDateStart) item.rangedSaleDateEnd
                        else item.rangedSaleDateStart
                    )
                    val isToday = (todayStart in s..e) || (dateFormatKey.format(Date(item.rangedSaleDateStart)) == todayKey)
                    val status = item.status.trim()
                    val isAllowedStatus = status.equals("Upcoming", ignoreCase = true) ||
                            status.equals("Released", ignoreCase = true)
                    isToday && isAllowedStatus
                }.sortedBy { it.rangedSaleDateStart }

                val themeColors = WidgetThemeUtils.getThemeColors(context, user)
                val currency = user?.currency ?: "$"
                val prefs = getPrefs(context)

                for (appWidgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.today_preorders_widget)

                    if (todayPreorders.isEmpty()) {
                        views.setViewVisibility(R.id.today_preorders_empty_container, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_preorder_item_root, View.GONE)

                        views.setInt(R.id.today_preorders_empty_bg, "setColorFilter", themeColors.surfaceVariant)
                        views.setTextColor(R.id.today_preorders_empty_text, themeColors.onSurfaceVariant)
                        val emptyIconColor = Color.argb(128, Color.red(themeColors.primary), Color.green(themeColors.primary), Color.blue(themeColors.primary))
                        views.setInt(R.id.today_preorders_empty_icon, "setColorFilter", emptyIconColor)

                        // Open preorders in app on empty click
                        val clickIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("NAV_TARGET", "preorders")
                        }
                        val emptyPendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId,
                            clickIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.today_preorders_empty, emptyPendingIntent)
                    } else {
                        views.setViewVisibility(R.id.today_preorders_empty_container, View.GONE)
                        views.setViewVisibility(R.id.widget_preorder_item_root, View.VISIBLE)

                        val rawIdx = prefs.getInt(KEY_INDEX_PREFIX + appWidgetId, 0)
                        val safeIdx = ((rawIdx % todayPreorders.size) + todayPreorders.size) % todayPreorders.size
                        val currentItem = todayPreorders[safeIdx]

                        val options = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                            appWidgetManager.getAppWidgetOptions(appWidgetId)
                        } else null
                        val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
                        val isOneRow = minHeight in 1..75

                        // Populate card views directly into the widget
                        populatePreorderCard(
                            context = context,
                            views = views,
                            item = currentItem,
                            bookstores = bookstores,
                            currency = currency,
                            themeColors = themeColors,
                            position = safeIdx,
                            totalCount = todayPreorders.size,
                            isOneRow = isOneRow
                        )

                        // PendingIntent on card to open MainActivity with this preorder
                        val itemClickIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("NAV_TARGET", "preorders")
                            putExtra("preorder_id", currentItem.id)
                        }
                        val itemPendingIntent = PendingIntent.getActivity(
                            context,
                            appWidgetId + 2000,
                            itemClickIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_preorder_card_container, itemPendingIntent)

                        // When multiple preorders exist, tapping the slider dots area advances to the next preorder
                        if (todayPreorders.size > 1) {
                            val nextIntent = Intent(context, TodayPreordersWidgetProvider::class.java).apply {
                                action = ACTION_NEXT_PREORDER
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            val nextPendingIntent = PendingIntent.getBroadcast(
                                context,
                                appWidgetId + 3000,
                                nextIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            views.setOnClickPendingIntent(R.id.widget_preorder_dots_touch_area, nextPendingIntent)
                        }
                    }

                    // Update AppWidget directly
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult?.finish()
            }
        }
    }
}
