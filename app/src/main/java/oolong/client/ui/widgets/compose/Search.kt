package oolong.client.ui.widgets.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import oolong.client.R
import oolong.client.model.QuotedPost
import oolong.client.model.SearchAuthor
import oolong.client.model.SearchedPost
import oolong.client.repository.BlockVerdict
import oolong.client.ui.common.PbContentText
import oolong.client.ui.common.relativeTime
import oolong.client.ui.common.rememberTextLinkActions
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.PressFeedback
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.common.theme.compose.Symbols
import oolong.client.ui.common.windowsizeclass.LocalWindowSizeClass
import oolong.client.ui.common.windowsizeclass.WindowWidthSizeClass
import oolong.client.utils.StringUtil
import oolong.client.utils.StringUtil.buildAnnotatedStringWithUser
import oolong.client.utils.appPreferences

@Composable
fun QuotePostCard(
    quoted: QuotedPost,
    thread: QuotedPost,
    onThreadClick: (QuotedPost) -> Unit,
    modifier: Modifier = Modifier,
    pictures: ImmutableList<String> = persistentListOf(),
    mediaCount: Int = pictures.size,
    keyword: String? = null,
) {
    val links = rememberTextLinkActions()
    val quoteContentString = remember(quoted, links) {
        buildAnnotatedStringWithUser(
            quoted.author.id,
            quoted.author.name,
            quoted.author.nameShow,
            quoted.content,
            links,
        )
    }
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HighlightText(
            text = quoteContentString,
            style = MaterialTheme.typography.body2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            highlightKeywords = (keyword?.split(" ") ?: emptyList()).toImmutableList()
        )
        MainPostCard(
            thread = thread,
            modifier = Modifier
                .fillMaxWidth()
                .clip(SmoothCornerShape(6.dp))
                .background(ExtendedTheme.colors.card)
                .clickable {
                    onThreadClick(thread)
                },
            pictures = pictures,
            mediaCount = mediaCount,
            keyword = keyword
        )
    }
}

@Composable
fun MainPostCard(
    thread: QuotedPost,
    modifier: Modifier = Modifier,
    pictures: ImmutableList<String> = persistentListOf(),
    mediaCount: Int = pictures.size,
    keyword: String? = null,
) {
    val links = rememberTextLinkActions()
    val titleString = remember(thread, links) {
        buildAnnotatedStringWithUser(
            thread.author.id,
            thread.author.name,
            thread.author.nameShow,
            thread.title,
            links,
        )
    }
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HighlightText(
            text = titleString,
            style = MaterialTheme.typography.subtitle2,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            highlightKeywords = (keyword?.split(" ") ?: emptyList()).toImmutableList()
        )
        if (thread.content.isNotBlank()) {
            PbContentText(
                text = thread.content,
                style = MaterialTheme.typography.body2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SearchMedia(pictures = pictures, mediaCount = mediaCount)
    }
}

@Composable
fun SearchThreadList(
    data: ImmutableList<SearchedPost>,
    lazyListState: LazyListState,
    onItemClick: (SearchedPost) -> Unit,
    onItemUserClick: (SearchAuthor) -> Unit,
    onItemForumClick: (forumName: String) -> Unit,
    modifier: Modifier = Modifier,
    onQuotePostClick: (QuotedPost) -> Unit = {},
    onMainPostClick: (QuotedPost) -> Unit = {},
    hideForum: Boolean = false,
    searchKeyword: String? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    blockOf: (SearchedPost) -> BlockVerdict? = { null },
    footer: LazyListScope.() -> Unit = {},
    header: LazyListScope.() -> Unit = {},
) {
    MyLazyColumn(
        state = lazyListState,
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        header()
        itemsIndexed(data) { index, item ->
            if (index > 0) {
                VerticalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
            BlockableContent(block = blockOf(item), modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SearchThreadItem(
                    item = item,
                    onClick = onItemClick,
                    onUserClick = onItemUserClick,
                    onForumClick = onItemForumClick,
                    onQuotePostClick = onQuotePostClick,
                    onMainPostClick = onMainPostClick,
                    hideForum = hideForum,
                    searchKeyword = searchKeyword
                )
            }
        }
        footer()
    }
}

@Composable
fun SearchThreadUserHeader(
    user: SearchAuthor,
    time: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    UserHeader(
        avatar = {
            Avatar(
                data = StringUtil.getAvatarUrl(user.portrait),
                size = Sizes.Small,
                contentDescription = null
            )
        },
        name = {
            AuthorName(
                text = StringUtil.getUsernameAnnotatedString(
                    LocalContext.current,
                    user.name,
                    user.nameShow,
                    color = LocalContentColor.current
                )
            )
        },
        desc = {
            MetaText(text = relativeTime(time))
        },
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun SearchThreadItem(
    item: SearchedPost,
    onClick: (SearchedPost) -> Unit,
    onUserClick: (SearchAuthor) -> Unit,
    onForumClick: (forumName: String) -> Unit,
    modifier: Modifier = Modifier,
    onQuotePostClick: (QuotedPost) -> Unit = {},
    onMainPostClick: (QuotedPost) -> Unit = {},
    hideForum: Boolean = false,
    searchKeyword: String? = null,
) {
    Card(
        modifier = modifier,
        header = {
            SearchThreadUserHeader(
                user = item.author,
                time = item.time,
                onClick = { onUserClick(item.author) }
            )
        },
        content = {
            ThreadContent(
                title = item.title,
                abstractText = item.content,
                showTitle = item.thread == null && item.title.isNotBlank(),
                showAbstract = item.content.isNotBlank(),
                maxLines = 2,
                highlightKeywords = (searchKeyword?.split(" ") ?: emptyList()).toImmutableList(),
            )
            val thread = item.thread
            val quoted = item.quoted
            if (thread != null) {
                if (quoted != null) {
                    QuotePostCard(
                        quoted = quoted,
                        thread = thread,
                        onThreadClick = onMainPostClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SmoothCornerShape(6.dp))
                            .background(ExtendedTheme.colors.inset)
                            .clickable {
                                onQuotePostClick(quoted)
                            },
                        pictures = item.pictures,
                        mediaCount = item.mediaCount,
                        keyword = searchKeyword
                    )
                } else {
                    MainPostCard(
                        thread = thread,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SmoothCornerShape(6.dp))
                            .background(ExtendedTheme.colors.inset)
                            .clickable {
                                onMainPostClick(thread)
                            },
                        pictures = item.pictures,
                        mediaCount = item.mediaCount,
                        keyword = searchKeyword
                    )
                }
            } else {
                SearchMedia(pictures = item.pictures, mediaCount = item.mediaCount)
            }
        },
        action = {
            FeedCardActions(
                forum = {
                    if (!hideForum && item.forumName.isNotEmpty()) {
                        ForumInfoChip(
                            imageUriProvider = { item.forumAvatar },
                            nameProvider = { item.forumName }
                        ) {
                            onForumClick(item.forumName)
                        }
                    }
                }
            ) {
                ThreadShareBtn(shareNum = item.shareCount, onClick = {})
                ThreadReplyBtn(replyNum = item.replyCount, onClick = {})
                ThreadAgreeBtn(hasAgree = false, agreeNum = item.agreeCount, onClick = {})
            }
        },
        onClick = { onClick(item) },
    )
}

/** Up to three of a search result's pictures, with a badge counting its media when there are more. */
@Composable
fun SearchMedia(
    pictures: ImmutableList<String>,
    modifier: Modifier = Modifier,
    mediaCount: Int = pictures.size,
) {
    val context = LocalContext.current

    val picCount = pictures.size
    val hasPhoto = picCount > 0
    val isSinglePhoto = picCount == 1
    val hideMedia = context.appPreferences.hideMedia

    val windowWidthSizeClass = LocalWindowSizeClass.current.widthSizeClass
    val singleMediaFraction = remember(windowWidthSizeClass) {
        if (windowWidthSizeClass == WindowWidthSizeClass.Compact)
            1f
        else 0.5f
    }

    if (hasPhoto && !hideMedia) {
        val mediaWidthFraction = if (isSinglePhoto) singleMediaFraction else 1f
        val mediaAspectRatio = if (isSinglePhoto) 2f else 3f
        val showMedias = remember(pictures) { pictures.take(3) }
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(mediaWidthFraction)
                        .aspectRatio(mediaAspectRatio)
                        .clip(Corners.sm),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    showMedias.fastForEach { pic ->
                        NetworkImage(
                            imageUri = pic,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight(1f),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                if (picCount > 3) {
                    Badge(
                        icon = Symbols.Image,
                        text = "$mediaCount",
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBox(
    keyword: String,
    onKeywordChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onKeywordSubmit: (String) -> Unit = {},
    placeholder: @Composable () -> Unit = {},
    prependIcon: @Composable () -> Unit = {},
    appendIcon: @Composable () -> Unit = {},
    focusRequester: FocusRequester = remember { FocusRequester() },
    shape: Shape = RectangleShape,
    color: Color = ExtendedTheme.colors.searchField,
    contentColor: Color = ExtendedTheme.colors.onSearchField,
    elevation: Dp = 0.dp,
    // Without it, as where the field narrows as it is typed in, the clear button stays shown
    submitButton: Boolean = true,
) {
    val isKeywordNotEmpty = remember(keyword) { keyword.isNotEmpty() }
    var isFocused by remember { mutableStateOf(false) }
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        contentColor = contentColor,
        elevation = elevation
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            prependIcon()
            BaseTextField(
                value = keyword,
                onValueChange = {
                    onKeywordChange(it)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onKeywordSubmit(keyword)
                    }
                ),
                placeholder = placeholder,
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusEvent {
                        isFocused = it.isFocused
                    }
            )
            appendIcon()
            AnimatedVisibility(visible = isKeywordNotEmpty && (isFocused || !submitButton)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(Corners.full)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = PressFeedback.Overlay,
                                role = Role.Button
                            ) { onKeywordChange("") },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Symbols.Close,
                            contentDescription = stringResource(id = R.string.button_clear)
                        )
                    }
                }
            }
            AnimatedVisibility(visible = isKeywordNotEmpty && submitButton) {
                Box(
                    modifier = Modifier
                        .clip(Corners.full)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = PressFeedback.Overlay,
                            role = Role.Button
                        ) { onKeywordSubmit(keyword) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Symbols.Search,
                        contentDescription = stringResource(id = R.string.button_search)
                    )
                }
            }
        }
    }
}
