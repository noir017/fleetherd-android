package io.github.noir017.fleetherd.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.noir017.fleetherd.R
import io.github.noir017.fleetherd.data.ServerConfigStore
import io.github.noir017.fleetherd.data.ServerProfile
import io.github.noir017.fleetherd.databinding.DialogServerConfigBinding
import io.github.noir017.fleetherd.web.OriginGuard

object ServerSetupDialog {
    fun show(
        context: Context,
        store: ServerConfigStore,
        cancelable: Boolean,
        onSelected: (ServerProfile) -> Unit
    ) {
        val binding = DialogServerConfigBinding.inflate(LayoutInflater.from(context))
        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.server_dialog_title)
            .setView(binding.root)
            .setCancelable(cancelable)
            .setPositiveButton(R.string.server_save_connect, null)
            .apply {
                if (cancelable) setNegativeButton(R.string.server_cancel, null)
            }
            .create()

        fun renderSaved() {
            val saved = store.list()
            val activeId = store.active()?.id
            binding.savedServersContainer.removeAllViews()
            binding.savedServersContainer.visibility = if (saved.isEmpty()) View.GONE else View.VISIBLE

            for (profile in saved) {
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 8, 0, 8)
                }

                val label = TextView(context).apply {
                    text = if (profile.id == activeId) "● ${profile.name}\n${profile.url}" else "${profile.name}\n${profile.url}"
                    setTextColor(ContextCompat.getColor(context, R.color.fh_fg))
                    textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    setOnClickListener {
                        store.setActive(profile.id)
                        dialog.dismiss()
                        onSelected(profile)
                    }
                }

                val deleteBtn = Button(context, null, android.R.attr.borderlessButtonStyle).apply {
                    text = context.getString(R.string.server_delete)
                    setTextColor(ContextCompat.getColor(context, R.color.fh_dim))
                    setOnClickListener {
                        store.delete(profile.id)
                        renderSaved()
                    }
                }

                row.addView(label)
                row.addView(deleteBtn)
                binding.savedServersContainer.addView(row)
            }
        }

        renderSaved()

        dialog.setOnShowListener {
            val saveBtn = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            saveBtn.setOnClickListener {
                val rawUrl = binding.inputServerUrl.text?.toString().orEmpty()
                val normalized = OriginGuard.normalizeServerUrl(rawUrl)
                if (normalized == null) {
                    binding.inputError.text = context.getString(R.string.server_url_invalid)
                    binding.inputError.visibility = View.VISIBLE
                    return@setOnClickListener
                }
                binding.inputError.visibility = View.GONE
                val name = binding.inputServerName.text?.toString().orEmpty()
                val ssoHosts = binding.inputSsoHosts.text?.toString().orEmpty()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                val savedProfile = store.upsert(name, normalized, ssoHosts)
                dialog.dismiss()
                onSelected(savedProfile)
            }
        }

        dialog.show()
    }
}
