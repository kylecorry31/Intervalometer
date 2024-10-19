package com.kylecorry.intervalometer.infrastructure.services

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.kylecorry.andromeda.notify.Notify
import com.kylecorry.andromeda.preferences.SharedPreferences
import com.kylecorry.intervalometer.R
import com.kylecorry.luna.timer.CoroutineTimer

class IntervalometerService : AccessibilityService() {

    private val prefs by lazy { SharedPreferences(this) }

    private var stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                disableSelf()
            } else {
                // Open accessibility settings
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }

        }
    }

    private val timer = CoroutineTimer {
        clickShutterButton()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        println(event?.packageName)
    }

    override fun onInterrupt() {
        println("Interrupted")
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

        val stopIntent = Intent("com.kylecorry.intervalometer.STOP")
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        Notify.send(
            this,
            1,
            Notify.persistent(
                this, "intervalometer", "Intervalometer", null, R.drawable.bubble,
                actions = listOf(
                    Notify.action("Stop", stopPendingIntent, R.drawable.ic_info),
                )
            ),
        )

        restartTimer()

        prefs.onChange.subscribe(this::onPrefsChanged)
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.stop()
        Notify.cancel(this, 1)
        unregisterReceiver(stopReceiver)
        prefs.onChange.unsubscribe(this::onPrefsChanged)
    }

    private fun onPrefsChanged(key: String): Boolean {
        if (key == "interval") {
            restartTimer()
        }
        return true
    }

    private fun restartTimer() {
        val interval = prefs.getLong("interval") ?: 0
        if (interval > 0) {
            timer.interval(interval * 1000)
        } else {
            timer.stop()
        }
    }

    private fun clickShutterButton() {
        val knownShutterButtons = listOf(
            "com.android.camera:id/shutter_button",
            "com.android.camera2:id/shutter_button",
            "com.google.android.GoogleCamera:id/shutter_button"
        )

        val nodeInfo = rootInActiveWindow
        for (shutterButton in knownShutterButtons) {
            val nodes = nodeInfo?.findAccessibilityNodeInfosByViewId(shutterButton)
            if (nodes != null && nodes.isNotEmpty()) {
                nodes.forEach {
                    it.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                return
            }
        }
    }
}