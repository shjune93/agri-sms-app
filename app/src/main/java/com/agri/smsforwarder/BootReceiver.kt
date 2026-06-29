package com.agri.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * 폰 재부팅 후 자동으로 SMS 수신 리시버가 다시 활성화되도록 합니다.
 * (Android에서 BroadcastReceiver는 앱이 한 번이라도 실행된 뒤에는 부팅 후에도 자동 등록됨)
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "부팅 완료 — SMS 수신 대기 중")
        }
    }
}
