package com.example.permissionmonitor

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnRefresh: Button
    private val abusedPermissionsList = mutableListOf<PermissionUsage>()

    private val CHANNEL_ID = "permission_monitor_channel"
    private val NOTIFICATION_ID = 1
    private val REQUEST_NOTIFICATION_PERMISSION = 1002

    private val refreshReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            checkPermissions()
            Toast.makeText(this@MainActivity,
                "应用列表发生变化，刷新权限状态。",
                Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        btnRefresh = findViewById(R.id.btn_refresh)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = PermissionAdapter(abusedPermissionsList)

        btnRefresh.setOnClickListener {
            checkPermissions()
        }

        // 注册本地广播接收器
        LocalBroadcastManager.getInstance(this).registerReceiver(
            refreshReceiver,
            IntentFilter("com.example.permissionmonitor.ACTION_REFRESH")
        )

        // 检查是否有权限，如果没有则请求
        if (!hasUsageStatsPermission()) {
            showUsageStatsPermissionDialog()
        } else {
            checkPermissions()
        }

        // 检查并请求通知权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_NOTIFICATION_PERMISSION
                )
            }
        }

        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 取消注册接收器
        LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshReceiver)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATION_PERMISSION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // 权限已授予
                Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                // 权限被拒绝
                Toast.makeText(this, "通知权限被拒绝，无法发送通知", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun showUsageStatsPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限请求")
            .setMessage("PermissionMonitor 需要访问使用情况统计来监测权限滥用。请授予相关权限。")
            .setPositiveButton("前往设置") { dialog, _ ->
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
                dialog.dismiss()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "权限未授予，应用无法正常工作。", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun checkPermissions() {
        abusedPermissionsList.clear()
        recyclerView.adapter?.notifyDataSetChanged()

        CoroutineScope(Dispatchers.Main).launch {
            val apps = withContext(Dispatchers.IO) {
                packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            }

            for (app in apps) {
                val permissions = getAppPermissions(app.packageName)
                if (permissions.isNotEmpty()) {
                    val appName = packageManager.getApplicationLabel(app).toString()
                    val appIcon = packageManager.getApplicationIcon(app)
                    abusedPermissionsList.add(PermissionUsage(appName, appIcon, permissions))
                }
            }

            recyclerView.adapter?.notifyDataSetChanged()

            if (abusedPermissionsList.isNotEmpty()) {
                abusedPermissionsList.forEach { usage ->
                    triggerPermissionAlert(usage)
                }
            }
        }
    }


    private fun getAppPermissions(packageName: String): List<String> {
        val abusedPermissions = mutableListOf<String>()
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val requestedPermissions = packageInfo.requestedPermissions
            if (requestedPermissions != null) {
                for (permission in requestedPermissions) {
                    // 这里你可以定义滥用的标准，例如危险权限
                    if (isDangerousPermission(permission)) {
                        abusedPermissions.add(permission)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return abusedPermissions
    }

    private fun isDangerousPermission(permission: String): Boolean {
        // 定义危险权限列表
        val dangerousPermissions = listOf(
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.READ_SMS
            // 你可以根据需要添加更多权限
        )
        return dangerousPermissions.contains(permission)
    }

    private fun triggerAlert() {
        // 显示一个简单的警报对话框
        AlertDialog.Builder(this)
            .setTitle("权限滥用警报")
            .setMessage("检测到应用存在权限滥用，请及时检查。")
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    @SuppressLint("ServiceCast")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Permission Monitor Channel"
            val descriptionText = "Channel for Permission Monitor alerts"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // 确保有这个图标
            .setContentTitle("权限滥用警报")
            .setContentText("检测到应用存在权限滥用，请检查详细信息。")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(this)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun triggerPermissionAlert(permissionUsage: PermissionUsage) {
        val permissions = permissionUsage.abusedPermissions
        val permissionDescriptions = permissions.map { permission ->
            "$permission: ${getPermissionDescription(permission)}"
        }

        val checkedPermissions = BooleanArray(permissions.size) { true } // 默认全选
        val permissionDisplay = permissions.map { permission ->
            getPermissionDescription(permission)
        }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("权限滥用警告 - ${permissionUsage.appName}")
            .setMultiChoiceItems(permissionDisplay, checkedPermissions) { _, which, isChecked ->
                checkedPermissions[which] = isChecked
            }
            .setPositiveButton("应用选择") { _, _ ->
                val enabledPermissions = permissions.filterIndexed { index, _ -> checkedPermissions[index] }
                val disabledPermissions = permissions.filterIndexed { index, _ -> !checkedPermissions[index] }

                // 更新权限表
                PermissionManager.updatePermissionWhitelist(permissionUsage.appName, enabledPermissions)

                // 通知用户结果
                Toast.makeText(
                    this,
                    "已允许权限：${enabledPermissions.joinToString(", ")}\n已禁用权限：${disabledPermissions.joinToString(", ")}",
                    Toast.LENGTH_LONG
                ).show()

                // 如果禁用了权限，可选操作，例如关闭应用
                if (disabledPermissions.isNotEmpty()) {
                    closeApp(permissionUsage.appName)
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun closeApp(packageName: String) {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.killBackgroundProcesses(packageName)
            Toast.makeText(this, "$packageName 已被关闭", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "无法关闭 $packageName", Toast.LENGTH_LONG).show()
        }
    }

    private fun getPermissionDescription(permission: String): String {
        return when (permission) {
            android.Manifest.permission.READ_CONTACTS -> "读取联系人数据。"
            android.Manifest.permission.CAMERA -> "访问摄像头以拍照或录像。"
            android.Manifest.permission.RECORD_AUDIO -> "录制音频。"
            android.Manifest.permission.ACCESS_FINE_LOCATION -> "访问精确位置数据。"
            android.Manifest.permission.READ_SMS -> "读取短信内容。"
            else -> "未知权限。"
        }
    }

}

object PermissionManager {
    private val appPermissionWhitelist = mutableMapOf<String, List<String>>(
        "com.example.app1" to listOf(
            android.Manifest.permission.READ_CONTACTS,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ),
        "com.example.app2" to listOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
    )

    fun isPermissionAllowed(packageName: String, permission: String): Boolean {
        return appPermissionWhitelist[packageName]?.contains(permission) ?: false
    }

    fun getAbusedPermissions(packageName: String, permissions: List<String>): List<String> {
        val whitelist = appPermissionWhitelist[packageName] ?: emptyList()
        return permissions.filterNot { whitelist.contains(it) }
    }

    fun updatePermissionWhitelist(packageName: String, newPermissions: List<String>) {
        val existingPermissions = appPermissionWhitelist[packageName]?.toMutableList() ?: mutableListOf()
        existingPermissions.addAll(newPermissions)
        appPermissionWhitelist[packageName] = existingPermissions.distinct()
    }
}
