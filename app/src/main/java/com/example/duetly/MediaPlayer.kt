package com.example.duetly

import android.content.Context
import android.media.MediaPlayer

object MediaPlayerManager {
    private var mediaPlayer: MediaPlayer? = null

    fun initialize(context: Context, data: String) {
        try {
            val mediaPlayerS = MediaPlayer()
            mediaPlayerS.setDataSource(data)
            mediaPlayerS.prepare()
            mediaPlayer = mediaPlayerS
        } catch (e: Exception) {
            // Обробка помилок
            e.printStackTrace()
        }
    }
    fun getMediaPlayer(): MediaPlayer {
        if (mediaPlayer == null) {
            throw IllegalStateException("MediaPlayer is not initialized. Call initialize() first.")
        }
        return mediaPlayer!!
    }

    fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}