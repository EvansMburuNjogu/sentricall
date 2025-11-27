package com.ke.sentricall

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ChatActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatActivity"
    }

    private lateinit var tvChatTitle: TextView
    private lateinit var tvChatSubtitle: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var scrollMessages: ScrollView
    private lateinit var layoutMessages: LinearLayout
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    private var chatId: String? = null
    private var sessionId: String? = null

    private var isSending: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_chat)

        tvChatTitle = findViewById(R.id.tvChatTitle)
        tvChatSubtitle = findViewById(R.id.tvChatSubtitle)
        btnBack = findViewById(R.id.btnBack)
        scrollMessages = findViewById(R.id.scrollMessages)
        layoutMessages = findViewById(R.id.layoutMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        chatId = intent.getStringExtra("chat_id")
        sessionId = intent.getStringExtra("session_id")

        if (chatId == null && sessionId == null) {
            Log.e(TAG, "No chat_id or session_id passed, finishing activity")
            finish()
            return
        }

        val initialTitle = intent.getStringExtra("chat_name")
            ?: intent.getStringExtra("session_name")
            ?: if (chatId != null) "Copilot chat" else "Copilot session"

        tvChatTitle.text = initialTitle

        tvChatSubtitle.text = if (sessionId != null) {
            "Review what happened in this recorded session."
        } else {
            "Ask questions about scams, fraud and safety."
        }

        btnBack.setOnClickListener { finish() }

        tvChatTitle.setOnClickListener {
            showRenameChatDialog(tvChatTitle.text.toString())
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            if (!isOnline()) {
                Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (chatId == null && sessionId != null) {
                Toast.makeText(
                    this,
                    "AI questions for sessions are coming soon.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Show user message immediately
            addUserMessageBubble(text)
            etMessage.setText("")
            scrollToBottom()

            // Call backend -> ask_chat
            sendMessageToCopilot(text)
        }

        loadChatOrSession()
    }

    // --------------------------------------------------
    // DP HELPER
    // --------------------------------------------------

    private fun dp(value: Int): Int {
        val density = resources.displayMetrics.density
        return (value * density).toInt()
    }

    // --------------------------------------------------
    // LOAD HEADER + CONVERSATIONS
    // --------------------------------------------------

    private fun loadChatOrSession() {
        if (!isOnline()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_LONG).show()
            return
        }

        val prefs = getSharedPreferences("sentricall_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        val isChatMode = chatId != null
        val id = chatId ?: sessionId ?: return

        val detailUrl = if (isChatMode) {
            AppConfig.BASE_URL + "chats/get_chat/$id"
        } else {
            AppConfig.BASE_URL + "sessions/get_session/$id"
        }

        val convUrl = if (isChatMode) {
            AppConfig.BASE_URL + "chats/get_chat_conversations/$id"
        } else {
            AppConfig.BASE_URL + "sessions/get_session_conversations/$id"
        }

        Thread {
            var detailConnection: HttpURLConnection? = null
            var convConnection: HttpURLConnection? = null

            try {
                Log.d(TAG, "GET detail URL: $detailUrl")
                detailConnection = (URL(detailUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val detailStatus = detailConnection.responseCode
                val detailBody = if (detailStatus in 200..299) {
                    detailConnection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    detailConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                Log.d(TAG, "detailStatus=$detailStatus body=$detailBody")

                if (detailStatus == 401 || detailStatus == 403) {
                    runOnUiThread {
                        forceLogout("Session expired. Please log in again.")
                    }
                    return@Thread
                }

                Log.d(TAG, "GET conv URL: $convUrl")
                convConnection = (URL(convUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val convStatus = convConnection.responseCode
                val convBody = if (convStatus in 200..299) {
                    convConnection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    convConnection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                Log.d(TAG, "convStatus=$convStatus body=$convBody")

                if (convStatus == 401 || convStatus == 403) {
                    runOnUiThread {
                        forceLogout("Session expired. Please log in again.")
                    }
                    return@Thread
                }

                runOnUiThread {
                    if (detailStatus in 200..299) {
                        updateTitleFromDetail(detailBody, isChatMode)
                    }

                    if (convStatus in 200..299) {
                        renderConversations(convBody)
                    } else {
                        Toast.makeText(
                            this,
                            "Error loading conversation ($convStatus)",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chat/session", e)
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Network error loading conversation: ${e.localizedMessage ?: "check your connection"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                detailConnection?.disconnect()
                convConnection?.disconnect()
            }
        }.start()
    }

    private fun updateTitleFromDetail(json: String, isChatMode: Boolean) {
        try {
            val root = JSONObject(json)
            val obj = if (isChatMode) root.optJSONObject("chat") else root.optJSONObject("session")
            val name = obj?.optString("name", "") ?: ""
            if (name.isNotBlank()) {
                tvChatTitle.text = name
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing detail JSON", e)
        }
    }

    private fun renderConversations(json: String) {
        try {
            val root = JSONObject(json)
            val arr: JSONArray = root.optJSONArray("conversations") ?: JSONArray()

            layoutMessages.removeAllViews()

            if (arr.length() == 0) {
                return
            }

            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue

                val role = obj.optString("role", "assistant").lowercase()
                val text = obj.optString(
                    "message",
                    obj.optString("content", "")
                )

                if (text.isBlank()) continue

                if (role == "user" || role == "client") {
                    addUserMessageBubble(text)
                } else {
                    addAssistantMessageBubble(text)
                }
            }

            scrollToBottom()
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing conversations JSON", e)
            Toast.makeText(this, "Error parsing conversation", Toast.LENGTH_LONG).show()
        }
    }

    // --------------------------------------------------
    // SEND MESSAGE TO COPILOT
    // --------------------------------------------------

    private fun sendMessageToCopilot(userMessage: String) {
        if (isSending) return
        isSending = true
        btnSend.isEnabled = false

        val prefs = getSharedPreferences("sentricall_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            isSending = false
            btnSend.isEnabled = true
            return
        }

        val isChatMode = chatId != null
        val id = chatId ?: sessionId

        if (id == null) {
            Toast.makeText(this, "Missing chat/session ID.", Toast.LENGTH_LONG).show()
            isSending = false
            btnSend.isEnabled = true
            return
        }

        val assistantBubble = addAssistantMessageBubble("Copilot is thinking…")
        scrollToBottom()

        Thread {
            var connection: HttpURLConnection? = null
            try {
                val askUrl = if (isChatMode) {
                    AppConfig.BASE_URL + "chats/ask_chat/$id/ask"
                } else {
                    AppConfig.BASE_URL + "sessions/ask_session/$id"
                }

                Log.d(TAG, "POST ask URL: $askUrl")

                connection = (URL(askUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val payload = JSONObject().apply {
                    put("question", userMessage)
                }

                connection.outputStream.use { os ->
                    val bytes = payload.toString().toByteArray(Charsets.UTF_8)
                    os.write(bytes, 0, bytes.size)
                }

                val status = connection.responseCode
                val body = if (status in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                Log.d(TAG, "ask_chat status=$status body=$body")

                runOnUiThread {
                    isSending = false
                    btnSend.isEnabled = true

                    if (status in 200..299) {
                        assistantBubble.text = "Updating conversation…"
                        loadChatOrSession()
                    } else {
                        val msg = if (status == 404) {
                            "Error from Copilot (404). Please update the app or contact support."
                        } else {
                            "Error from Copilot ($status)."
                        }
                        assistantBubble.text = msg
                        scrollToBottom()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error calling Copilot", e)
                runOnUiThread {
                    isSending = false
                    btnSend.isEnabled = true
                    assistantBubble.text =
                        "Network error talking to Copilot: ${e.localizedMessage ?: "check your connection"}"
                    scrollToBottom()
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    // --------------------------------------------------
    // BUBBLES (CARD STYLE)
    // --------------------------------------------------

    private fun addUserMessageBubble(message: String): TextView {
        val bubble = TextView(this).apply {
            text = message
            setTextColor(Color.parseColor("#022C22"))
            textSize = 14f

            // Rounded green background (card-like)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#22C55E"))
                cornerRadius = dp(16).toFloat()
            }

            // Inner padding
            setPadding(dp(12), dp(8), dp(12), dp(8))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dp(6)
            params.bottomMargin = dp(6)
            params.marginEnd = dp(16)
            params.gravity = android.view.Gravity.END
            layoutParams = params

            // Limit width so it doesn't span the whole screen
            maxWidth = (resources.displayMetrics.widthPixels * 0.75f).toInt()
        }

        layoutMessages.addView(bubble)
        return bubble
    }

    private fun addAssistantMessageBubble(message: String): TextView {
        val bubble = TextView(this).apply {
            text = message
            setTextColor(Color.WHITE)
            textSize = 14f

            // Rounded dark background (card-like)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#111827"))
                cornerRadius = dp(16).toFloat()
            }

            // Inner padding
            setPadding(dp(12), dp(8), dp(12), dp(8))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = dp(6)
            params.bottomMargin = dp(6)
            params.marginStart = dp(16)
            params.gravity = android.view.Gravity.START
            layoutParams = params

            maxWidth = (resources.displayMetrics.widthPixels * 0.75f).toInt()
        }

        layoutMessages.addView(bubble)
        return bubble
    }

    private fun scrollToBottom() {
        scrollMessages.post {
            scrollMessages.fullScroll(View.FOCUS_DOWN)
        }
    }

    // --------------------------------------------------
    // RENAME DIALOG
    // --------------------------------------------------

    private fun showRenameChatDialog(currentTitle: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_chat, null)

        val tvDialogTitle: TextView = dialogView.findViewById(R.id.tvDialogTitle)
        val tvDialogSubtitle: TextView = dialogView.findViewById(R.id.tvDialogSubtitle)
        val etChatName: TextInputEditText = dialogView.findViewById(R.id.etChatName)
        val btnCancel: View = dialogView.findViewById(R.id.btnCancel)
        val btnCreate: Button = dialogView.findViewById(R.id.btnCreate)

        tvDialogTitle.text = "Rename chat"
        tvDialogSubtitle.text = "Update how this chat appears in your list."
        etChatName.setText(currentTitle)
        etChatName.setSelection(currentTitle.length)
        btnCreate.text = "Save"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnCreate.setOnClickListener {
            var newName = etChatName.text?.toString()?.trim() ?: ""
            if (newName.isEmpty()) {
                newName = "Copilot chat"
            }
            tvChatTitle.text = newName
            // TODO: persist rename in backend if needed
            dialog.dismiss()
        }

        dialog.show()
    }

    // --------------------------------------------------
    // CONNECTIVITY + AUTH HELPERS
    // --------------------------------------------------

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

        if (cm != null) {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val nw = cm.activeNetwork ?: return false
                    val actNw = cm.getNetworkCapabilities(nw) ?: return false
                    actNw.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                } catch (e: SecurityException) {
                    Log.e(TAG, "Missing ACCESS_NETWORK_STATE permission", e)
                    false
                }
            } else {
                @Suppress("DEPRECATION")
                val nwInfo = cm.activeNetworkInfo ?: return false
                @Suppress("DEPRECATION")
                nwInfo.isConnected
            }
        }
        return false
    }

    private fun forceLogout(message: String? = null) {
        val prefs = getSharedPreferences("sentricall_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        if (!message.isNullOrEmpty()) {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}