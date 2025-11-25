package com.ke.sentricall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.ImageView
import android.widget.TextView

class ChatFragment : Fragment() {

    companion object {
        private const val ARG_CHAT_NAME = "arg_chat_name"

        fun newInstance(chatName: String): ChatFragment {
            val fragment = ChatFragment()
            val args = Bundle()
            args.putString(ARG_CHAT_NAME, chatName)
            fragment.arguments = args
            return fragment
        }
    }

    private var chatName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chatName = arguments?.getString(ARG_CHAT_NAME)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvChatTitle: TextView = view.findViewById(R.id.tvChatTitle)
        val btnBack: ImageView = view.findViewById(R.id.btnBack)

        tvChatTitle.text = chatName ?: "New chat"

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Later: hook up RecyclerView / message list and send button
        // val btnSend: View = view.findViewById(R.id.btnSend)
        // val etMessage: TextInputEditText = view.findViewById(R.id.etMessage)
    }
}