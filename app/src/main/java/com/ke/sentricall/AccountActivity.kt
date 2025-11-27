package com.ke.sentricall

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class AccountActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("AccountActivity", "onCreate")
        setContentView(R.layout.activity_account)

        bottomNav = findViewById(R.id.bottom_nav)

        // Default tab = Guard (only first time)
        if (savedInstanceState == null) {
            Log.d("AccountActivity", "savedInstanceState == null → load GuardFragment")
            replaceFragment(GuardFragment())
            bottomNav.selectedItemId = R.id.nav_guard
        }

        bottomNav.setOnItemSelectedListener { item ->
            Log.d("AccountActivity", "BottomNav clicked: ${item.itemId}")
            val handled = when (item.itemId) {
                R.id.nav_guard -> {
                    Log.d("AccountActivity", "Switch to GuardFragment")
                    replaceFragment(GuardFragment())
                    true
                }
                R.id.nav_lock -> {
                    Log.d("AccountActivity", "Switch to LockFragment")
                    replaceFragment(LockFragment())
                    true
                }
                R.id.nav_copilot -> {
                    Log.d("AccountActivity", "Switch to CopilotFragment")
                    replaceFragment(CopilotFragment())
                    true
                }
                R.id.nav_profile -> {
                    Log.d("AccountActivity", "Switch to ProfileFragment")
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
            handled
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        Log.d("AccountActivity", "replaceFragment: ${fragment::class.java.simpleName}")
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}