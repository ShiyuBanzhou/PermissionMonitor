package com.example.permissionmonitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class PermissionChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                Intent.ACTION_PACKAGE_CHANGED -> {
                    // 发送本地广播通知 MainActivity 刷新数据
                    val refreshIntent = Intent("com.example.permissionmonitor.ACTION_REFRESH")
                    LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent)
                }
            }
        }
    }
}