package oolong.client.model

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList

@Immutable
data class ForumRuleDetail(
    val title: String,
    val publishTime: String,
    val preface: String,
    val author: ForumRuleAuthor,
    val rules: ImmutableList<ForumRuleItem>,
)

@Immutable
data class ForumRuleAuthor(
    val name: String,
    val nameShow: String,
    val portrait: String,
)

@Immutable
data class ForumRuleItem(
    val title: String,
    val content: ImmutableList<PostContent>,
)
