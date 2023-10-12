package com.example.fire_warning_app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class SoundActivity : Fragment() {
    private lateinit var sharedPref: SharedPreferences
    private lateinit var stopSoundButton: Button
    private var isPlaying: Boolean = false
    private var mediaPlayer: MediaPlayer? = null

    private val handler = Handler()
    private val delayMillis = 1000 // 1 second

    private val updateButtonRunnable = object : Runnable {
        override fun run() {
            val fireDetectedValue = sharedPref.getBoolean("fireDetectedValue", false)
            val smokeValue = sharedPref.getFloat("smokeValue", 0.0f)
            updateButtonState(fireDetectedValue || smokeValue > 0)

            handler.postDelayed(this, delayMillis.toLong())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_sound, container, false)
        sharedPref = requireActivity().getSharedPreferences("MY_PREFS_NAME", Context.MODE_PRIVATE)

        stopSoundButton = view.findViewById(R.id.stopSoundButton)
        stopSoundButton.setOnClickListener { stopSound(view) }

        return view
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(updateButtonRunnable, delayMillis.toLong())
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateButtonRunnable)
    }

    private fun updateButtonState(isPlaying: Boolean) {
        this.isPlaying = isPlaying
        val fireDetectedTextView: TextView = requireView().findViewById(R.id.fireDetectedTextView)
        if (isPlaying) {
            fireDetectedTextView.text = "FIRE WARNING"
            // Phát âm thanh
            playSound()
        } else {
            fireDetectedTextView.text = ""
            // Tắt âm thanh
            stopSound()
        }
    }

    private fun playSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(requireContext(), R.raw.sound_alarm)
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

    override fun onDestroy() {
        super.onDestroy()
        stopSound()
    }

    private fun stopSound(view: View) {
        if (isPlaying) {
            // Gửi thông báo tới SoundService để tắt âm thanh
            val intent = Intent(requireActivity(), SoundService::class.java)
            intent.action = "STOP_SOUND"
            requireActivity().startService(intent)
            updateButtonState(false)
            val fireDetectedTextView: TextView? = view.findViewById(R.id.fireDetectedTextView)
            fireDetectedTextView?.text = "Mute"
        } else {
            val fireDetectedTextView: TextView? = view.findViewById(R.id.fireDetectedTextView)
            fireDetectedTextView?.text = ""
        }

        // Cập nhật giá trị fireDetectedValue trong sharedPref
        sharedPref.edit().putBoolean("fireDetectedValue", false).apply()
    }
}
