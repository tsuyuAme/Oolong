package oolong.client.ui.page.settings.block

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import oolong.client.R
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.Motion
import oolong.client.ui.common.theme.compose.Radius
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.common.theme.compose.Spacing
import oolong.client.ui.common.theme.compose.TextStyles
import oolong.client.ui.common.theme.compose.scrim
import oolong.client.ui.widgets.compose.Button
import oolong.client.ui.widgets.compose.SecondaryButton
import oolong.client.ui.widgets.compose.WordPicker
import oolong.client.ui.widgets.compose.rememberWordPickerState

/**
 * Picking keywords to block out of [text], in a sheet over everything: its words as chips, or
 * typed in, which is where it starts when there is no text. [onBlock] gets the words, all of
 * which content must have.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun KeywordSheet(text: String, onBlock: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val colors = ExtendedTheme.colors
    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden, animationSpec = Motion.settle(), skipHalfExpanded = true)
    LaunchedEffect(state) {
        state.show()
        // Swiped down or the scrim tapped
        snapshotFlow { state.currentValue }.first { it == ModalBottomSheetValue.Hidden }
        onDismiss()
    }
    fun close(then: () -> Unit = {}) {
        scope.launch {
            then()
            state.hide()
        }
    }
    BackHandler { close() }

    val picker = rememberWordPickerState(text)
    var typing by remember { mutableStateOf(text.isBlank()) }
    var typed by remember { mutableStateOf("") }
    val words = if (typing) typed.split(' ', '　').filter { it.isNotBlank() } else picker.words
    ModalBottomSheetLayout(
        sheetContent = {
            Column(
                modifier = Modifier
                    // Above the keyboard while typing
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                    .padding(horizontal = Spacing.lg)
                    .padding(bottom = Spacing.lg),
            ) {
                // The handle says the sheet can be dragged down to close
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp, bottom = Spacing.sm)
                        .size(width = 36.dp, height = 5.dp)
                        .background(colors.text.copy(alpha = 0.18f), SmoothCornerShape.Capsule),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(id = R.string.title_block_keywords),
                        style = TextStyles.title,
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    if (text.isNotBlank()) {
                        SecondaryButton(
                            onClick = {
                                if (!typing) typed = picker.words.joinToString(" ")
                                typing = !typing
                            },
                            color = colors.accent,
                        ) {
                            Text(
                                text = stringResource(id = if (typing) R.string.keywords_pick else R.string.keywords_type),
                                style = TextStyles.label,
                            )
                        }
                    }
                }
                // Typing grows out of picking and back, the sheet settling to its new height
                AnimatedContent(
                    targetState = typing,
                    transitionSpec = {
                        (fadeIn(Motion.fadeIn()) + scaleIn(Motion.settle(), initialScale = 0.96f)) togetherWith
                            fadeOut(Motion.fadeOut()) using SizeTransform(clip = false) { _, _ -> Motion.settle() }
                    },
                    label = "keywords",
                    modifier = Modifier.padding(top = Spacing.sm),
                ) { typingNow ->
                    if (typingNow) {
                        KeywordInput(typed) { typed = it }
                    } else {
                        WordPicker(
                            state = picker,
                            modifier = Modifier
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.padding(top = Spacing.lg),
                ) {
                    SecondaryButton(onClick = { close() }, modifier = Modifier.weight(1f).heightIn(min = 44.dp)) {
                        Text(text = stringResource(id = R.string.button_cancel), style = TextStyles.label)
                    }
                    Button(
                        onClick = { close { onBlock(words) } },
                        enabled = words.isNotEmpty(),
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                    ) {
                        Text(text = stringResource(id = R.string.button_block), style = TextStyles.label)
                    }
                }
            }
        },
        sheetState = state,
        sheetShape = SmoothCornerShape(Radius.xl, bottom = false),
        sheetBackgroundColor = colors.sheet,
        scrimColor = colors.scrim,
        content = {},
    )
}

@Composable
private fun KeywordInput(value: String, onChange: (String) -> Unit) {
    val colors = ExtendedTheme.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 46.dp)
            .background(colors.inset, Corners.sm)
            .padding(horizontal = Spacing.sm, vertical = 12.dp),
    ) {
        if (value.isEmpty()) Text(text = stringResource(id = R.string.rule_hint_keyword), style = TextStyles.body, color = colors.textSecondary)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyles.body.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
    }
}
