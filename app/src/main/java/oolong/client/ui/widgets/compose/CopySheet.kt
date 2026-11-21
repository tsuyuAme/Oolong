package oolong.client.ui.widgets.compose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import oolong.client.R
import oolong.client.model.PostContent
import oolong.client.model.TextSpan
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.Motion
import oolong.client.ui.common.theme.compose.Radius
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.common.theme.compose.Spacing
import oolong.client.ui.common.theme.compose.TextStyles
import oolong.client.ui.common.theme.compose.scrim
import oolong.client.ui.navigation.SearchKey
import oolong.client.ui.page.LocalNavigator
import oolong.client.utils.TiebaUtil

/**
 * The window's sheet for copying part of a post: [show] opens it over everything, with the
 * post's words as chips to pick. [CopySheetHost] shows it.
 */
@Stable
class CopySheets {
    internal var request by mutableStateOf<Request?>(null)

    fun show(content: List<PostContent>) {
        request = Request(content)
    }

    /** One opening of the sheet; a new one starts a fresh sheet even while another is up. */
    internal class Request(val content: List<PostContent>)
}

val LocalCopySheets = staticCompositionLocalOf<CopySheets?> { null }

private val Whitespace = Regex("\\s+")

@Composable
fun CopySheetHost(sheets: CopySheets) {
    val request = sheets.request ?: return
    key(request) {
        CopySheet(content = request.content, onDismiss = { if (sheets.request === request) sheets.request = null })
    }
}

/**
 * The pieces of [content] to pick from: its text cut into words, with marks that can be picked
 * too, links, mentions and emoticons each whole, and a line break between blocks. Images and
 * other media are left out.
 */
fun copyPieces(content: List<PostContent>): List<WordPiece> = buildList {
    content.filterIsInstance<PostContent.Text>().forEachIndexed { index, block ->
        if (index > 0) add(WordPiece("\n", isWord = false))
        block.spans.forEach { span ->
            when (span) {
                is TextSpan.Plain -> addAll(splitWords(span.text, punctuation = true))
                is TextSpan.Emoticon -> add(WordPiece(span.text, isWord = true, look = WordPiece.Look.Emoticon))
                is TextSpan.Link, is TextSpan.Mention, is TextSpan.VideoLink, is TextSpan.Topic ->
                    add(WordPiece(span.text, isWord = true, look = WordPiece.Look.Link))
            }
        }
    }
}

/**
 * Copying [content] or part of it: nothing is picked at first, which copies it all; picked words
 * are copied, or searched for in the app. The clipboard is written only by copying.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun CopySheet(content: List<PostContent>, onDismiss: () -> Unit) {
    val colors = ExtendedTheme.colors
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val state = rememberModalBottomSheetState(ModalBottomSheetValue.Hidden, animationSpec = Motion.settle(), skipHalfExpanded = true)
    LaunchedEffect(state) {
        // A drag or a tap on the scrim can cut the opening short, which ends show() with a
        // cancellation; the sheet is then hidden all the same and must still be dismissed
        launch { state.show() }
        // Swiped down, the scrim tapped, or closed: open first, then hidden and going nowhere
        snapshotFlow { state.currentValue == ModalBottomSheetValue.Hidden && state.targetValue == ModalBottomSheetValue.Hidden }
            .dropWhile { it }
            .first { it }
        onDismiss()
    }
    fun close(then: () -> Unit = {}) {
        scope.launch {
            then()
            state.hide()
        }
    }
    BackHandler { close() }

    val picker = remember(content) { WordPickerState(copyPieces(content)) }
    val scroll = rememberScrollState()
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.6f
    ModalBottomSheetLayout(
        sheetContent = {
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
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
                        text = stringResource(id = R.string.title_pick_text),
                        style = TextStyles.title,
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    val all = picker.allPicked
                    SecondaryButton(onClick = { picker.pickAll(!all) }, color = colors.accent) {
                        Text(
                            text = stringResource(id = if (all) R.string.button_deselect_all else R.string.button_select_all),
                            style = TextStyles.label,
                        )
                    }
                }
                WordPicker(
                    state = picker,
                    scroll = scroll,
                    modifier = Modifier
                        .padding(top = Spacing.sm)
                        .heightIn(max = maxHeight)
                        .verticalScroll(scroll),
                )
                val picked = picker.anyPicked
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.padding(top = Spacing.lg),
                ) {
                    SecondaryButton(
                        onClick = {
                            val keyword = picker.copied.replace(Whitespace, " ")
                            close { navigator.navigate(SearchKey(keyword)) }
                        },
                        enabled = picked,
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                    ) {
                        Text(text = stringResource(id = R.string.button_search), style = TextStyles.label)
                    }
                    Button(
                        onClick = {
                            val text = picker.copied
                            close { TiebaUtil.copyText(context, text) }
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 44.dp),
                    ) {
                        Text(text = stringResource(id = if (picked) R.string.menu_copy else R.string.btn_copy_all), style = TextStyles.label)
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
