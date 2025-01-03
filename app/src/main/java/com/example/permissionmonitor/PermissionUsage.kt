package com.example.permissionmonitor
import android.graphics.drawable.Drawable

data class PermissionUsage(
    val appName: String,
    val appIcon: Drawable,
    val abusedPermissions: List<String>,  // 所有权限
    var disabledPermissions: MutableList<String> = mutableListOf()  // 禁用的权限
)
