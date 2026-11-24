package oolong.client.ui.widgets.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import oolong.client.R
import oolong.client.api.retrofit.exception.isNetworkError
import oolong.client.arch.LoadStatus
import oolong.client.ui.common.theme.compose.ExtendedTheme

private const val LoadMoreFooterKey = "LoadMoreFooter"

/** Apart from the footer's key, so a list kept where it was by this row lets go of it once rows come. */
private const val LoadMoreScreenKey = "LoadMoreScreen"

/** How much of the list may be left below the screen, in screens, before the next page loads. */
private const val PreloadScreens = 1.5f

/** How far a list that has ended must be pulled up past its end for each answer of its end row. */
private val EndPullSteps = listOf(48.dp, 96.dp)

/**
 * The row at the end of a paged list: the loader while the next page loads, why it could not load
 * with a way to retry, or, once the list has ended, that there is no more. Pulled up past its end
 * ([EndPull.Tease]) that last line answers the pull; with [pullToLoadLatest] it says instead that
 * pulling up loads the newest replies ([EndPull.Action]).
 *
 * With [fillScreen] the row takes a screen's height, for a list loading anew under rows that stay:
 * those can stay where they are on screen, instead of the list falling short and moving them.
 */
fun LazyListScope.loadMoreFooter(
    append: LoadStatus,
    hasMore: Boolean,
    onRetry: () -> Unit,
    pullToLoadLatest: Boolean = false,
    fillScreen: Boolean = false,
) {
    if (append == LoadStatus.Idle && hasMore) return
    item(key = if (fillScreen) LoadMoreScreenKey else LoadMoreFooterKey, contentType = LoadMoreFooterKey) {
        Box(modifier = if (fillScreen) Modifier.fillParentMaxHeight() else Modifier) {
            FooterRow(append, onRetry, pullToLoadLatest)
        }
    }
}

@Composable
private fun FooterRow(append: LoadStatus, onRetry: () -> Unit, pullToLoadLatest: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            append == LoadStatus.Loading -> WavyLoader()

            append is LoadStatus.Error -> {
                val message = if (append.error.isNetworkError()) R.string.title_no_internet_connectivity else R.string.title_api_error
                Text(
                    text = stringResource(id = message),
                    color = ExtendedTheme.colors.textSecondary,
                    style = MaterialTheme.typography.body2,
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onRetry) { Text(text = stringResource(id = R.string.button_retry)) }
            }

            else -> EndText(pullToLoadLatest)
        }
    }
}

/** 「没有更多了」, which answers a pull past the end, or says what pulling there loads. */
@Composable
private fun EndText(pullToLoadLatest: Boolean) {
    val pulled = LocalEndPull.current
    val density = LocalDensity.current
    val step by remember(pullToLoadLatest) {
        derivedStateOf {
            val distance = pulled()
            with(density) {
                if (pullToLoadLatest) {
                    if (distance >= EndActionDistance.toPx()) 1 else 0
                } else {
                    EndPullSteps.count { distance >= it.toPx() }
                }
            }
        }
    }
    val text = when {
        pullToLoadLatest -> if (step == 0) R.string.pull_to_load_latest_posts else R.string.release_to_load_latest_posts
        step == 0 -> R.string.no_more
        step == 1 -> R.string.no_more_really
        else -> R.string.no_more_bottom_line
    }
    Refocus(targetState = text) {
        Text(
            text = stringResource(id = it),
            color = ExtendedTheme.colors.textSecondary,
            style = MaterialTheme.typography.caption,
        )
    }
}

/**
 * Loads the next page while [enabled] and less than [PreloadScreens] of the list is left below the
 * screen, so it is usually there before it is reached. A load that failed is not repeated here: it
 * leaves its error in the end row, whose retry button loads again.
 */
@Composable
fun LoadMoreEffect(state: LazyListState, enabled: Boolean, onLoadMore: () -> Unit) {
    val curEnabled by rememberUpdatedState(enabled)
    val curOnLoadMore by rememberUpdatedState(onLoadMore)
    LaunchedEffect(state) {
        snapshotFlow { curEnabled && state.layoutInfo.totalItemsCount > 0 && state.distanceToEnd() < state.layoutInfo.viewportSize.height * PreloadScreens }
            .distinctUntilChanged()
            .filter { it }
            .collect { curOnLoadMore() }
    }
}

/** Roughly how far below the screen the list goes on, from the size of the items in view. */
private fun LazyListState.distanceToEnd(): Float {
    val info = layoutInfo
    val visible = info.visibleItemsInfo
    val last = visible.lastOrNull() ?: return 0f
    val below = (last.offset + last.size - info.viewportEndOffset).coerceAtLeast(0)
    val average = visible.sumOf { it.size }.toFloat() / visible.size
    return below + average * (info.totalItemsCount - 1 - last.index)
}
