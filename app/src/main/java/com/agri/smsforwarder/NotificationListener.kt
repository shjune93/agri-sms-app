package com.agri.smsforwarder

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class NotificationListener : NotificationListenerService() {

    companion object {
        private const val KAKAO_PACKAGE = "com.kakao.talk"
        private const val TAG = "NotificationListener"
        // 이 키워드가 포함된 카카오톡 알림만 전송
        private val KEYWORDS = listOf("판매내역 안내", "출하내역", "거래일자")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != KAKAO_PACKAGE) return

        val prefs = getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(MainActivity.KEY_ENABLED, false)
        if (!enabled) return

        val webhookUrl = prefs.getString(MainActivity.KEY_WEBHOOK_URL, "") ?: return
        if (webhookUrl.isEmpty()) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: text

        val fullText = "$title\n$bigText".trim()

        // 판매내역 키워드 포함 여부 확인
        val hasKeyword = KEYWORDS.any { fullText.contains(it) }
        if (!hasKeyword) return

        Log.d(TAG, "카카오톡 판매내역 알림 감지: $fullText")
        sendToWebhook(webhookUrl, "kakao", fullText)
    }

    private fun sendToWebhook(url: String, sender: String, message: String) {
        val json = JSONObject().apply {
            put("sender", sender)
            put("message", message)
        }.toString()

        val client = OkHttpClient()
        val body = json.toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(url).post(body).build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                Log.d(TAG, "전송 완료: ${response.code}")
            } catch (e: Exception) {
                Log.e(TAG, "전송 오류: ${e.message}")
            }
        }
    }
}
