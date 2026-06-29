package com.agri.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val prefs = context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(MainActivity.KEY_ENABLED, false)
        if (!enabled) return

        val webhookUrl = prefs.getString(MainActivity.KEY_WEBHOOK_URL, "") ?: return
        val filterRaw = prefs.getString(MainActivity.KEY_SENDER_FILTER, "") ?: ""

        // 발신번호 필터 목록 (쉼표 구분, 숫자만)
        val filters = filterRaw.split(",")
            .map { it.trim().replace(Regex("\\D"), "") }
            .filter { it.isNotEmpty() }

        // SMS 메시지 파싱
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        // 같은 발신자의 메시지를 하나로 합침
        val grouped = messages.groupBy { it.originatingAddress ?: "" }

        for ((address, msgs) in grouped) {
            val senderNorm = address.replace(Regex("\\D"), "")

            // 필터 체크: 필터가 없으면 모두 허용, 있으면 매칭되는 것만
            if (filters.isNotEmpty() && filters.none { senderNorm.endsWith(it) || it.endsWith(senderNorm) }) {
                Log.d(TAG, "발신번호 미매칭, 무시: $address")
                continue
            }

            val body = msgs.joinToString("") { it.messageBody ?: "" }
            Log.d(TAG, "SMS 수신: $address → 전송 시작")

            sendToWebhook(context, webhookUrl, address, body)
        }
    }

    private fun sendToWebhook(context: Context, url: String, sender: String, message: String) {
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

        // 백그라운드에서 전송 (메인 스레드 차단 방지)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""
                Log.d(TAG, "전송 성공: ${response.code} — $responseBody")

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(context, "✅ 판매내역 자동 저장됨", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "⚠️ 전송 실패: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "전송 오류: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "❌ 네트워크 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
