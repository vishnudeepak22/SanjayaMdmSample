package com.teachmint.mdmsample

/** The sensors this sample can block. [restriction] is the UserManager.DISALLOW_* key a Device Owner sets. */
enum class Sensor(val label: String, val restriction: String) {
    CAMERA("camera", "no_camera"), // UserManager.DISALLOW_CAMERA
    MIC("microphone", "no_unmute_microphone"), // UserManager.DISALLOW_UNMUTE_MICROPHONE
}

/** What a block or allow did. */
sealed interface Result {
    data class Done(val detail: String) : Result
    data class Failed(val detail: String) : Result
    data object NotDeviceOwner : Result
}

/** The device calls the policy needs, so it is unit-testable without Android. [DeviceOps] is the real one. */
interface Ops {
    fun isDeviceOwner(): Boolean
    fun setRestriction(key: String, on: Boolean)
    fun hasRestriction(key: String): Boolean
    /** The legacy camera switch; a Device Owner may also set it. */
    fun setCameraDisabled(disabled: Boolean)
    fun cameraDisabled(): Boolean
    fun micMuted(): Boolean
}

/**
 * Camera and mic blocks as a Device Owner. Unlike SanjayaCast (a system app, not an owner), the owner sets the
 * restriction through DevicePolicyManager.addUserRestriction, so Android enforces it itself: the camera service
 * refuses every app, the mic stays muted, and Settings shows the controls greyed out as "Blocked by your admin".
 * Every call reads back before it reports done.
 */
class SensorPolicy(private val ops: Ops) {

    fun set(sensor: Sensor, blocked: Boolean): Result {
        if (!ops.isDeviceOwner()) return Result.NotDeviceOwner
        runCatching { ops.setRestriction(sensor.restriction, blocked) }
            .onFailure { return Result.Failed("${sensor.label}: ${it.message}") }
        if (sensor == Sensor.CAMERA) runCatching { ops.setCameraDisabled(blocked) }
        return if (isBlocked(sensor) == blocked) Result.Done("${sensor.label} ${if (blocked) "blocked" else "allowed"}")
        else Result.Failed("${sensor.label}: Android did not apply it")
    }

    /** Blocked when the owner's restriction holds and Android agrees (camera service / mic mute). */
    fun isBlocked(sensor: Sensor): Boolean = ops.hasRestriction(sensor.restriction) && when (sensor) {
        Sensor.CAMERA -> ops.cameraDisabled()
        Sensor.MIC -> ops.micMuted()
    }
}
