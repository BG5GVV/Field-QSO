package com.ham.qso.domain.audio

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AudioPlayerManager(private val context: Context) {

    private var exoPlayer: ExoPlayer? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(AudioPlayerState())
    val playerState = _playerState.asStateFlow()

    init {
        initPlayer()
    }

    private fun initPlayer() {
        exoPlayer = ExoPlayer.Builder(context)
            .setSeekParameters(SeekParameters.EXACT)
            .build().apply {
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
                        if (isPlaying) startProgressPolling() else stopProgressPolling()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) {
                            _playerState.value = _playerState.value.copy(
                                isPlaying = false,
                                activeQsoId = null
                            )
                            stopProgressPolling()
                        }
                    }
                })
            }
    }

    fun playQsoAudio(qsoId: Long, filePath: String, offsetMs: Long) {
        val file = File(filePath)
        if (!file.exists()) {
            _playerState.value = _playerState.value.copy(
                errorMessage = "录音文件不存在或已删除"
            )
            return
        }

        val player = exoPlayer ?: return
        val targetOffsetMs = maxOf(0L, offsetMs - 3000L) // 预留 3 秒提前量

        val mediaItem = MediaItem.fromUri(file.toURI().toString())
        player.setMediaItem(mediaItem)
        player.prepare()
        player.seekTo(targetOffsetMs)
        player.play()

        _playerState.value = _playerState.value.copy(
            activeQsoId = qsoId,
            currentFilePath = filePath,
            currentPositionMs = targetOffsetMs,
            isPlaying = true,
            errorMessage = null
        )
    }

    fun playRecordingFile(filePath: String, startOffsetMs: Long = 0L) {
        val file = File(filePath)
        if (!file.exists()) {
            _playerState.value = _playerState.value.copy(
                errorMessage = "录音文件不存在或已删除"
            )
            return
        }

        val player = exoPlayer ?: return
        val targetOffsetMs = maxOf(0L, startOffsetMs)

        val mediaItem = MediaItem.fromUri(file.toURI().toString())
        player.setMediaItem(mediaItem)
        player.prepare()
        player.seekTo(targetOffsetMs)
        player.play()

        _playerState.value = _playerState.value.copy(
            activeQsoId = -1L,
            currentFilePath = filePath,
            currentPositionMs = targetOffsetMs,
            isPlaying = true,
            errorMessage = null
        )
    }

    fun playPause() {
        val player = exoPlayer ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun stop() {
        exoPlayer?.stop()
        _playerState.value = _playerState.value.copy(
            isPlaying = false,
            activeQsoId = null
        )
        stopProgressPolling()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
    }

    fun forward5s() {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition + 5000L).coerceAtMost(player.duration)
        player.seekTo(newPos)
    }

    fun rewind5s() {
        val player = exoPlayer ?: return
        val newPos = (player.currentPosition - 5000L).coerceAtLeast(0L)
        player.seekTo(newPos)
    }

    fun clearError() {
        _playerState.value = _playerState.value.copy(errorMessage = null)
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            while (isActive) {
                exoPlayer?.let { p ->
                    _playerState.value = _playerState.value.copy(
                        currentPositionMs = p.currentPosition,
                        totalDurationMs = p.duration.coerceAtLeast(0L)
                    )
                }
                delay(200)
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
    }

    fun release() {
        stopProgressPolling()
        coroutineScope.cancel()
        exoPlayer?.release()
        exoPlayer = null
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioPlayerManager? = null

        fun getInstance(context: Context): AudioPlayerManager {
            return INSTANCE ?: synchronized(this) {
                val inst = AudioPlayerManager(context.applicationContext)
                INSTANCE = inst
                inst
            }
        }
    }
}

data class AudioPlayerState(
    val isPlaying: Boolean = false,
    val activeQsoId: Long? = null,
    val currentFilePath: String = "",
    val currentPositionMs: Long = 0,
    val totalDurationMs: Long = 0,
    val errorMessage: String? = null
)
