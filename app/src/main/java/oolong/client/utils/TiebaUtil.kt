package oolong.client.utils

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PersistableBundle
import oolong.client.R
import oolong.client.api.legacyThreadRemoteDataSource
import oolong.client.services.AutoSign
import oolong.client.toastShort
import oolong.client.ui.navigation.Navigator
import oolong.client.ui.navigation.WebViewKey
import oolong.client.ui.widgets.compose.AppDialogs

object TiebaUtil {
    /** The label of what the app copies, so it can tell its own copies apart. */
    const val ClipLabel = "Oolong"

    private fun ClipData.setIsSensitive(isSensitive: Boolean): ClipData = apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, isSensitive)
            }
        }
    }

    @JvmStatic
    @JvmOverloads
    fun copyText(
        context: Context,
        text: String?,
        toast: String = context.getString(R.string.toast_copy_success),
        isSensitive: Boolean = false
    ) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText(ClipLabel, text).setIsSensitive(isSensitive)
        cm.setPrimaryClip(clipData)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            context.toastShort(toast)
        }
    }

    fun initAutoSign(context: Context) = AutoSign.schedule(context)

    @JvmStatic
    fun startSign(context: Context) = AutoSign.start(context)

    /**
     * Shares [text], under [title] when given, through the system's share sheet. Always the sheet:
     * sent bare, some systems hand it to a remembered app or drop it without a word.
     */
    @JvmStatic
    @JvmOverloads
    fun shareText(context: Context, text: String, title: String? = null) {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "${if (title != null) "「$title」\n" else ""}$text\n" + context.getString(R.string.share_suffix)
            )
        }
        runCatching { context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            .onFailure { context.toastShort(R.string.toast_share_unavailable) }
    }

    suspend fun reportPost(
        context: Context,
        navigator: Navigator,
        postId: String,
    ) {
        val url = AppDialogs.withLoading { runCatching { legacyThreadRemoteDataSource.reportUrl(postId.toLong()) } }
        url.onSuccess { navigator.navigate(WebViewKey(it)) }
            .onFailure { context.toastShort(R.string.toast_load_failed) }
    }
}
