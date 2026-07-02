package com.agri.smsforwarder

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class LogEntry(
    val time: Long,
    val sender: String,
    val message: String,
    val status: String,   // "success" | "fail" | "error"
    val code: Int,        // HTTP 응답 코드 (오류 시 0)
    val detail: String,   // 상세 메시지
    val source: String,   // "sms" | "kakao"
)

object LogStore {
    private const val PREFS = "forward_log"
    private const val KEY_LOGS = "logs"
    private const val MAX_ENTRIES = 100

    fun save(context: Context, entry: LogEntry) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_LOGS, "[]") ?: "[]"
        val arr = try { JSONArray(existing) } catch (e: Exception) { JSONArray() }

        val obj = JSONObject().apply {
            put("time", entry.time)
            put("sender", entry.sender)
            put("message", entry.message)
            put("status", entry.status)
            put("code", entry.code)
            put("detail", entry.detail)
            put("source", entry.source)
        }

        // 새 항목을 맨 앞에 삽입
        val newArr = JSONArray()
        newArr.put(obj)
        val limit = minOf(arr.length(), MAX_ENTRIES - 1)
        for (i in 0 until limit) newArr.put(arr.get(i))

        prefs.edit().putString(KEY_LOGS, newArr.toString()).apply()
    }

    fun load(context: Context): List<LogEntry> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LOGS, "[]") ?: "[]"
        val arr = try { JSONArray(raw) } catch (e: Exception) { return emptyList() }
        val result = mutableListOf<LogEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            result.add(
                LogEntry(
                    time   = o.optLong("time", 0L),
                    sender = o.optString("sender", ""),
                    message = o.optString("message", ""),
                    status = o.optString("status", "error"),
                    code   = o.optInt("code", 0),
                    detail = o.optString("detail", ""),
                    source = o.optString("source", "sms"),
                )
            )
        }
        return result
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_LOGS).apply()
    }
}
