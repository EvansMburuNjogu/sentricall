package com.ke.sentricall

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnUpdateProfile: Button = view.findViewById(R.id.btnUpdateProfile)
        val btnLogout: Button = view.findViewById(R.id.btnLogout)

        // TODO: later – actually call API / save to storage
        btnUpdateProfile.setOnClickListener {
            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
        }

        // Logout → Go back to Login screen (MainActivity) and clear back stack
        btnLogout.setOnClickListener {
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
        }
    }
}