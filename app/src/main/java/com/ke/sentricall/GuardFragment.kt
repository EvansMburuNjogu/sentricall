package com.ke.sentricall

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class GuardFragment : Fragment(R.layout.fragment_guard) {

    private lateinit var btnAddSession: MaterialButton
    private lateinit var rvSessions: RecyclerView
    private lateinit var tvEmptySessions: TextView

    private val sessions = mutableListOf<GuardSession>()
    private lateinit var sessionsAdapter: GuardSessionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnAddSession = view.findViewById(R.id.btnAddSession)
        rvSessions = view.findViewById(R.id.rvSessions)
        tvEmptySessions = view.findViewById(R.id.tvEmptySessions)

        sessionsAdapter = GuardSessionAdapter(sessions) { session ->
            openSession(session)
        }
        rvSessions.layoutManager = LinearLayoutManager(requireContext())
        rvSessions.adapter = sessionsAdapter

        updateEmptyState()

        // Start new session – open type chooser
        btnAddSession.setOnClickListener {
            val dialog = SessionTypeDialogFragment()
            dialog.sessionTypeSelectedListener = { type ->
                val session = addNewSession(type)
                openSession(session)
            }
            dialog.show(childFragmentManager, "session_type")
        }
    }

    private fun addNewSession(type: SessionType): GuardSession {
        val (title, subtitle) = when (type) {
            SessionType.LISTEN_AUDIO ->
                "Listening session" to "Guard is ready to listen for suspicious phrases."
            SessionType.RECORD_SCREEN ->
                "Screen recording session" to "Guard will watch your screen for risky flows."
            SessionType.UPLOAD_MEDIA ->
                "Uploaded media session" to "Guard will analyze your audio or screenshots."
            SessionType.WEBSITE_LINK ->
                "Link scan session" to "Guard will scan your URL for scams and red flags."
        }

        val session = GuardSession(
            id = System.currentTimeMillis(),
            type = type,
            title = title,
            subtitle = subtitle
        )

        sessions.add(0, session)
        sessionsAdapter.notifyItemInserted(0)
        rvSessions.scrollToPosition(0)
        updateEmptyState()

        return session
    }

    private fun updateEmptyState() {
        tvEmptySessions.visibility = if (sessions.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openSession(session: GuardSession) {
        val ctx = requireContext()
        val intent = Intent(ctx, SessionActivity::class.java).apply {
            // 🔹 Send the string id that matches your enum: "listen_audio", "record_screen", etc.
            putExtra("session_mode", session.type.id)

            // 🔹 Also send title & subtitle for the header
            putExtra(SessionActivity.EXTRA_SESSION_TITLE, session.title)
            putExtra(SessionActivity.EXTRA_SESSION_SUBTITLE, session.subtitle)
        }
        startActivity(intent)
    }

    // --------- Models & Adapter ----------

    data class GuardSession(
        val id: Long,
        val type: SessionType,
        val title: String,
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
                tvTitle.text = session.title
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