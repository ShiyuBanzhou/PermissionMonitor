package com.example.permissionmonitor
import android.graphics.drawable.Drawable

data class PermissionUsage(
    val appName: String,
    val appIcon: Drawable,
    val abusedPermissions: List<String>,
    val packageName: String // 包名字段
)