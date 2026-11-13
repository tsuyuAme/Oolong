package oolong.client.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.TextLinkStyles
import oolong.client.ui.navigation.TopicKey
import oolong.client.ui.navigation.UserProfileKey
import oolong.client.ui.page.LocalNavigator
import oolong.client.utils.launchUrl

/** What tapping a link in text opens: an address, a user's profile or a hot topic. */
@Stable
class TextLinkActions(
    val openUrl: (String) -> Unit,
    val openUser: (Long) -> Unit,
    val openTopic: (String) -> Unit,
) {
    fun url(url: String, styles: TextLinkStyles? = null): LinkAnnotation =
        LinkAnnotation.Clickable(TAG_URL, styles) { openUrl(url) }

    fun user(id: Long, styles: TextLinkStyles? = null): LinkAnnotation =
        LinkAnnotation.Clickable(TAG_USER, styles) { openUser(id) }

    fun topic(name: String, styles: TextLinkStyles? = null): LinkAnnotation =
        LinkAnnotation.Clickable(TAG_TOPIC, styles) { openTopic(name) }

    companion object {
        const val TAG_URL = "url"
        const val TAG_USER = "user"
        const val TAG_TOPIC = "topic"
    }
}

/**
 * Link actions that open pages with the current context and navigator. The same instance lasts
 * the whole composition, so text built with it is not rebuilt when either changes.
 */
@Composable
fun rememberTextLinkActions(): TextLinkActions {
    val context by rememberUpdatedState(LocalContext.current)
    val navigator by rememberUpdatedState(LocalNavigator.current)
    return remember {
        TextLinkActions(
            openUrl = { launchUrl(context, navigator, it) },
            openUser = { navigator.navigate(UserProfileKey(it)) },
            openTopic = { navigator.navigate(TopicKey(id = 0, name = it)) },
        )
    }
}
