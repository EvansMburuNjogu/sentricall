package com.ke.sentricall

import android.app.Dialog
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Dialog to pick apps to add into Club mode.
 *
 * Result is sent back using Fragment Result API:
 *  - RESULT_KEY
 *  - EXTRA_SELECTED_PACKAGES (ArrayList<String> of package names)
 */
class AddAppDialogFragment : DialogFragment() {

    companion object {
        const val RESULT_KEY = "add_app_result"
        const val EXTRA_SELECTED_PACKAGES = "selected_packages"
        private const val ARG_EXISTING_PACKAGES = "existing_packages"

        fun newInstance(existingPackages: ArrayList<String>? = null): AddAppDialogFragment {
            val f = AddAppDialogFragment()
            f.arguments = bundleOf(ARG_EXISTING_PACKAGES to existingPackages)
            return f
        }
    }

    private lateinit var recyclerApps: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnAdd: MaterialButton

    private lateinit var adapter: AppChoiceAdapter
    private val appChoices = mutableListOf<AppChoice>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val root = inflater.inflate(R.layout.dialog_add_app, null)

        recyclerApps = root.findViewById(R.id.recyclerApps)
        tvEmpty = root.findViewById(R.id.tvEmpty)
        btnCancel = root.findViewById(R.id.btnCancel)
        btnAdd = root.findViewById(R.id.btnAdd)

        // Load installed apps that have launcher icons
        appChoices.clear()
        appChoices.addAll(loadInstalledApps())

        adapter = AppChoiceAdapter(appChoices)
        recyclerApps.layoutManager = LinearLayoutManager(requireContext())
        recyclerApps.adapter = adapter

        updateEmptyState()

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(root)
            .setCancelable(true)
            .create()

        dialog.setCanceledOnTouchOutside(false)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnAdd.setOnClickListener {
            val selectedPackages = adapter.getSelectedPackages()
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                bundleOf(EXTRA_SELECTED_PACKAGES to ArrayList(selectedPackages))
            )
            dialog.dismiss()
        }

        return dialog
    }

    // ---------- Helpers ----------

    private fun loadInstalledApps(): List<AppChoice> {
        val pm = requireContext().packageManager

        // Existing protected packages (to hide them from list)
        val existing = arguments
            ?.getStringArrayList(ARG_EXISTING_PACKAGES)
            ?.toSet()
            ?: emptySet()

        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = pm.queryIntentActivities(intent, 0)

        val myPackage = requireContext().packageName
        val blockedPackages = setOf(
            myPackage,
            "com.android.settings"
        )

        return resolveInfos
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
            .map { ri ->
                val label = ri.loadLabel(pm).toString()
                val pkg = ri.activityInfo.packageName
                val icon = ri.loadIcon(pm)
                AppChoice(label, pkg, icon, selected = false)
            }
            .filter { choice ->
                !blockedPackages.contains(choice.packageName) &&
                        !existing.contains(choice.packageName)
            }
    }

    private fun updateEmptyState() {
        if (appChoices.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            recyclerApps.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            recyclerApps.visibility = View.VISIBLE
        }
    }

    // ---------- Data + Adapter ----------

    data class AppChoice(
        val label: String,
        val packageName: String,
        val icon: Drawable?,
        var selected: Boolean
    )

    private inner class AppChoiceAdapter(
        private val items: List<AppChoice>
    ) : RecyclerView.Adapter<AppChoiceAdapter.AppChoiceViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppChoiceViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app_choice, parent, false)
            return AppChoiceViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppChoiceViewHolder, position: Int) {
            val item = items[position]

            holder.icon.setImageDrawable(item.icon)
            holder.label.text = item.label
            holder.pkg.text = item.packageName
            holder.checkBox.isChecked = item.selected

            holder.itemView.setOnClickListener {
                item.selected = !item.selected
                holder.checkBox.isChecked = item.selected
            }

            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                item.selected = isChecked
            }
        }

        override fun getItemCount(): Int = items.size

        fun getSelectedPackages(): List<String> =
            items.filter { it.selected }.map { it.packageName }

        inner class AppChoiceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.imgIcon)
            val label: TextView = view.findViewById(R.id.tvLabel)
            val pkg: TextView = view.findViewById(R.id.tvPackage)
            val checkBox: MaterialCheckBox = view.findViewById(R.id.chkSelect)
        }
    }
}