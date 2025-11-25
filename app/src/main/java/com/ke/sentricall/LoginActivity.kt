package com.ke.sentricall

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val btnLogin: Button = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            // later: validate email/password
            val intent = Intent(this, AccountActivity::class.java)
            startActivity(intent)
            // optional: finish login screen so back button doesn't return here
            // finish()
        }
    }
}