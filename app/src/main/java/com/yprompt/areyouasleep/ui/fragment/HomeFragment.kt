package com.yprompt.areyouasleep.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.yprompt.areyouasleep.data.database.AppDatabase
import com.yprompt.areyouasleep.data.model.DailyRecord
import com.yprompt.areyouasleep.data.preferences.UserPreferencesRepository
import com.yprompt.areyouasleep.databinding.FragmentHomeBinding
import com.yprompt.areyouasleep.logic.SleepAnalysisResult
import com.yprompt.areyouasleep.logic.SleepDetectionManager
import com.yprompt.areyouasleep.logic.StatisticsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var sleepDetectionManager: SleepDetectionManager
    private lateinit var statsManager: StatisticsManager
    private lateinit var db: AppDatabase
    private lateinit var prefsRepo: UserPreferencesRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        sleepDetectionManager = SleepDetectionManager(context)
        statsManager = StatisticsManager(context)
        db = AppDatabase.getDatabase(context)
        prefsRepo = UserPreferencesRepository(context)

        setupUI()
        loadTodayData()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupUI() {
        val today = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("M月d日 EEEE", Locale.CHINA)
        binding.tvDate.text = dateFormat.format(today.time)

        binding.btnCheckInCircle.setOnClickListener {
            performCheckIn()
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
                updateUIForCheckedIn(record)
            } else {
                binding.tvGreeting.text = "今晚准备几点睡？"
                binding.tvStatusSubtitle.text = "点击按钮打卡，记录美好睡眠"
                binding.btnCheckInCircle.isEnabled = true
                binding.btnCheckInCircle.setCardBackgroundColor(0xFF007AFF.toInt())
                binding.tvBtnText.text = "睡觉打卡"
                analyzeYesterdaySleep()
            }
        }
    }

    private fun updateUIForCheckedIn(record: DailyRecord) {
        binding.tvGreeting.text = if (record.isStayUpLate) "昨晚熬夜了 ✗" else "昨晚睡得不错 ✓"
        binding.tvStatusSubtitle.text = "已完成今日打卡"
        binding.btnCheckInCircle.isEnabled = false
        binding.btnCheckInCircle.setCardBackgroundColor(0xFF34C759.toInt())
        binding.tvBtnText.text = "已打卡"

        if (record.lastScreenOffTime != null) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            binding.tvLastSleepTime.visibility = View.VISIBLE
            binding.tvLastSleepTime.text = "最后玩手机: ${timeFormat.format(record.lastScreenOffTime)}"
        } else {
            binding.tvLastSleepTime.visibility = View.GONE
        }
    }

    private fun analyzeYesterdaySleep() {
        lifecycleScope.launch {
            if (!sleepDetectionManager.hasUsageStatsPermission()) {
                return@launch
            }

            val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            val prefs = prefsRepo.userPreferences.first()
            val result = sleepDetectionManager.analyzeSleep(yesterday, prefs)

            if (result is SleepAnalysisResult.Success) {
                binding.tvGreeting.text = if (result.isStayUpLate) "检测到昨晚熬夜了" else "检测到昨晚睡得不错"
                if (result.lastInteractionTime != null) {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    binding.tvLastSleepTime.visibility = View.VISIBLE
                    binding.tvLastSleepTime.text = "检测到最后玩手机: ${timeFormat.format(result.lastInteractionTime)}"
                }
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

            updateUIForCheckedIn(record)
            Toast.makeText(context, "打卡成功！", Toast.LENGTH_SHORT).show()
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
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
