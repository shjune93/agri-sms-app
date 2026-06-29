package com.agri.smsforwarder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val PREFS = "sms_forwarder"
        const val KEY_WEBHOOK_URL = "webhook_url"
        const val KEY_SENDER_FILTER = "sender_filter"
        const val KEY_ENABLED = "enabled"
        const val REQUEST_SMS = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        val etUrl = findViewById<EditText>(R.id.etWebhookUrl)
        val etFilter = findViewById<EditText>(R.id.etSenderFilter)
        val swEnabled = findViewById<Switch>(R.id.swEnabled)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnNotification = findViewById<Button>(R.id.btnNotificationAccess)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvNotiStatus = findViewById<TextView>(R.id.tvNotiStatus)

        etUrl.setText(prefs.getString(KEY_WEBHOOK_URL, ""))
        etFilter.setText(prefs.getString(KEY_SENDER_FILTER, ""))
        swEnabled.isChecked = prefs.getBoolean(KEY_ENABLED, false)

        updateStatus(tvStatus, prefs.getBoolean(KEY_ENABLED, false))

        btnSave.setOnClickListener {
            val url = etUrl.text.toString().trim()
            val filter = etFilter.text.toString().trim()
            val enabled = swEnabled.isChecked

            if (enabled && url.isEmpty()) {
                Toast.makeText(this, "Webhook URL을 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            prefs.edit()
                .putString(KEY_WEBHOOK_URL, url)
                .putString(KEY_SENDER_FILTER, filter)
                .putBoolean(KEY_ENABLED, enabled)
                .apply()

            updateStatus(tvStatus, enabled)
            Toast.makeText(this, "저장됐습니다", Toast.LENGTH_SHORT).show()

            if (enabled) requestSmsPermission()
        }

        // 카카오톡 알림 접근 권한 버튼
        btnNotification.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        swEnabled.setOnCheckedChangeListener { _, checked -> updateStatus(tvStatus, checked) }

        updateNotiStatus(tvNotiStatus)
    }

    override fun onResume() {
        super.onResume()
        val tvNotiStatus = findViewById<TextView>(R.id.tvNotiStatus)
        updateNotiStatus(tvNotiStatus)
    }

    private fun updateNotiStatus(tv: TextView) {
        val granted = isNotificationListenerEnabled()
        tv.text = if (granted) "✅ 카카오톡 알림 수신 허용됨" else "⚠️ 카카오톡 알림 권한 없음 (아래 버튼 탭)"
        tv.setTextColor(
            ContextCompat.getColor(this, if (granted) android.R.color.holo_green_dark else android.R.color.holo_orange_dark)
        )
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return enabledListeners.contains(packageName)
    }

    private fun updateStatus(tv: TextView, enabled: Boolean) {
        tv.text = if (enabled) "✅ 전송 활성화됨" else "⏸ 전송 비활성화됨"
        tv.setTextColor(
            ContextCompat.getColor(this, if (enabled) android.R.color.holo_green_dark else android.R.color.darker_gray)
        )
    }

    private fun requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS), REQUEST_SMS)
        }
    }
}
