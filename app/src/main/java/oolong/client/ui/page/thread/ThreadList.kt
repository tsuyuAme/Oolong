package oolong.client.ui.page.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.distinctUntilChanged
import oolong.client.R
import oolong.client.arch.LoadStatus
import oolong.client.model.Post
import oolong.client.model.PostContent
import oolong.client.model.knownFirstFloor
import oolong.client.models.LoadPicPageData
import oolong.client.models.PhotoViewData
import oolong.client.models.PicItem
import oolong.client.repository.ThreadSort
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.common.theme.compose.Symbols
import oolong.client.ui.widgets.compose.ChromeStyle
import oolong.client.ui.widgets.compose.Container
import oolong.client.ui.widgets.compose.EndPull
import oolong.client.ui.widgets.compose.ErrorScreen
import oolong.client.ui.widgets.compose.GlassTab
import oolong.client.ui.widgets.compose.GlassTabRow
import oolong.client.ui.widgets.compose.LoadMoreEffect
import oolong.client.ui.widgets.compose.LocalChromeStyle
import oolong.client.ui.widgets.compose.MyLazyColumn
import oolong.client.ui.widgets.compose.PullRefresh
import oolong.client.ui.widgets.compose.SegmentedControl
import oolong.client.ui.widgets.compose.SharedThreadCard
import oolong.client.ui.widgets.compose.VerticalDivider
import oolong.client.ui.widgets.compose.WavyLoader
import oolong.client.ui.widgets.compose.loadMoreFooter
import oolong.client.ui.widgets.compose.matchedGeometry
import oolong.client.ui.widgets.compose.matchedGeometryAnchor
import oolong.client.ui.widgets.compose.plus
import oolong.client.ui.widgets.compose.rememberScrollProgress
import oolong.client.ui.widgets.compose.states.StateScreenScope
import oolong.client.utils.ImageUtil

private const val FIRST_POST_KEY = "FirstPost"
private const val POST_KEY_PREFIX = "Post_"

/** The index of the first reply in the list, after the first floor and the reply count header. */
internal const val FIRST_REPLY_INDEX = 1

private const val THREAD_HEADER_KEY = "ThreadHeader"

/** The key the reply filter in the list and its copy docking in the bar are matched by. */
internal object ReplyFilter

/** The posts of the thread, or the first load or its error in their place. */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun ThreadContent(
    state: ThreadState,
    preferences: ThreadPreferences,
    listState: LazyListState,
    padding: PaddingValues,
    onEvent: (ThreadEvent) -> Unit,
    chromeEnd: () -> Int = { 0 },
) {
    val posts = state.posts
    val firstPost = state.firstPost ?: state.preview?.knownFirstFloor()
    val loadingFirstTime = posts.refresh == LoadStatus.Loading && posts.items.isEmpty()
    val error = (posts.refresh as? LoadStatus.Error)?.error

    if (firstPost == null && posts.items.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            when {
                error != null -> remember { StateScreenScope { onEvent(ThreadEvent.Retry) } }
                    .ErrorScreen(error = error, modifier = Modifier.fillMaxSize())

                loadingFirstTime -> WavyLoader()
            }
        }
        return
    }

    TrackReadPosition(listState, firstPost?.id, onEvent)

    val newestFirst = state.query.sort == ThreadSort.NewestFirst
    // The first load shows its own indicator in the list
    val pullRefreshing = !loadingFirstTime &&
        (if (newestFirst) posts.prepend == LoadStatus.Loading else posts.refresh == LoadStatus.Loading)
    val options = PostCardOptions(
        threadAuthorId = state.thread?.author?.id ?: firstPost?.author?.id ?: 0L,
        hideBlocked = preferences.hideBlocked,
        hideReply = preferences.hideReply,
    )
    val viewerId = state.viewer.user?.id
    val photoViewData = rememberPhotoViewData(state)

    // Until the first page of replies is in, the end of the list says how that load is going
    val firstLoad = posts.items.isEmpty() && (loadingFirstTime || error != null)
    LoadMoreEffect(listState, enabled = posts.hasMore && posts.append == LoadStatus.Idle, onLoadMore = { onEvent(ThreadEvent.LoadMore) })
    PullRefresh(
        refreshing = pullRefreshing,
        onRefresh = { onEvent(if (newestFirst) ThreadEvent.LoadPrevious else ThreadEvent.Refresh) },
        enabled = posts.hasPrevious || newestFirst,
        indicatorTop = padding.calculateTopPadding(),
        endPull = when {
            firstLoad || posts.hasMore -> EndPull.None
            newestFirst -> EndPull.Tease
            else -> EndPull.Action { onEvent(ThreadEvent.LoadLatest) }
        },
    ) {
        MyLazyColumn(
            state = listState,
            contentPadding = padding.plus(PaddingValues(bottom = 8.dp)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            item(key = FIRST_POST_KEY) {
                if (firstPost != null) {
                    FirstPost(state, firstPost, options, viewerId, photoViewData, onEvent)
                }
            }
            if (state.thread != null) {
                item(key = THREAD_HEADER_KEY) { ThreadHeader(state, onEvent) }
            }
            if (posts.hasPrevious && !newestFirst) {
                item(key = "LoadPrevious") { LoadPreviousButton(onEvent) }
            }
            itemsIndexed(
                items = posts.items,
                key = { _, it -> "$POST_KEY_PREFIX${it.post.id}" },
            ) { index, item ->
                Container {
                    ThreadFloor(
                        item = item,
                        options = options,
                        onEvent = onEvent,
                        listState = listState,
                        key = "$POST_KEY_PREFIX${item.post.id}",
                        stickBelow = chromeEnd,
                        startsRule = index == 0,
                        isStoreMark = item.post.id == state.thread?.storeMarkPostId,
                        // The thread's author may delete anyone's reply in it
                        canDelete = viewerId != null && (item.post.author.id == viewerId || options.threadAuthorId == viewerId),
                        photoViewData = { photoViewData(item.post, it) },
                        replies = state.floorReplies[item.post.id],
                        collapsible = true,
                    )
                }
            }
            loadMoreFooter(
                append = if (firstLoad) posts.refresh else posts.append,
                hasMore = posts.hasMore,
                pullToLoadLatest = !newestFirst && !firstLoad,
                onRetry = {
                    onEvent(
                        when {
                            firstLoad -> ThreadEvent.Retry
                            posts.hasMore -> ThreadEvent.LoadMore
                            else -> ThreadEvent.LoadLatest
                        }
                    )
                },
            )
        }
    }
}

@Composable
private fun FirstPost(
    state: ThreadState,
    post: Post,
    options: PostCardOptions,
    viewerId: Long?,
    photoViewData: (Post, PostContent.Image) -> PhotoViewData?,
    onEvent: (ThreadEvent) -> Unit,
) {
    Container {
        Column {
            ThreadFirstPost(
                post = post,
                state = state,
                options = options,
                isStoreMark = post.id == state.thread?.storeMarkPostId,
                canDelete = viewerId != null && post.author.id == viewerId,
                photoViewData = { photoViewData(post, it) },
                onEvent = onEvent,
            )
            state.thread?.shared?.let { shared ->
                SharedThreadCard(
                    shared = shared,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .clip(SmoothCornerShape(6.dp))
                        .background(ExtendedTheme.colors.inset)
                        .clickable {
                            onEvent(ThreadEvent.OpenOriginThread(shared.id, shared.forumId))
                        }
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * The reply count and the switch between all posts and the thread author's, above the replies.
 * The count is content and scrolls away. The switch is where the bar's copy of it ([ThreadFilter])
 * docks from as it scrolls up into the bar's row, crossfading from this flat one to the glass one.
 */
@Composable
internal fun ThreadHeader(
    state: ThreadState,
    onEvent: (ThreadEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val docking = rememberScrollProgress(ReplyFilter)
    val newestFirst = state.query.sort == ThreadSort.NewestFirst
    Container(modifier = modifier) {
        Row(
            modifier = Modifier
                .background(ExtendedTheme.colors.background)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 回复数在前，筛选紧随其后（与旧版顺序一致）
            Text(
                text = stringResource(R.string.title_thread_header, "${(state.thread?.replyCount ?: 1) - 1}"),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = ExtendedTheme.colors.text,
                maxLines = 1,
            )
            SegmentedControl(
                options = persistentListOf(stringResource(R.string.text_all), stringResource(R.string.title_see_lz)),
                selected = if (state.query.seeLz) 1 else 0,
                onSelect = { onEvent(ThreadEvent.SetSeeLz(it == 1)) },
                compact = true,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .matchedGeometryAnchor(ReplyFilter)
                    .graphicsLayer { alpha = 1f - docking.fraction(0f..FilterCrossfade) }
                    .width(IntrinsicSize.Max),
            )
            Spacer(modifier = Modifier.weight(1f))
            // 右侧：正序 / 倒序（与筛选同尺寸）
            SegmentedControl(
                options = persistentListOf(
                    stringResource(R.string.text_sort_asc),
                    stringResource(R.string.text_sort_desc),
                ),
                selected = if (newestFirst) 1 else 0,
                onSelect = { index ->
                    val wantNewest = index == 1
                    if (wantNewest != newestFirst) onEvent(ThreadEvent.ToggleSort)
                },
                compact = true,
                modifier = Modifier.width(IntrinsicSize.Max),
            )
        }
    }
}

@Composable
private fun LoadPreviousButton(onEvent: (ThreadEvent) -> Unit) {
    Container {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEvent(ThreadEvent.LoadPrevious) }
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Symbols.AlignVerticalTop,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(id = R.string.btn_load_previous),
                color = ExtendedTheme.colors.text,
                fontSize = 14.sp
            )
        }
    }
}

/** Where [postId] is in the list, or null when it isn't loaded. */
internal fun ThreadState.indexOfPost(postId: Long): Int? {
    if (postId == firstPost?.id) return 0
    val index = posts.items.indexOfFirst { it.post.id == postId }
    if (index == -1) return null
    val loadPrevious = if (posts.hasPrevious && query.sort != ThreadSort.NewestFirst) 1 else 0
    return FIRST_REPLY_INDEX + 1 + loadPrevious + index
}

/** Reports the last post on screen as the list scrolls, or the first floor when no reply is. */
@Composable
private fun TrackReadPosition(listState: LazyListState, firstPostId: Long?, onEvent: (ThreadEvent) -> Unit) {
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentFirstPostId by rememberUpdatedState(firstPostId)
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo
                .lastOrNull { (it.key as? String)?.startsWith(POST_KEY_PREFIX) == true }
                ?.let { (it.key as String).removePrefix(POST_KEY_PREFIX).toLongOrNull() }
                ?: currentFirstPostId?.takeIf { it != 0L }
        }
            .distinctUntilChanged()
            .collect { postId -> if (postId != null) currentOnEvent(ThreadEvent.ReadUpTo(postId)) }
    }
}

/** What the image viewer needs to open an image of a post and page through the thread's images. */
@Composable
private fun rememberPhotoViewData(state: ThreadState): (Post, PostContent.Image) -> PhotoViewData? {
    val context = LocalContext.current
    val thread = state.thread
    val seeLz = state.query.seeLz
    return remember(thread?.id, thread?.forum, seeLz) {
        { post, image ->
            thread?.let {
                PhotoViewData(
                    data = LoadPicPageData(
                        forumId = it.forum.id,
                        forumName = it.forum.name,
                        threadId = it.id,
                        postId = post.id,
                        objType = "pb",
                        picId = image.id,
                        picIndex = 1,
                        seeLz = seeLz,
                        originUrl = image.originUrl,
                    ),
                    picItems = persistentListOf(
                        PicItem(
                            picId = image.id,
                            picIndex = 1,
                            url = ImageUtil.getUrl(context, true, image.originUrl, *image.urls.toTypedArray()),
                            originUrl = image.originUrl,
                            showOriginBtn = image.showOriginButton,
                            originSize = image.originSize,
                            postId = post.id,
                        )
                    ),
                )
            }
        }
    }
}

/** The switch between all posts and the thread author's, docked in the top bar on a glass capsule. */
@Composable
internal fun ThreadFilter(seeLz: Boolean, onEvent: (ThreadEvent) -> Unit) {
    val selected = if (seeLz) 1 else 0
    val docking = rememberScrollProgress(ReplyFilter)
    if (LocalChromeStyle.current == ChromeStyle.Classic) {
        // On a classic bar it stays the switch it is in the list, rather than becoming tabs
        SegmentedControl(
            options = persistentListOf(stringResource(R.string.text_all), stringResource(R.string.title_see_lz)),
            selected = selected,
            onSelect = { onEvent(ThreadEvent.SetSeeLz(it == 1)) },
            compact = true,
            modifier = Modifier
                .matchedGeometry(ReplyFilter, docking)
                .graphicsLayer { alpha = docking.fraction(0f..FilterCrossfade) }
                .width(IntrinsicSize.Max),
        )
        return
    }
    GlassTabRow(
        selectedTabIndex = selected,
        onSelect = { onEvent(ThreadEvent.SetSeeLz(it == 1)) },
        modifier = Modifier
            .matchedGeometry(ReplyFilter, docking)
            .graphicsLayer { alpha = docking.fraction(0f..FilterCrossfade) },
    ) {
        listOf(R.string.text_all, R.string.title_see_lz).forEachIndexed { index, label ->
            GlassTab(
                selected = index == selected,
                onClick = { if (index != selected) onEvent(ThreadEvent.SetSeeLz(index == 1)) },
                selectedContentColor = ExtendedTheme.colors.text,
                unselectedContentColor = ExtendedTheme.colors.textSecondary,
            ) {
                Text(
                    text = stringResource(label),
                    modifier = Modifier.padding(horizontal = 10.dp),
                    fontSize = 12.sp,
                    fontWeight = if (index == selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

/** How much of its docking the reply filter spends turning from the list's flat switch into glass. */
private const val FilterCrossfade = 0.3f
