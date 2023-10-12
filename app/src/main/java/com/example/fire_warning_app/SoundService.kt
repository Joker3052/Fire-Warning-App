package com.example.fire_warning_app

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.IBinder

class SoundService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private lateinit var sharedPref: SharedPreferences

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        sharedPref = getSharedPreferences("MY_PREFS_NAME", Context.MODE_PRIVATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            "PLAY_SOUND" -> {
                // Kích hoạt âm thanh loa
                playSound()
            }
            "STOP_SOUND" -> {
                // Vô hiệu hóa âm thanh loa
                stopSound()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSound()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Đảm bảo âm thanh vẫn được phát khi ứng dụng bị dừng hoặc thoát khỏi giao diện người dùng
        startForeground(1, null)
    }

    private fun playSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(this, R.raw.sound_alarm)
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
        }
    }

    private fun stopSound() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }
}
