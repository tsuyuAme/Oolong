package oolong.client.ui.page.forum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import oolong.client.R
import oolong.client.arch.LoadStatus
import oolong.client.arch.PagerState
import oolong.client.model.ForumTab
import oolong.client.model.ThreadSummary
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.PressFeedback
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.common.theme.compose.Symbols
import oolong.client.ui.common.theme.compose.TextStyles
import oolong.client.ui.common.windowsizeclass.LocalWindowSizeClass
import oolong.client.ui.common.windowsizeclass.WindowWidthSizeClass
import oolong.client.ui.page.main.explore.rememberThreadPeeker
import oolong.client.ui.widgets.compose.BlockTip
import oolong.client.ui.widgets.compose.BlockableContent
import oolong.client.ui.widgets.compose.FeedCard
import oolong.client.ui.widgets.compose.LoadMoreEffect
import oolong.client.ui.widgets.compose.MyLazyColumn
import oolong.client.ui.widgets.compose.VerticalDivider
import oolong.client.ui.widgets.compose.loadMoreFooter
import oolong.client.ui.widgets.compose.pageAccentOrPrimary

private enum class ItemType { Top, PlainText, SingleMedia, MultiMedia, Video }

/**
 * One tab's threads: those pinned to the top as single lines, then the rest. With [topsFolded],
 * only the first pinned thread shows, with how many there are, which unfolds them; unfolded, a row
 * after them folds them again. The good threads tab starts with the classes to filter them by.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ForumThreadList(
    tab: ForumTab,
    list: PagerState<ForumThreadItem>,
    onEvent: (ForumEvent) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    topsFolded: Boolean = false,
    onFoldTops: (Boolean) -> Unit = {},
) {
    val itemFraction = when (LocalWindowSizeClass.current.widthSizeClass) {
        WindowWidthSizeClass.Expanded -> 0.5f
        else -> 1f
    }
    val items = list.items
    val tops = remember(items) { items.filter { it.thread.isTop == true } }
    val threads = remember(items) { items.filter { it.thread.isTop != true } }
    // One line alone is not worth folding
    val foldable = tops.size >= 2
    val folded = foldable && topsFolded
    val peeker = rememberThreadPeeker()

    @Composable
    fun Item(item: ForumThreadItem, content: @Composable (ThreadSummary) -> Unit) {
        BlockableContent(
            block = item.block,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth(itemFraction)) { content(item.thread) }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LoadMoreEffect(listState, enabled = list.hasMore && list.append == LoadStatus.Idle, onLoadMore = { onEvent(ForumEvent.LoadMore) })
        MyLazyColumn(
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding,
        ) {
            items(items = if (folded) tops.take(1) else tops, key = { it.thread.id }, contentType = { ItemType.Top }) { item ->
                Item(item) { thread ->
                    TopThreadItem(
                        title = thread.title.takeUnless { it.isBlank() } ?: thread.abstract,
                        onClick = { onEvent(ForumEvent.OpenThread(thread)) },
                        modifier = Modifier.fillMaxWidth(),
                        unfold = if (folded) {
                            { UnfoldTopsButton(count = tops.size, onClick = { onFoldTops(false) }) }
                        } else {
                            null
                        },
                    )
                }
            }
            if (foldable && !folded) {
                item(key = "FoldTops", contentType = "FoldTops") {
                    FoldTopsItem(onClick = { onFoldTops(true) }, modifier = Modifier.fillMaxWidth(itemFraction))
                }
            }
            itemsIndexed(
                items = threads,
                key = { _, item -> item.thread.id },
                contentType = { _, item ->
                    val thread = item.thread
                    when {
                        thread.images.size == 1 -> ItemType.SingleMedia
                        thread.images.isNotEmpty() -> ItemType.MultiMedia
                        thread.video != null -> ItemType.Video
                        else -> ItemType.PlainText
                    }
                }
            ) { index, item ->
                Item(item) { thread ->
                    if (index > 0 || tops.isNotEmpty()) {
                        if (index == 0) Spacer(modifier = Modifier.height(8.dp))
                        VerticalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                    FeedCard(
                        thread = thread,
                        onClick = { onEvent(ForumEvent.OpenThread(it)) },
                        onClickReply = { onEvent(ForumEvent.OpenReplies(it)) },
                        onAgree = { onEvent(ForumEvent.Agree(tab, it.id)) },
                        onClickShared = { onEvent(ForumEvent.OpenOriginThread(it)) },
                        onClickUser = { onEvent(ForumEvent.OpenUser(it.id)) },
                        showForum = false,
                        onLongPress = peeker?.let { { thread, from, at, origin -> it.peek(thread, from, at, origin) } },
                    )
                }
            }
            loadMoreFooter(list.append, list.hasMore, onRetry = { onEvent(ForumEvent.LoadMore) })
        }
    }
}

/** How many threads are pinned, after the first while the rest are folded away, which unfolds them. */
@Composable
private fun UnfoldTopsButton(count: Int, onClick: () -> Unit) {
    val description = pluralStringResource(id = R.plurals.button_unfold_forum_tops, count = count, count)
    Row(
        modifier = Modifier
            .clickable(interactionSource = null, indication = PressFeedback.overlay(SmoothCornerShape.Capsule), onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(start = 8.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "$count",
            style = TextStyles.caption,
            color = ExtendedTheme.colors.textSecondary,
        )
        Icon(
            imageVector = Symbols.ExpandMore,
            contentDescription = null,
            tint = ExtendedTheme.colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** The row after the unfolded pinned threads that folds them. */
@Composable
private fun FoldTopsItem(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
    ) {
        Text(
            text = stringResource(id = R.string.button_fold_forum_tops),
            style = TextStyles.caption,
            color = ExtendedTheme.colors.textSecondary,
        )
        Icon(
            imageVector = Symbols.ExpandLess,
            contentDescription = null,
            tint = ExtendedTheme.colors.textSecondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** What kind a top row is, as a small tinted capsule. */
@Composable
private fun TypeChip(text: String, accent: Color) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = accent,
        modifier = Modifier
            .background(accent.copy(alpha = 0.14f), SmoothCornerShape.Capsule)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun TopThreadItem(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    unfold: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TypeChip(stringResource(id = R.string.content_top), pageAccentOrPrimary())
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp
        )
        unfold?.invoke()
    }
}
