package com.ke.sentricall

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class ChatActivity : AppCompatActivity() {

    private lateinit var tvChatTitle: TextView
    private lateinit var tvChatSubtitle: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var scrollMessages: ScrollView
    private lateinit var layoutMessages: LinearLayout
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Using fragment_chat as the screen layout for this Activity
        setContentView(R.layout.fragment_chat)

        tvChatTitle = findViewById(R.id.tvChatTitle)
        tvChatSubtitle = findViewById(R.id.tvChatSubtitle)
        btnBack = findViewById(R.id.btnBack)
        scrollMessages = findViewById(R.id.scrollMessages)
        layoutMessages = findViewById(R.id.layoutMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        // Get title from intent
        val initialTitle = intent.getStringExtra("chat_title") ?: "Copilot chat"
        tvChatTitle.text = initialTitle

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Click title to rename chat (beautified modal)
        tvChatTitle.setOnClickListener {
            showRenameChatDialog(tvChatTitle.text.toString())
        }

        // Simple "Send" behaviour (append bubbles)
        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                addUserMessageBubble(text)
                etMessage.setText("")
                scrollToBottom()
            }
        }
    }

    private fun addUserMessageBubble(message: String) {
        val bubble = TextView(this).apply {
            text = message
            setTextColor(resources.getColor(android.R.color.white, theme))
            textSize = 14f
            setBackgroundColor(android.graphics.Color.parseColor("#1F2937")) // dark gray
            setPadding(16, 12, 16, 12)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.topMargin = 8
            params.bottomMargin = 4
            params.marginEnd = 24
            params.gravity = android.view.Gravity.END
            layoutParams = params
        }

        layoutMessages.addView(bubble)
    }

    private fun scrollToBottom() {
        scrollMessages.post {
            scrollMessages.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun showRenameChatDialog(currentTitle: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_new_chat, null)

        val tvDialogTitle: TextView = dialogView.findViewById(R.id.tvDialogTitle)
        val tvDialogSubtitle: TextView = dialogView.findViewById(R.id.tvDialogSubtitle)
        val etChatName: TextInputEditText = dialogView.findViewById(R.id.etChatName)
        val btnCancel: View = dialogView.findViewById(R.id.btnCancel)
        val btnCreate: Button = dialogView.findViewById(R.id.btnCreate)

        // Customize for "Rename chat"
        tvDialogTitle.text = "Rename chat"
        tvDialogSubtitle.text = "Update how this chat appears in your list."
        etChatName.setText(currentTitle)
        etChatName.setSelection(currentTitle.length)
        btnCreate.text = "Save"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnCreate.setOnClickListener {
            var newName = etChatName.text?.toString()?.trim() ?: ""
            if (newName.isEmpty()) {
                newName = "Copilot chat"
            }
            tvChatTitle.text = newName
            dialog.dismiss()
        }

        dialog.show()
    }
}