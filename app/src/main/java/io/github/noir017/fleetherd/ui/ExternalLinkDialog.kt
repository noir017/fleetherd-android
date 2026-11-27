package io.github.noir017.fleetherd.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.ContextThemeWrapper
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.noir017.fleetherd.R

object ExternalLinkDialog {
    fun show(context: Context, uri: Uri) {
        val url = uri.toString()
        val title = uri.host?.takeIf { it.isNotBlank() }
            ?: uri.scheme?.takeIf { it.isNotBlank() }
            ?: url

        val themedContext = ContextThemeWrapper(context, R.style.Theme_Fleetherd_LinkDialog)
        MaterialAlertDialogBuilder(themedContext)
            .setTitle(title)
            .setMessage(url)
            .setPositiveButton(R.string.link_open_in_browser) { _, _ ->
                openInExternalApp(context, uri)
            }
            .setNeutralButton(R.string.link_copy) { _, _ ->
                copyToClipboard(context, title, url)
            }
            .setNegativeButton(R.string.link_cancel, null)
            .show()
    }

    fun openInExternalApp(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, R.string.link_no_handler, Toast.LENGTH_SHORT).show()
        }
    }

    private fun copyToClipboard(context: Context, label: String, url: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, url))
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
        }
    }
}
