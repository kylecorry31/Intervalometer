package com.kylecorry.intervalometer.infrastructure.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.preferences.SharedPreferences
import com.kylecorry.intervalometer.R
import com.kylecorry.intervalometer.ui.MainActivity
import com.kylecorry.luna.timer.CoroutineTimer

class IntervalometerService : AccessibilityService() {

    private val prefs by lazy { SharedPreferences(this) }
    private val knownShutterButtonIds = listOf(
        "com.android.camera:id/shutter_button",
        "com.android.camera2:id/shutter_button",
        "com.google.android.GoogleCamera:id/shutter_button",
        "com.riseupgames.proshot2:id/cameraButton",
        "net.sourceforge.opencamera:id/take_photo"
    )

    private val shutterButtonIds: List<String>
        get() = (prefs.getString("shutter_buttons") ?: "").split(",").map { it.trim() }
            .filter { it.isNotBlank() } + knownShutterButtonIds

    private var stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            timer.stop()
            Notify.cancel(this@IntervalometerService, 2)
        }
    }

    private var secondsUntilNextPhoto = 0L

    private val timer = CoroutineTimer {
        // Trigger a notification with the remaining time
        secondsUntilNextPhoto--

        // Only cancel and reshow when the time is less than 5 seconds
        if (secondsUntilNextPhoto <= 5) {
            Notify.cancel(this, 2)
        }
        Notify.send(
            this,
            2,
            Notify.alert(
                this,
                "intervalometer",
                secondsUntilNextPhoto.toString(),
                null,
                R.drawable.bubble,
                group = "alerts",
                intent = PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        )
        if (secondsUntilNextPhoto <= 0) {
            Notify.cancel(this, 2)
            clickShutterButton()
            secondsUntilNextPhoto = prefs.getLong("interval") ?: 0
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Start the timer when a shutter button is clicked
        if (!timer.isRunning() && shutterButtonIds.contains(
                event?.source?.viewIdResourceName ?: ""
            )
        ) {
            restartTimer()
        } else {
            println(event?.source?.viewIdResourceName)
        }
    }

    override fun onInterrupt() {
        // Do nothing
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()
        println("Service connected")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                stopReceiver, IntentFilter("com.kylecorry.intervalometer.STOP"),
                RECEIVER_NOT_EXPORTED
            )
        } else {
            registerReceiver(stopReceiver, IntentFilter("com.kylecorry.intervalometer.STOP"))
        }

        Notify.send(
            this,
            1,
            Notify.persistent(
                this, "intervalometer", "Intervalometer", null, R.drawable.bubble,
                actions = listOf(
                    Notify.action(
                        "Stop", PendingIntent.getBroadcast(
                            this,
                            0,
                            Intent("com.kylecorry.intervalometer.STOP"),
                            PendingIntent.FLAG_IMMUTABLE
                        ), R.drawable.ic_info
                    ),
                ),
                intent = PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            ),
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.stop()
        Notify.cancel(this, 1)
        Notify.cancel(this, 2)
        unregisterReceiver(stopReceiver)
    }

    private fun restartTimer() {
        val interval = prefs.getLong("interval") ?: 0
        Notify.cancel(this, 2)
        if (interval > 0) {
            secondsUntilNextPhoto = prefs.getLong("interval") ?: 0
            timer.interval(1000)
        } else {
            timer.stop()
        }
    }

    private fun clickShutterButton() {
        val nodeInfo = rootInActiveWindow
        for (shutterButton in shutterButtonIds) {
            val nodes = nodeInfo?.findAccessibilityNodeInfosByViewId(shutterButton)
            if (nodes != null && nodes.isNotEmpty()) {
                nodes.forEach {
                    it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                return
            }
        }

        // If it got this far, no shutter button was found, so stop the timer
        timer.stop()
    }
}