package com.ke.sentricall

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SignupActivity : AppCompatActivity() {

    private lateinit var tilFirstName: TextInputLayout
    private lateinit var tilLastName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout

    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var btnSignup: MaterialButton
    private lateinit var tvGoToLogin: TextView

    // OkHttp client
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        bindViews()
        setupListeners()
    }

    private fun bindViews() {
        tilFirstName = findViewById(R.id.tilFirstName)
        tilLastName = findViewById(R.id.tilLastName)
        tilEmail = findViewById(R.id.tilSignupEmail)
        tilPassword = findViewById(R.id.tilSignupPassword)
        tilConfirmPassword = findViewById(R.id.tilSignupConfirmPassword)

        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etSignupEmail)
        etPassword = findViewById(R.id.etSignupPassword)
        etConfirmPassword = findViewById(R.id.etSignupConfirmPassword)

        btnSignup = findViewById(R.id.btnSignup)
        tvGoToLogin = findViewById(R.id.tvGoToLoginFromSignup)
    }

    private fun setupListeners() {
        btnSignup.setOnClickListener { attemptSignup() }

        tvGoToLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun attemptSignup() {
        // clear previous errors
        tilFirstName.error = null
        tilLastName.error = null
        tilEmail.error = null
        tilPassword.error = null
        tilConfirmPassword.error = null

        val firstName = etFirstName.text?.toString()?.trim().orEmpty()
        val lastName = etLastName.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty()
        val password = etPassword.text?.toString().orEmpty()
        val confirmPassword = etConfirmPassword.text?.toString().orEmpty()

        var hasError = false

        if (firstName.isEmpty()) {
            tilFirstName.error = "First name is required"
            hasError = true
        }

        if (lastName.isEmpty()) {
            tilLastName.error = "Last name is required"
            hasError = true
        }

        if (email.isEmpty()) {
            tilEmail.error = "Email is required"
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Enter a valid email"
            hasError = true
        }

        if (password.isEmpty()) {
            tilPassword.error = "Password is required"
            hasError = true
        } else if (password.length < 6) {
            tilPassword.error = "At least 6 characters"
            hasError = true
        }

        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.error = "Confirm your password"
            hasError = true
        } else if (password != confirmPassword) {
            tilConfirmPassword.error = "Passwords do not match"
            hasError = true
        }

        if (hasError) return

        // Build JSON body: { firstName, lastName, email, password }
        val json = JSONObject().apply {
            put("firstName", firstName)
            put("lastName", lastName)
            put("email", email)
            put("password", password)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)

        val url = AppConfig.BASE_URL + "auth/register"
        android.util.Log.d("Signup", "POST $url body=$json")

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        setLoading(true)

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                android.util.Log.e("Signup", "Network error", e)
                runOnUiThread {
                    setLoading(false)
                    Toast.makeText(
                        this@SignupActivity,
                        "Network error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string().orEmpty()
                android.util.Log.d(
                    "Signup",
                    "Response code=${response.code} body=$bodyString"
                )

                runOnUiThread {
                    setLoading(false)

                    if (response.isSuccessful) {
                        val message = try {
                            val obj = JSONObject(bodyString)
                            obj.optString("message", "Account created successfully.")
                        } catch (_: Exception) {
                            "Account created successfully."
                        }

                        Toast.makeText(
                            this@SignupActivity,
                            message,
                            Toast.LENGTH_LONG
                        ).show()

                        // after signup go to login/main
                        startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                        finish()
                    } else {
                        val errorMessage = try {
                            val obj = JSONObject(bodyString)
                            obj.optString(
                                "message",
                                "Signup failed. Please check your details and try again."
                            )
                        } catch (_: Exception) {
                            "Signup failed. Please try again."
                        }

                        if (errorMessage.contains("email", ignoreCase = true)) {
                            tilEmail.error = errorMessage
                        }

                        Toast.makeText(
                            this@SignupActivity,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }

    private fun setLoading(isLoading: Boolean) {
        btnSignup.isEnabled = !isLoading
        btnSignup.text = if (isLoading) "Creating account..." else "Sign up"
    }
}