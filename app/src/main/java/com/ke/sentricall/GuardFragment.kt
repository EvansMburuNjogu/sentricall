package com.ke.sentricall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class GuardFragment : Fragment(R.layout.fragment_guard) {

    private lateinit var quickLock: View
    private lateinit var quickCopilot: View
    private lateinit var quickProfile: View
    private lateinit var btnAddSession: MaterialButton
    private lateinit var rvSessions: RecyclerView

    private val sessions = mutableListOf<GuardSession>()
    private lateinit var sessionsAdapter: GuardSessionAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        quickLock = view.findViewById(R.id.quickLock)
        quickCopilot = view.findViewById(R.id.quickCopilot)
        quickProfile = view.findViewById(R.id.quickProfile)
        btnAddSession = view.findViewById(R.id.btnAddSession)
        rvSessions = view.findViewById(R.id.rvSessions)

        // Recycler setup
        sessionsAdapter = GuardSessionAdapter(sessions)
        rvSessions.layoutManager = LinearLayoutManager(requireContext())
        rvSessions.adapter = sessionsAdapter

        // Quick links – placeholder actions for now
        quickLock.setOnClickListener {
            Toast.makeText(requireContext(), "Lock quick link tapped", Toast.LENGTH_SHORT).show()
        }

        quickCopilot.setOnClickListener {
            Toast.makeText(requireContext(), "Copilot quick link tapped", Toast.LENGTH_SHORT).show()
        }

        quickProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Profile quick link tapped", Toast.LENGTH_SHORT).show()
        }

        // Add session – open the dialog
        btnAddSession.setOnClickListener {
            val dialog = SessionTypeDialogFragment()
            dialog.sessionTypeSelectedListener = { type ->
                addNewSession(type)
            }
            dialog.show(childFragmentManager, "session_type")
        }
    }

    private fun addNewSession(type: SessionType) {
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
    }

    // --------- Models & Adapter ----------

    data class GuardSession(
        val id: Long,
        val type: SessionType,
        val title: String,
        val subtitle: String
    )

    private class GuardSessionAdapter(
        private val items: List<GuardSession>
    ) : RecyclerView.Adapter<GuardSessionAdapter.SessionViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_guard_session, parent, false)
            return SessionViewHolder(view)
        }

        override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val tvTitle: TextView = itemView.findViewById(R.id.tvSessionTitle)
            private val tvSubtitle: TextView = itemView.findViewById(R.id.tvSessionSubtitle)

            fun bind(session: GuardSession) {
                tvTitle.text = session.title
                tvSubtitle.text = session.subtitle

                // Click to open session detail later
                itemView.setOnClickListener {
                    Toast.makeText(
                        itemView.context,
                        "Open ${session.title}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}