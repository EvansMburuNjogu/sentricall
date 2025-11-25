package com.ke.sentricall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar

data class PreviousSessionUi(
    val id: Long,
    val type: SessionType,
    val riskLabel: String,
    val timestampLabel: String
)

class PreviousSessionsActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private val items = mutableListOf<PreviousSessionUi>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_previous_sessions)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbarPreviousSessions)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        recycler = findViewById(R.id.recyclerPreviousSessions)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = PreviousSessionsAdapter(items)

        // TODO: load real data from DB later
        seedFakeData()
    }

    private fun seedFakeData() {
        items.clear()
        items.addAll(
            listOf(
                PreviousSessionUi(
                    id = 1,
                    type = SessionType.LISTEN_AUDIO,
                    riskLabel = "Risk: Medium",
                    timestampLabel = "Today • 22:10"
                ),
                PreviousSessionUi(
                    id = 2,
                    type = SessionType.WEBSITE_LINK,
                    riskLabel = "Risk: High",
                    timestampLabel = "Today • 20:34"
                ),
                PreviousSessionUi(
                    id = 3,
                    type = SessionType.UPLOAD_MEDIA,
                    riskLabel = "Risk: Low",
                    timestampLabel = "Yesterday • 18:02"
                )
            )
        )
        recycler.adapter?.notifyDataSetChanged()
    }
}

class PreviousSessionsAdapter(
    private val items: List<PreviousSessionUi>
) : RecyclerView.Adapter<PreviousSessionsAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvPrevSessionTitle)
        val tvMeta: TextView = itemView.findViewById(R.id.tvPrevSessionMeta)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_previous_session, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvTitle.text = "${item.type.title} — ${item.timestampLabel.substringAfter('•').trim()}"
        holder.tvMeta.text = "${item.riskLabel} • ${item.timestampLabel}"
        // Later: onClick -> open session details or AI report
    }

    override fun getItemCount(): Int = items.size
}