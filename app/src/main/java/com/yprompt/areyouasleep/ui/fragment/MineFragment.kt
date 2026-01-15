package com.yprompt.areyouasleep.ui.fragment

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.yprompt.areyouasleep.data.database.AppDatabase
import com.yprompt.areyouasleep.data.model.DailyRecord
import com.yprompt.areyouasleep.databinding.FragmentMineBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MineFragment : Fragment() {

    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase
    private val gson = Gson()

    private val exportLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportData(it) }
    }

    private val importLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importData(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())

        setupUI()
        loadStats()
        loadUserInfo()
    }

    private fun setupUI() {
        binding.tvUserName.setOnClickListener {
            showEditNameDialog()
        }

        binding.btnExport.setOnClickListener {
            val fileName = "sleep_data_backup_${System.currentTimeMillis()}.json"
            exportLauncher.launch(fileName)
        }

        binding.btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("application/json"))
        }
    }

    private fun loadUserInfo() {
        val sp = requireContext().getSharedPreferences("user_info", Activity.MODE_PRIVATE)
        binding.tvUserName.text = sp.getString("nickname", "早睡达人")
    }

    private fun showEditNameDialog() {
        val editText = EditText(context)
        editText.hint = "请输入新昵称"

        AlertDialog.Builder(context)
            .setTitle("修改昵称")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val newName = editText.text.toString()
                if (newName.isNotBlank()) {
                    binding.tvUserName.text = newName
                    val sp = requireContext().getSharedPreferences("user_info", Activity.MODE_PRIVATE)
                    sp.edit().putString("nickname", newName).apply()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val month = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
            val stat = db.monthlyStatDao().getStatByMonth(month).first()
            val consecutive = stat?.maxConsecutiveCheckIns ?: 0
            binding.tvConsecutiveDays.text = consecutive.toString()
        }
    }

    private fun exportData(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val records = db.dailyRecordDao().getAllRecordsSync()
                val json = gson.toJson(records)

                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray())
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "数据导出成功！", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导出失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun importData(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val stringBuilder = StringBuilder()
                requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            stringBuilder.append(line)
                            line = reader.readLine()
                        }
                    }
                }

                val json = stringBuilder.toString()
                val listType = object : TypeToken<List<DailyRecord>>() {}.type
                val records: List<DailyRecord> = gson.fromJson(json, listType)

                db.dailyRecordDao().insertAll(records)

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "成功导入 ${records.size} 条数据！", Toast.LENGTH_SHORT).show()
                    loadStats()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "导入失败，文件格式可能不正确", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
