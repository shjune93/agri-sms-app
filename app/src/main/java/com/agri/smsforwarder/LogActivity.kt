package com.agri.smsforwarder

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class LogActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var tvEmpty: TextView
    private lateinit var tvTotal: TextView
    private lateinit var tvSuccess: TextView
    private lateinit var tvFail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        listView = findViewById(R.id.listLogs)
        tvEmpty  = findViewById(R.id.tvEmpty)
        tvTotal  = findViewById(R.id.tvTotal)
        tvSuccess = findViewById(R.id.tvSuccess)
        tvFail   = findViewById(R.id.tvFail)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnClear).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("전체 삭제")
                .setMessage("전송 내역을 모두 삭제할까요?")
                .setPositiveButton("삭제") { _, _ ->
                    LogStore.clear(this)
                    loadLogs()
                }
                .setNegativeButton("취소", null)
                .show()
        }

        loadLogs()
    }

    override fun onResume() {
        super.onResume()
        loadLogs()
    }

    private fun loadLogs() {
        val logs = LogStore.load(this)
        val successCount = logs.count { it.status == "success" }
        val failCount    = logs.count { it.status != "success" }

        tvTotal.text   = "전체 ${logs.size}건"
        tvSuccess.text = "✅ $successCount"
        tvFail.text    = "❌ $failCount"

        if (logs.isEmpty()) {
            tvEmpty.visibility  = View.VISIBLE
            listView.visibility = View.GONE
        } else {
            tvEmpty.visibility  = View.GONE
            listView.visibility = View.VISIBLE
            listView.adapter = LogAdapter(this, logs)
        }
    }
}

class LogAdapter(
    private val ctx: Context,
    private val items: List<LogEntry>,
) : BaseAdapter() {

    private val timeFmt  = SimpleDateFormat("MM/dd HH:mm:ss", Locale.KOREA)
    private val dateFmt  = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

    override fun getCount() = items.size
    override fun getItem(pos: Int) = items[pos]
    override fun getItemId(pos: Int) = pos.toLong()

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: android.view.LayoutInflater.from(ctx)
            .inflate(R.layout.item_log, parent, false)

        val entry = items[pos]
        val date  = Date(entry.time)

        // 날짜 헤더 표시 여부 (이전 항목과 날짜가 다를 때)
        // (간단 구현: 현재 항목과 이전 항목의 날짜 비교)
        view.findViewById<TextView>(R.id.tvIcon).text = when {
            entry.status == "success" -> if (entry.source == "kakao") "💬" else "📩"
            entry.status == "fail"    -> "⚠️"
            else                      -> "❌"
        }

        view.findViewById<TextView>(R.id.tvSender).apply {
            text = if (entry.source == "kakao")
                entry.sender.removePrefix("KakaoTalk:")
            else
                entry.sender.ifEmpty { "(발신자 없음)" }
        }

        view.findViewById<TextView>(R.id.tvTime).text = timeFmt.format(date)

        view.findViewById<TextView>(R.id.tvMessage).text = entry.message

        view.findViewById<TextView>(R.id.tvDetail).apply {
            text = entry.detail
            setTextColor(
                ctx.getColor(
                    when (entry.status) {
                        "success" -> android.R.color.holo_green_dark
                        "fail"    -> android.R.color.holo_orange_dark
                        else      -> android.R.color.holo_red_dark
                    }
                )
            )
        }

        return view
    }
}
