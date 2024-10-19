package com.kylecorry.intervalometer.app

import android.content.Context
import com.kylecorry.andromeda.notify.Notify

object NotificationChannels {

    fun createChannels(context: Context) {
        // Create channels here
        Notify.createChannel(
            context,
            "intervalometer",
            "Intervalometer",
            "Notifications for the intervalometer",
            Notify.CHANNEL_IMPORTANCE_HIGH
        )
    }

}