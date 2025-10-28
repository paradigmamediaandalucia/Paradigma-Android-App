package com.example.paradigmaapp.android.utils

import android.content.Context
import android.content.pm.PackageManager

object UpdateChecker {
    fun getInstalledVersionName(context: Context): String {
        val packageManager = context.packageManager
        val packageName = context.packageName
        return try {
            val info = packageManager.getPackageInfo(packageName, 0)
            info.versionName ?: ""
        } catch (e: PackageManager.NameNotFoundException) {
            ""
        }
    }
}
