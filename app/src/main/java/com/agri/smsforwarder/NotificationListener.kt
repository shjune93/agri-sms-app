package com.agri.smsforwarder

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
        private const val TAG = "NotificationListener"
        private const val KAKAO_PKG = "com.kakao.talk"
        // 판매 관련 키워드
        private val KEYWORDS = listOf("판매내역", "출하내역", "거래일자", "동부청과", "동화청과", "판매금액", "공판장")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != KAKAO_PKG) return

        val extras = sbn.notification?.extras ?: return
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val bigText = extras.getCharSequence("android.bigText")?.toString() ?: ""

        val fullText = if (bigText.isNotEmpty()) bigText else text

        // 키워드 포함 여부 확인
        val hasKeyword = KEYWORDS.any { fullText.contains(it) || title.contains(it) }
        if (!hasKeyword) return

        Log.d(TAG, "카카오톡 판매 알림 감지: $title — $fullText")

        val prefs = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
        val enabled = prefs.getBoolean(MainActivity.KEY_ENABLED, false)
        if (!enabled) return

        val webhookUrl = prefs.getString(MainActivity.KEY_WEBHOOK_URL, "") ?: return
        if (webhookUrl.isEmpty()) return

        sendToWebhook(webhookUrl, "KakaoTalk:$title", fullText)
    }

    private fun sendToWebhook(url: String, sender: String, message: String) {
        val json = JSONObject().apply {
            put("sender", sender)
            put("message", message)
        }.toString()

        val client = OkHttpClient()
        val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "카카오 전송: ${response.code} — $responseBody")

                val success = response.isSuccessful
                LogStore.save(
                    applicationContext,
                    LogEntry(
                        time    = System.currentTimeMillis(),
                        sender  = sender,
                        message = message,
                        status  = if (success) "success" else "fail",
                        code    = response.code,
                        detail  = if (success) "✅ 서버 저장 완료 (${response.code})" else "⚠️ 서버 오류 (${response.code})",
                        source  = "kakao",
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "카카오 전송 오류: ${e.message}")
                LogStore.save(
                    applicationContext,
                    LogEntry(
                        time    = System.currentTimeMillis(),
                        sender  = sender,
                        message = message,
                        status  = "error",
                        code    = 0,
                        detail  = "❌ 네트워크 오류: ${e.message}",
                        source  = "kakao",
                    )
                )
            }
        }
    }
}
