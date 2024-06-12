package com.example.duetly

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class MusicService : Service() {

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaPlayer: MediaPlayer

    override fun onCreate() {
        super.onCreate()
        mediaPlayer = MediaPlayerManager.getMediaPlayer()
        updateNotification()
        mediaSession = MediaSessionCompat(this, "MusicService")
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                super.onPlay()
                mediaPlayer.start()
                updateNotification()
            }

            override fun onPause() {
                super.onPause()
                mediaPlayer.pause()
                updateNotification()
            }

            override fun onSkipToNext() {
                super.onSkipToNext()
                // Додайте логіку для перемикання на наступний трек
                updateNotification()
            }

            override fun onSkipToPrevious() {
                super.onSkipToPrevious()
                // Додайте логіку для перемикання на попередній трек
                updateNotification()
            }
        })
        mediaSession.isActive = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleIntent(intent)
        return START_STICKY
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null || intent.action == null) return

        when (intent.action) {
            ACTION_PLAY -> mediaSession.controller.transportControls.play()
            ACTION_PAUSE -> mediaSession.controller.transportControls.pause()
            ACTION_NEXT -> mediaSession.controller.transportControls.skipToNext()
            ACTION_PREVIOUS -> mediaSession.controller.transportControls.skipToPrevious()
        }
    }


    private fun updateNotification() {
        val remoteViews = RemoteViews(packageName, R.layout.notyfication_custom_layout).apply {
            setTextViewText(R.id.notification_title, "Current Song Title")
            setTextViewText(R.id.notification_artist, "Current Artist")
            setImageViewResource(R.id.notification_icon, R.drawable.music_note_4_svgrepo_com)
            setOnClickPendingIntent(R.id.notification_prev, getPendingIntent(ACTION_PREVIOUS))
            setOnClickPendingIntent(R.id.notification_play_pause, getPendingIntent(if (mediaPlayer.isPlaying) ACTION_PAUSE else ACTION_PLAY))
            setOnClickPendingIntent(R.id.notification_next, getPendingIntent(ACTION_NEXT))
            setImageViewResource(R.id.notification_play_pause, if (mediaPlayer.isPlaying) R.drawable.pause_square_svgrepo_com else R.drawable.play_square_svgrepo_com)
        }

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.music_note_4_svgrepo_com)
            .setCustomContentView(remoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomBigContentView(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun getPendingIntent(action: String): PendingIntent {
        val intent = Intent(this, MusicService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        mediaSession.release()
        mediaPlayer.release()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.example.duetly.ACTION_PLAY"
        const val ACTION_PAUSE = "com.example.duetly.ACTION_PAUSE"
        const val ACTION_NEXT = "com.example.duetly.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.example.duetly.ACTION_PREVIOUS"
        const val CHANNEL_ID = "music_channel"
        const val NOTIFICATION_ID = 2
    }

}
