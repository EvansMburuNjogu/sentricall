package com.ke.sentricall

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class SessionTypeDialogFragment : DialogFragment() {

    var sessionTypeSelectedListener: ((SessionType) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view: View = inflater.inflate(R.layout.dialog_new_session_type, null, false)

        val optionListenAudio: View = view.findViewById(R.id.optionListenAudio)
        val optionRecordScreen: View = view.findViewById(R.id.optionRecordScreen)
        val optionUpload: View = view.findViewById(R.id.optionUpload)
        val optionWebsite: View = view.findViewById(R.id.optionWebsite)

        optionListenAudio.setOnClickListener {
            sessionTypeSelectedListener?.invoke(SessionType.LISTEN_AUDIO)
            dismiss()
        }

        optionRecordScreen.setOnClickListener {
            sessionTypeSelectedListener?.invoke(SessionType.RECORD_SCREEN)
            dismiss()
        }

        optionUpload.setOnClickListener {
            sessionTypeSelectedListener?.invoke(SessionType.UPLOAD_MEDIA)
            dismiss()
        }

        optionWebsite.setOnClickListener {
            sessionTypeSelectedListener?.invoke(SessionType.WEBSITE_LINK)
            dismiss()
        }

        return AlertDialog.Builder(requireContext(), R.style.ThemeOverlay_MaterialComponents_Dialog)
            .setView(view)
            .create()
    }
}