// app/src/main/java/com/ke/sentricall/SessionTypeDialogFragment.kt
package com.ke.sentricall

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import androidx.fragment.app.DialogFragment
import com.google.android.material.card.MaterialCardView

class SessionTypeDialogFragment : DialogFragment() {

    interface OnSessionTypeSelectedListener {
        fun onSessionTypeSelected(type: SessionType)
    }

    private val callback: OnSessionTypeSelectedListener?
        get() = targetFragment as? OnSessionTypeSelectedListener
            ?: parentFragment as? OnSessionTypeSelectedListener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_guard_session_type, null, false)

        dialog.setContentView(view)
        dialog.setCanceledOnTouchOutside(true)

        // Make the window background fully transparent so only our card is visible
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Hook up the action cards
        val cardListen: MaterialCardView = view.findViewById(R.id.cardListenAudio)
        val cardScreen: MaterialCardView = view.findViewById(R.id.cardRecordScreen)
        val cardUpload: MaterialCardView = view.findViewById(R.id.cardUploadMedia)
        val cardLink: MaterialCardView = view.findViewById(R.id.cardWebsiteLink)

        cardListen.setOnClickListener {
            callback?.onSessionTypeSelected(SessionType.LISTEN_AUDIO)
            dismiss()
        }

        cardScreen.setOnClickListener {
            callback?.onSessionTypeSelected(SessionType.RECORD_SCREEN)
            dismiss()
        }

        cardUpload.setOnClickListener {
            callback?.onSessionTypeSelected(SessionType.UPLOAD_MEDIA)
            dismiss()
        }

        cardLink.setOnClickListener {
            callback?.onSessionTypeSelected(SessionType.WEBSITE_LINK)
            dismiss()
        }

        return dialog
    }

    companion object {
        fun newInstance(): SessionTypeDialogFragment = SessionTypeDialogFragment()
    }
}