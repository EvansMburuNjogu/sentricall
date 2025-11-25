package com.ke.sentricall

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class LockGateActivity : AppCompatActivity() {

    private var blockedPkg: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock_gate)

        val tvTitle: TextView = findViewById(R.id.tvGateTitle)
        val tvSubtitle: TextView = findViewById(R.id.tvGateSubtitle)
        val btnUnlock: MaterialButton = findViewById(R.id.btnUnlock)
        val btnClose: MaterialButton = findViewById(R.id.btnClose)

        blockedPkg = intent.getStringExtra("blocked_package")
        val labelForText = blockedPkg ?: "this app"

        tvTitle.text = "Sentricall Club mode"
        tvSubtitle.text = "Before opening $labelForText, complete your security check."

        btnUnlock.setOnClickListener {
            blockedPkg?.let { pkg ->
                ClubModeState.markTemporarilyUnlocked(pkg)
            }
            finish()
        }

        btnClose.setOnClickListener {
            // just close & leave user where they were (usually home)
            finish()
        }
    }

    override fun onBackPressed() {
        // Block back; behave like "Close"
        // Do NOT call super.onBackPressed()
        // Just finish so user can’t bypass
        // but still exits the lock screen.
        finish()
    }
}