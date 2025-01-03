package com.example.permissionmonitor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class PermissionAdapter(private val permissionList: List<PermissionUsage>) :
    RecyclerView.Adapter<PermissionAdapter.PermissionViewHolder>() {

    // ViewHolder类，用来绑定视图
    class PermissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.img_app_icon)
        val appName: TextView = itemView.findViewById(R.id.tv_app_name)
        val permissions: TextView = itemView.findViewById(R.id.tv_permissions)
        val btnInfo: ImageButton = itemView.findViewById(R.id.btn_info)
        val btnPermissionSettings: Button = itemView.findViewById(R.id.btn_permission_settings) // 按钮控件
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PermissionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_permission, parent, false)
        return PermissionViewHolder(view)
    }

    override fun onBindViewHolder(holder: PermissionViewHolder, position: Int) {
        val permissionUsage = permissionList[position]
        holder.appIcon.setImageDrawable(permissionUsage.appIcon)
        holder.appName.text = permissionUsage.appName

        // 显示权限列表
        val permissionDescriptions = permissionUsage.abusedPermissions
            .joinToString(", ") { permission -> getPermissionDescription(permission) }
        holder.permissions.text = permissionDescriptions

        // 点击事件 - 显示权限详情
        holder.btnInfo.setOnClickListener {
            showPermissionDetails(holder.itemView.context, permissionUsage.abusedPermissions)
        }

        // 点击事件 - 打开应用的权限设置
        holder.btnPermissionSettings.setOnClickListener {
            openAppPermissionSettings(holder.itemView.context, permissionUsage)
        }
    }

    // 显示权限详情对话框
    private fun showPermissionDetails(context: Context, permissions: List<String>) {
        val permissionDescriptions = permissions.joinToString("\n\n") { permission ->
            "$permission:\n${getPermissionDescription(permission)}"
        }

        AlertDialog.Builder(context)
            .setTitle("权限详情")
            .setMessage(permissionDescriptions)
            .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    // 打开应用的权限设置页面
    private fun openAppPermissionSettings(context: Context, permissionUsage: PermissionUsage) {
        // 获取应用的包名
        val packageName = permissionUsage.packageName

        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }

        // 检查是否存在的包名并启动
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            AlertDialog.Builder(context)
                .setTitle("错误")
                .setMessage("无法打开权限设置页面。")
                .setPositiveButton("确定") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }

    // 获取权限的描述
    private fun getPermissionDescription(permission: String): String {
        return when (permission) {
            android.Manifest.permission.READ_CONTACTS -> "读取联系人数据。"
            android.Manifest.permission.CAMERA -> "访问摄像头拍照或录像。"
            android.Manifest.permission.RECORD_AUDIO -> "录制音频。"
            android.Manifest.permission.ACCESS_FINE_LOCATION -> "访问精确位置。"
            android.Manifest.permission.READ_SMS -> "读取短信内容。"
            else -> "未知权限。"
        }
    }

    override fun getItemCount(): Int {
        return permissionList.size
    }
}