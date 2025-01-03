package com.example.permissionmonitor

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView

class PermissionAdapter(private val permissionList: List<PermissionUsage>) :
    RecyclerView.Adapter<PermissionAdapter.PermissionViewHolder>() {

    class PermissionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appIcon: ImageView = itemView.findViewById(R.id.img_app_icon)
        val appName: TextView = itemView.findViewById(R.id.tv_app_name)
        val permissions: TextView = itemView.findViewById(R.id.tv_permissions)
        val btnInfo: ImageButton = itemView.findViewById(R.id.btn_info)
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

        // 将权限英文转为中文描述
        val permissionDescriptions = permissionUsage.abusedPermissions
            .joinToString(", ") { permission -> getPermissionDescription(permission) }
        holder.permissions.text = permissionDescriptions

        holder.btnInfo.setOnClickListener {
            showPermissionDetails(holder.itemView.context, permissionUsage.abusedPermissions)
        }
    }

    private fun showPermissionDetails(context: Context, permissions: List<String>) {
        val permissionDescriptions = permissions.joinToString("\n\n") { permission ->
            "$permission:\n${getPermissionDescription(permission)}"
        }

        AlertDialog.Builder(context)
            .setTitle("权限详情")
            .setMessage(permissionDescriptions)
            .setPositiveButton("确定") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

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
