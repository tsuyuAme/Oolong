package oolong.client.ui.page.hottopic.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import oolong.client.R
import oolong.client.arch.LoadStatus
import oolong.client.model.RankedTopic
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.Spacing
import oolong.client.ui.common.theme.compose.TextStyles
import oolong.client.ui.navigation.Navigator
import oolong.client.ui.navigation.TopicKey
import oolong.client.ui.page.LocalNavigator
import oolong.client.ui.page.topic.TopicTag
import oolong.client.ui.widgets.compose.BackNavigationIcon
import oolong.client.ui.widgets.compose.ContentImage
import oolong.client.ui.widgets.compose.ErrorScreen
import oolong.client.ui.widgets.compose.GlassScaffold
import oolong.client.ui.widgets.compose.GlassTopBar
import oolong.client.ui.widgets.compose.GlassTopBarTitle
import oolong.client.ui.widgets.compose.MyLazyColumn
import oolong.client.ui.widgets.compose.PullRefresh
import oolong.client.ui.widgets.compose.RankNumber
import oolong.client.ui.widgets.compose.states.DefaultEmptyScreen
import oolong.client.ui.widgets.compose.states.DefaultLoadingScreen
import oolong.client.ui.widgets.compose.states.StateScreen
import oolong.client.utils.StringUtil.getShortNumString

@Composable
fun HotTopicListPage(
    navigator: Navigator = LocalNavigator.current,
    viewModel: HotTopicListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HotTopicListScreen(
        state = state,
        onBack = { navigator.navigateUp() },
        onRefresh = viewModel::refresh,
        onOpen = { navigator.navigate(TopicKey(it.id, it.name)) },
    )
}

/** The full hot topics ranking, each topic opening its page; pulls down to refresh. */
@Composable
fun HotTopicListScreen(
    state: HotTopicListState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onOpen: (RankedTopic) -> Unit = {},
) {
    val isRefreshing = state.load == LoadStatus.Loading
    val topicList = state.topics
    GlassScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            GlassTopBar(
                title = { GlassTopBarTitle(text = stringResource(id = R.string.hot_section_topics)) },
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
            )
        },
    ) { contentPaddings ->
        val error = (state.load as? LoadStatus.Error)?.error
        StateScreen(
            isEmpty = topicList.isEmpty(),
            isError = error != null,
            isLoading = isRefreshing,
            modifier = Modifier.fillMaxSize(),
            onReload = onRefresh,
            errorScreen = { ErrorScreen(error = error, modifier = Modifier.padding(contentPaddings)) },
            loadingScreen = { Box(modifier = Modifier.padding(contentPaddings)) { DefaultLoadingScreen() } },
            emptyScreen = { Box(modifier = Modifier.padding(contentPaddings)) { DefaultEmptyScreen() } },
        ) {
            PullRefresh(
                refreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
                indicatorTop = contentPaddings.calculateTopPadding(),
            ) {
                MyLazyColumn(contentPadding = contentPaddings) {
                    itemsIndexed(items = topicList, key = { _, item -> item.id }) { index, item ->
                        TopicRow(rank = index + 1, topic = item, onClick = { onOpen(item) })
                    }
                }
            }
        }
    }
}

/** A topic: its place, name and tag, two lines of what it is about and its heat, its picture beside. */
@Composable
private fun TopicRow(rank: Int, topic: RankedTopic, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        RankNumber(rank = rank, modifier = Modifier.padding(top = 1.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                Text(
                    text = topic.name,
                    style = TextStyles.name,
                    color = ExtendedTheme.colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                TopicTag(topic.tag)
            }
            if (topic.description.isNotBlank()) {
                Text(
                    text = topic.description,
                    style = TextStyles.caption,
                    color = ExtendedTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = stringResource(id = R.string.hot_num, topic.discussCount.getShortNumString()),
                style = TextStyles.footnote,
                color = ExtendedTheme.colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (topic.image.isNotEmpty()) {
            ContentImage(
                imageUri = topic.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(72.dp),
                shape = Corners.md,
            )
        }
    }
}
