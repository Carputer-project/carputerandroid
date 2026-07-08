package com.carputer.android.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.carputer.android.service.AutoStartService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, AutoStartService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
