package com.pit.bahromtaxi.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TripReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tripId = intent.getStringExtra(TripReminderScheduler.EXTRA_TRIP_ID) ?: return
        val title = intent.getStringExtra(TripReminderScheduler.EXTRA_TITLE) ?: return
        val departureMillis = intent.getLongExtra(TripReminderScheduler.EXTRA_DEPARTURE_MILLIS, 0L)
        TripReminderScheduler.showNotification(context, tripId, title, departureMillis)
    }
}
