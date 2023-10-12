package com.example.fire_warning_app

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class UI_main : Fragment() {
    private lateinit var sensorService: SensorService
    private val sensorDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "SENSOR_DATA_UPDATE") {
                val smokeValue = intent.getFloatExtra("smokeValue", 0f)
                val temperatureValue = intent.getFloatExtra("temperatureValue", 0f)

                // Cập nhật giao diện với giá trị smokeValue và temperatureValue
                val smokeTextView: TextView = requireView().findViewById(R.id.sensor_gas)
                val temperatureTextView: TextView = requireView().findViewById(R.id.sensor_temp)
                smokeTextView.text = smokeValue.toString() + " V"
                temperatureTextView.text = temperatureValue.toString() + " ℃"
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_ui_main, container, false)
        val sharedPreferences =
            requireActivity().getSharedPreferences("MY_PREFS_NAME", Context.MODE_PRIVATE)

        // Khởi tạo dịch vụ
        sensorService = SensorService()

        // Bắt đầu dịch vụ
        requireActivity().startService(Intent(requireContext(), SensorService::class.java))
        val filter = IntentFilter("SENSOR_DATA_UPDATE")
        requireActivity().registerReceiver(sensorDataReceiver, filter)

        // Khởi tạo UserApi
        val userRetrofit = Retrofit.Builder()
            .baseUrl("https://anor.pythonanywhere.com/user/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val userApi = userRetrofit.create(UserApi::class.java)
        val sharedPref = requireActivity().getSharedPreferences("MY_PREFS_NAME", Context.MODE_PRIVATE)
        val email = sharedPref.getString("email", null)

        val handler = Handler(Looper.getMainLooper())
        val delayMillis2 = 2000L
        userApi.getUsers().enqueue(object: Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                val users = response.body()
                if (users != null) {
                    // Tìm kiếm người dùng có email cần tìm
                    val emailToFind = email
                    val foundUser = users.find { it.email == emailToFind }
                    if (foundUser != null) {
                        // Lấy thông tin người dùng
                        val userId = foundUser.id
                        val username = foundUser.username
                        val password = foundUser.password
                        // Tiếp tục xử lý tại đây
                        val editor = sharedPref.edit()
                        editor.putString("username", username)
                        editor.putString("password", password)
                        editor.apply()

                        val etusername: TextView = view.findViewById(R.id.et_user_name)
                        etusername.text = username
                    }
                } else {
                    Toast.makeText(requireContext(), "failed?", Toast.LENGTH_SHORT).show()
                }
                // Gọi lại API sau 2 giây
                handler.postDelayed({
                    userApi.getUsers().enqueue(this)
                }, delayMillis2)
            }
            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                // Xử lý lỗi tại đây

                // Gọi lại API sau 2 giây khi có lỗi xảy ra
                handler.postDelayed({
                    userApi.getUsers().enqueue(this)
                }, delayMillis2)
            }
        })

        return view

    }



    override fun onDestroy() {
        super.onDestroy()

        // Dừng dịch vụ khi Activity bị hủy
        requireActivity().stopService(Intent(requireContext(), SensorService::class.java))
        requireActivity().unregisterReceiver(sensorDataReceiver)
    }
}


