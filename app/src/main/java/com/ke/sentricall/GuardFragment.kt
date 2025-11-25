// app/src/main/java/com/ke/sentricall/GuardFragment.kt
package com.ke.sentricall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class GuardFragment : Fragment(), SessionTypeDialogFragment.OnSessionTypeSelectedListener {

    private lateinit var tvGuardTitle: TextView
    private lateinit var tvGuardSubtitle: TextView

    private lateinit var cardQuickLock: MaterialCardView
    private lateinit var cardQuickCopilot: MaterialCardView
    private lateinit var cardQuickProfile: MaterialCardView

    private lateinit var btnAddSession: MaterialButton
    private lateinit var recyclerSessions: RecyclerView
    private lateinit var tvEmptySessions: TextView

    private val sessions = mutableListOf<GuardSession>()
    private lateinit var adapter: GuardSessionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // You can preload sessions from DB here later
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_guard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvGuardTitle = view.findViewById(R.id.tvGuardTitle)
        tvGuardSubtitle = view.findViewById(R.id.tvGuardSubtitle)

        cardQuickLock = view.findViewById(R.id.cardQuickLock)
        cardQuickCopilot = view.findViewById(R.id.cardQuickCopilot)
        cardQuickProfile = view.findViewById(R.id.cardQuickProfile)

        btnAddSession = view.findViewById(R.id.btnAddSession)
        recyclerSessions = view.findViewById(R.id.recyclerSessions)
        tvEmptySessions = view.findViewById(R.id.tvEmptySessions)

        // Header
        tvGuardTitle.text = "Guard"
        tvGuardSubtitle.text = "Create AI-powered sessions to listen, watch, or review suspicious activity."

        // Quick link actions (for now just show toasts – we can wire real nav later)
        cardQuickLock.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Lock screen…", Toast.LENGTH_SHORT).show()
            // TODO: navigate to Lock tab / fragment if you want
        }

        cardQuickCopilot.setOnClickListener {
            Toast.makeText(requireContext(), "Guard Copilot coming soon.", Toast.LENGTH_SHORT).show()
        }

        cardQuickProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Profile & settings coming soon.", Toast.LENGTH_SHORT).show()
        }

        // Sessions list
        adapter = GuardSessionAdapter(sessions) { session ->
            openSessionDetail(session)
        }
        recyclerSessions.layoutManager = LinearLayoutManager(requireContext())
        recyclerSessions.adapter = adapter

        updateEmptyState()

        btnAddSession.setOnClickListener {
            showSessionTypeDialog()
        }
    }

    private fun showSessionTypeDialog() {
        val dialog = SessionTypeDialogFragment.newInstance()
        dialog.setTargetFragment(this, 0) // deprecated but simple; fine for now
        dialog.show(parentFragmentManager, "SessionTypeDialog")
    }

    override fun onSessionTypeSelected(type: SessionType) {
        // Create a new in-memory session. Later you can persist to Room.
        val newSession = GuardSession(
            id = System.currentTimeMillis(),
            type = type,
            title = when (type) {
                SessionType.LISTEN_AUDIO -> "Listening session"
                SessionType.RECORD_SCREEN -> "Screen watch"
                SessionType.UPLOAD_MEDIA -> "Uploaded media review"
                SessionType.WEBSITE_LINK -> "Website scan"
            },
            summary = "New ${type.displayName} session. AI will analyse activity and flag risks."
        )

        sessions.add(0, newSession)
        adapter.notifyItemInserted(0)
        recyclerSessions.scrollToPosition(0)
        updateEmptyState()

        openSessionDetail(newSession)
    }

    private fun updateEmptyState() {
        if (sessions.isEmpty()) {
            tvEmptySessions.visibility = View.VISIBLE
            recyclerSessions.visibility = View.GONE
        } else {
            tvEmptySessions.visibility = View.GONE
            recyclerSessions.visibility = View.VISIBLE
        }
    }

    private fun openSessionDetail(session: GuardSession) {
        // For now just toast – later navigate to the dedicated session screen.
        Toast.makeText(
            requireContext(),
            "Open session: ${session.title} (${session.type.displayName})",
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Simple in-memory model for Guard sessions.
 */
data class GuardSession(
    val id: Long,
    val type: SessionType,
    val title: String,
    val summary: String
)

/**
 * RecyclerView adapter for sessions.
 */
class GuardSessionAdapter(
    private val items: MutableList<GuardSession>,
    private val onClick: (GuardSession) -> Unit
) : RecyclerView.Adapter<GuardSessionAdapter.SessionViewHolder>() {

    inner class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.ivSessionIcon)
        val title: TextView = itemView.findViewById(R.id.tvSessionTitle)
        val subtitle: TextView = itemView.findViewById(R.id.tvSessionSubtitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_guard_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.subtitle.text = item.summary

        val iconRes = when (item.type) {
            SessionType.LISTEN_AUDIO -> R.drawable.ic_guard_mic
            SessionType.RECORD_SCREEN -> R.drawable.ic_guard_screen
            SessionType.UPLOAD_MEDIA -> R.drawable.ic_guard_upload
            SessionType.WEBSITE_LINK -> R.drawable.ic_guard_link
        }
        holder.icon.setImageResource(iconRes)

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }
}