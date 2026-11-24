package oolong.client.ui.widgets.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import oolong.client.ui.common.theme.compose.Dimens
import oolong.client.ui.common.theme.compose.PressFeedback
import oolong.client.ui.common.theme.compose.pressScale

/**
 * A 48dp round button around an icon, with the app's press feedback instead of Material's
 * unbounded ripple. Same parameters as Material's `IconButton`. In a floating top bar's row the
 * glass it sits on answers the press (see [floatingGlass]), so it gives none of its own.
 */
@Composable
fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val onBarGlass = pressedByGlass()
    Box(
        modifier = modifier
            .then(if (onBarGlass) Modifier else Modifier.pressScale(source))
            .size(Dimens.MinTouchTarget)
            .clip(CircleShape)
            .clickable(
                onClick = onClick,
                enabled = enabled,
                role = Role.Button,
                interactionSource = source,
                indication = if (onBarGlass) null else PressFeedback.Overlay,
            ),
        contentAlignment = Alignment.Center,
    ) {
        val contentAlpha = if (enabled) LocalContentAlpha.current else ContentAlpha.disabled
        CompositionLocalProvider(LocalContentAlpha provides contentAlpha, content = content)
    }
}
