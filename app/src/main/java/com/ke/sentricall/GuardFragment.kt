package com.ke.sentricall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Guard screen:
 * - Quick links: Lock, Copilot, Profile
 * - "Add new session" button → choose session type
 * - Sessions list shown inline
 */
class GuardFragment : Fragment() {

    private lateinit var tvGuardTitle: TextView
    private lateinit var tvGuardSubtitle: TextView

    // Quick links
    private lateinit var quickLock: LinearLayout
    private lateinit var quickCopilot: LinearLayout
    private lateinit var quickProfile: LinearLayout

    // Sessions UI
    private lateinit var btnAddSession: MaterialButton
    private lateinit var tvNoSessions: TextView
    private lateinit var recyclerSessions: RecyclerView

    private lateinit var sessionsAdapter: GuardSessionsAdapter
    private val sessions: MutableList<GuardSession> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_guard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Header
        tvGuardTitle = view.findViewById(R.id.tvGuardTitle)
        tvGuardSubtitle = view.findViewById(R.id.tvGuardSubtitle)

        // Quick links
        quickLock = view.findViewById(R.id.quickLock)
        quickCopilot = view.findViewById(R.id.quickCopilot)
        quickProfile = view.findViewById(R.id.quickProfile)

        // Sessions
        btnAddSession = view.findViewById(R.id.btnAddSession)
        tvNoSessions = view.findViewById(R.id.tvNoSessions)
        recyclerSessions = view.findViewById(R.id.recyclerSessions)

        setupQuickLinks()
        setupSessionsList()
        setupAddSessionButton()

        // For now you can load mock sessions or leave empty
        loadInitialSessions()
        updateSessionsVisibility()
    }

    private fun setupQuickLinks() {
        quickLock.setOnClickListener {
            // Later: navigate to Lock tab / LockFragment
            Toast.makeText(requireContext(), "Open Lock screen", Toast.LENGTH_SHORT).show()
        }

        quickCopilot.setOnClickListener {
            // Later: navigate to AI Copilot screen
            Toast.makeText(requireContext(), "Open Guard Copilot", Toast.LENGTH_SHORT).show()
        }

        quickProfile.setOnClickListener {
            // Later: navigate to profile/settings
            Toast.makeText(requireContext(), "Open Profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSessionsList() {
        recyclerSessions.layoutManager = LinearLayoutManager(requireContext())
        sessionsAdapter = GuardSessionsAdapter(
            items = sessions,
            onSessionClick = { session ->
                // Later: open detailed session screen
                Toast.makeText(
                    requireContext(),
                    "Open session: ${session.title}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        recyclerSessions.adapter = sessionsAdapter
    }

    private fun setupAddSessionButton() {
        btnAddSession.setOnClickListener {
            showSessionTypeDialog()
        }
    }

    private fun showSessionTypeDialog() {
        val sessionTypes = arrayOf(
            "Listen to Audio",
            "Record Screen",
            "Upload Audio / Image",
            "Website Link"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Start new session")
            .setItems(sessionTypes) { _, which ->
                val type = when (which) {
                    0 -> SessionType.LISTEN_AUDIO
                    1 -> SessionType.RECORD_SCREEN
                    2 -> SessionType.UPLOAD_MEDIA
                    3 -> SessionType.WEBSITE_LINK
                    else -> SessionType.LISTEN_AUDIO
                }

                // For now we create a dummy session & add it to the list.
                // Later you can redirect to a dedicated Session screen.
                createNewSession(type)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createNewSession(type: SessionType) {
        val newSession = GuardSession(
            id = System.currentTimeMillis(),
            type = type,
            title = when (type) {
                SessionType.LISTEN_AUDIO -> "Listen to Audio"
                SessionType.RECORD_SCREEN -> "Record Screen"
                SessionType.UPLOAD_MEDIA -> "Upload Audio / Image"
                SessionType.WEBSITE_LINK -> "Website Link"
            },
            timeLabel = "Just now",
            riskLabel = "Pending",
            riskLevel = RiskLevel.UNKNOWN,
            shortSummary = "Guard will analyze this session once started."
        )

        sessions.add(0, newSession)
        sessionsAdapter.notifyItemInserted(0)
        recyclerSessions.scrollToPosition(0)
        updateSessionsVisibility()
    }

    private fun loadInitialSessions() {
        // Optional: mock a couple of previous sessions so UI doesn't look empty
        // You can remove this and load from Room later.
        // sessions.add(
        //     GuardSession(
        //         id = 1L,
        //         type = SessionType.LISTEN_AUDIO,
        //         title = "Safaricom call",
        //         timeLabel = "Today, 10:32",
        //         riskLabel = "Low risk",
        //         riskLevel = RiskLevel.LOW,
        //         shortSummary = "Guard found no suspicious phrases in this audio."
        //     )
        // )
        // sessionsAdapter.notifyDataSetChanged()
    }

    private fun updateSessionsVisibility() {
        if (sessions.isEmpty()) {
            tvNoSessions.visibility = View.VISIBLE
            recyclerSessions.visibility = View.GONE
        } else {
            tvNoSessions.visibility = View.GONE
            recyclerSessions.visibility = View.VISIBLE
        }
    }
}

/**
 * Domain model for a Guard session.
 */
data class GuardSession(
    val id: Long,
    val type: SessionType,
    val title: String,
    val timeLabel: String,
    val riskLabel: String,
    val riskLevel: RiskLevel,
    val shortSummary: String
)

enum class SessionType {
    LISTEN_AUDIO,
    RECORD_SCREEN,
    UPLOAD_MEDIA,
    WEBSITE_LINK
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    UNKNOWN
}

/**
 * Simple RecyclerView adapter for sessions list.
 */
class GuardSessionsAdapter(
    private val items: MutableList<GuardSession>,
    private val onSessionClick: (GuardSession) -> Unit
) : RecyclerView.Adapter<GuardSessionsAdapter.SessionViewHolder>() {

    inner class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvType: TextView = itemView.findViewById(R.id.tvSessionType)
        val tvTime: TextView = itemView.findViewById(R.id.tvSessionTime)
        val tvRisk: TextView = itemView.findViewById(R.id.tvSessionRisk)
        val tvSummary: TextView = itemView.findViewById(R.id.tvSessionSummary)

        init {
            itemView.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onSessionClick(items[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_guard_session, parent, false)
        return SessionViewHolder(view)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        val item = items[position]
        holder.tvType.text = item.title
        holder.tvTime.text = item.timeLabel
        holder.tvSummary.text = item.shortSummary

        // Risk pill text + color (you already have background drawables)
        holder.tvRisk.text = item.riskLabel
        when (item.riskLevel) {
            RiskLevel.LOW -> {
                holder.tvRisk.setTextColor(0xFF16A34A.toInt()) // green-ish
            }
            RiskLevel.MEDIUM -> {
                holder.tvRisk.setTextColor(0xFFEAB308.toInt()) // yellow-ish
            }
            RiskLevel.HIGH -> {
                holder.tvRisk.setTextColor(0xFFEF4444.toInt()) // red-ish
            }
            RiskLevel.UNKNOWN -> {
                holder.tvRisk.setTextColor(0xFF9CA3AF.toInt()) // grey
            }
        }
    }

    override fun getItemCount(): Int = items.size
}