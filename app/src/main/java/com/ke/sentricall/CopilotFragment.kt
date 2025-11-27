package com.ke.sentricall

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class CopilotFragment : Fragment(R.layout.fragment_copilot) {

    private var btnNewChat: MaterialButton? = null
    private var containerChats: LinearLayout? = null
    private var containerSessions: LinearLayout? = null
    private var progressLoading: ProgressBar? = null

    companion object {
        private const val TAG = "CopilotFragment"
    }

    // --------------------------------------------------------------------
    // LIFECYCLE
    // --------------------------------------------------------------------

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnNewChat = view.findViewById(R.id.btnNewChat)
        containerChats = view.findViewById(R.id.containerChats)
        containerSessions = view.findViewById(R.id.containerSessions)
        progressLoading = view.findViewById(R.id.progressLoading)

        // Hide design-only sample items
        view.findViewById<View>(R.id.itemChatSample)?.visibility = View.GONE
        view.findViewById<View>(R.id.itemSessionSample)?.visibility = View.GONE

        btnNewChat?.setOnClickListener {
            if (!isOnline()) {
                Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            showNewChatDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    // --------------------------------------------------------------------
    // LOAD CHATS + SESSIONS
    // --------------------------------------------------------------------

    private fun loadData() {
        if (!isOnline()) {
            Toast.makeText(
                requireContext(),
                "You are offline. Connect to the internet to load Copilot.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        showLoading(true)
        fetchChats()
        fetchSessions()
    }

    // --------------------------------------------------------------------
    // FETCH CHATS: GET /api/v1/chats/get_chats
    // --------------------------------------------------------------------

    private fun fetchChats() {
        val ctx = context ?: return

        val prefs = ctx.getSharedPreferences("sentricall_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            showLoading(false)
            Toast.makeText(ctx, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        Thread {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(AppConfig.BASE_URL + "chats/get_chats")
                Log.d(TAG, "GET chats URL: $url")

                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val status = connection.responseCode
                val body = if (status in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                Log.d(TAG, "GET /chats/get_chats status=$status body=$body")

                activity?.runOnUiThread {
                    if (status in 200..299) {
                        renderChats(body)
                    } else {
                        Toast.makeText(ctx, "Error loading chats ($status)", Toast.LENGTH_LONG)
                            .show()
                        showLoading(false)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching chats", e)
                activity?.runOnUiThread {
                    Toast.makeText(ctx, "Network error loading chats", Toast.LENGTH_LONG).show()
                    showLoading(false)
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    /**
     * Chats whose names look like "Record Screen — Session 2" etc
     * are *session-derived* and should NOT appear under "Existing chats".
     */
    private fun isSessionDerivedChat(name: String): Boolean {
        val n = name.lowercase().trim()
        if (!n.contains("session")) return false

        return n.startsWith("record screen") ||
                n.startsWith("upload media") ||
                n.startsWith("website link") ||
                n.startsWith("listen to audio")
    }

    private fun renderChats(json: String) {
        val ctx = context ?: return
        val chatsContainer = containerChats ?: return

        chatsContainer.removeAllViews()

        try {
            val root = JSONObject(json)
            val chatsArray: JSONArray = root.optJSONArray("chats") ?: JSONArray()

            var visibleCount = 0

            for (i in 0 until chatsArray.length()) {
                val obj = chatsArray.optJSONObject(i) ?: continue
                val chatId = obj.optString("_id", "")
                if (chatId.isBlank()) continue

                val rawName = obj.optString("name", "").ifBlank { "Untitled chat" }

                // Filter out session-derived chats
                if (isSessionDerivedChat(rawName)) continue

                val itemView = buildChatItemView(chatId, rawName)
                chatsContainer.addView(itemView)
                visibleCount++
            }

            if (visibleCount == 0) {
                val empty = TextView(ctx).apply {
                    text = "No AI chats yet. Start your first investigation above."
                    setTextColor(ContextCompat.getColor(ctx, R.color.guard_subtle))
                    textSize = 13f
                }
                chatsContainer.addView(empty)
            }

            showLoading(false)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing chats json", e)
            Toast.makeText(ctx, "Error parsing chats", Toast.LENGTH_LONG).show()
            showLoading(false)
        }
    }

    // --------------------------------------------------------------------
    // CHAT ITEM + DELETE
    // --------------------------------------------------------------------

    private fun buildChatItemView(chatId: String, name: String): View {
        val ctx = requireContext()

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#111827"))
            isClickable = true
            isFocusable = true
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(8)
            layoutParams = lp
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val headerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val tvTitle = TextView(ctx).apply {
            text = name
            setTextColor(ContextCompat.getColor(ctx, R.color.guard_text))
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val btnDelete = ImageButton(ctx).apply {
            setImageResource(android.R.drawable.ic_menu_delete)
            background = null
            imageTintList = ContextCompat.getColorStateList(ctx, android.R.color.holo_red_light)
            val lp = LinearLayout.LayoutParams(dp(32), dp(32))
            layoutParams = lp
            contentDescription = "Delete chat"
        }

        headerRow.addView(tvTitle)
        headerRow.addView(btnDelete)

        val tvSubtitle = TextView(ctx).apply {
            text = "Tap to continue your AI investigation."
            setTextColor(ContextCompat.getColor(ctx, R.color.guard_subtle))
            textSize = 12f
            setPadding(0, dp(2), 0, 0)
        }

        root.addView(headerRow)
        root.addView(tvSubtitle)

        // Open chat on row click
        root.setOnClickListener {
            if (!isOnline()) {
                Toast.makeText(ctx, "No internet connection", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val intent = Intent(ctx, ChatActivity::class.java).apply {
                putExtra("chat_id", chatId)
                putExtra("chat_name", name)
            }
            startActivity(intent)
        }

        // Delete button
        btnDelete.setOnClickListener {
            if (!isOnline()) {
                Toast.makeText(ctx, "No internet connection", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            confirmDeleteChat(chatId, root)
        }

        return root
    }

    private fun confirmDeleteChat(chatId: String, viewToRemove: View) {
        if (!isAdded) return
        val ctx = requireContext()

        AlertDialog.Builder(ctx)
            .setTitle("Delete chat")
            .setMessage("Are you sure you want to delete this chat?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteChat(chatId, viewToRemove)
            }
            .show()
    }

    private fun deleteChat(chatId: String, viewToRemove: View) {
        val ctx = context ?: return

        val prefs = ctx.getSharedPreferences("sentricall_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(ctx, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        showLoading(true)

        Thread {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(AppConfig.BASE_URL + "chats/delete_chat/$chatId")
                Log.d(TAG, "DELETE chat URL: $url")

                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "DELETE"
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val status = connection.responseCode
                val body = if (status in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                Log.d(TAG, "DELETE /chats/delete_chat status=$status body=$body")

                activity?.runOnUiThread {
                    showLoading(false)

                    val chatsContainer = containerChats ?: return@runOnUiThread

                    if (status in 200..299) {
                        chatsContainer.removeView(viewToRemove)
                        Toast.makeText(ctx, "Chat deleted", Toast.LENGTH_SHORT).show()

                        if (chatsContainer.childCount == 0) {
                            val emptyView = TextView(ctx).apply {
                                text = "No AI chats yet. Start your first investigation above."
                                setTextColor(ContextCompat.getColor(ctx, R.color.guard_subtle))
                                textSize = 13f
                            }
                            chatsContainer.addView(emptyView)
                        }
                    } else {
                        val msg = try {
                            JSONObject(body).optString("message", "Error deleting chat")
                        } catch (_: Exception) {
                            "Error deleting chat ($status)"
                        }
                        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error deleting chat", e)
                activity?.runOnUiThread {
                    showLoading(false)
                    Toast.makeText(
                        ctx,
                        "Network error deleting chat: ${e.localizedMessage ?: "check your connection"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    // --------------------------------------------------------------------
    // FETCH SESSIONS: GET /api/v1/sessions/get_sessions
    // --------------------------------------------------------------------

    private fun fetchSessions() {
        val ctx = context ?: return

        val prefs = ctx.getSharedPreferences("sentricall_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            renderSessions("""{"sessions":[]}""")
            return
        }

        Thread {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(AppConfig.BASE_URL + "sessions/get_sessions")
                Log.d(TAG, "GET sessions URL: $url")

                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val status = connection.responseCode
                val body = if (status in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                }

                Log.d(TAG, "GET /sessions/get_sessions status=$status body=$body")

                activity?.runOnUiThread {
                    if (status in 200..299) {
                        renderSessions(body)
                    } else {
                        Log.w(TAG, "Error loading sessions: $status")
                        renderSessions("""{"sessions":[]}""")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching sessions", e)
                activity?.runOnUiThread {
                    renderSessions("""{"sessions":[]}""")
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun renderSessions(json: String) {
        val ctx = context ?: return
        val sessionsContainer = containerSessions ?: return

        sessionsContainer.removeAllViews()

        try {
            val root = JSONObject(json)
            val sessionsArray: JSONArray = root.optJSONArray("sessions") ?: JSONArray()

            if (sessionsArray.length() == 0) {
                val empty = TextView(ctx).apply {
                    text = "No Guard sessions yet. Start one on the Guard tab."
                    setTextColor(ContextCompat.getColor(ctx, R.color.guard_subtle))
                    textSize = 13f
                }
                sessionsContainer.addView(empty)
                return
            }

            for (i in 0 until sessionsArray.length()) {
                val obj = sessionsArray.optJSONObject(i) ?: continue

                val name = obj.optString("name", "").ifBlank { "Session ${i + 1}" }
                val typeId = obj.optString("type", "listen_audio")
                val createdAt = obj.optString("created_at", "")

                val typeLabel = when (typeId) {
                    "listen_audio" -> "Listen to Audio"
                    "record_screen" -> "Record Screen"
                    "upload_media" -> "Upload Media"
                    "website_link" -> "Website Link"
                    else -> "Guard Session"
                }

                val meta = if (createdAt.isNotBlank()) {
                    "Type: $typeLabel • $createdAt"
                } else {
                    "Type: $typeLabel"
                }

                val itemView = buildSessionItemView(typeLabel, name, meta)
                sessionsContainer.addView(itemView)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing sessions json", e)
            val errorView = TextView(ctx).apply {
                text = "Could not load Guard sessions."
                setTextColor(ContextCompat.getColor(ctx, R.color.guard_subtle))
                textSize = 13f
            }
            sessionsContainer.addView(errorView)
        }
    }

    /**
     * Session item: only in Sessions history.
     * Clicking it creates a fresh AI chat and redirects.
     */
    private fun buildSessionItemView(
        typeLabel: String,
        name: String,
        meta: String
    ): View {
        val ctx = requireContext()

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#111827"))
            isClickable = true
            isFocusable = true
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = dp(8)
            layoutParams = lp
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val titleRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(ctx).apply {
            text = "$typeLabel — $name"
            setTextColor(ContextCompat.getColor(ctx, R.color.guard_text))
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        titleRow.addView(title)

        val subtitle = TextView(ctx).apply {
            text = meta
            setTextColor(ContextCompat.getColor(ctx, R.color.guard_subtle))
            textSize = 12f
            setPadding(0, dp(2), 0, 0)
        }

        root.addView(titleRow)
        root.addView(subtitle)

        root.setOnClickListener {
            if (!isOnline()) {
                Toast.makeText(ctx, "No internet connection", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val chatName = "$typeLabel — $name"
            createChat(chatName)
        }

        return root
    }

    // --------------------------------------------------------------------
    // NEW CHAT DIALOG + CREATE CHAT (POST /chats/create_chat)
    // --------------------------------------------------------------------

    private fun showNewChatDialog() {
        if (!isAdded) return
        val ctx = requireContext()
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_chat, null)

        val tilChatName = dialogView.findViewById<TextInputLayout>(R.id.tilChatName)
        val etChatName = dialogView.findViewById<TextInputEditText>(R.id.etChatName)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnCreate = dialogView.findViewById<Button>(R.id.btnCreate)

        val dialog = AlertDialog.Builder(ctx)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        fun submit() {
            val name = etChatName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                tilChatName.error = "Chat name is required"
            } else {
                tilChatName.error = null
                dialog.dismiss()
                createChat(name)
            }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        btnCreate.setOnClickListener { submit() }

        // Handle keyboard "Done"/Enter
        etChatName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submit()
                true
            } else {
                false
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun createChat(chatName: String) {
        val ctx = context ?: return

        if (!isOnline()) {
            Toast.makeText(ctx, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = ctx.getSharedPreferences("sentricall_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)

        if (token.isNullOrEmpty()) {
            Toast.makeText(ctx, "Session expired. Please log in again.", Toast.LENGTH_LONG).show()
            navigateToLogin()
            return
        }

        showLoading(true)

        Thread {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(AppConfig.BASE_URL + "chats/create_chat")
                Log.d(TAG, "POST create chat URL: $url")

                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val payload = JSONObject().apply {
                    put("name", chatName)
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

                Log.d(TAG, "POST /chats/create_chat status=$status body=$body")

                activity?.runOnUiThread {
                    showLoading(false)

                    if (status in 200..299) {
                        try {
                            val rootJson = JSONObject(body)
                            val chatObj = rootJson.optJSONObject("chat")
                            val chatId = chatObj?.optString("_id", "") ?: ""
                            val returnedNameRaw = chatObj?.optString("name", chatName)
                            val returnedName =
                                if (returnedNameRaw.isNullOrBlank()) chatName else returnedNameRaw

                            if (chatId.isNotBlank()) {
                                val intent = Intent(ctx, ChatActivity::class.java).apply {
                                    putExtra("chat_id", chatId)
                                    putExtra("chat_name", returnedName)
                                }
                                startActivity(intent)
                            } else {
                                Toast.makeText(
                                    ctx,
                                    "Chat created. Reload Copilot to see it.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                loadData()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing create_chat response", e)
                            Toast.makeText(
                                ctx,
                                "Chat created. Reload Copilot to see it.",
                                Toast.LENGTH_SHORT
                            ).show()
                            loadData()
                        }
                    } else {
                        val msg = try {
                            JSONObject(body).optString("message", "Error creating chat")
                        } catch (_: Exception) {
                            "Error creating chat ($status)"
                        }
                        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error creating chat", e)
                activity?.runOnUiThread {
                    showLoading(false)
                    Toast.makeText(
                        ctx,
                        "Network error creating chat: ${e.localizedMessage ?: "check your connection"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    // --------------------------------------------------------------------
    // HELPERS
    // --------------------------------------------------------------------

    private fun dp(value: Int): Int {
        val metrics = resources.displayMetrics
        return (value * metrics.density).toInt()
    }

    private fun isOnline(): Boolean {
        val ctx = context ?: return false
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

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

    private fun navigateToLogin() {
        if (!isAdded) return
        val ctx = requireContext()
        val intent = Intent(ctx, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        activity?.finish()
    }

    private fun showLoading(show: Boolean) {
        progressLoading?.visibility = if (show) View.VISIBLE else View.GONE
    }
}