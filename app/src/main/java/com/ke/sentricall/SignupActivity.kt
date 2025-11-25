package com.ke.sentricall

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class SignupActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val etFirstName = findViewById<TextInputEditText>(R.id.etFirstName)
        val etLastName = findViewById<TextInputEditText>(R.id.etLastName)
        val etEmail = findViewById<TextInputEditText>(R.id.etSignupEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etSignupPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etSignupConfirmPassword)
        val btnSignup = findViewById<Button>(R.id.btnSignup)
        val tvGoToLogin = findViewById<TextView>(R.id.tvGoToLoginFromSignup)

        btnSignup.setOnClickListener {
            // Very basic validation for now
            val first = etFirstName.text?.toString()?.trim().orEmpty()
            val email = etEmail.text?.toString()?.trim().orEmpty()
            val pass = etPassword.text?.toString().orEmpty()
            val confirm = etConfirmPassword.text?.toString().orEmpty()

            if (first.isEmpty() || email.isEmpty() || pass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirm) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // TODO: Call backend/signup API later
            Toast.makeText(this, "Account created (demo)", Toast.LENGTH_SHORT).show()

            // After signup, go back to Login for now
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}