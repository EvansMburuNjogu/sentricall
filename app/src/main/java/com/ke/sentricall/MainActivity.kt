package com.ke.sentricall

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read the stored token
        val prefs = getSharedPreferences("sentricall_prefs", MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (!token.isNullOrEmpty()) {
            // ✅ User is already logged in → go to guard/home screen
            val intent = Intent(this, AccountActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        } else {
            // ❌ No token → go to Login screen
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }

        // We don't show any UI in MainActivity itself
        finish()
    }
}