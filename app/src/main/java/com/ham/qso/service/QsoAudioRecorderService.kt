package com.ham.qso.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Environment
import android.os.IBinder
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.ham.qso.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class QsoAudioRecorderService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var sessionStartTimeUtc: Long = 0

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    private var amplitudeJob: Job? = null

    private lateinit var vibratorManager: VibratorManager

    override fun onCreate() {
        super.onCreate()
        vibratorManager = getSystemService(VibratorManager::class.java)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val title = intent.getStringExtra(EXTRA_SESSION_TITLE) ?: ""
                startRecording(title)
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
            ACTION_TRIGGER_MARK -> {
                triggerMark()
            }
        }
        return START_NOT_STICKY
    }

    private fun startRecording(sessionTitle: String) {
        if (_recordingState.value.isRecording) return

        try {
            val recordDir = getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)
                ?: File(filesDir, "Recordings").apply { mkdirs() }
            if (!recordDir.exists()) recordDir.mkdirs()

            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "QSO_REC_${timestampStr}.m4a"
            recordingFile = File(recordDir, fileName)

            sessionStartTimeUtc = System.currentTimeMillis()

            mediaRecorder = MediaRecorder(this).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(48000)
                setAudioSamplingRate(16000)
                setAudioChannels(1)
                setOutputFile(recordingFile!!.absolutePath)
                prepare()
                start()
            }

            _recordingState.value = _recordingState.value.copy(
                isRecording = true,
                startTimeUtc = sessionStartTimeUtc,
                filePath = recordingFile!!.absolutePath,
                fileName = fileName,
                markerCount = 0
            )

            // Android 15 强类型前台麦克风服务启动
            startForeground(
                NOTIFICATION_ID,
                buildNotification(0, 0),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )

            startTimers()

            // 震动提示录音开始
            try {
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
                )
            } catch (_: Exception) {}

        } catch (e: Exception) {
            e.printStackTrace()
            stopRecording()
        }
    }

    private fun startTimers() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - sessionStartTimeUtc
                _recordingState.value = _recordingState.value.copy(durationMs = elapsed)
                updateNotification(elapsed, _recordingState.value.markerCount)
                delay(1000)
            }
        }

        amplitudeJob?.cancel()
        amplitudeJob = serviceScope.launch {
            while (isActive) {
                val amp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (_: Exception) {
                    0
                }
                _recordingState.value = _recordingState.value.copy(amplitude = amp)
                delay(100)
            }
        }
    }

    fun triggerMark() {
        if (!_recordingState.value.isRecording) return

        val newCount = _recordingState.value.markerCount + 1
        val elapsed = System.currentTimeMillis() - sessionStartTimeUtc
        _recordingState.value = _recordingState.value.copy(
            markerCount = newCount,
            lastMarkerOffsetMs = elapsed
        )

        // 触发物理短震动反馈
        try {
            vibratorManager.defaultVibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            )
        } catch (_: Exception) {}

        updateNotification(elapsed, newCount)
    }

    private fun stopRecording() {
        timerJob?.cancel()
        amplitudeJob?.cancel()

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaRecorder = null
        }

        _recordingState.value = QsoAudioRecordingState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "FieldQSO 通联录音服务",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "正在录制通联音频与时间锚点"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(durationMs: Long, markerCount: Int): Notification {
        val totalSec = durationMs / 1000
        val mm = totalSec / 60
        val ss = totalSec % 60
        val timeStr = "%02d:%02d".format(mm, ss)

        val openAppIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val markIntent = Intent(this, QsoAudioRecorderService::class.java).apply {
            action = ACTION_TRIGGER_MARK
        }
        val markPendingIntent = PendingIntent.getService(
            this, 1, markIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, QsoAudioRecorderService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔴 正在录制通联语音 ($timeStr)")
            .setContentText("已标记通联: $markerCount 次 · 通联记录自动锚定")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(android.R.drawable.ic_input_add, "＋ MARK 打点", markPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "⏹ 结束录音", stopPendingIntent)
            .build()
    }

    private fun updateNotification(durationMs: Long, markerCount: Int) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(durationMs, markerCount))
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        mediaRecorder?.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val CHANNEL_ID = "fieldqso_recorder_channel"
        const val NOTIFICATION_ID = 2001

        const val ACTION_START_RECORDING = "com.ham.qso.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.ham.qso.STOP_RECORDING"
        const val ACTION_TRIGGER_MARK = "com.ham.qso.TRIGGER_MARK"
        const val EXTRA_SESSION_TITLE = "extra_session_title"

        private val _recordingState = MutableStateFlow(QsoAudioRecordingState())
        val recordingState = _recordingState.asStateFlow()

        fun start(context: Context, title: String = "") {
            val intent = Intent(context, QsoAudioRecorderService::class.java).apply {
                action = ACTION_START_RECORDING
                putExtra(EXTRA_SESSION_TITLE, title)
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, QsoAudioRecorderService::class.java).apply {
                action = ACTION_STOP_RECORDING
            }
            context.startService(intent)
        }

        fun mark(context: Context) {
            val intent = Intent(context, QsoAudioRecorderService::class.java).apply {
                action = ACTION_TRIGGER_MARK
            }
            context.startService(intent)
        }
    }
}

data class QsoAudioRecordingState(
    val isRecording: Boolean = false,
    val startTimeUtc: Long = 0,
    val durationMs: Long = 0,
    val amplitude: Int = 0,
    val markerCount: Int = 0,
    val filePath: String = "",
    val fileName: String = "",
    val lastMarkerOffsetMs: Long? = null
)
