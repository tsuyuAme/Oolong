package oolong.client.ui.page.forum.rule

import kotlinx.collections.immutable.persistentListOf
import oolong.client.api.models.protos.PbContent
import oolong.client.arch.LoadState
import oolong.client.model.ForumRuleAuthor
import oolong.client.model.ForumRuleDetail
import oolong.client.model.ForumRuleItem
import oolong.client.repository.toPostContent
import oolong.client.testing.LegacyAppRule
import oolong.client.testing.captureScreenshot
import oolong.client.utils.EmoticonManager
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "+w393dp-h851dp-xxhdpi")
class ForumRuleDetailScreenshotTest {
    @get:Rule
    val legacyApp = LegacyAppRule()

    @Test
    fun loaded() {
        EmoticonManager.registerEmoticon("image_emoticon25", "滑稽")
        val detail = ForumRuleDetail(
            title = "本吧吧规",
            publishTime = "2026-10-31",
            preface = "为维护良好的讨论氛围，请遵守以下规定。",
            author = ForumRuleAuthor(name = "bazhu", nameShow = "吧主", portrait = ""),
            rules = persistentListOf(
                ForumRuleItem("一、发帖规范", rule(PbContent(type = 0, text = "标题需清楚描述帖子内容。"))),
                ForumRuleItem(
                    "二、联系方式",
                    rule(
                        PbContent(type = 0, text = "有问题请联系"),
                        PbContent(type = 4, text = "@吧主", uid = 1),
                        PbContent(type = 0, text = "，或查看"),
                        PbContent(type = 1, text = "申诉指南", link = "https://tieba.baidu.com/guide"),
                        PbContent(type = 2, text = "image_emoticon25", c = "滑稽"),
                    ),
                ),
            ),
        )
        captureScreenshot { ForumRuleDetailScreen(state = LoadState.Success(detail), onRetry = {}, onBack = {}) }
    }

    private fun rule(vararg content: PbContent) = listOf(*content).toPostContent { id, name -> EmoticonManager.registerEmoticon(id, name) }
}
