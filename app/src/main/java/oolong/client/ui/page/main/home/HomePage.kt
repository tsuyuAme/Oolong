package oolong.client.ui.page.main.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import oolong.client.R
import oolong.client.arch.GlobalEvent
import oolong.client.arch.LoadStatus
import oolong.client.arch.onGlobalEvent
import oolong.client.dataStore
import oolong.client.model.FollowedForum
import oolong.client.model.VisitedForum
import oolong.client.ui.common.navigation.pageOrigin
import oolong.client.ui.common.navigation.rememberPageOrigin
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.LocalReduceMotion
import oolong.client.ui.common.theme.compose.LocalTextScale
import oolong.client.ui.common.theme.compose.Motion
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.common.theme.compose.Symbols
import oolong.client.ui.common.theme.compose.TextScale
import oolong.client.ui.navigation.ForumKey
import oolong.client.ui.navigation.LoginKey
import oolong.client.ui.navigation.SearchKey
import oolong.client.ui.page.LocalNavigator
import oolong.client.ui.widgets.compose.ActionItem
import oolong.client.ui.widgets.compose.Avatar
import oolong.client.ui.widgets.compose.Button
import oolong.client.ui.widgets.compose.Chip
import oolong.client.ui.widgets.compose.ConfirmDialog
import oolong.client.ui.widgets.compose.ContextMenuItem
import oolong.client.ui.widgets.compose.ErrorScreen
import oolong.client.ui.widgets.compose.GlassScaffold
import oolong.client.ui.widgets.compose.GlassTopBar
import oolong.client.ui.widgets.compose.GlassTopBarTitle
import oolong.client.ui.widgets.compose.MyLazyVerticalGrid
import oolong.client.ui.widgets.compose.PullRefresh
import oolong.client.ui.widgets.compose.SecondaryButton
import oolong.client.ui.widgets.compose.TipScreen
import oolong.client.ui.widgets.compose.longPressMenu
import oolong.client.ui.widgets.compose.placeholder.placeholder
import oolong.client.ui.widgets.compose.plus
import oolong.client.ui.widgets.compose.rememberDialogState
import oolong.client.ui.widgets.compose.states.StateScreen
import oolong.client.utils.ImageUtil
import oolong.client.utils.TiebaUtil
import oolong.client.utils.appPreferences

@Preview("SearchBoxPreview")
@Composable
fun SearchBoxPreview() {
    SearchBox(
        backgroundColor = Color(0xFFF8F8F8),
        contentColor = Color(0xFFBFBFBF),
        onClick = {}
    )
}

@Composable
fun SearchBox(
    modifier: Modifier = Modifier,
    backgroundColor: Color = ExtendedTheme.colors.searchField,
    contentColor: Color = ExtendedTheme.colors.onSearchField,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .background(ExtendedTheme.colors.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Surface(
            color = backgroundColor,
            contentColor = contentColor,
            shape = Corners.md,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
        ) {
            Row(
                verticalAlignment = CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Symbols.Search,
                    contentDescription = null,
                    modifier = Modifier
                        .align(CenterVertically)
                        .size(24.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = stringResource(id = R.string.hint_search),
                    modifier = Modifier.align(CenterVertically),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
internal fun Header(
    text: String,
    modifier: Modifier = Modifier,
    invert: Boolean = false
) {
    Chip(
        text = text,
        modifier = Modifier
            .padding(start = 16.dp)
            .then(modifier),
        invertColor = invert
    )
}

@Composable
private fun ForumItemPlaceholder(
    showAvatar: Boolean,
) {
    if (!showAvatar) {
        CompactForumItemPlaceholder()
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        run {
            Image(
                painter = rememberDrawablePainter(
                    drawable = ImageUtil.getPlaceHolder(
                        LocalContext.current,
                        0
                    )
                ),
                contentDescription = null,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(40.dp)
                    .align(CenterVertically)
                    .placeholder(visible = true, color = ExtendedTheme.colors.chip, shape = RectangleShape),
            )
            Spacer(modifier = Modifier.width(14.dp))
        }
        Text(
            color = ExtendedTheme.colors.text,
            text = "",
            modifier = Modifier
                .weight(1f)
                .align(CenterVertically)
                .placeholder(visible = true, color = ExtendedTheme.colors.chip, shape = RectangleShape),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(54.dp)
                .background(
                    color = ExtendedTheme.colors.chip,
                    shape = Corners.xs
                )
                .padding(vertical = 4.dp)
                .align(CenterVertically)
                .placeholder(visible = true, color = ExtendedTheme.colors.chip, shape = RectangleShape)
        ) {
            Text(
                text = "0",
                color = ExtendedTheme.colors.onChip,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Center)
            )
        }
    }
}

@Composable
private fun CompactForumItemPlaceholder() {
    val placeholder = Modifier.placeholder(visible = true, color = ExtendedTheme.colors.chip, shape = SmoothCornerShape.Capsule)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = CenterVertically,
    ) {
        Box(modifier = Modifier.size(CompactAvatarSize).then(placeholder))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "",
            modifier = Modifier
                .weight(1f)
                .placeholder(visible = true, color = ExtendedTheme.colors.chip, shape = RectangleShape),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(modifier = Modifier.width(CompactLevelWidth).height(18.dp).then(placeholder))
    }
}

@Composable
private fun ForumItemContent(
    item: FollowedForum,
    showAvatar: Boolean
) {
    if (!showAvatar) {
        CompactForumItemContent(item)
        return
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Avatar(data = item.avatar, size = 40.dp, contentDescription = null)
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            color = ExtendedTheme.colors.text,
            text = item.name,
            modifier = Modifier
                .weight(1f)
                .align(CenterVertically),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(54.dp)
                .background(
                    color = ExtendedTheme.colors.chip,
                    shape = Corners.xs
                )
                .padding(vertical = 4.dp)
                .align(CenterVertically)
        ) {
            Row(
                modifier = Modifier.align(Center),
            ) {
                Text(
                    text = "Lv.${item.level}",
                    color = ExtendedTheme.colors.onChip,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(CenterVertically)
                )
                if (item.signedIn) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Symbols.Check,
                        contentDescription = stringResource(id = R.string.tip_signed),
                        modifier = Modifier
                            .size(12.dp)
                            .align(CenterVertically),
                        tint = ExtendedTheme.colors.onChip
                    )
                }
            }
        }
    }
}

/**
 * A forum in the two-column list: a small avatar, the name, and its level with the signed-in mark
 * in a pill of fixed width, so the pills line up down each column whether signed in or not.
 */
@Composable
private fun CompactForumItemContent(item: FollowedForum) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = CenterVertically,
    ) {
        Avatar(data = item.avatar, size = CompactAvatarSize, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            color = ExtendedTheme.colors.text,
            text = item.name,
            modifier = Modifier.weight(1f),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(6.dp))
        Row(
            modifier = Modifier
                .width(CompactLevelWidth)
                .height(18.dp)
                .background(color = ExtendedTheme.colors.chip, shape = SmoothCornerShape.Capsule),
            verticalAlignment = CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "${item.level}",
                color = ExtendedTheme.colors.onChip,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
            if (item.signedIn) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Symbols.Check,
                    contentDescription = stringResource(id = R.string.tip_signed),
                    modifier = Modifier.size(11.dp),
                    tint = ExtendedTheme.colors.onChip
                )
            }
        }
    }
}

private val CompactAvatarSize = 24.dp
private val CompactLevelWidth = 38.dp

@Composable
private fun ForumItem(
    item: FollowedForum,
    showAvatar: Boolean,
    onClick: (FollowedForum) -> Unit,
    onUnfollow: (FollowedForum) -> Unit,
    onAddTopForum: (FollowedForum) -> Unit,
    onDeleteTopForum: (FollowedForum) -> Unit,
    isTopForum: Boolean = false,
) {
    val context = LocalContext.current
    val pin = stringResource(id = if (isTopForum) R.string.menu_top_del else R.string.menu_top)
    val copyName = stringResource(id = R.string.title_copy_forum_name)
    val unfollow = stringResource(id = R.string.button_unfollow)
    // The forum's page opens out of its item, and closes back into it
    val origin = rememberPageOrigin()
    Box(
        modifier = Modifier.pageOrigin(origin).longPressMenu(
            items = {
                listOf(
                    ContextMenuItem(if (isTopForum) Symbols.KeepFill else Symbols.Keep, pin) {
                        if (isTopForum) onDeleteTopForum(item) else onAddTopForum(item)
                    },
                    ContextMenuItem(Symbols.ContentCopy, copyName) { TiebaUtil.copyText(context, item.name) },
                    ContextMenuItem(Symbols.HeartBroken, unfollow, startsGroup = true) { onUnfollow(item) },
                )
            },
            onClick = {
                origin.leave()
                onClick(item)
            },
        ),
    ) {
        ForumItemContent(item = item, showAvatar = showAvatar)
    }
}

/** What the user does on the home page. */
sealed interface HomeEvent {
    data object Refresh : HomeEvent
    data object Search : HomeEvent
    data object OneKeySign : HomeEvent
    data class OpenForum(val name: String) : HomeEvent
    data class Pin(val forum: FollowedForum) : HomeEvent
    data class Unpin(val forum: FollowedForum) : HomeEvent

    /** Asks to unfollow, which the page confirms first. */
    data class Unfollow(val forum: FollowedForum) : HomeEvent
}

@Composable
fun HomePage(
    canOpenExplore: Boolean = false,
    showSearchBox: Boolean = true,
    searchField: (@Composable () -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenExplore: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val navigator = LocalNavigator.current

    onGlobalEvent<GlobalEvent.Refresh>(filter = { it.key == "home" }) { viewModel.refresh() }
    // Back from elsewhere, the followed forums load again if they have gone stale
    LifecycleResumeEffect(viewModel) {
        viewModel.freshen()
        onPauseOrDispose {}
    }
    val guideShown by remember { context.dataStore.data.map { it[RecentForumsGuideKey] ?: false } }.collectAsState(initial = true)

    var unfollowForum by remember { mutableStateOf<FollowedForum?>(null) }
    val confirmUnfollowDialog = rememberDialogState()
    ConfirmDialog(
        dialogState = confirmUnfollowDialog,
        onConfirm = { unfollowForum?.let(viewModel::unfollow) },
    ) {
        Text(text = stringResource(id = R.string.title_dialog_unfollow_forum, unfollowForum?.name.orEmpty()))
    }

    // Followed from the store, so changes made in the settings show on returning here
    val listSingle by remember { context.dataStore.data.map { it[booleanPreferencesKey("listSingle")] ?: false } }
        .collectAsState(initial = context.appPreferences.listSingle)
    val showHistory by remember { context.dataStore.data.map { it[booleanPreferencesKey("homePageShowHistoryForum")] ?: true } }
        .collectAsState(initial = context.appPreferences.homePageShowHistoryForum)
    HomeScreen(
        state = state,
        listSingle = listSingle,
        showHistory = showHistory,
        canOpenExplore = canOpenExplore,
        showSearchBox = showSearchBox,
        searchField = searchField,
        onOpenExplore = onOpenExplore,
        onToggleListSingle = { context.appPreferences.listSingle = !listSingle },
        showRecentForumsGuide = !guideShown,
        onRecentForumsGuideShown = { MainScope().launch { context.dataStore.edit { it[RecentForumsGuideKey] = true } } },
        onEvent = { event ->
            when (event) {
                HomeEvent.Refresh -> viewModel.refresh()
                HomeEvent.Search -> navigator.navigate(SearchKey())
                HomeEvent.OneKeySign -> TiebaUtil.startSign(context)
                is HomeEvent.OpenForum -> navigator.navigate(ForumKey(event.name))
                is HomeEvent.Pin -> viewModel.pin(event.forum)
                is HomeEvent.Unpin -> viewModel.unpin(event.forum)
                is HomeEvent.Unfollow -> {
                    unfollowForum = event.forum
                    confirmUnfollowDialog.show()
                }
            }
        },
    )
}

/** The followed forums, the pinned ones first, under the search box and the visited forums. */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
    listSingle: Boolean = false,
    showHistory: Boolean = true,
    canOpenExplore: Boolean = false,
    showSearchBox: Boolean = true,
    searchField: (@Composable () -> Unit)? = null,
    onOpenExplore: () -> Unit = {},
    onToggleListSingle: () -> Unit = {},
    showRecentForumsGuide: Boolean = false,
    onRecentForumsGuideShown: () -> Unit = {},
) {
    // Forum names don't fit two to a row once text is larger than the bars allow
    val single = listSingle || LocalTextScale.current > TextScale.ChromeMax
    val gridCells = if (single) GridCells.Fixed(1) else ForumGridCells
    val forums = state.forums
    val pinned = state.pinned
    val showHistoryForum = showHistory && state.visited.isNotEmpty()
    val isLoading = state.load == LoadStatus.Loading
    val error = (state.load as? LoadStatus.Error)?.error
    val gridState = rememberLazyGridState()
    val expansion = rememberRecentForumsExpansion()
    expansion.inView = { gridState.layoutInfo.visibleItemsInfo.any { it.key == RecentForumsKey } }
    // Once there is a second row to pull out, show once that it is there
    val density = LocalDensity.current.density
    val reduceMotion = LocalReduceMotion.current
    LaunchedEffect(showRecentForumsGuide, expansion.canExpand, isLoading) {
        if (!showRecentForumsGuide || !expansion.canExpand || isLoading) return@LaunchedEffect
        if (gridState.firstVisibleItemIndex == 0 && !reduceMotion) {
            kotlinx.coroutines.delay(GuideDelay)
            expansion.peek(GuidePeek, density)
        }
        onRecentForumsGuideShown()
    }

    GlassScaffold(
        largeTitle = true,
        backgroundColor = Color.Transparent,
        topBar = {
            GlassTopBar(
                title = { GlassTopBarTitle(text = stringResource(id = R.string.title_main)) },
                actions = {
                    ActionItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_oksign),
                        contentDescription = stringResource(id = R.string.title_oksign)
                    ) { onEvent(HomeEvent.OneKeySign) }
                    ActionItem(
                        icon = Symbols.ViewAgenda,
                        contentDescription = stringResource(id = R.string.title_switch_list_single),
                        onClick = onToggleListSingle,
                    )
                }
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { contentPaddings ->
        // On a phone search opens from the button beside the bottom bar, or from [searchField]
        // where the user keeps it at the top
        val searchBox: @Composable () -> Unit = {
            when {
                searchField != null -> Box(
                    modifier = Modifier
                        .background(ExtendedTheme.colors.background)
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                ) { searchField() }

                showSearchBox -> SearchBox(modifier = Modifier.padding(bottom = 4.dp)) { onEvent(HomeEvent.Search) }
            }
        }
        val listPadding = contentPaddings.plus(PaddingValues(bottom = 12.dp))
        PullRefresh(
            refreshing = state.refreshing,
            enabled = expansion.refreshAllowed,
            revealUnpulled = true,
            onRefresh = { onEvent(HomeEvent.Refresh) },
            indicatorTop = contentPaddings.calculateTopPadding(),
        ) {
            StateScreen(
                isEmpty = forums.isEmpty(),
                isError = error != null,
                isLoading = isLoading,
                modifier = Modifier.fillMaxSize(),
                onReload = { onEvent(HomeEvent.Refresh) },
                emptyScreen = {
                    Column(modifier = Modifier.padding(contentPaddings)) {
                        searchBox()
                        EmptyScreen(loggedIn = state.loggedIn, canOpenExplore = canOpenExplore, onOpenExplore = onOpenExplore)
                    }
                },
                loadingScreen = {
                    HomePageSkeletonScreen(
                        listSingle = single,
                        gridCells = gridCells,
                        contentPadding = listPadding,
                        header = searchBox,
                    )
                },
                errorScreen = {
                    Column(modifier = Modifier.padding(contentPaddings)) {
                        searchBox()
                        error?.let { ErrorScreen(error = it) }
                    }
                }
            ) {
                MyLazyVerticalGrid(
                    columns = gridCells,
                    state = gridState,
                    contentPadding = listPadding,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(expansion.connection),
                ) {
                    item(key = "SearchBox", span = { GridItemSpan(maxLineSpan) }) {
                        searchBox()
                    }
                    if (showHistoryForum) {
                        item(key = RecentForumsKey, span = { GridItemSpan(maxLineSpan) }) {
                            RecentForums(
                                visited = state.visited,
                                expansion = expansion,
                                // Five to a row, four once their names are larger than the bars allow
                                columns = if (LocalTextScale.current > TextScale.ChromeMax) 4 else 5,
                                onOpen = { onEvent(HomeEvent.OpenForum(it.name)) },
                            )
                        }
                    }
                    if (pinned.isNotEmpty()) {
                        item(key = "TopForumHeader", span = { GridItemSpan(maxLineSpan) }) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Header(text = stringResource(id = R.string.title_top_forum), invert = true)
                            }
                        }
                        items(items = pinned, key = { "Top${it.id}" }) { item ->
                            ForumItem(
                                item,
                                single,
                                onClick = { onEvent(HomeEvent.OpenForum(it.name)) },
                                onUnfollow = { onEvent(HomeEvent.Unfollow(it)) },
                                onAddTopForum = { onEvent(HomeEvent.Pin(it)) },
                                onDeleteTopForum = { onEvent(HomeEvent.Unpin(it)) },
                                isTopForum = true
                            )
                        }
                    }
                    if (showHistoryForum || pinned.isNotEmpty()) {
                        item(key = "ForumHeader", span = { GridItemSpan(maxLineSpan) }) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Header(text = stringResource(id = R.string.forum_list_title), invert = true)
                            }
                        }
                    }
                    items(items = forums, key = { it.id }) { item ->
                        ForumItem(
                            item,
                            single,
                            onClick = { onEvent(HomeEvent.OpenForum(it.name)) },
                            onUnfollow = { onEvent(HomeEvent.Unfollow(it)) },
                            onAddTopForum = { onEvent(HomeEvent.Pin(it)) },
                            onDeleteTopForum = { onEvent(HomeEvent.Unpin(it)) },
                            isTopForum = item.id in state.pinnedIds,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePageSkeletonScreen(
    listSingle: Boolean,
    gridCells: GridCells,
    contentPadding: PaddingValues,
    header: @Composable () -> Unit,
) {
    MyLazyVerticalGrid(
        columns = gridCells,
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize(),
    ) {
        item(key = "Header", span = { GridItemSpan(maxLineSpan) }) {
            header()
        }
        item(key = "TopForumHeaderPlaceholder", span = { GridItemSpan(maxLineSpan) }) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Header(
                    text = stringResource(id = R.string.title_top_forum),
                    modifier = Modifier.placeholder(
                        visible = true,
                        color = ExtendedTheme.colors.chip,
                        shape = RectangleShape,
                    ),
                    invert = true
                )
            }
        }
        items(6, key = { "TopPlaceholder$it" }) {
            ForumItemPlaceholder(listSingle)
        }
        item(
            key = "Spacer",
            span = { GridItemSpan(maxLineSpan) }) {
            Spacer(
                modifier = Modifier.height(
                    16.dp
                )
            )
        }
        item(key = "ForumHeaderPlaceholder", span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Header(
                    text = stringResource(id = R.string.forum_list_title),
                    modifier = Modifier.placeholder(
                        visible = true,
                        color = ExtendedTheme.colors.chip,
                        shape = RectangleShape,
                    ),
                    invert = true
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        items(12, key = { "Placeholder$it" }) {
            ForumItemPlaceholder(listSingle)
        }
    }
}

@Composable
fun EmptyScreen(
    loggedIn: Boolean,
    canOpenExplore: Boolean,
    onOpenExplore: () -> Unit
) {
    val navigator = LocalNavigator.current
    TipScreen(
        title = {
            if (!loggedIn) {
                Text(text = stringResource(id = R.string.title_empty_login))
            } else {
                Text(text = stringResource(id = R.string.title_empty))
            }
        },
        image = {
            val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.lottie_astronaut))
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f)
            )
        },
        message = {
            if (!loggedIn) {
                Text(
                    text = stringResource(id = R.string.home_empty_login),
                    style = MaterialTheme.typography.body1,
                    color = ExtendedTheme.colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        actions = {
            if (!loggedIn) {
                Button(
                    onClick = {
                        navigator.navigate(LoginKey)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.button_login))
                }
            }
            if (canOpenExplore) {
                SecondaryButton(
                    onClick = onOpenExplore,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.button_go_to_explore))
                }
            }
        },
    )
}

private const val RecentForumsKey = "HistoryForums"
private val RecentForumsGuideKey = booleanPreferencesKey("recentForumsGuideShown")
private val GuidePeek = 44.dp
private const val GuideDelay = 600L

/**
 * The followed forums two to a row or more: one more for every 180dp, and two already on a narrow
 * phone, as long as each gets 150dp. Only narrower than that one to a row.
 */
private object ForumGridCells : GridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val count = when {
            availableSize + spacing >= 2 * (180.dp.roundToPx() + spacing) ->
                (availableSize + spacing) / (180.dp.roundToPx() + spacing)
            availableSize + spacing >= 2 * (150.dp.roundToPx() + spacing) -> 2
            else -> 1
        }
        val gaps = spacing * (count - 1)
        val size = (availableSize - gaps) / count
        val rest = (availableSize - gaps) % count
        return List(count) { size + if (it < rest) 1 else 0 }
    }
}
