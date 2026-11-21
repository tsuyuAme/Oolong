package oolong.client.ui.page.forum.detail

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import oolong.client.R
import oolong.client.arch.LoadStatus
import oolong.client.model.ForumIntro
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.PressFeedback
import oolong.client.ui.common.theme.compose.Symbols
import oolong.client.ui.common.theme.compose.TiebaLiteTheme
import oolong.client.ui.navigation.ForumDetailKey
import oolong.client.ui.navigation.ForumRuleDetailKey
import oolong.client.ui.navigation.Navigator
import oolong.client.ui.page.LocalNavigator
import oolong.client.ui.widgets.compose.Avatar
import oolong.client.ui.widgets.compose.BackNavigationIcon
import oolong.client.ui.widgets.compose.Chip
import oolong.client.ui.widgets.compose.Container
import oolong.client.ui.widgets.compose.ErrorScreen
import oolong.client.ui.widgets.compose.GlassScaffold
import oolong.client.ui.widgets.compose.GlassTopBar
import oolong.client.ui.widgets.compose.GlassTopBarTitle
import oolong.client.ui.widgets.compose.HorizontalDivider
import oolong.client.ui.widgets.compose.Sizes
import oolong.client.ui.widgets.compose.states.StateScreen
import oolong.client.ui.widgets.compose.verticalScrollUnderBar
import oolong.client.utils.StringUtil.getShortNumeralString

@Composable
fun ForumDetailPage(
    key: ForumDetailKey,
    navigator: Navigator = LocalNavigator.current,
    viewModel: ForumDetailViewModel = hiltViewModel<ForumDetailViewModel, ForumDetailViewModel.Factory> { it.create(key) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ForumDetailScreen(
        state = state,
        onBack = { navigator.navigateUp() },
        onReload = viewModel::refresh,
        ruleTitle = key.ruleTitle,
        onOpenRule = { navigator.navigate(ForumRuleDetailKey(key.forumId)) },
    )
}

/** A forum's avatar, counts, slogan and introduction, then its rules when it has them. */
@Composable
fun ForumDetailScreen(
    state: ForumDetailState,
    onBack: () -> Unit,
    onReload: () -> Unit,
    ruleTitle: String? = null,
    onOpenRule: () -> Unit = {},
) {
    val error = (state.load as? LoadStatus.Error)?.error
    StateScreen(
        isEmpty = state.intro == null,
        isError = error != null,
        isLoading = state.load == LoadStatus.Loading,
        onReload = onReload,
        errorScreen = { ErrorScreen(error = error) },
        modifier = Modifier.fillMaxSize(),
    ) {
        GlassScaffold(
            topBar = {
                GlassTopBar(
                    title = {
                        GlassTopBarTitle(text = stringResource(id = R.string.title_forum_info))
                    },
                    navigationIcon = {
                        BackNavigationIcon(onBackPressed = onBack)
                    }
                )
            }
        ) { paddingValues ->
            Container {
                state.intro?.let {
                    ForumDetailContent(
                        forumInfo = it,
                        ruleTitle = ruleTitle,
                        onOpenRule = onOpenRule,
                        contentPadding = paddingValues,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ForumDetailContent(
    forumInfo: ForumIntro,
    ruleTitle: String?,
    onOpenRule: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    Column(
        modifier = modifier
            .verticalScrollUnderBar()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(
                data = forumInfo.avatar,
                size = Sizes.Medium,
                contentDescription = null,
            )
            Text(
                text = stringResource(id = R.string.title_forum, forumInfo.name),
                style = MaterialTheme.typography.h6
            )
        }
        Row(
            modifier = Modifier
                .background(color = ExtendedTheme.colors.chip, shape = Corners.sm)
                .padding(vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatCardItem(
                statNum = forumInfo.memberCount,
                statText = stringResource(id = R.string.forum_stat_members)
            )
            HorizontalDivider(color = Color(if (ExtendedTheme.colors.isNightMode) 0xFF808080 else 0xFFDEDEDE))
            StatCardItem(
                statNum = forumInfo.threadCount,
                statText = stringResource(id = R.string.text_stat_threads)
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip(text = stringResource(id = R.string.title_forum_intro))
            Column {
                Text(text = forumInfo.slogan, style = MaterialTheme.typography.body1)
                Text(text = forumInfo.intro, style = MaterialTheme.typography.body1)
            }
        }
        if (!ruleTitle.isNullOrEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(text = stringResource(id = R.string.desc_forum_rule))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = null, indication = PressFeedback.overlay(Corners.sm), onClick = onOpenRule)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ruleTitle,
                        style = MaterialTheme.typography.body1,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Symbols.ChevronRight,
                        contentDescription = null,
                        tint = ExtendedTheme.colors.textSecondary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatCardItem(
    statNum: Int,
    statText: String,
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = statNum.getShortNumeralString(),
            fontSize = 20.sp,
            fontFamily = FontFamily(
                Typeface.createFromAsset(
                    LocalContext.current.assets,
                    "bebas.ttf"
                )
            ),
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = statText,
            fontSize = 12.sp,
            color = ExtendedTheme.colors.textSecondary
        )
    }
}

@Preview("ForumDetailPage", backgroundColor = 0xFFFFFFFF, showBackground = true)
@Composable
fun PreviewForumDetailPage() {
    TiebaLiteTheme {
        ForumDetailContent(
            forumInfo = ForumDetailPreviewData.intro,
            ruleTitle = "测试吧吧规",
            onOpenRule = {},
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** A sample forum, shared by the preview above and the screenshot tests. */
internal object ForumDetailPreviewData {
    val intro = ForumIntro(
        name = "minecraft",
        avatar = "",
        memberCount = 2520287,
        threadCount = 31531580,
        slogan = "位于百度贴吧的像素点之家",
        intro = "minecraft……",
    )
}
