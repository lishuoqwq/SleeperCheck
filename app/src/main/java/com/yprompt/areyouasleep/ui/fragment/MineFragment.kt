package com.yprompt.areyouasleep.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.yprompt.areyouasleep.data.database.AppDatabase
import com.yprompt.areyouasleep.databinding.FragmentMineBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MineFragment : Fragment() {

    private var _binding: FragmentMineBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext())
        loadStats()
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val month = SimpleDateFormat("yyyy-MM", Locale.US).format(Calendar.getInstance().time)
            val stat = db.monthlyStatDao().getStatByMonth(month).first()

            val consecutive = stat?.maxConsecutiveCheckIns ?: 0
            binding.tvConsecutiveDays.text = consecutive.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
