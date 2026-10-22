package com.teachmint.mdmsample

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/** The admin component Android makes Device Owner (`dpm set-device-owner …/.MdmAdminReceiver`). */
class MdmAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        Log.i(TAG, "admin enabled; device owner = ${DeviceOps(context).isDeviceOwner()}")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        Log.i(TAG, "admin disabled")
    }

    companion object {
        const val TAG = "MdmSample"
    }
}
