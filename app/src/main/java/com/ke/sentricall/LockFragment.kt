package com.ke.sentricall

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.ke.sentricall.data.local.ClubSettingsDao
import com.ke.sentricall.data.local.ClubSettingsEntity
import com.ke.sentricall.data.local.ProtectedAppEntity
import com.ke.sentricall.data.local.ProtectedAppsDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class LockFragment : Fragment() {

    private lateinit var switchClubMode: Switch
    private lateinit var tvClubStatus: TextView
    private lateinit var tvEmptyApps: TextView
    private lateinit var recyclerClubApps: RecyclerView
    private lateinit var btnAddProtectedApps: MaterialButton

    private lateinit var db: SentricallDatabase
    private lateinit var protectedAppsDao: ProtectedAppsDao
    private lateinit var clubSettingsDao: ClubSettingsDao

    private val protectedApps = mutableListOf<ProtectedAppEntity>()
    private lateinit var adapter: ProtectedAppsAdapter

    // Used to avoid triggering the PIN dialog on programmatic changes
    private var suppressSwitchCallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = SentricallDatabase.getInstance(requireContext())
        protectedAppsDao = db.protectedAppsDao()
        clubSettingsDao = db.clubSettingsDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_lock, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        switchClubMode = view.findViewById(R.id.switchClubMode)
        tvClubStatus = view.findViewById(R.id.tvClubStatus)
        tvEmptyApps = view.findViewById(R.id.tvEmptyApps)
        recyclerClubApps = view.findViewById(R.id.recyclerClubApps)
        btnAddProtectedApps = view.findViewById(R.id.btnAddProtectedApps)

        adapter = ProtectedAppsAdapter(
            packageManager = requireContext().packageManager,
            items = protectedApps,
            onRemoveClick = { app -> removeProtectedApp(app) }
        )
        recyclerClubApps.layoutManager = LinearLayoutManager(requireContext())
        recyclerClubApps.adapter = adapter

        parentFragmentManager.setFragmentResultListener(
            AddAppDialogFragment.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val selectedPackages =
                bundle.getStringArrayList(AddAppDialogFragment.EXTRA_SELECTED_PACKAGES)
                    ?: arrayListOf()
            if (selectedPackages.isNotEmpty()) {
                addSelectedApps(selectedPackages)
            }
        }

        // Only show PIN dialog for real user toggles
        switchClubMode.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitchCallback) {
                // Ignore programmatic changes
                return@setOnCheckedChangeListener
            }

            if (isChecked) {
                // Turning ON
                if (!isAccessibilityServiceEnabled()) {
                    Toast.makeText(
                        requireContext(),
                        "To use Club mode, enable Sentricall in Accessibility settings.",
                        Toast.LENGTH_LONG
                    ).show()
                    openAccessibilitySettings()

                    suppressSwitchCallback = true
                    switchClubMode.isChecked = false
                    suppressSwitchCallback = false
                    return@setOnCheckedChangeListener
                }

                lifecycleScope.launch {
                    val settings = withContext(Dispatchers.IO) {
                        clubSettingsDao.getSettings()
                    }
                    val currentHash = settings?.pinHash

                    showPinDialog(
                        currentPinHash = currentHash,
                        onPinVerifiedOrSet = { finalHash ->
                            lifecycleScope.launch(Dispatchers.IO) {
                                val now = System.currentTimeMillis()
                                val entity = (settings ?: ClubSettingsEntity()).copy(
                                    clubModeEnabled = true,
                                    pinHash = finalHash,
                                    updatedAt = now
                                )
                                clubSettingsDao.upsert(entity)
                                ClubModeState.enabled = true

                                val allProtected = protectedAppsDao.getAll()
                                ClubModeState.protectedPackages =
                                    allProtected.map { it.packageName }.toSet()

                                withContext(Dispatchers.Main) {
                                    suppressSwitchCallback = true
                                    switchClubMode.isChecked = true
                                    suppressSwitchCallback = false

                                    updateClubStatusText(true)
                                    Toast.makeText(
                                        requireContext(),
                                        "Club mode ON",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onCancelled = {
                            suppressSwitchCallback = true
                            switchClubMode.isChecked = false
                            suppressSwitchCallback = false
                        }
                    )
                }
            } else {
                // Turning OFF → still require PIN confirmation
                lifecycleScope.launch {
                    val settings = withContext(Dispatchers.IO) {
                        clubSettingsDao.getSettings()
                    }
                    val currentHash = settings?.pinHash

                    // If no PIN was set somehow, just turn off
                    if (currentHash == null) {
                        withContext(Dispatchers.IO) {
                            val now = System.currentTimeMillis()
                            val entity = (settings ?: ClubSettingsEntity()).copy(
                                clubModeEnabled = false,
                                updatedAt = now
                            )
                            clubSettingsDao.upsert(entity)
                            ClubModeState.enabled = false
                        }
                        withContext(Dispatchers.Main) {
                            suppressSwitchCallback = true
                            switchClubMode.isChecked = false
                            suppressSwitchCallback = false
                            updateClubStatusText(false)
                            Toast.makeText(
                                requireContext(),
                                "Club mode OFF",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }

                    // Ask for PIN before disabling
                    showPinDialog(
                        currentPinHash = currentHash,
                        onPinVerifiedOrSet = {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val now = System.currentTimeMillis()
                                val entity = (settings ?: ClubSettingsEntity()).copy(
                                    clubModeEnabled = false,
                                    updatedAt = now
                                )
                                clubSettingsDao.upsert(entity)
                                ClubModeState.enabled = false

                                withContext(Dispatchers.Main) {
                                    suppressSwitchCallback = true
                                    switchClubMode.isChecked = false
                                    suppressSwitchCallback = false
                                    updateClubStatusText(false)
                                    Toast.makeText(
                                        requireContext(),
                                        "Club mode OFF",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        onCancelled = {
                            suppressSwitchCallback = true
                            switchClubMode.isChecked = true
                            suppressSwitchCallback = false
                        }
                    )
                }
            }
        }

        btnAddProtectedApps.setOnClickListener {
            val existingPackages = ArrayList(protectedApps.map { it.packageName })
            val dialog = AddAppDialogFragment.newInstance(existingPackages)
            dialog.show(parentFragmentManager, "AddAppDialog")
        }

        loadClubSettings()
        loadProtectedApps()
    }

    override fun onResume() {
        super.onResume()
        if (!isAccessibilityServiceEnabled() && switchClubMode.isChecked) {
            suppressSwitchCallback = true
            switchClubMode.isChecked = false
            suppressSwitchCallback = false

            updateClubStatusText(false)
            ClubModeState.enabled = false
        }
    }

    private fun loadClubSettings() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val settings = clubSettingsDao.getSettings()
            val enabled = settings?.clubModeEnabled ?: false

            ClubModeState.enabled = enabled

            withContext(Dispatchers.Main) {
                suppressSwitchCallback = true
                switchClubMode.isChecked = enabled
                suppressSwitchCallback = false

                updateClubStatusText(enabled)
            }
        }
    }

    private fun loadProtectedApps() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val list = protectedAppsDao.getAll()
            val pkgSet = list.map { it.packageName }.toSet()
            ClubModeState.protectedPackages = pkgSet

            withContext(Dispatchers.Main) {
                protectedApps.clear()
                protectedApps.addAll(list)
                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
        }
    }

    private fun addSelectedApps(selectedPackages: List<String>) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val pm = requireContext().packageManager
            val newEntities = selectedPackages.map { pkg ->
                val label = try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    pkg.substringAfterLast('.')
                }

                ProtectedAppEntity(
                    label = label,
                    packageName = pkg
                )
            }

            protectedAppsDao.insertAll(newEntities)
            val updated = protectedAppsDao.getAll()
            val pkgSet = updated.map { it.packageName }.toSet()
            ClubModeState.protectedPackages = pkgSet

            withContext(Dispatchers.Main) {
                protectedApps.clear()
                protectedApps.addAll(updated)
                adapter.notifyDataSetChanged()
                updateEmptyState()
                Toast.makeText(
                    requireContext(),
                    "Apps added to Club mode.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun removeProtectedApp(app: ProtectedAppEntity) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            protectedAppsDao.delete(app)
            val updated = protectedAppsDao.getAll()
            val pkgSet = updated.map { it.packageName }.toSet()
            ClubModeState.protectedPackages = pkgSet

            withContext(Dispatchers.Main) {
                protectedApps.clear()
                protectedApps.addAll(updated)
                adapter.notifyDataSetChanged()
                updateEmptyState()
                Toast.makeText(
                    requireContext(),
                    "Removed ${app.label} from Club mode.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun updateEmptyState() {
        if (protectedApps.isEmpty()) {
            tvEmptyApps.visibility = View.VISIBLE
            recyclerClubApps.visibility = View.GONE
        } else {
            tvEmptyApps.visibility = View.GONE
            recyclerClubApps.visibility = View.VISIBLE
        }
    }

    private fun updateClubStatusText(enabled: Boolean) {
        if (enabled) {
            tvClubStatus.text = "Club mode is ON. Protected apps are hidden on your phone."
        } else {
            tvClubStatus.text = "Club mode is OFF. Apps behave normally."
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedId =
            "${requireContext().packageName}/${SentricallAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.split(":").any { it.equals(expectedId, ignoreCase = true) }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun showPinDialog(
        currentPinHash: String?,
        onPinVerifiedOrSet: (String) -> Unit,
        onCancelled: () -> Unit
    ) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_otp_club_mode, null)
        val etOtp: TextInputEditText = dialogView.findViewById(R.id.etOtp)
        val btnConfirm: MaterialButton = dialogView.findViewById(R.id.btnConfirmOtp)
        val btnCancel: MaterialButton = dialogView.findViewById(R.id.btnCancelOtp)

        val title =
            if (currentPinHash == null) "Set Club mode PIN" else "Enter your Club mode PIN"

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnConfirm.setOnClickListener {
            val pin = etOtp.text?.toString()?.trim().orEmpty()
            if (pin.length < 4) {
                etOtp.error = "PIN must be at least 4 digits"
                return@setOnClickListener
            }

            val hash = hashPin(pin)

            if (currentPinHash != null && currentPinHash != hash) {
                etOtp.error = "Incorrect PIN"
                return@setOnClickListener
            }

            dialog.dismiss()
            onPinVerifiedOrSet(hash)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            onCancelled()
        }

        dialog.show()
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}