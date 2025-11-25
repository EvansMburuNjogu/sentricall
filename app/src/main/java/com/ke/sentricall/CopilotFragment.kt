package com.ke.sentricall

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton

class CopilotFragment : Fragment(R.layout.fragment_copilot) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnNewChat: MaterialButton = view.findViewById(R.id.btnNewChat)
        val containerChats: LinearLayout = view.findViewById(R.id.containerChats)
        val containerSessions: LinearLayout = view.findViewById(R.id.containerSessions)
        val itemChatSample: View = view.findViewById(R.id.itemChatSample)
        val itemSessionSample: View = view.findViewById(R.id.itemSessionSample)
        val btnDeleteChat: ImageButton = view.findViewById(R.id.btnDeleteChat)
        val btnDeleteSession: ImageButton = view.findViewById(R.id.btnDeleteSession)

        // Open chat screen from any chat/session
        val openChatScreen = View.OnClickListener {
            val intent = Intent(requireContext(), ChatActivity::class.java)
            startActivity(intent)
        }

        itemChatSample.setOnClickListener(openChatScreen)
        itemSessionSample.setOnClickListener(openChatScreen)
        btnNewChat.setOnClickListener(openChatScreen)

        // Delete sample chat
        btnDeleteChat.setOnClickListener {
            containerChats.removeView(itemChatSample)
            Toast.makeText(requireContext(), "Chat deleted", Toast.LENGTH_SHORT).show()
        }

        // Delete sample session
        btnDeleteSession.setOnClickListener {
            containerSessions.removeView(itemSessionSample)
            Toast.makeText(requireContext(), "Session deleted", Toast.LENGTH_SHORT).show()
        }
    }
}