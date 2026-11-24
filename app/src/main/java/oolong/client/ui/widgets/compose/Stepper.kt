package oolong.client.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.PressFeedback
import oolong.client.ui.common.theme.compose.Symbols
import oolong.client.ui.common.theme.compose.TextStyles
import oolong.client.ui.common.theme.compose.pressScale

/**
 * Setting a number one step at a time within [range]: − and + on the page's own colour on either
 * side of [text], the value as shown. [down] and [up] say what the buttons do, for accessibility.
 */
@Composable
fun Stepper(
    value: Int,
    range: IntRange,
    text: String,
    down: String,
    up: String,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        StepButton(Symbols.Remove, down, enabled = value > range.first) { onChange(value - 1) }
        Text(
            text = text,
            style = TextStyles.bodyCompact.copy(fontFeatureSettings = "tnum"),
            color = ExtendedTheme.colors.text,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(52.dp),
        )
        StepButton(Symbols.Add, up, enabled = value < range.last) { onChange(value + 1) }
    }
}

@Composable
private fun StepButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = ExtendedTheme.colors
    val press = remember { MutableInteractionSource() }
    Icon(
        icon,
        contentDescription = label,
        tint = if (enabled) colors.text else colors.textDisabled,
        modifier = Modifier
            .pressScale(press)
            .size(32.dp)
            .background(colors.background, Corners.full)
            .clickable(interactionSource = press, indication = PressFeedback.overlay(Corners.full), enabled = enabled, onClick = onClick)
            .padding(7.dp),
    )
}
