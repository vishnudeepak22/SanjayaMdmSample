package com.teachmint.mdmsample

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SensorPolicyTest {

    /** A device where restrictions take effect, or not ([enforced] false = Android ignores them). */
    private class FakeOps(var owner: Boolean = true, val enforced: Boolean = true) : Ops {
        val restrictions = mutableSetOf<String>()
        var cameraSwitch = false
        override fun isDeviceOwner() = owner
        override fun setRestriction(key: String, on: Boolean) {
            if (on) restrictions += key else restrictions -= key
        }
        override fun hasRestriction(key: String) = key in restrictions
        override fun setCameraDisabled(disabled: Boolean) { cameraSwitch = disabled }
        override fun cameraDisabled() = enforced && (cameraSwitch || "no_camera" in restrictions)
        override fun micMuted() = enforced && "no_unmute_microphone" in restrictions
    }

    @Test fun `not device owner changes nothing`() {
        val ops = FakeOps(owner = false)
        assertEquals(Result.NotDeviceOwner, SensorPolicy(ops).set(Sensor.CAMERA, blocked = true))
        assertTrue(ops.restrictions.isEmpty())
    }

    @Test fun `camera block sets the restriction and the camera switch, allow clears both`() {
        val ops = FakeOps()
        val policy = SensorPolicy(ops)
        assertEquals(Result.Done("camera blocked"), policy.set(Sensor.CAMERA, blocked = true))
        assertTrue(ops.cameraSwitch)
        assertTrue(policy.isBlocked(Sensor.CAMERA))
        assertEquals(Result.Done("camera allowed"), policy.set(Sensor.CAMERA, blocked = false))
        assertFalse(ops.cameraSwitch)
        assertFalse(policy.isBlocked(Sensor.CAMERA))
    }

    @Test fun `mic block sets the unmute restriction`() {
        val ops = FakeOps()
        assertEquals(Result.Done("microphone blocked"), SensorPolicy(ops).set(Sensor.MIC, blocked = true))
        assertTrue("no_unmute_microphone" in ops.restrictions)
    }

    @Test fun `a restriction Android does not enforce is reported as failed, not done`() {
        val result = SensorPolicy(FakeOps(enforced = false)).set(Sensor.CAMERA, blocked = true)
        assertTrue(result is Result.Failed)
    }
}
