package com.ke.sentricall

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ProfileFragment : Fragment() {

    // Header views
    private lateinit var tvAvatar: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmailSmall: TextView

    // Account inputs
    private lateinit var etFirstName: TextInputEditText
    private lateinit var etLastName: TextInputEditText
    private lateinit var etEmail: TextInputEditText

    // Security inputs
    private lateinit var etCurrentPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    // Buttons
    private lateinit var btnUpdateProfile: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private var isLoadingUser = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind views
        tvAvatar = view.findViewById(R.id.tvAvatar)
        tvProfileName = view.findViewById(R.id.tvProfileName)
        tvProfileEmailSmall = view.findViewById(R.id.tvProfileEmailSmall)

        etFirstName = view.findViewById(R.id.etFirstName)
        etLastName = view.findViewById(R.id.etLastName)
        etEmail = view.findViewById(R.id.etEmail)

        etCurrentPassword = view.findViewById(R.id.etCurrentPassword)
        etNewPassword = view.findViewById(R.id.etNewPassword)
        etConfirmPassword = view.findViewById(R.id.etConfirmPassword)

        btnUpdateProfile = view.findViewById(R.id.btnUpdateProfile)
        btnLogout = view.findViewById(R.id.btnLogout)

        btnUpdateProfile.setOnClickListener {
            handleUpdateProfileClick()
        }

        btnLogout.setOnClickListener {
            handleLogoutClick()
        }
    }

    override fun onResume() {
        super.onResume()
        fetchUserDetails()
    }

    // ---------------------------
    //  FETCH USER DETAILS
    // ---------------------------

    private fun fetchUserDetails() {
        val ctx = context ?: return

        val prefs = ctx.getSharedPreferences("sentricall_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(ctx, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        if (isLoadingUser) return
        isLoadingUser = true

        Thread {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(AppConfig.BASE_URL + "users/get_user")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val status = connection.responseCode
                val responseBody = if (status in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                activity?.runOnUiThread {
                    isLoadingUser = false

                    if (status in 200..299) {
                        try {
                            // 🔑 Your backend: { "message": "...", "user": { ... } }
                            val root = JSONObject(responseBody)
                            val userObj = root.optJSONObject("user")

                            if (userObj == null) {
                                Toast.makeText(ctx, "User object missing in response", Toast.LENGTH_LONG).show()
                                return@runOnUiThread
                            }

                            val firstName = userObj.optString("firstName", "")
                            val lastName = userObj.optString("lastName", "")
                            val email = userObj.optString("email", "")

                            // Header
                            val fullName = listOf(firstName, lastName)
                                .filter { it.isNotBlank() }
                                .joinToString(" ")

                            tvProfileName.text =
                                if (fullName.isNotBlank()) fullName else "Sentricall User"
                            tvProfileEmailSmall.text =
                                if (email.isNotBlank()) email else "user@example.com"

                            val avatarSource =
                                if (fullName.isNotBlank()) fullName else email.ifBlank { "S" }
                            val initial = avatarSource.trim().firstOrNull()?.uppercaseChar() ?: 'S'
                            tvAvatar.text = initial.toString()

                            // Inputs
                            etFirstName.setText(firstName)
                            etLastName.setText(lastName)
                            etEmail.setText(email)

                            // Clear password fields
                            etCurrentPassword.text?.clear()
                            etNewPassword.text?.clear()
                            etConfirmPassword.text?.clear()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(ctx, "Error parsing user details", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(ctx, "Error loading profile ($status)", Toast.LENGTH_LONG).show()

                        if (status == 401 || status == 403) {
                            handleLogoutClick()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    isLoadingUser = false
                    Toast.makeText(
                        ctx,
                        "Network error loading profile: ${e.localizedMessage ?: "check your connection"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    // ---------------------------
    //  UPDATE PROFILE
    // ---------------------------

    private fun handleUpdateProfileClick() {
        val ctx = context ?: return

        val firstName = etFirstName.text?.toString()?.trim().orEmpty()
        val lastName = etLastName.text?.toString()?.trim().orEmpty()
        val email = etEmail.text?.toString()?.trim().orEmpty()

        val currentPassword = etCurrentPassword.text?.toString().orEmpty()
        val newPassword = etNewPassword.text?.toString().orEmpty()
        val confirmPassword = etConfirmPassword.text?.toString().orEmpty()

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty()) {
            Toast.makeText(ctx, "First name, last name and email are required", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val isChangingPassword =
            currentPassword.isNotEmpty() || newPassword.isNotEmpty() || confirmPassword.isNotEmpty()

        if (isChangingPassword) {
            if (currentPassword.isEmpty()) {
                Toast.makeText(ctx, "Enter your current password", Toast.LENGTH_SHORT).show()
                return
            }
            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(
                    ctx,
                    "Enter both new password and confirm password",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(
                    ctx,
                    "New password and confirm password do not match",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
        }

        val prefs = ctx.getSharedPreferences("sentricall_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(ctx, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        btnUpdateProfile.isEnabled = false

        Thread {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(AppConfig.BASE_URL + "users/update")
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val payload = JSONObject().apply {
                    put("firstName", firstName)
                    put("lastName", lastName)
                    put("email", email)

                    if (isChangingPassword) {
                        put("existingPassword", currentPassword)
                        put("newPassword", newPassword)
                    }
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

                activity?.runOnUiThread {
                    btnUpdateProfile.isEnabled = true

                    if (status in 200..299) {
                        Toast.makeText(ctx, "Profile updated", Toast.LENGTH_SHORT).show()
                        fetchUserDetails()
                    } else {
                        val msg = try {
                            JSONObject(responseBody).optString("message", "Failed to update profile")
                        } catch (_: Exception) {
                            "Failed to update profile ($status)"
                        }
                        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    btnUpdateProfile.isEnabled = true
                    Toast.makeText(
                        ctx,
                        "Network error updating profile: ${e.localizedMessage ?: "check your connection"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    // ---------------------------
    //  LOGOUT
    // ---------------------------

    private fun handleLogoutClick() {
        val ctx = context ?: return
        val prefs = ctx.getSharedPreferences("sentricall_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        Toast.makeText(ctx, "Logged out", Toast.LENGTH_SHORT).show()
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }
}