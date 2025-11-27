package com.ke.sentricall

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvGoToSignup: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If you later want auto-skip when token exists, you can move that logic to MainActivity
        setContentView(R.layout.activity_login)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoToSignup = findViewById(R.id.tvGoToSignup)

        btnLogin.setOnClickListener {
            handleLoginClick()
        }

        tvGoToSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    private fun handleLoginClick() {
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString()?.trim().orEmpty()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
            return
        }

        btnLogin.isEnabled = false

        Thread {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(AppConfig.BASE_URL + "auth/login")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val payload = JSONObject().apply {
                    put("email", email)
                    put("password", password)
                }

                connection.outputStream.use { os ->
                    val bytes = payload.toString().toByteArray(Charsets.UTF_8)
                    os.write(bytes, 0, bytes.size)
                }

                val status = connection.responseCode
                val responseBody = if (status in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                runOnUiThread {
                    btnLogin.isEnabled = true

                    if (status in 200..299) {
                        var token: String? = null
                        try {
                            val json = JSONObject(responseBody)
                            token = json.optString("token", null)
                        } catch (_: Exception) { }

                        if (token.isNullOrEmpty()) {
                            Toast.makeText(
                                this,
                                "Login succeeded but no token returned",
                                Toast.LENGTH_LONG
                            ).show()
                            return@runOnUiThread
                        }

                        // Save token + email
                        val prefs = getSharedPreferences("sentricall_prefs", MODE_PRIVATE)
                        prefs.edit()
                            .putString("auth_token", token)
                            .putString("user_email", email)
                            .apply()

                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                        // ⬇️ Redirect to Account / Guard screen
                        goToGuardScreen()
                    } else {
                        val msg = try {
                            JSONObject(responseBody).optString("message", "Invalid email or password")
                        } catch (_: Exception) {
                            "Login failed: $status"
                        }
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    btnLogin.isEnabled = true
                    Toast.makeText(
                        this,
                        "Network error: ${e.localizedMessage ?: "check your connection"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun goToGuardScreen() {
        val intent = Intent(this, AccountActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}