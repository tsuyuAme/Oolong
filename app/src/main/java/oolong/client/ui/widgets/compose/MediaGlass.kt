package oolong.client.ui.widgets.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import oolong.client.ui.common.theme.compose.ExtendedColors
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.LocalExtendedColors
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.utils.ThemeUtil

/**
 * Glass on black takes its tone from the page and would be black itself: over pictures and video
 * it is lifted to a grey of its own, so the controls stand off the black by tone.
 */
private val MediaChromeTone = Color(0xFF3A3B40)

/** [colors] as glass over pictures and video on black takes them. */
fun mediaChromeColors(colors: ExtendedColors): ExtendedColors = colors.copy(
    theme = if (ThemeUtil.isTranslucentTheme(colors.theme)) ThemeUtil.THEME_BLUE_DARK else colors.theme,
    background = MediaChromeTone,
)

/**
 * Glass in [content] samples what [state] captures, a viewer's picture or video: [mediaGlass],
 * clear and dimmed, in the dark appearance whatever the app's, as the controls on it are white;
 * where the device draws no glass, frosted or solid chrome toned for black. The picture or video
 * is marked with `hazeSource(state)`; the glass is drawn over it, outside that.
 */
@Composable
fun ProvideMediaGlass(state: HazeState, content: @Composable () -> Unit) {
    val colors = ExtendedTheme.colors
    val chrome = remember(colors) { mediaChromeColors(colors).copy(isNightMode = true) }
    val source = remember(state) { GlassSource(state) }
    CompositionLocalProvider(LocalExtendedColors provides chrome, LocalGlassSource provides source) {
        ProvideGlassAppearance(content)
    }
}

/**
 * A group of actions over a picture or video, side by side on one piece of glass, as a top bar's
 * actions are: [IconButton]s with white icons.
 */
@Composable
fun MediaGlassActions(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.mediaGlass(SmoothCornerShape.Capsule, pressable = true),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The glass answers presses on them as one piece
        CompositionLocalProvider(LocalContentColor provides Color.White, LocalPressedByGlass provides true) { content() }
    }
}

/** A round glass button over a picture or video, which answers a press itself. */
@Composable
fun MediaGlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .mediaGlass(CircleShape, pressable = enabled)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClickLabel = onClickLabel,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** [MediaGlassButton] showing [icon] in white. */
@Composable
fun MediaGlassIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
) {
    MediaGlassButton(onClick = onClick, modifier = modifier, size = size) {
        Icon(imageVector = icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(iconSize))
    }
}

/** A glass capsule over a picture or video holding a line of white text; a button when [onClick] is given. */
@Composable
fun MediaGlassPill(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .mediaGlass(SmoothCornerShape.Capsule, pressable = onClick != null)
            .then(
                if (onClick == null) Modifier
                else Modifier.clickable(interactionSource = interaction, indication = null, onClickLabel = onClickLabel, role = Role.Button, onClick = onClick),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White, content = { content() })
    }
}
