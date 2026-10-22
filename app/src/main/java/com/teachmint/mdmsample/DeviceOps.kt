package com.teachmint.mdmsample

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.os.UserManager

/** The real [Ops]: DevicePolicyManager as this app's admin component. */
class DeviceOps(context: Context) : Ops {
    private val app = context.applicationContext
    private val dpm = app.getSystemService(DevicePolicyManager::class.java)
    private val um = app.getSystemService(UserManager::class.java)
    private val audio = app.getSystemService(AudioManager::class.java)
    val admin = ComponentName(app, MdmAdminReceiver::class.java)

    override fun isDeviceOwner() = dpm.isDeviceOwnerApp(app.packageName)

    override fun setRestriction(key: String, on: Boolean) {
        if (on) dpm.addUserRestriction(admin, key) else dpm.clearUserRestriction(admin, key)
    }

    override fun hasRestriction(key: String) = um.hasUserRestriction(key)

    override fun setCameraDisabled(disabled: Boolean) = dpm.setCameraDisabled(admin, disabled)

    /** What the camera service enforces: every admin's camera disable and the camera restriction. */
    override fun cameraDisabled() = dpm.getCameraDisabled(null)

    override fun micMuted() = audio.isMicrophoneMute

    /** Gives up Device Owner, clearing what this sample set first. */
    fun release() {
        Sensor.entries.forEach { runCatching { setRestriction(it.restriction, false) } }
        runCatching { setCameraDisabled(false) }
        @Suppress("DEPRECATION")
        dpm.clearDeviceOwnerApp(app.packageName)
    }
}
