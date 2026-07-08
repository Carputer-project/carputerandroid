package com.carputer.android.viewmodel

import androidx.lifecycle.ViewModel
import com.carputer.android.data.ConfigRepository
import com.carputer.android.service.MediaPlayerService
import kotlinx.coroutines.flow.*

class MediaViewModel(
    private val mediaPlayerService: MediaPlayerService,
    private val configRepository: ConfigRepository,
) : ViewModel() {

    val playlist = mediaPlayerService.playlist
    val currentIndex = mediaPlayerService.currentIndex
    val playing = mediaPlayerService.playing
    val position = mediaPlayerService.position
    val duration = mediaPlayerService.duration
    val currentTrack = mediaPlayerService.currentTrack
    val volume = mediaPlayerService.volume
    val repeatMode = mediaPlayerService.repeatMode
    val shuffleOn = mediaPlayerService.shuffleOn
    val spectrumData = mediaPlayerService.spectrumData
    val artworkBytes = mediaPlayerService.artworkBytes

    fun scanMedia(path: String) = mediaPlayerService.scanMedia(path)
    fun play() = mediaPlayerService.play()
    fun pause() = mediaPlayerService.pause()
    fun next() = mediaPlayerService.next()
    fun previous() = mediaPlayerService.previous()
    fun seek(pos: Long) = mediaPlayerService.seek(pos)
    fun playTrack(index: Int) = mediaPlayerService.playTrack(index)
    fun setVolume(vol: Int) {
        mediaPlayerService.setVolume(vol)
        configRepository.audioVolume = vol
    }
    fun setRepeatMode(mode: Int) {
        mediaPlayerService.setRepeatMode(mode)
        configRepository.repeatMode = mode
    }
    fun setShuffleOn(on: Boolean) {
        mediaPlayerService.setShuffleOn(on)
        configRepository.shuffleOn = on
    }
}
