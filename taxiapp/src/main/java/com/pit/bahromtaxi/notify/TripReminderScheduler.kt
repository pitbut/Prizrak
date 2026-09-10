package com.pit.bahromtaxi.notify

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Будильник-напоминание перед отправлением межгородней/групповой поездки: за час до
 * отправления начинает повторяться каждые 10 минут (как и просили — "как будильник"), пока
 * поездка не отправится или не будет отменена. Реализовано на голом AlarmManager +
 * BroadcastReceiver — без WorkManager, чтобы не тащить новую зависимость ради этого.
 *
 * setRepeating — неточный (батчится системой), а не setExactAndAllowWhileIdle, поэтому не
 * требует разрешения SCHEDULE_EXACT_ALARM: для будильника "плюс-минус пара минут" не критично.
 */
object TripReminderScheduler {
    private const val CHANNEL_ID = "trip_reminders"
    const val EXTRA_TRIP_ID = "trip_id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_DEPARTURE_MILLIS = "departure_millis"
    private const val REMINDER_INTERVAL_MS = 10 * 60 * 1000L
    private const val REMINDER_LEAD_MS = 60 * 60 * 1000L

    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Напоминания о поездках", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Будильник перед отправлением межгородней или групповой поездки" }
            appContext.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /** Ставит/обновляет будильник для поездки — вызывать повторно безопасно (перезаписывает). */
    fun schedule(id: String, title: String, departureMillis: Long) {
        if (!::appContext.isInitialized) return
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()
        // Отправление уже прошло больше чем на один интервал повтора — будильник не нужен.
        if (departureMillis + REMINDER_INTERVAL_MS < now) {
            cancel(id)
            return
        }
        val firstTrigger = (departureMillis - REMINDER_LEAD_MS).coerceAtLeast(now + 5_000)
        runCatching {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, firstTrigger, REMINDER_INTERVAL_MS, buildPendingIntent(id, title, departureMillis))
        }
    }

    fun cancel(id: String) {
        if (!::appContext.isInitialized) return
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        runCatching { alarmManager.cancel(buildPendingIntent(id, "", 0)) }
    }

    private fun buildPendingIntent(id: String, title: String, departureMillis: Long): PendingIntent {
        val intent = Intent(appContext, TripReminderReceiver::class.java).apply {
            putExtra(EXTRA_TRIP_ID, id)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DEPARTURE_MILLIS, departureMillis)
        }
        return PendingIntent.getBroadcast(
            appContext, id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Пытается разобрать "yyyy-MM-dd HH:mm" (формат нашего date/time picker'а) и пару запасных вариантов. */
    fun parseDateTime(raw: String): Long? {
        for (pattern in listOf("yyyy-MM-dd HH:mm", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd")) {
            runCatching {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.isLenient = false
                return sdf.parse(raw.trim())?.time
            }
        }
        return null
    }

    internal fun showNotification(context: Context, tripId: String, title: String, departureMillis: Long) {
        val now = System.currentTimeMillis()
        if (now >= departureMillis) cancel(tripId)
        val minutesLeft = ((departureMillis - now) / 60_000).coerceAtLeast(0)
        val text = if (now >= departureMillis) "Отправление уже началось" else "До отправления примерно $minutesLeft мин"
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(tripId.hashCode(), notification) }
    }
}
