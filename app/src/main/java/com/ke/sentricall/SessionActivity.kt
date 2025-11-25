package com.ke.sentricall

import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText

class SessionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SESSION_TYPE_ID = "session_type_id"
    }

    private var sessionType: SessionType = SessionType.LISTEN_AUDIO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session)

        val typeId = intent.getStringExtra(EXTRA_SESSION_TYPE_ID)
        sessionType = SessionType.fromId(typeId) ?: SessionType.LISTEN_AUDIO

        setupToolbar()
        bindSessionUi()
        bindButtons()
    }

    private fun setupToolbar() {
        val toolbar: MaterialToolbar = findViewById(R.id.toolbarSession)
        toolbar.title = sessionType.title
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun bindSessionUi() {
        val imgType: ImageView = findViewById(R.id.imgSessionTypeIcon)
        val tvTypeTitle: TextView = findViewById(R.id.tvSessionTypeTitle)
        val tvTypeDesc: TextView = findViewById(R.id.tvSessionTypeDescription)
        val tvSpecificTitle: TextView = findViewById(R.id.tvSessionSpecificTitle)
        val tvSpecificBody: TextView = findViewById(R.id.tvSessionSpecificBody)
        val tvAiSummaryBody: TextView = findViewById(R.id.tvAiSummaryBody)

        tvTypeTitle.text = sessionType.title
        tvTypeDesc.text = sessionType.description

        when (sessionType) {
            SessionType.LISTEN_AUDIO -> {
                imgType.setImageResource(android.R.drawable.ic_btn_speak_now)
                tvSpecificTitle.text = "Live audio monitoring"
                tvSpecificBody.text =
                    "Guard will listen to your environment or calls and highlight phrases that look risky."
                tvAiSummaryBody.text =
                    "AI will mark pushy sales tactics, urgent money requests, and identity-verification tricks."
            }
            SessionType.RECORD_SCREEN -> {
                imgType.setImageResource(android.R.drawable.presence_video_online)
                tvSpecificTitle.text = "Screen monitoring"
                tvSpecificBody.text =
                    "Guard will watch your screen recording for suspicious links, OTP requests, or payment flows."
                tvAiSummaryBody.text =
                    "AI looks for fake login pages, unusual payment amounts, and copy-paste prompts from scammers."
            }
            SessionType.UPLOAD_MEDIA -> {
                imgType.setImageResource(android.R.drawable.ic_menu_upload)
                tvSpecificTitle.text = "Upload & scan"
                tvSpecificBody.text =
                    "Upload call recordings or screenshots. Guard will analyse them once and give you a summary."
                tvAiSummaryBody.text =
                    "AI pulls out red flags, risky phrases, and any sign that someone is trying to pressure you."
            }
            SessionType.WEBSITE_LINK -> {
                imgType.setImageResource(android.R.drawable.ic_menu_view)
                tvSpecificTitle.text = "Website safety check"
                tvSpecificBody.text =
                    "Paste a link and Guard will check for phishing patterns, spoofed brands, or unsafe redirects."
                tvAiSummaryBody.text =
                    "AI checks URL patterns, page content, and typical scam tricks before you trust the site."
            }
        }
    }

    private fun bindButtons() {
        val btnOpenAiChat: MaterialButton = findViewById(R.id.btnOpenAiChat)
        val btnReport: MaterialButton = findViewById(R.id.btnReportSession)

        btnOpenAiChat.setOnClickListener {
            // Later: open dedicated AI chat area for this session
            Toast.makeText(
                this,
                "AI chat area coming soon.",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnReport.setOnClickListener {
            showReportDialog()
        }
    }

    private fun showReportDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_session_report, null)
        val etTitle: TextInputEditText = dialogView.findViewById(R.id.etReportTitle)
        val etDetails: TextInputEditText = dialogView.findViewById(R.id.etReportDetails)
        val cbSuspicious: CheckBox = dialogView.findViewById(R.id.cbMarkSuspicious)
        val btnCancel: MaterialButton = dialogView.findViewById(R.id.btnCancelReport)
        val btnSubmit: MaterialButton = dialogView.findViewById(R.id.btnSubmitReport)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Report this session")
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnSubmit.setOnClickListener {
            val title = etTitle.text?.toString()?.trim().orEmpty()
            val details = etDetails.text?.toString()?.trim().orEmpty()
            val suspicious = cbSuspicious.isChecked

            if (title.isEmpty()) {
                etTitle.error = "Title is required"
                return@setOnClickListener
            }

            // TODO: send to backend / save in Room
            // For now: just show a toast
            val msg = buildString {
                append("Report submitted: $title")
                if (suspicious) append(" (marked suspicious)")
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}