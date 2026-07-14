package com.samyak.repostore

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.samyak.repostore.databinding.ActivityMainBinding
import com.samyak.repostore.ui.fragment.GameFragment
import com.samyak.repostore.ui.fragment.HomeFragment
import com.samyak.repostore.ui.fragment.SearchFragment
import com.samyak.repostore.ui.fragment.SettingsFragment
import com.samyak.repostore.ui.fragment.TrendingFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestNotificationPermissionIfNeeded()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        setupBottomNavigation()

        if (savedInstanceState == null) {
            loadFragment(GameFragment.newInstance())
        }
    }

    /**
     * Ask for POST_NOTIFICATIONS on Android 13+ so update-available
     * notifications can be shown.
     */
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(GameFragment.newInstance())
                    true
                }
                R.id.nav_apps -> {
                    loadFragment(HomeFragment.newInstance())
                    true
                }
                R.id.nav_trending -> {
                    loadFragment(TrendingFragment.newInstance())
                    true
                }
                R.id.nav_search -> {
                    loadFragment(SearchFragment.newInstance())
                    true
                }
                R.id.nav_settings -> {
                    loadFragment(SettingsFragment.newInstance())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
