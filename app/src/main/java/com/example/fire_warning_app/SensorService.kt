package com.example.fire_warning_app

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class SensorService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val delayMillis = 1000L // Thời gian chờ giữa các lần gọi API

    // Khởi tạo SensorApi
    private val sensorApi: SensorApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://anor.pythonanywhere.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SensorApi::class.java)
    }

    override fun onCreate() {
        super.onCreate()
        // Khởi tạo dịch vụ
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Thực hiện tác vụ ngầm trong onStartCommand
        startBackgroundTask()

        // Trả về cách thức khởi động lại dịch vụ khi bị gián đoạn
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        // Trả về liên kết dịch vụ (nếu cần)
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hủy tác vụ ngầm khi dịch vụ bị hủy
        handler.removeCallbacksAndMessages(null)
    }

    private fun startBackgroundTask() {
        // Gọi API và xử lý dữ liệu cảm biến
        handler.postDelayed({
            sensorApi.getSensorData().enqueue(object : Callback<SensorApi.SensorData> {
                override fun onResponse(
                    call: Call<SensorApi.SensorData>,
                    response: Response<SensorApi.SensorData>
                ) {
                    if (response.isSuccessful) {
                        val sensorData = response.body()
                        if (sensorData != null) {
                            // Xử lý dữ liệu cảm biến ở đây
                            val fireDetectedValue = sensorData.fireDetectedValue
                            val smokeValue = sensorData.smokeValue
                            val temperatureValue = sensorData.temperatureValue

                            // Lưu giá trị smokeValue và temperatureValue vào SharedPreferences
                            val sharedPref = getSharedPreferences("MY_PREFS_NAME", MODE_PRIVATE)
                            val editor = sharedPref.edit()
                            editor.putFloat("smokeValue", smokeValue)
                            editor.putFloat("temperatureValue", temperatureValue)
                            editor.putBoolean("fireDetectedValue", fireDetectedValue)
                            editor.apply()
                            // Ví dụ: gửi thị giá trị smokeValue và temperatureValue qua ui_main
                            val intent = Intent("SENSOR_DATA_UPDATE")
                            intent.putExtra("smokeValue", smokeValue)
                            intent.putExtra("temperatureValue", temperatureValue)
                            sendBroadcast(intent)
                        }
                    } else {
                        // Xử lý lỗi khi không thành công
                        Toast.makeText(this@SensorService, "sensor failed", Toast.LENGTH_SHORT).show()
                    }

                    // Gọi lại API sau một khoảng thời gian delayMillis
                    startBackgroundTask()
                }

                override fun onFailure(call: Call<SensorApi.SensorData>, t: Throwable) {
                    // Xử lý lỗi kết nối
                    Toast.makeText(this@SensorService, "Sensor failed", Toast.LENGTH_SHORT).show()

                    // Gọi lại API sau một khoảng thời gian delayMillis khi có lỗi xảy ra
                    startBackgroundTask()
                }
            })
        }, delayMillis)
    }
    private fun sendDataToUI(fireDetectedValue: Boolean, smokeValue: Float) {
        val intent = Intent("SENSOR_DATA_UPDATE")
        intent.putExtra("fireDetectedValue", fireDetectedValue)
        intent.putExtra("smokeValue", smokeValue)
        sendBroadcast(intent)
    }

}