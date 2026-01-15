package com.yprompt.areyouasleep.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.yprompt.areyouasleep.data.database.AppDatabase
import com.yprompt.areyouasleep.databinding.FragmentTrendsBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TrendsFragment : Fragment() {

    private var _binding: FragmentTrendsBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTrendsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())

        loadMonthlyStats()
        loadRecentHistory()
    }

    private fun loadMonthlyStats() {
        lifecycleScope.launch {
            val month = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
            val stat = db.monthlyStatDao().getStatByMonth(month).first()

            if (stat != null) {
                binding.tvMonthlyStats.text = "本月打卡: ${stat.totalCheckIns}天\n熬夜天数: ${stat.totalStayUpLateDays}天\n最长连续: ${stat.maxConsecutiveCheckIns}天"
            } else {
                binding.tvMonthlyStats.text = "本月暂无数据"
            }
        }
    }

    private fun loadRecentHistory() {
        lifecycleScope.launch {
            val calendar = Calendar.getInstance()
            val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, -6)
            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)

            val records = db.dailyRecordDao().getRecordsForPeriod(startDate, endDate).first()

            binding.chartView.setData(records)

            binding.layoutHistoryContainer.removeAllViews()

            if (records.isEmpty()) {
                val emptyView = TextView(context).apply {
                    text = "暂无最近记录"
                    setPadding(0, 32, 0, 32)
                    gravity = Gravity.CENTER
                }
                binding.layoutHistoryContainer.addView(emptyView)
                return@launch
            }

            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            records.forEach { record ->
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setPadding(0, 24, 0, 24)
                }

                val dateView = TextView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    text = record.date
                    textSize = 16f
                    setTextColor(Color.BLACK)
                }

                val statusView = TextView(context).apply {
                    text = if (record.isStayUpLate) "熬夜 ✗  " else "早睡 ✓  "
                    textSize = 16f
                    setTextColor(if (record.isStayUpLate) Color.RED else Color.parseColor("#34C759"))
                }

                val timeView = TextView(context).apply {
                    text = if (record.lastScreenOffTime != null) timeFormat.format(record.lastScreenOffTime) else "--:--"
                    textSize = 16f
                    setTextColor(Color.GRAY)
                }

                row.addView(dateView)
                row.addView(statusView)
                row.addView(timeView)

                binding.layoutHistoryContainer.addView(row)

                val divider = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(Color.parseColor("#F2F2F7"))
                }
                binding.layoutHistoryContainer.addView(divider)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
