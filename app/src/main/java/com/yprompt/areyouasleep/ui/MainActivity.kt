package com.yprompt.areyouasleep.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yprompt.areyouasleep.data.database.AppDatabase
import com.yprompt.areyouasleep.data.model.DailyRecord
import com.yprompt.areyouasleep.data.preferences.UserPreferencesRepository
import com.yprompt.areyouasleep.databinding.ActivityMainBinding
import com.yprompt.areyouasleep.logic.SleepAnalysisResult
import com.yprompt.areyouasleep.logic.SleepDetectionManager
import com.yprompt.areyouasleep.logic.StatisticsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var sleepDetectionManager: SleepDetectionManager
    private lateinit var statsManager: StatisticsManager
    private lateinit var db: AppDatabase
    private lateinit var prefsRepo: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sleepDetectionManager = SleepDetectionManager(this)
        statsManager = StatisticsManager(this)
        db = AppDatabase.getDatabase(this)
        prefsRepo = UserPreferencesRepository(this)

        setupUI()
        loadTodayData()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupUI() {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
        binding.tvDate.text = dateFormat.format(today.time)

        binding.btnCheckIn.setOnClickListener {
            performCheckIn()
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.tvPermissionHint.setOnClickListener {
            requestUsageStatsPermission()
        }
    }

    private fun loadTodayData() {
        lifecycleScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
            val record = db.dailyRecordDao().getRecordByDate(today).first()

            if (record?.didCheckIn == true) {
                binding.tvStatus.text = if (record.isStayUpLate) "昨晚熬夜了 ✗" else "昨晚睡得不错 ✓"
                binding.btnCheckIn.text = "已打卡"
                binding.btnCheckIn.isEnabled = false
            } else {
                analyzeYesterdaySleep()
            }

            loadMonthlyStats()
        }
    }

    private fun analyzeYesterdaySleep() {
        lifecycleScope.launch {
            if (!sleepDetectionManager.hasUsageStatsPermission()) {
                binding.tvStatus.text = "点击打卡记录睡眠"
                return@launch
            }

            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val prefs = prefsRepo.userPreferences.first()
            val result = sleepDetectionManager.analyzeSleep(yesterday, prefs)

            if (result is SleepAnalysisResult.Success) {
                binding.tvStatus.text = if (result.isStayUpLate) "检测到昨晚熬夜了" else "检测到昨晚睡得不错"
            }
        }
    }

    private fun performCheckIn() {
        lifecycleScope.launch {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val prefs = prefsRepo.userPreferences.first()

            var isStayUpLate = false
            var lastInteractionTime: Long? = null

            if (sleepDetectionManager.hasUsageStatsPermission()) {
                val result = sleepDetectionManager.analyzeSleep(yesterday, prefs)
                if (result is SleepAnalysisResult.Success) {
                    isStayUpLate = result.isStayUpLate
                    lastInteractionTime = result.lastInteractionTime
                }
            }

            val record = DailyRecord(
                date = today,
                didCheckIn = true,
                checkInTimestamp = System.currentTimeMillis(),
                isStayUpLate = isStayUpLate,
                lastScreenOffTime = lastInteractionTime
            )

            db.dailyRecordDao().insertOrUpdate(record)
            statsManager.updateMonthlyStats(today)

            binding.tvStatus.text = if (isStayUpLate) "昨晚熬夜了 ✗" else "昨晚睡得不错 ✓"
            binding.btnCheckIn.text = "已打卡"
            binding.btnCheckIn.isEnabled = false

            Toast.makeText(this@MainActivity, "打卡成功！", Toast.LENGTH_SHORT).show()
            loadMonthlyStats()
        }
    }

    private fun loadMonthlyStats() {
        lifecycleScope.launch {
            val month = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
            val stat = db.monthlyStatDao().getStatByMonth(month).first()

            if (stat != null) {
                binding.tvStats.text = "打卡: ${stat.totalCheckIns}天\n熬夜: ${stat.totalStayUpLateDays}天\n连续打卡: ${stat.maxConsecutiveCheckIns}天"
            } else {
                binding.tvStats.text = "暂无数据"
            }
        }
    }

    private fun updatePermissionStatus() {
        if (!sleepDetectionManager.hasUsageStatsPermission()) {
            binding.tvPermissionHint.visibility = View.VISIBLE
        } else {
            binding.tvPermissionHint.visibility = View.GONE
        }
    }

    private fun requestUsageStatsPermission() {
        Toast.makeText(this, "请授予使用统计权限以启用自动检测", Toast.LENGTH_LONG).show()
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }
}
