package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.BookishDatabase
import com.example.data.Bookstore
import com.example.data.Preorder
import com.example.data.User
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.*

class TodayPreordersWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        return TodayPreordersViewsFactory(applicationContext, appWidgetId)
    }
}

class TodayPreordersViewsFactory(
    private val context: Context,
    private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var items = listOf<Preorder>()
    private var bookstores = listOf<Bookstore>()
    private var user: User? = null

    override fun onCreate() {
        loadData()
    }

    override fun onDataSetChanged() {
        loadData()
    }

    private fun loadData() {
        try {
            val db = BookishDatabase.getDatabase(context)
            runBlocking {
                user = db.userDao().getUserDirect() ?: db.userDao().getUser().firstOrNull()
                bookstores = db.bookstoreDao().getAllBookstoresList()
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

                items = allPreorders.filter { item ->
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
            items = emptyList()
        }
    }

    override fun onDestroy() {
        items = emptyList()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= items.size) return null
        val item = items[position]
        val currency = user?.currency ?: "$"
        val themeColors = WidgetThemeUtils.getThemeColors(context, user)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val options = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            appWidgetManager.getAppWidgetOptions(appWidgetId)
        } else null
        val itemHeightDp = TodayPreordersWidgetProvider.calculateItemHeightDp(context, options)
        val minHeight = options?.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) ?: 0
        val isOneRow = minHeight in 1..75

        val views = TodayPreordersWidgetProvider.buildPreorderItemViews(
            context = context,
            item = item,
            bookstores = bookstores,
            currency = currency,
            themeColors = themeColors,
            position = position,
            totalCount = items.size,
            itemHeightDp = itemHeightDp,
            isOneRow = isOneRow
        )

        val fillInIntent = Intent().apply {
            putExtra("NAV_TARGET", "preorders")
            putExtra("preorder_id", item.id)
        }
        views.setOnClickFillInIntent(R.id.widget_preorder_card_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position in items.indices) items[position].id.toLong() else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
