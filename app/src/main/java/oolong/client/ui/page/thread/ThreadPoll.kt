package oolong.client.ui.page.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import oolong.client.R
import oolong.client.model.Poll
import oolong.client.model.PollOption
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.PressFeedback
import oolong.client.ui.common.theme.compose.Spacing
import oolong.client.ui.common.theme.compose.Symbols
import oolong.client.ui.common.theme.compose.TextStyles
import oolong.client.ui.widgets.compose.Button
import kotlin.math.roundToInt

/**
 * A thread's poll. Until the viewer has voted, the options are chosen and sent with a button;
 * after that, or once the poll is closed or for a guest, each option shows its share of the votes,
 * filled behind it, with the viewer's choice ticked.
 */
@Composable
fun PollCard(
    poll: Poll,
    canVote: Boolean,
    onVote: (Set<Int>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ExtendedTheme.colors
    val voting = canVote && !poll.hasVoted && !poll.closed
    var chosen by rememberSaveable(poll.title) { mutableStateOf(emptySet<Int>()) }
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = modifier
            .background(colors.inset, Corners.md)
            .padding(Spacing.md),
    ) {
        if (poll.title.isNotBlank()) Text(text = poll.title, style = TextStyles.name, color = colors.text)
        Text(
            text = listOfNotNull(
                stringResource(if (poll.multiple) R.string.poll_multiple else R.string.poll_single),
                pluralStringResource(R.plurals.poll_voters, poll.voters.toInt(), poll.voters),
                stringResource(R.string.poll_closed).takeIf { poll.closed },
            ).joinToString(" · "),
            style = TextStyles.caption,
            color = colors.textSecondary,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            modifier = Modifier.padding(top = Spacing.xxs),
        ) {
            poll.options.forEach { option ->
                if (voting) {
                    ChoiceRow(
                        option = option,
                        selected = option.id in chosen,
                        onClick = {
                            chosen = when {
                                option.id in chosen -> chosen - option.id
                                poll.multiple -> chosen + option.id
                                else -> setOf(option.id)
                            }
                        },
                    )
                } else {
                    ResultRow(option = option, total = poll.votes, chosen = option.id in poll.chosen)
                }
            }
        }
        if (voting) {
            Button(
                onClick = { onVote(chosen) },
                enabled = chosen.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xxs)
                    .heightIn(min = 40.dp),
            ) {
                Text(text = stringResource(R.string.button_vote), style = TextStyles.label)
            }
        }
    }
}

@Composable
private fun ChoiceRow(option: PollOption, selected: Boolean, onClick: () -> Unit) {
    val colors = ExtendedTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(if (selected) colors.accent.copy(alpha = 0.12f) else colors.card, Corners.sm)
            .then(if (selected) Modifier.border(1.dp, colors.accent, Corners.sm) else Modifier)
            .clickable(interactionSource = null, indication = PressFeedback.overlay(Corners.sm), onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    ) {
        Text(
            text = option.text,
            style = TextStyles.bodyCompact,
            color = if (selected) colors.accent else colors.text,
            modifier = Modifier.weight(1f),
        )
        if (selected) Icon(Symbols.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ResultRow(option: PollOption, total: Long, chosen: Boolean) {
    val colors = ExtendedTheme.colors
    val share = if (total > 0) option.votes.toFloat() / total else 0f
    val fill = if (chosen) colors.accent.copy(alpha = 0.22f) else colors.textSecondary.copy(alpha = 0.12f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .background(colors.card, Corners.sm)
            .drawBehind {
                val outline = Corners.sm.createOutline(size, layoutDirection, this)
                clipRect(right = size.width * share) { drawOutline(outline, fill) }
            }
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    ) {
        Text(
            text = option.text,
            style = TextStyles.bodyCompact,
            color = if (chosen) colors.accent else colors.text,
            modifier = Modifier.weight(1f),
        )
        if (chosen) Icon(Symbols.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
        Text(
            text = "${(share * 100).roundToInt()}%",
            style = TextStyles.label,
            color = if (chosen) colors.accent else colors.textSecondary,
        )
    }
}
