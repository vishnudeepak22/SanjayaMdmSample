package com.teachmint.mdmsample

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.widget.TextView

/**
 * The setup wizard sends GET_PROVISIONING_MODE, then ADMIN_POLICY_COMPLIANCE, when this app is
 * provisioned by QR code or `afw#`. By compliance time the app is already Device Owner, so the
 * blocks can be applied right away. The QR's admin extras choose what to block; the default is
 * to block everything, so `afw#` (which carries no extras) still ends up fully blocked.
 */
class ProvisioningActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = getString(R.string.app_name)
            textSize = 18f
            setPadding(64, 64, 64, 64)
        })
        when (intent?.action) {
            DevicePolicyManager.ACTION_GET_PROVISIONING_MODE -> answerProvisioningMode()
            DevicePolicyManager.ACTION_ADMIN_POLICY_COMPLIANCE -> applyPolicy()
            else -> Log.w(MdmAdminReceiver.TAG, "provisioning: unexpected action ${intent?.action}")
        }
        finish()
    }

    private fun answerProvisioningMode() {
        setResult(RESULT_OK, Intent().putExtra(
            DevicePolicyManager.EXTRA_PROVISIONING_MODE,
            DevicePolicyManager.PROVISIONING_MODE_FULLY_MANAGED_DEVICE))
    }

    private fun applyPolicy() {
        val extras = adminExtras()
        val policy = SensorPolicy(DeviceOps(this))
        Sensor.entries.forEach { sensor ->
            val block = extras?.getBoolean(extraKey(sensor), true) ?: true
            Log.i(MdmAdminReceiver.TAG, "provisioning: ${sensor.label} -> ${policy.set(sensor, block)}")
        }
        setResult(RESULT_OK)
    }

    private fun adminExtras(): PersistableBundle? =
        if (Build.VERSION.SDK_INT >= 33)
            intent.getParcelableExtra(
                DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE, PersistableBundle::class.java)
        else @Suppress("DEPRECATION")
            intent.getParcelableExtra(DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE)

    private fun extraKey(sensor: Sensor) = when (sensor) {
        Sensor.CAMERA -> "blockCamera"
        Sensor.MIC -> "blockMic"
    }
}
