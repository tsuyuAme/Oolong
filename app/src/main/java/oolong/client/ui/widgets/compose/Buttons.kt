package oolong.client.ui.widgets.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.Dimens
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.PressFeedback
import oolong.client.ui.common.theme.compose.TextStyles
import oolong.client.ui.common.theme.compose.pressScale

/**
 * A button (design-system.md §9.1): a capsule at least [Dimens.ButtonHeight] tall. This is the
 * primary kind, filled with the emphasis colour, which is the theme colour or, in the plain
 * colour style, black; [SecondaryButton] and [TextButton] are the other two. [minHeight] is
 * lowered only for buttons set in running content, such as a floor's like count.
 */
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    minHeight: Dp = Dimens.ButtonHeight,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
    shape: Shape = Corners.full,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        backgroundColor = ExtendedTheme.colors.emphasis,
        contentColor = ExtendedTheme.colors.onEmphasis
    ),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    val contentColor by colors.contentColor(enabled)
    // The press veil takes the content colour, so a filled button lightens rather than darkens
    CompositionLocalProvider(LocalContentColor provides contentColor.copy(alpha = 1f)) {
        Surface(
            // The caller's modifier first, so its padding lies outside the shape
            modifier = modifier
                .heightIn(min = minHeight)
                .pressScale(interactionSource),
            shape = shape,
            color = colors.backgroundColor(enabled).value,
            contentColor = contentColor.copy(alpha = 1f),
            border = border,
            elevation = elevation?.elevation(enabled, interactionSource)?.value ?: 0.dp,
        ) {
            CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
                ProvideTextStyle(value = TextStyles.label) {
                    // Surface passes its minimum height on, so the row centres its content in it.
                    // Clickable in here, over the Surface's bounds, its press veil drawn in shape.
                    Row(
                        Modifier
                            .clickable(
                                onClick = onClick,
                                enabled = enabled,
                                interactionSource = interactionSource,
                                indication = PressFeedback.overlay(shape)
                            )
                            .padding(contentPadding),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        content = content
                    )
                }
            }
        }
    }
}

/** The secondary kind of [Button]: tinted with a tenth of [color] instead of filled. */
@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = Corners.full,
    border: BorderStroke? = null,
    color: Color = ExtendedTheme.colors.text,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = shape,
        border = border,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = color.copy(alpha = 0.1f),
            contentColor = color,
            disabledBackgroundColor = color.copy(alpha = ContentAlpha.disabled * 0.1f),
            disabledContentColor = color.copy(alpha = ContentAlpha.disabled)
        ),
        contentPadding = contentPadding,
        content = content
    )
}

/** The text kind of [Button]: only its label, in [color], with the press veil behind it. */
@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    color: Color = ExtendedTheme.colors.accent,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = Color.Transparent,
            contentColor = color,
            disabledBackgroundColor = Color.Transparent,
            disabledContentColor = color.copy(alpha = ContentAlpha.disabled)
        ),
        contentPadding = contentPadding,
        content = content
    )
}
