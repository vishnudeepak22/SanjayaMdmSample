package com.teachmint.mdmsample

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * adb control (DUMP-guarded, so only `adb shell` can send it):
 *
 *   adb shell am broadcast -a com.teachmint.mdmsample.CONTROL -n com.teachmint.mdmsample/.AdbControlReceiver \
 *     --es sensor camera|mic --ez blocked true|false
 *   adb shell am broadcast ... --es action status
 *   adb shell am broadcast ... --es action release      # give up Device Owner
 */
class AdbControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val ops = DeviceOps(context)
        val policy = SensorPolicy(ops)
        resultData = when (intent.getStringExtra("action")) {
            "status" -> status(ops, policy)
            "release" -> runCatching { ops.release(); "released device owner" }
                .getOrElse { "release failed: ${it.message}" }
            else -> {
                val sensor = when (intent.getStringExtra("sensor")) {
                    "camera" -> Sensor.CAMERA
                    "mic" -> Sensor.MIC
                    else -> null
                }
                if (sensor == null || !intent.hasExtra("blocked")) "usage: --es sensor camera|mic --ez blocked true|false"
                else policy.set(sensor, intent.getBooleanExtra("blocked", true)).toString()
            }
        }
        Log.i(MdmAdminReceiver.TAG, "adb: $resultData")
    }

    private fun status(ops: DeviceOps, policy: SensorPolicy) =
        "deviceOwner=${ops.isDeviceOwner()} " + Sensor.entries.joinToString(" ") { "${it.label}Blocked=${policy.isBlocked(it)}" }
}
