package oolong.client.ui.widgets.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.Motion
import oolong.client.ui.common.theme.compose.Radius
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.common.theme.compose.Spacing
import oolong.client.ui.common.theme.compose.TextStyles

/**
 * A toast (design-system.md §9.3): a glass capsule of [text] and an [action] button, with an
 * [icon] before the text when given. With a [detail], a line or two saying more under the text,
 * it is a rounded card instead. While [visible] it condenses where it stands, from blurred and
 * small, and when not it comes apart there again (§7.2), so it never crosses the bar it stands
 * over. Tapping it anywhere else runs [onClick], when given. The caller keeps [text] while it leaves.
 */
@Composable
fun GlassToast(
    visible: Boolean,
    text: String,
    action: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    icon: Painter? = null,
    detail: String? = null,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        progress.animateTo(
            targetValue = if (visible) 1f else 0f,
            animationSpec = if (visible) Motion.morph(Motion.ProgressThreshold) else Motion.dismiss(Motion.ProgressThreshold),
        )
    }
    if (!visible && progress.value == 0f) return
    val colors = ExtendedTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val shape = if (detail != null) DetailedToastShape else ToastShape
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .condense({ progress.value }, blur = ToastBlur, rise = ToastRise, scale = ToastScale, fadeFaster = 3f)
            .padding(horizontal = Spacing.md)
            .widthIn(max = 480.dp)
            .heightIn(min = 48.dp)
            .floatingGlass(shape, pressable = onClick != null)
            .then(if (onClick != null) Modifier.clickable(interactionSource, indication = null, onClick = onClick) else Modifier)
            .padding(start = if (icon != null) Spacing.md else Spacing.lg, end = Spacing.xxs)
            .padding(vertical = if (detail != null) Spacing.sm else 0.dp),
    ) {
        if (icon != null) {
            Icon(painter = icon, contentDescription = null, tint = colors.text, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(Spacing.sm))
        }
        Column(modifier = Modifier.weight(1f, fill = false)) {
            Text(
                text = text,
                style = TextStyles.label,
                fontWeight = if (detail != null) FontWeight.Bold else null,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = TextStyles.caption,
                    color = colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        // Clear of the text, and tinted as it is coloured: a grey ground under accent text read as
        // two controls in one
        Spacer(modifier = Modifier.width(Spacing.md))
        SecondaryButton(onClick = onAction, color = colors.accent) {
            Text(text = action, style = TextStyles.label)
        }
    }
}

private val ToastShape = SmoothCornerShape.Capsule

/** With a detail the toast is a few lines tall: rounded as the menus are, rather than a capsule. */
private val DetailedToastShape = SmoothCornerShape(Radius.lg)
private val ToastBlur = 8.dp
private val ToastRise = 12.dp
private const val ToastScale = 0.9f
