package com.example.webrtctest.utils

import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.webrtctest.model.data.ConnectActivityTAG
import java.util.*

@TargetApi(Build.VERSION_CODES.M)
fun Context.getMissingPermissions(): Array<String?>? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        return arrayOfNulls(0)
    }
    val info: PackageInfo = try {
        packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
    } catch (e: PackageManager.NameNotFoundException) {
        Log.w(ConnectActivityTAG, "Failed to retrieve permissions.")
        return arrayOfNulls(0)
    }
    if (info.requestedPermissions == null) {
        Log.w(ConnectActivityTAG, "No requested permissions.")
        return arrayOfNulls(0)
    }
    val missingPermissions =
        ArrayList<String?>()
    for (i in info.requestedPermissions.indices) {
        if (info.requestedPermissionsFlags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED == 0) {
            missingPermissions.add(info.requestedPermissions[i])
        }
    }

    Log.d(ConnectActivityTAG, "Missing permissions: $missingPermissions")
    return missingPermissions.toTypedArray()
}