package com.ham.qso.domain.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.abs

/**
 * 罗盘传感器状态数据
 * @param azimuth 当前设备航向角 (0° ~ 360°，正北为 0°/360°，顺时针递增)
 * @param accuracy 传感器精度 (SensorManager.SENSOR_STATUS_ACCURACY_*)
 * @param isSupported 设备是否支持地磁/旋转矢量传感器
 * @param cardinalDirection 简短方位中文 (如 "正北 N", "东北 NE", "正东 E" 等)
 */
data class CompassState(
    val azimuth: Float = 0f,
    val accuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_HIGH,
    val isSupported: Boolean = true,
    val cardinalDirection: String = "北 N"
)

/**
 * Android 硬件地磁与旋转矢量罗盘管理器
 */
class CompassSensorManager(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val rotationSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer: Sensor? = if (rotationSensor == null) sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) else null
    private val magneticField: Sensor? = if (rotationSensor == null) sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD) else null

    val isSensorAvailable: Boolean = rotationSensor != null || (accelerometer != null && magneticField != null)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var currentAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
    private var currentAzimuth = 0f
    private var listener: ((CompassState) -> Unit)? = null

    fun startListening(onCompassUpdated: (CompassState) -> Unit) {
        if (!isSensorAvailable || sensorManager == null) {
            onCompassUpdated(CompassState(isSupported = false))
            return
        }
        listener = onCompassUpdated

        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            magneticField?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
    }

    fun stopListening() {
        sensorManager?.unregisterListener(this)
        hasGravity = false
        hasGeomagnetic = false
        listener = null
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        var rawAzimuth: Float? = null

        if (event.accuracy != 0 || event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            currentAccuracy = event.accuracy
        }

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)
            val azimuthRad = orientationAngles[0]
            rawAzimuth = (Math.toDegrees(azimuthRad.toDouble()).toFloat() + 360f) % 360f
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravityValues, 0, 3)
            hasGravity = true
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagneticValues, 0, 3)
            hasGeomagnetic = true
        }

        if (rawAzimuth == null && hasGravity && hasGeomagnetic) {
            if (SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geomagneticValues)) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthRad = orientationAngles[0]
                rawAzimuth = (Math.toDegrees(azimuthRad.toDouble()).toFloat() + 360f) % 360f
            }
        }

        if (rawAzimuth != null) {
            // 平滑滤波处理（处理 359° <-> 0° 环形边界跳变与微小手抖）
            currentAzimuth = smoothAngle(currentAzimuth, rawAzimuth, alpha = 0.25f)
            val cardinal = getCardinalDirection(currentAzimuth)

            listener?.invoke(
                CompassState(
                    azimuth = currentAzimuth,
                    accuracy = currentAccuracy,
                    isSupported = true,
                    cardinalDirection = cardinal
                )
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        currentAccuracy = accuracy
        listener?.invoke(
            CompassState(
                azimuth = currentAzimuth,
                accuracy = currentAccuracy,
                isSupported = true,
                cardinalDirection = getCardinalDirection(currentAzimuth)
            )
        )
    }

    /**
     * 角度平滑插值，避免 359° 与 0° 之间的环形跳变
     */
    private fun smoothAngle(current: Float, target: Float, alpha: Float): Float {
        var diff = (target - current) % 360f
        if (diff > 180f) diff -= 360f
        if (diff < -180f) diff += 360f
        return (current + diff * alpha + 360f) % 360f
    }

    companion object {
        fun getCardinalDirection(azimuth: Float): String {
            val deg = (azimuth + 360f) % 360f
            return when {
                deg >= 337.5f || deg < 22.5f -> "北 N"
                deg in 22.5f..67.5f -> "东北 NE"
                deg in 67.5f..112.5f -> "东 E"
                deg in 112.5f..157.5f -> "东南 SE"
                deg in 157.5f..202.5f -> "南 S"
                deg in 202.5f..247.5f -> "西南 SW"
                deg in 247.5f..292.5f -> "西 W"
                deg in 292.5f..337.5f -> "西北 NW"
                else -> "北 N"
            }
        }

        fun getAccuracyLabel(accuracy: Int): String {
            return when (accuracy) {
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "极佳 (HIGH)"
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "良好 (MEDIUM)"
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "偏低 (LOW)"
                SensorManager.SENSOR_STATUS_UNRELIABLE -> "不可靠 (UNRELIABLE)"
                else -> "检测中"
            }
        }
    }
}

/**
 * Compose 状态扩展：生命周期感知的硬件罗盘数据
 */
@Composable
fun rememberCompassState(): CompassState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var compassState by remember { mutableStateOf(CompassState()) }

    val manager = remember { CompassSensorManager(context) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    manager.startListening { state ->
                        compassState = state
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    manager.stopListening()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            manager.stopListening()
        }
    }

    return compassState
}
