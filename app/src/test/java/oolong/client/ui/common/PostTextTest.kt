package oolong.client.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import oolong.client.model.TextSpan
import oolong.client.utils.EmoticonUtil
import org.junit.Test
import kotlin.test.assertEquals

class PostTextTest {
    private val accent = Color.Blue
    private val warning = Color.Red
    private val opened = mutableListOf<String>()
    private val links = TextLinkActions(
        openUrl = { opened += "url $it" },
        openUser = { opened += "user $it" },
        openTopic = { opened += "topic $it" },
    )

    private fun render(vararg spans: TextSpan) =
        spans.toList().toAnnotatedString(accent, videoTag = "视频：", warning = warning, actualHostFormat = "（%s）", links = links)

    /** The links in [text] as (covered text, styles), opening each in turn. */
    private fun linksOf(text: AnnotatedString): List<Pair<String, TextLinkStyles?>> =
        text.getLinkAnnotations(0, text.length).map { range ->
            val link = range.item as LinkAnnotation.Clickable
            link.linkInteractionListener?.onClick(link)
            text.text.substring(range.start, range.end) to link.styles
        }

    @Test
    fun `links, mentions and topics are accent links that open their targets`() {
        val text = render(
            TextSpan.Plain("看 "),
            TextSpan.Mention("@楼主", userId = 7),
            TextSpan.Plain(" 的 "),
            TextSpan.Link("链接", url = "https://example.com"),
            TextSpan.Topic("#话题#", name = "话题"),
        )

        val accentLink = TextLinkStyles(SpanStyle(color = accent))
        assertEquals(listOf("@楼主" to accentLink, "链接" to accentLink, "#话题#" to accentLink), linksOf(text))
        assertEquals(listOf("user 7", "url https://example.com", "topic 话题"), opened)
    }

    @Test
    fun `a video link covers its tag and address, after its icon`() {
        val text = render(TextSpan.VideoLink("https://v", url = "https://v"))

        assertEquals("🎥视频：https://v", text.text)
        assertEquals(listOf("视频：https://v" to TextLinkStyles(SpanStyle(color = accent))), linksOf(text))
        assertEquals(listOf("url https://v"), opened)
    }

    @Test
    fun `a misleading link is followed by where it really goes, in the warning colour`() {
        val text = render(TextSpan.Link("baidu.com", url = "https://evil.example", actualHost = "evil.example"))

        assertEquals("🔗baidu.com（evil.example）", text.text)
        assertEquals(
            listOf(
                "baidu.com" to TextLinkStyles(SpanStyle(color = accent)),
                "（evil.example）" to TextLinkStyles(SpanStyle(color = warning)),
            ),
            linksOf(text),
        )
        assertEquals(listOf("url https://evil.example", "url https://evil.example"), opened)
    }

    @Test
    fun `emoticons are inline content by id, written as their names`() {
        val text = render(TextSpan.Plain("好"), TextSpan.Emoticon("image_emoticon25", "滑稽"), TextSpan.Emoticon("image_emoticon2", "哈哈"))

        assertEquals("好#(滑稽)#(哈哈)", text.text)
        val inline = text.getStringAnnotations(EmoticonUtil.INLINE_CONTENT_TAG, 0, text.length)
            .map { Triple(it.item, it.start, it.end) }
        assertEquals(listOf(Triple("Emoticon#image_emoticon25", 1, 6), Triple("Emoticon#image_emoticon2", 6, 11)), inline)
        assertEquals(setOf("image_emoticon25", "image_emoticon2"), EmoticonUtil.emoticonIds(text))
    }

    @Test
    fun `text without emoticons needs no inline images`() {
        assertEquals(emptySet(), EmoticonUtil.emoticonIds(render(TextSpan.Link("链接", url = "https://example.com"))))
    }
}
