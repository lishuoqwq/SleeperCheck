package com.yprompt.areyouasleep.ui

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.yprompt.areyouasleep.data.preferences.UserPreferencesRepository
import com.yprompt.areyouasleep.databinding.ActivitySettingsBinding
import com.yprompt.areyouasleep.logic.ReminderManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefsRepo: UserPreferencesRepository
    private lateinit var reminderManager: ReminderManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsRepo = UserPreferencesRepository(this)
        reminderManager = ReminderManager(this)

        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        lifecycleScope.launch {
            val prefs = prefsRepo.userPreferences.first()

            binding.tvTargetSleepTime.text = String.format("%02d:%02d", prefs.targetSleepTimeHour, prefs.targetSleepTimeMinute)
            binding.tvStayUpLateThreshold.text = String.format("%02d:00", prefs.stayUpLateThresholdHour)
            binding.switchReminder.isChecked = prefs.isDailyReminderEnabled
            binding.tvReminderTime.text = String.format("%02d:%02d", prefs.reminderTimeHour, prefs.reminderTimeMinute)

            binding.layoutReminderTime.alpha = if (prefs.isDailyReminderEnabled) 1.0f else 0.5f
            binding.btnSetReminderTime.isEnabled = prefs.isDailyReminderEnabled
        }
    }

    private fun setupListeners() {
        binding.btnSetTargetSleepTime.setOnClickListener {
            showTargetSleepTimePicker()
        }

        binding.btnSetThreshold.setOnClickListener {
            showThresholdPicker()
        }

        binding.switchReminder.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                val prefs = prefsRepo.userPreferences.first()
                prefsRepo.updateDailyReminder(isChecked, prefs.reminderTimeHour, prefs.reminderTimeMinute)

                binding.layoutReminderTime.alpha = if (isChecked) 1.0f else 0.5f
                binding.btnSetReminderTime.isEnabled = isChecked

                val updatedPrefs = prefsRepo.userPreferences.first()
                reminderManager.scheduleReminder(updatedPrefs)
            }
        }

        binding.btnSetReminderTime.setOnClickListener {
            showReminderTimePicker()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showTargetSleepTimePicker() {
        lifecycleScope.launch {
            val prefs = prefsRepo.userPreferences.first()

            TimePickerDialog(this@SettingsActivity, { _, hour, minute ->
                lifecycleScope.launch {
                    prefsRepo.updateTargetSleepTime(hour, minute)
                    binding.tvTargetSleepTime.text = String.format("%02d:%02d", hour, minute)
                }
            }, prefs.targetSleepTimeHour, prefs.targetSleepTimeMinute, true).show()
        }
    }

    private fun showThresholdPicker() {
        lifecycleScope.launch {
            val prefs = prefsRepo.userPreferences.first()

            TimePickerDialog(this@SettingsActivity, { _, hour, _ ->
                lifecycleScope.launch {
                    prefsRepo.updateStayUpLateThreshold(hour)
                    binding.tvStayUpLateThreshold.text = String.format("%02d:00", hour)
                }
            }, prefs.stayUpLateThresholdHour, 0, true).show()
        }
    }

    private fun showReminderTimePicker() {
        lifecycleScope.launch {
            val prefs = prefsRepo.userPreferences.first()

            TimePickerDialog(this@SettingsActivity, { _, hour, minute ->
                lifecycleScope.launch {
                    prefsRepo.updateDailyReminder(true, hour, minute)
                    binding.tvReminderTime.text = String.format("%02d:%02d", hour, minute)

                    val updatedPrefs = prefsRepo.userPreferences.first()
                    reminderManager.scheduleReminder(updatedPrefs)
                }
            }, prefs.reminderTimeHour, prefs.reminderTimeMinute, true).show()
        }
    }
}
