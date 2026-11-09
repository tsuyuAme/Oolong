package oolong.client.ui.page.forum.rule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.persistentListOf
import oolong.client.R
import oolong.client.arch.LoadState
import oolong.client.model.ForumRuleAuthor
import oolong.client.model.ForumRuleDetail
import oolong.client.model.ForumRuleItem
import oolong.client.model.PostContent
import oolong.client.model.TextSpan
import oolong.client.ui.common.PostBody
import oolong.client.ui.common.theme.compose.TiebaLiteTheme
import oolong.client.ui.navigation.ForumRuleDetailKey
import oolong.client.ui.navigation.Navigator
import oolong.client.ui.page.LocalNavigator
import oolong.client.ui.page.ProvideNavigator
import oolong.client.ui.widgets.compose.Avatar
import oolong.client.ui.widgets.compose.BackNavigationIcon
import oolong.client.ui.widgets.compose.GlassScaffold
import oolong.client.ui.widgets.compose.GlassTopBar
import oolong.client.ui.widgets.compose.GlassTopBarTitle
import oolong.client.ui.widgets.compose.Sizes
import oolong.client.ui.widgets.compose.UserHeader
import oolong.client.ui.widgets.compose.states.LoadStateScreen
import oolong.client.ui.widgets.compose.verticalScrollUnderBar
import oolong.client.utils.StringUtil

@Composable
fun ForumRuleDetailPage(
    key: ForumRuleDetailKey,
    navigator: Navigator = LocalNavigator.current,
    viewModel: ForumRuleDetailViewModel = hiltViewModel<ForumRuleDetailViewModel, ForumRuleDetailViewModel.Factory> { it.create(key) },
) {
    val state by viewModel.ruleDetail.state.collectAsStateWithLifecycle()

    ProvideNavigator(navigator = navigator) {
        ForumRuleDetailScreen(
            state = state,
            onRetry = viewModel.ruleDetail::retry,
            onBack = navigator::navigateUp,
        )
    }
}

@Composable
fun ForumRuleDetailScreen(
    state: LoadState<ForumRuleDetail>,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassScaffold(
        modifier = modifier,
        topBar = {
            GlassTopBar(
                title = { GlassTopBarTitle(text = stringResource(id = R.string.title_forum_rule)) },
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) }
            )
        }
    ) { contentPadding ->
        LoadStateScreen(
            state = state,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize(),
            isEmpty = { it.rules.isEmpty() },
        ) { detail ->
            ForumRuleDetailContent(detail, contentPadding)
        }
    }
}

@Composable
private fun ForumRuleDetailContent(detail: ForumRuleDetail, contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScrollUnderBar()
            .padding(contentPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = detail.title, style = MaterialTheme.typography.h5)
        UserHeader(
            avatar = {
                Avatar(
                    data = StringUtil.getAvatarUrl(detail.author.portrait),
                    size = Sizes.Small,
                    contentDescription = null
                )
            },
            name = {
                Text(
                    text = StringUtil.getUsernameAnnotatedString(
                        LocalContext.current,
                        detail.author.name,
                        detail.author.nameShow,
                        LocalContentColor.current
                    )
                )
            },
            desc = (@Composable {
                Text(text = detail.publishTime)
            }).takeIf { detail.publishTime.isNotEmpty() }
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProvideTextStyle(value = MaterialTheme.typography.body1) {
                Text(text = detail.preface)
                detail.rules.fastForEach { rule ->
                    if (rule.title.isNotEmpty()) {
                        Text(
                            text = rule.title,
                            style = MaterialTheme.typography.subtitle1
                        )
                    }
                    PostBody(content = rule.content, blockSpacing = 4.dp)
                }
            }
        }
    }
}

private fun plainRule(text: String) = persistentListOf<PostContent>(PostContent.Text(persistentListOf(TextSpan.Plain(text))))

private val previewDetail = ForumRuleDetail(
    title = "本吧吧规",
    publishTime = "2026-10-31",
    preface = "为维护良好的讨论氛围，请遵守以下规定。",
    author = ForumRuleAuthor(name = "bazhu", nameShow = "吧主", portrait = ""),
    rules = persistentListOf(
        ForumRuleItem("一、发帖规范", plainRule("标题需清楚描述帖子内容。")),
        ForumRuleItem("二、禁止行为", plainRule("禁止广告、引战和人身攻击。")),
    ),
)

@Preview("ForumRuleDetailScreen", showBackground = true)
@Composable
private fun PreviewForumRuleDetailScreen() {
    TiebaLiteTheme {
        ForumRuleDetailScreen(state = LoadState.Success(previewDetail), onRetry = {}, onBack = {})
    }
}

@Preview("ForumRuleDetailScreen Loading", showBackground = true)
@Composable
private fun PreviewForumRuleDetailScreenLoading() {
    TiebaLiteTheme {
        ForumRuleDetailScreen(state = LoadState.Loading, onRetry = {}, onBack = {})
    }
}

@Preview("ForumRuleDetailScreen Error", showBackground = true)
@Composable
private fun PreviewForumRuleDetailScreenError() {
    TiebaLiteTheme {
        ForumRuleDetailScreen(
            state = LoadState.Error(IllegalStateException("加载失败")),
            onRetry = {},
            onBack = {},
        )
    }
}
