package com.yprompt.areyouasleep.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.yprompt.areyouasleep.R
import com.yprompt.areyouasleep.databinding.ActivityMainBinding
import com.yprompt.areyouasleep.ui.fragment.HomeFragment
import com.yprompt.areyouasleep.ui.fragment.MineFragment
import com.yprompt.areyouasleep.ui.fragment.TrendsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupListeners()
    }

    private fun setupNavigation() {
        loadFragment(HomeFragment())

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.navigation_sleep -> HomeFragment()
                R.id.navigation_trends -> TrendsFragment()
                R.id.navigation_mine -> MineFragment()
                else -> HomeFragment()
            }
            loadFragment(fragment)
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()

        binding.tvToolbarTitle.text = when (fragment) {
            is HomeFragment -> "睡了么"
            is TrendsFragment -> "睡眠趋势"
            is MineFragment -> "个人中心"
            else -> "睡了么"
        }
    }

    private fun setupListeners() {
        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_bottom, R.anim.fade_out)
        }
    }
}
