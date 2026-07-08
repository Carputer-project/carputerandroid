package com.carputer.android.service

import android.app.Application
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import java.io.File

class SystemService(private val application: Application) {

    private val powerManager = application.getSystemService(Application.POWER_SERVICE) as PowerManager

    fun shutdown() {
        // Requires DEVICE_POWER permission - not granted to normal apps
        // Fallback: let the system handle display timeout
    }

    fun reboot() {
        // Requires REBOOT permission (system app) — fallback to intent
        try {
            powerManager.reboot(null)
        } catch (e: SecurityException) {
            // Fallback: show dialog instructing user
        }
    }

    fun getSystemUptime(): String {
        val uptimeMs = android.os.SystemClock.uptimeMillis()
        val seconds = (uptimeMs / 1000) % 60
        val minutes = (uptimeMs / (1000 * 60)) % 60
        val hours = (uptimeMs / (1000 * 60 * 60)) % 24
        val days = uptimeMs / (1000 * 60 * 60 * 24)
        return if (days > 0) "${days}d ${hours}h ${minutes}m"
        else "${hours}h ${minutes}m ${seconds}s"
    }

    fun getSystemLoad(): String {
        return try {
            val loadAvg = File("/proc/loadavg").readText().trim()
            loadAvg.split(" ").take(3).joinToString(" ")
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun getDiskUsage(path: String = "/storage/emulated/0"): String {
        return try {
            val stat = android.os.StatFs(path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availBlocks = stat.availableBlocksLong
            val total = totalBlocks * blockSize
            val avail = availBlocks * blockSize
            val used = total - avail

            fun fmt(bytes: Long): String {
                val gb = bytes / (1024 * 1024 * 1024)
                val mb = (bytes % (1024 * 1024 * 1024)) / (1024 * 1024)
                return if (gb > 0) "${gb}G" else "${mb}M"
            }
            "${fmt(used)} used / ${fmt(total)} total"
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun runShellCommand(cmd: String): String {
        return try {
            val safe = cmd
                .replace(";", " ")
                .replace("&&", " ")
                .replace("||", " ")
                .replace("|", " ")
                .replace(">", " ")
                .replace("<", " ")
                .replace("`", " ")
                .replace("\$", " ")
            val process = Runtime.getRuntime().exec(arrayOf("/system/bin/sh", "-c", safe))
            process.inputStream.bufferedReader().readText().trim()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
