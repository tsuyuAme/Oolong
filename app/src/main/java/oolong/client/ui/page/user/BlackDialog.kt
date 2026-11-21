package oolong.client.ui.page.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import oolong.client.R
import oolong.client.repository.BlackSettings
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.PressFeedback
import oolong.client.ui.common.theme.compose.TextStyles
import oolong.client.ui.widgets.compose.CheckboxRowInset
import oolong.client.ui.widgets.compose.Dialog
import oolong.client.ui.widgets.compose.DialogNegativeButton
import oolong.client.ui.widgets.compose.DialogPositiveButton
import oolong.client.ui.widgets.compose.rememberDialogState

/**
 * What to bar the user called [name] from, each on its own. Shows a spinner until [settings]
 * arrive, then starts from them.
 */
@Composable
fun BlackDialog(
    name: String,
    settings: BlackSettings?,
    onSave: (BlackSettings) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDialogState()
    LaunchedEffect(Unit) {
        state.show()
        snapshotFlow { state.show }.filter { !it }.first()
        onDismiss()
    }
    var chosen by remember(settings) { mutableStateOf(settings) }
    Dialog(
        dialogState = state,
        title = { Text(text = stringResource(R.string.title_black_user, name)) },
        buttons = {
            val current = chosen
            if (current != null) DialogPositiveButton(text = stringResource(R.string.button_save), onClick = { onSave(current) })
            DialogNegativeButton(text = stringResource(R.string.button_cancel))
        },
    ) {
        val current = chosen
        if (current == null) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().heightIn(min = 144.dp)) {
                CircularProgressIndicator(color = ExtendedTheme.colors.accent, modifier = Modifier.size(28.dp))
            }
        } else {
            // The boxes, past the checkbox's own margin, in line with the title
            Column(modifier = Modifier.padding(horizontal = CheckboxRowInset)) {
                BarRow(stringResource(R.string.black_follow), current.follow) { chosen = current.copy(follow = it) }
                BarRow(stringResource(R.string.black_interact), current.interact) { chosen = current.copy(interact = it) }
                BarRow(stringResource(R.string.black_chat), current.chat) { chosen = current.copy(chat = it) }
            }
        }
    }
}

@Composable
private fun BarRow(text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = ExtendedTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(interactionSource = null, indication = PressFeedback.overlay(Corners.sm)) { onChange(!checked) },
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onChange,
            colors = CheckboxDefaults.colors(checkedColor = colors.emphasis, uncheckedColor = colors.textSecondary),
        )
        Text(text = text, style = TextStyles.bodyCompact, color = colors.text)
    }
}
