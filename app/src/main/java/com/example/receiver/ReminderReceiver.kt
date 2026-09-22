package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "com.example.ACTION_REMINDER") return

        val type = intent.getStringExtra("type") ?: return
        val id = intent.getIntExtra("id", 0)
        val title = intent.getStringExtra("title") ?: "Reminder"
        val book = intent.getStringExtra("book") ?: ""
        val dateMs = intent.getLongExtra("date", 0L)

        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val isToday = if (dateMs != 0L) {
            val todayCal = Calendar.getInstance()
            val dateCal = Calendar.getInstance().apply { timeInMillis = dateMs }
            todayCal.get(Calendar.YEAR) == dateCal.get(Calendar.YEAR) &&
            todayCal.get(Calendar.DAY_OF_YEAR) == dateCal.get(Calendar.DAY_OF_YEAR)
        } else false

        val formattedDate = if (isToday) "today" else if (dateMs != 0L) dateFormat.format(Date(dateMs)) else ""

        val notificationTitle: String
        val notificationText: String
        val smallIconRes: Int

        if (type == "preorder") {
            smallIconRes = com.example.R.drawable.ic_book_5
            notificationTitle = "Preorder Release: $title"
            notificationText = if (isToday) {
                "Your preorder is releasing today!"
            } else if (formattedDate.isNotEmpty()) {
                "Your preorder is releasing on $formattedDate!"
            } else {
                "Your preorder is releasing soon!"
            }
        } else {
            smallIconRes = com.example.R.drawable.ic_subscriptions_auto_stories
            notificationTitle = "Subscription Renewal: $title"
            notificationText = if (isToday) {
                if (book.isNotEmpty()) "Renewal delivery '$book' is due today!" else "Your subscription is renewing today!"
            } else if (formattedDate.isNotEmpty()) {
                if (book.isNotEmpty()) "Renewal delivery '$book' is due on $formattedDate!" else "Your subscription is renewing on $formattedDate!"
            } else {
                "Your subscription is renewing soon!"
            }
        }

        // Open main app when clicked
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Make sure channel is created
        ReminderScheduler.createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(smallIconRes)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            notificationManager?.notify(id, notification)
        } catch (e: Exception) {
            Log.e("ReminderReceiver", "Error posting notification", e)
        }
    }
}

object ReminderScheduler {
    const val CHANNEL_ID = "sub_track_reminders_channel"

    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "Reminders"
                val descriptionText = "Notifications for preorders and subscription renewals"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableLights(true)
                    enableVibration(true)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to create notification channel", e)
        }
    }

    fun schedulePreorderReminder(context: Context, preorderId: Int, title: String, saleDateStart: Long, offsetDays: Int, hour: Int, minute: Int) {
        try {
            var targetTimeMs = calculateReminderTime(saleDateStart, offsetDays, hour, minute)
            if (targetTimeMs <= System.currentTimeMillis()) {
                if (saleDateStart >= System.currentTimeMillis() - 24 * 60 * 60 * 1000L) {
                    targetTimeMs = System.currentTimeMillis() + 1000L
                } else {
                    // Already in the past and not current/recent, don't schedule
                    return
                }
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.ACTION_REMINDER"
                putExtra("type", "preorder")
                putExtra("id", preorderId)
                putExtra("title", title)
                putExtra("date", saleDateStart)
            }

            // Generate a unique request code for preorder alarms (e.g. 100000 + preorderId)
            val requestCode = 100000 + preorderId
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, targetTimeMs, pendingIntent)
            }
            Log.d("ReminderScheduler", "Scheduled preorder reminder for $title at $targetTimeMs")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule preorder alarm", e)
        }
    }

    fun cancelPreorderReminder(context: Context, preorderId: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.ACTION_REMINDER"
            }
            val requestCode = 100000 + preorderId
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to cancel preorder alarm", e)
        }
    }

    fun scheduleScheduledSubReminder(context: Context, scheduledSubId: Int, subTitle: String, bookTitle: String, dueDate: Long, offsetDays: Int, hour: Int, minute: Int) {
        try {
            var targetTimeMs = calculateReminderTime(dueDate, offsetDays, hour, minute)
            if (targetTimeMs <= System.currentTimeMillis()) {
                if (dueDate >= System.currentTimeMillis() - 24 * 60 * 60 * 1000L) {
                    targetTimeMs = System.currentTimeMillis() + 1000L
                } else {
                    // Already in the past and not current/recent, don't schedule
                    return
                }
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.ACTION_REMINDER"
                putExtra("type", "subscription")
                putExtra("id", scheduledSubId)
                putExtra("title", subTitle)
                putExtra("book", bookTitle)
                putExtra("date", dueDate)
            }

            // Generate a unique request code for scheduled sub alarms (e.g. 200000 + scheduledSubId)
            val requestCode = 200000 + scheduledSubId
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetTimeMs, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, targetTimeMs, pendingIntent)
            }
            Log.d("ReminderScheduler", "Scheduled subscription reminder for $subTitle ($bookTitle) at $targetTimeMs")
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to schedule sub alarm", e)
        }
    }

    fun cancelScheduledSubReminder(context: Context, scheduledSubId: Int) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, ReminderReceiver::class.java).apply {
                action = "com.example.ACTION_REMINDER"
            }
            val requestCode = 200000 + scheduledSubId
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
            }
        } catch (e: Exception) {
            Log.e("ReminderScheduler", "Failed to cancel sub alarm", e)
        }
    }

    fun calculateReminderTime(eventDateMs: Long, dDayOffset: Int, hour: Int, minute: Int): Long {
        val utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = eventDateMs
        }
        val year = utcCalendar.get(Calendar.YEAR)
        val month = utcCalendar.get(Calendar.MONTH)
        val day = utcCalendar.get(Calendar.DAY_OF_MONTH)

        val localCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            add(Calendar.DAY_OF_YEAR, -dDayOffset)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return localCalendar.timeInMillis
    }
}
