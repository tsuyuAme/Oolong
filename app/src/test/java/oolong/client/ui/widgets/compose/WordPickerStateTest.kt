package oolong.client.ui.widgets.compose

import kotlinx.collections.immutable.persistentListOf
import oolong.client.model.PostContent
import oolong.client.model.TextSpan
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class WordPickerStateTest {
    private fun state(vararg pieces: String) =
        WordPickerState(pieces.map { WordPiece(it, isWord = it.any(Char::isLetterOrDigit)) })

    @Test
    fun `text is cut into words and what stands between them, losing nothing`() {
        val text = "第三章结尾那段剧透一下：主角其实没死！"
        val pieces = splitWords(text, Locale.SIMPLIFIED_CHINESE)

        assertEquals(text, pieces.joinToString("") { it.text })
        assertFalse(pieces.single { it.text == "：" }.isWord)
        assertTrue(pieces.count { it.isWord } > 3)
    }

    @Test
    fun `picked pieces next to each other make one word`() {
        val state = state("剧", "透", "一下", "，", "第三", "章")

        state.toggle(0)
        state.toggle(1)
        state.toggle(4)

        assertEquals(listOf("剧透", "第三"), state.words)
    }

    @Test
    fun `a slide picks the words between its ends and leaves the rest as they were`() {
        val state = state("甲", "，", "乙", "丙", "丁")
        state.toggle(4)
        val before = state.pieces.map { it.picked }

        state.set(0..2, picked = true, others = before)
        assertEquals(listOf(true, false, true, false, true), state.pieces.map { it.picked })

        state.set(0..0, picked = true, others = before)
        assertEquals(listOf("甲", "丁"), state.words)
    }

    @Test
    fun `a word splits into its characters, each picked as it was`() {
        val state = state("星穹铁道", "好玩")
        state.toggle(0)

        assertTrue(state.split(0))
        assertEquals(listOf("星", "穹", "铁", "道", "好玩"), state.pieces.map { it.text })
        assertTrue(state.pieces.take(4).all { it.picked })
        assertFalse(state.split(0))
    }

    @Test
    fun `with punctuation, marks can be picked and stay with the word before them`() {
        val pieces = splitWords("没死！真的", Locale.SIMPLIFIED_CHINESE, punctuation = true)
        val mark = pieces.single { it.text == "！" }

        assertTrue(mark.isWord && mark.clings)
        assertEquals("没死！真的", pieces.joinToString("") { it.text })
    }

    @Test
    fun `copying nothing picked takes the whole text`() {
        val state = WordPickerState(copyPieces(content))

        assertEquals("提取码 8f3k#(滑稽)\n@小林 你好", state.copied)
    }

    @Test
    fun `copying keeps what stood between picked pieces and parts further apart by a space`() {
        val state = state("提取码", " ", "8f3k", "，", "记得", "换")
        state.toggle(0)
        state.toggle(2)
        state.toggle(5)

        assertEquals("提取码 8f3k 换", state.copied)
    }

    @Test
    fun `copying keeps marks left unpicked between picked words`() {
        val pieces = splitWords("剧透一下：主角其实没死！真的", Locale.SIMPLIFIED_CHINESE, punctuation = true)
        val state = WordPickerState(pieces)
        state.pieces.forEachIndexed { i, piece -> if (piece.isWord && !piece.clings) state.toggle(i) }

        assertEquals("剧透一下：主角其实没死！真的", state.copied)
    }

    @Test
    fun `links, mentions and emoticons are whole pieces that do not split`() {
        val state = WordPickerState(copyPieces(content))
        val mention = state.pieces.indexOfFirst { it.text == "@小林" }

        assertEquals(WordPiece.Look.Link, state.pieces[mention].look)
        assertFalse(state.split(mention))
        assertEquals(WordPiece.Look.Emoticon, state.pieces.single { it.text == "#(滑稽)" }.look)
    }

    @Test
    fun `picking all picks every word and nothing between`() {
        val state = state("甲", " ", "乙")
        state.pickAll(true)

        assertTrue(state.allPicked)
        assertFalse(state.pieces[1].picked)
        assertEquals("甲 乙", state.copied)
    }

    private val content = listOf(
        PostContent.Text(persistentListOf(TextSpan.Plain("提取码 8f3k"), TextSpan.Emoticon("image_emoticon25", "滑稽"))),
        PostContent.Image("1", persistentListOf(), "", 1, 1, false, 0),
        PostContent.Text(persistentListOf(TextSpan.Mention("@小林", 1L), TextSpan.Plain(" 你好"))),
    )
}
