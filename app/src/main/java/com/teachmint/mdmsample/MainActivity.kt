package com.teachmint.mdmsample

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast

/** A small screen: Device Owner status, a Camera and a Mic block switch, and a button to give up Device Owner. */
class MainActivity : Activity() {

    private lateinit var ops: DeviceOps
    private lateinit var policy: SensorPolicy
    private lateinit var status: TextView
    private val switches = mutableMapOf<Sensor, Switch>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ops = DeviceOps(this)
        policy = SensorPolicy(ops)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(64, 64, 64, 64)
        }
        status = TextView(this).apply { textSize = 18f }
        root.addView(status)
        Sensor.entries.forEach { sensor ->
            val sw = Switch(this).apply {
                text = "Block ${sensor.label}"
                textSize = 18f
                setPadding(0, 32, 0, 0)
                setOnClickListener { apply(sensor, isChecked) }
            }
            switches[sensor] = sw
            root.addView(sw)
        }
        root.addView(Button(this).apply {
            text = "Give up Device Owner"
            setOnClickListener {
                runCatching { ops.release() }.onFailure { toast("Release failed: ${it.message}") }
                refresh()
            }
        })
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun apply(sensor: Sensor, blocked: Boolean) {
        when (val r = policy.set(sensor, blocked)) {
            is Result.Done -> toast(r.detail)
            is Result.Failed -> toast(r.detail)
            Result.NotDeviceOwner -> toast("Not Device Owner - see README")
        }
        refresh()
    }

    private fun refresh() {
        val owner = ops.isDeviceOwner()
        status.text = if (owner) "Device Owner: yes" else "Device Owner: no (set it with adb, see README)"
        switches.forEach { (sensor, sw) ->
            sw.isEnabled = owner
            sw.isChecked = owner && policy.isBlocked(sensor)
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
