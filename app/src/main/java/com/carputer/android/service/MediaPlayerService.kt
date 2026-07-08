package com.carputer.android.service

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.audiofx.Visualizer
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.carputer.android.data.model.TrackInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class MediaPlayerService(private val context: Context) {

    companion object {
        private const val TAG = "MediaPlayerService"
    }

    private var player: ExoPlayer? = null
    private var visualizer: Visualizer? = null

    private val _playlist = MutableStateFlow<List<TrackInfo>>(emptyList())
    val playlist: StateFlow<List<TrackInfo>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _playing = MutableStateFlow(false)
    val playing: StateFlow<Boolean> = _playing.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentTrack = MutableStateFlow<TrackInfo?>(null)
    val currentTrack: StateFlow<TrackInfo?> = _currentTrack.asStateFlow()

    private val _volume = MutableStateFlow(80)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleOn = MutableStateFlow(false)
    val shuffleOn: StateFlow<Boolean> = _shuffleOn.asStateFlow()

    private val _spectrumData = MutableStateFlow(List(32) { -80f })
    val spectrumData: StateFlow<List<Float>> = _spectrumData.asStateFlow()

    private val _artworkBytes = MutableStateFlow<ByteArray?>(null)
    val artworkBytes: StateFlow<ByteArray?> = _artworkBytes.asStateFlow()

    private val audioExtensions = listOf("mp3", "flac", "wav", "ogg", "aac", "m4a", "wma")

    fun initPlayer() {
        if (player != null) return
        player = ExoPlayer.Builder(context).build().apply {
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _playing.value = isPlaying
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    updateCurrentTrackInfo()
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        if (_repeatMode.value == 2) {
                            seekTo(0)
                            play()
                        } else {
                            next()
                        }
                    }
                }
            })
            repeatMode = Player.REPEAT_MODE_OFF
        }
        setupVisualizer()
    }

    private fun setupVisualizer() {
        try {
            visualizer?.release()
            visualizer = Visualizer(0).apply {
                captureSize = Visualizer.getCaptureSizeRange()[0]
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int
                        ) {}

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int
                        ) {
                            if (fft != null) {
                                val magnitudes = (0 until 32).map { i ->
                                    val real = fft.getOrElse(i * 2) { 0 }.toFloat()
                                    val imag = fft.getOrElse(i * 2 + 1) { 0 }.toFloat()
                                    val mag = 20f * kotlin.math.log10(
                                        kotlin.math.sqrt(real * real + imag * imag).coerceAtLeast(1f)
                                    )
                                    mag.coerceIn(-80f, 0f)
                                }
                                _spectrumData.value = magnitudes
                            }
                        }
                    },
                    Visualizer.getMaxCaptureRate() / 2,
                    false,
                    true
                )
                enabled = true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Visualizer not available", e)
        }
    }

    fun scanMedia(path: String) {
        val dir = File(path)
        if (!dir.isDirectory) return
        val tracks = dir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in audioExtensions }
            .sortedBy { it.name }
            .map { file ->
                TrackInfo(
                    filePath = file.absolutePath,
                    fileName = file.nameWithoutExtension,
                    dirName = file.parentFile?.name ?: "",
                )
            }
            .toList()
        _playlist.value = tracks
        if (tracks.isNotEmpty() && _currentIndex.value < 0) {
            _currentIndex.value = 0
            playTrack(0)
        }
    }

    fun play() {
        player?.play()
    }

    fun pause() {
        player?.pause()
    }

    fun stop() {
        player?.stop()
        _playing.value = false
    }

    fun next() {
        val list = _playlist.value
        if (list.isEmpty()) return
        val idx = _currentIndex.value
        if (_shuffleOn.value) {
            val nextIdx = (idx + 1..list.indices.last).randomOrNull()
                ?: if (_repeatMode.value >= 1) 0 else return
            playTrack(nextIdx)
        } else {
            val nextIdx = idx + 1
            if (nextIdx >= list.size) {
                if (_repeatMode.value >= 1) playTrack(0) else return
            } else {
                playTrack(nextIdx)
            }
        }
    }

    fun previous() {
        val list = _playlist.value
        if (list.isEmpty()) return
        val idx = _currentIndex.value
        val prevIdx = idx - 1
        if (prevIdx < 0) {
            if (_repeatMode.value >= 1) playTrack(list.size - 1) else return
        } else {
            playTrack(prevIdx)
        }
    }

    fun seek(positionMs: Long) {
        player?.seekTo(positionMs)
    }

    fun playTrack(index: Int) {
        val list = _playlist.value
        if (index !in list.indices) return
        _currentIndex.value = index
        val track = list[index]
        val mediaItem = MediaItem.fromUri(Uri.fromFile(File(track.filePath)))
        player?.apply {
            setMediaItem(mediaItem)
            prepare()
            play()
        }
        updateTrackMetadata(track)
    }

    fun setVolume(vol: Int) {
        val clamped = vol.coerceIn(0, 100)
        _volume.value = clamped
        player?.volume = clamped / 100f
    }

    fun setRepeatMode(mode: Int) {
        _repeatMode.value = mode.coerceIn(0, 2)
        player?.repeatMode = when (mode) {
            1 -> Player.REPEAT_MODE_ALL
            2 -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun setShuffleOn(on: Boolean) {
        _shuffleOn.value = on
    }

    private fun updateCurrentTrackInfo() {
        val idx = _currentIndex.value
        if (idx in _playlist.value.indices) {
            updateTrackMetadata(_playlist.value[idx])
        }
    }

    private fun updateTrackMetadata(track: TrackInfo) {
        _currentTrack.value = track
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, Uri.fromFile(File(track.filePath)))
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val artwork = retriever.embeddedPicture

            _currentTrack.value = track.copy(
                title = title ?: track.fileName,
                artist = artist ?: "",
                album = album ?: "",
                duration = durationStr?.toLongOrNull() ?: 0L,
                hasArtwork = artwork != null
            )
            _artworkBytes.value = artwork
            _duration.value = durationStr?.toLongOrNull() ?: 0L

            player?.let { p ->
                p.playbackParameters = p.playbackParameters
            }

            retriever.release()
        } catch (e: Exception) {
            Log.w(TAG, "Metadata error", e)
        }
    }

    fun release() {
        visualizer?.release()
        visualizer = null
        player?.release()
        player = null
    }
}
