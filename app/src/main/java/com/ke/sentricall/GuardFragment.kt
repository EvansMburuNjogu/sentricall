package com.ke.sentricall

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GuardFragment : Fragment(R.layout.fragment_guard) {

    private lateinit var btnAddSession: MaterialButton
    private lateinit var rvSessions: RecyclerView
    private lateinit var tvEmptySessions: TextView
    private lateinit var etSearchSessions: TextInputEditText

    // Full list from backend
    private val allSessions = mutableListOf<GuardSession>()

    // Filtered list shown in RecyclerView
    private val visibleSessions = mutableListOf<GuardSession>()

    private lateinit var sessionsAdapter: GuardSessionAdapter
    private var currentQuery: String = ""

    // You already use AppConfig elsewhere – keeping it consistent.
    // Make sure BASE_URL ends with `/api/v1/`
    private val baseUrl: String
        get() = AppConfig.BASE_URL

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnAddSession = view.findViewById(R.id.btnAddSession)
        rvSessions = view.findViewById(R.id.rvSessions)
        tvEmptySessions = view.findViewById(R.id.tvEmptySessions)
        etSearchSessions = view.findViewById(R.id.etSearchSessions)

        sessionsAdapter = GuardSessionAdapter(visibleSessions) { session ->
            openSession(session)
        }
        rvSessions.layoutManager = LinearLayoutManager(requireContext())
        rvSessions.adapter = sessionsAdapter

        // Search filter
        etSearchSessions.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                currentQuery = s?.toString().orEmpty()
                applyFilter(currentQuery)
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        updateEmptyState()

        // Start new session – open type chooser, then create on backend
        btnAddSession.setOnClickListener {
            val dialog = SessionTypeDialogFragment()
            dialog.sessionTypeSelectedListener = { type ->
                createSessionOnBackend(type)
            }
            dialog.show(childFragmentManager, "session_type")
        }

        // Initial load from backend
        fetchSessionsFromBackend()
    }

    // --------- Networking helpers ----------

    private fun getAuthToken(ctx: Context): String? {
        // TODO: adjust to your real storage if different
        val prefs = ctx.getSharedPreferences("sentricall_prefs", Context.MODE_PRIVATE)
        return prefs.getString("auth_token", null)
    }

    /**
     * GET /api/v1/sessions/get_sessions
     */
    private fun fetchSessionsFromBackend() {
        val token = getAuthToken(requireContext()) ?: run {
            // If user is not logged in yet, just show empty state
            updateEmptyState()
            return
        }

        Thread {
            var conn: HttpURLConnection? = null
            try {
                val url = URL(baseUrl + "sessions/get_sessions")
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val status = conn.responseCode
                if (status !in 200..299) {
                    throw Exception("HTTP $status")
                }

                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
                val arr: JSONArray? = root.optJSONArray("sessions")

                val newList = mutableListOf<GuardSession>()
                if (arr != null) {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        newList.add(parseSessionJson(obj))
                    }
                }

                requireActivity().runOnUiThread {
                    allSessions.clear()
                    allSessions.addAll(newList)
                    applyFilter(currentQuery)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Error loading sessions: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    applyFilter(currentQuery) // keep UI consistent
                }
            } finally {
                conn?.disconnect()
            }
        }.start()
    }

    /**
     * POST /api/v1/sessions/create_session
     * Default name: "Session {count+1}"
     */
    private fun createSessionOnBackend(type: SessionType) {
        val token = getAuthToken(requireContext()) ?: run {
            Toast.makeText(
                requireContext(),
                "Please sign in to create Guard sessions.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        Thread {
            var conn: HttpURLConnection? = null
            try {
                val url = URL(baseUrl + "sessions/create_session")
                conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                // Default name: Session {existing+1}
                val nextIndex = allSessions.size + 1
                val sessionName = "Session $nextIndex"

                val payload = JSONObject().apply {
                    put("name", sessionName)
                    put("type", type.id) // "listen_audio" / "record_screen" / ...
                }

                conn.outputStream.use { os ->
                    os.write(payload.toString().toByteArray(Charsets.UTF_8))
                }

                val status = conn.responseCode
                if (status !in 200..299) {
                    val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() }
                    throw Exception("HTTP $status $errorText")
                }

                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
                val sessionJson = root.optJSONObject("session")

                val newSession = if (sessionJson != null) {
                    parseSessionJson(sessionJson)
                } else {
                    // Fallback if backend doesn’t return the session object
                    GuardSession(
                        id = "",
                        type = type,
                        name = sessionName,
                        subtitle = defaultSubtitleForType(type)
                    )
                }

                requireActivity().runOnUiThread {
                    // Keep local cache in sync
                    allSessions.add(0, newSession)
                    applyFilter(currentQuery)

                    // Immediately open this session (same UX as before)
                    openSession(newSession)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                requireActivity().runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "Error creating session: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                conn?.disconnect()
            }
        }.start()
    }

    private fun parseSessionJson(obj: JSONObject): GuardSession {
        val id = obj.optString("_id", "")
        val name = obj.optString("name", "").ifBlank { "Session" }
        val typeId = obj.optString("type", null)
        val type = SessionType.fromId(typeId) ?: SessionType.LISTEN_AUDIO

        return GuardSession(
            id = id,
            type = type,
            name = name,
            subtitle = defaultSubtitleForType(type)
        )
    }

    private fun defaultSubtitleForType(type: SessionType): String =
        when (type) {
            SessionType.LISTEN_AUDIO ->
                "Guard is ready to listen for suspicious phrases."
            SessionType.RECORD_SCREEN ->
                "Guard will watch your screen for risky flows."
            SessionType.UPLOAD_MEDIA ->
                "Guard will analyze your audio or screenshots."
            SessionType.WEBSITE_LINK ->
                "Guard will scan your URL for scams and red flags."
        }

    // --------- Filtering & state ----------

    private fun applyFilter(query: String) {
        val q = query.trim().lowercase()

        val result = if (q.isEmpty()) {
            allSessions
        } else {
            allSessions.filter { s ->
                s.name.lowercase().contains(q) ||
                        s.type.title.lowercase().contains(q) ||
                        s.type.description.lowercase().contains(q)
            }
        }

        visibleSessions.clear()
        visibleSessions.addAll(result)
        sessionsAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        tvEmptySessions.visibility =
            if (visibleSessions.isEmpty()) View.VISIBLE else View.GONE
    }

    // --------- Navigation ----------

    private fun openSession(session: GuardSession) {
        val ctx = requireContext()
        val intent = android.content.Intent(ctx, SessionActivity::class.java).apply {
            // Session mode for SessionActivity’s existing logic
            putExtra("session_mode", session.type.id)

            // Header text (reuses your existing extras)
            putExtra(SessionActivity.EXTRA_SESSION_TITLE, session.name)
            putExtra(SessionActivity.EXTRA_SESSION_SUBTITLE, session.subtitle)

            // NOTE: not touching SessionActivity internals yet,
            // so we are NOT passing session_id here.
        }
        startActivity(intent)
    }

    // --------- Models & Adapter ----------

    data class GuardSession(
        val id: String,          // backend _id (not used yet in SessionActivity)
        val type: SessionType,
        val name: String,        // "Session 1", custom name, etc.
        val subtitle: String
    )

    private class GuardSessionAdapter(
        private val items: List<GuardSession>,
        private val onClick: (GuardSession) -> Unit
    ) : RecyclerView.Adapter<GuardSessionAdapter.SessionViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_guard_session, parent, false)
            return SessionViewHolder(view)
        }

        override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
            holder.bind(items[position], onClick)
        }

        override fun getItemCount(): Int = items.size

        class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvTitle: TextView = itemView.findViewById(R.id.tvSessionTitle)
            private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSessionSubtitle)
            private val tvTypeChip: TextView = itemView.findViewById(R.id.tvSessionTypeChip)

            fun bind(session: GuardSession, onClick: (GuardSession) -> Unit) {
                tvTitle.text = session.name
                tvSubtitle.text = session.subtitle

                tvTypeChip.text = when (session.type) {
                    SessionType.LISTEN_AUDIO -> "Listen to audio"
                    SessionType.RECORD_SCREEN -> "Record screen"
                    SessionType.UPLOAD_MEDIA -> "Upload media"
                    SessionType.WEBSITE_LINK -> "Website link"
                }

                itemView.setOnClickListener { onClick(session) }
            }
        }
    }
}