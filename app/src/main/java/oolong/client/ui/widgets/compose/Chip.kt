package oolong.client.ui.widgets.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.Motion
import oolong.client.ui.common.theme.compose.PressFeedback
import oolong.client.ui.common.theme.compose.pressScale

@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    prefixIcon: (@Composable () -> Unit)? = null,
    appendIcon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    invertColor: Boolean = false,
    shape: Shape = Corners.full,
) {
    val color =
        if (invertColor) ExtendedTheme.colors.onInvertChip else ExtendedTheme.colors.onChip
    val backgroundColor =
        if (invertColor) ExtendedTheme.colors.invertChip else ExtendedTheme.colors.chip

    val interactionSource = remember { MutableInteractionSource() }
    val animatedColor by animateColorAsState(targetValue = color, animationSpec = Motion.snap(), label = "ChipColor")
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = Motion.snap(),
        label = "ChipBackgroundColor"
    )

    ProvideContentColor(color = animatedColor) {
        Row(
            modifier = modifier
                // Pressed, a tappable chip draws its edges in, as buttons do
                .then(if (onClick != null) Modifier.pressScale(interactionSource) else Modifier)
                .background(color = animatedBackgroundColor, shape = shape)
                .clickable(
                    enabled = onClick != null,
                    interactionSource = interactionSource,
                    indication = PressFeedback.overlay(shape),
                ) {
                    onClick?.invoke()
                }
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            prefixIcon?.let {
                Box(modifier = Modifier.size(16.dp)) {
                    it()
                }
            }
            Text(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                text = text
            )
            appendIcon?.let {
                Box(modifier = Modifier.size(16.dp)) {
                    it()
                }
            }
        }
    }
}
