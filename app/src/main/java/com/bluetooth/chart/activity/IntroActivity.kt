package com.bluetooth.chart.activity

import CheckPermissionManager
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bluetooth.chart.databinding.ActivityIntroBinding
import com.bluetooth.chart.module.interface_callback.PermissionResultCallback

class IntroActivity : AppCompatActivity(), PermissionResultCallback {
    final lateinit var binding: ActivityIntroBinding

    val REQUEST_ALL_PERMISSION = 2
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        // 풀 스크린 설정
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
        binding  = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)
       /* ViewCompat.setOnApplyWindowInsetsListener(binding.intro) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/


        binding.btnNext.setOnClickListener {
            CheckPermissionManager.checkAndRequestPermissions(this@IntroActivity,this)


        }
    }

    fun startBluetoothDiscovery() {
        // 권한이 이미 허용되었을 경우 블루투스 장치 검색 시작
        val intent = Intent(this@IntroActivity, MainActivity::class.java)
        startActivity(intent)
        finish();
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        CheckPermissionManager.handlePermissionsResult(requestCode, permissions,
            grantResults, this@IntroActivity, this)


    }

    override fun onPermissionsGranted() {
        startBluetoothDiscovery()
    }

    override fun onPermissionsDenied() {
    }
}