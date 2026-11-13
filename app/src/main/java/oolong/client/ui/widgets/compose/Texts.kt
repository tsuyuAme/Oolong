package oolong.client.ui.widgets.compose

import android.util.LruCache
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.layout.ApproachLayoutModifierNode
import androidx.compose.ui.layout.ApproachMeasureScope
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import oolong.client.R
import oolong.client.pxToDp
import oolong.client.pxToSp
import oolong.client.ui.common.PbContentText
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.Symbols
import oolong.client.utils.EmoticonManager
import oolong.client.utils.EmoticonUtil
import oolong.client.utils.EmoticonUtil.emoticonString
import oolong.client.utils.calcLineHeightPx
import java.util.regex.Pattern

@Composable
fun EmoticonText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    lineSpacing: TextUnit = 0.sp,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val emoticonString = remember(key1 = text) { text.emoticonString }

    EmoticonText(
        emoticonString,
        modifier,
        color,
        fontSize,
        fontStyle,
        fontWeight,
        fontFamily,
        letterSpacing,
        textDecoration,
        textAlign,
        lineHeight,
        lineSpacing,
        overflow,
        softWrap,
        maxLines,
        minLines,
        inlineContent = emptyMap(),
        onTextLayout = onTextLayout,
        style = style
    )
}

@Composable
fun EmoticonText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    lineSpacing: TextUnit = 0.sp,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    emoticonSize: Float = 0.9f,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val textColor = color.takeOrElse {
        style.color.takeOrElse {
            LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
        }
    }
    val mergedStyle = style.merge(
        TextStyle(
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing
        )
    )
    val sizePx = calcLineHeightPx(mergedStyle)
    // As a multiple of the font size: back in sp it would be scaled up again where large text
    // grows less than small text, so a line would end up taller than measured
    val density = LocalDensity.current
    val spacingLineHeight = remember(sizePx, lineSpacing, density, mergedStyle.fontSize) {
        with(density) {
            val fontPx = if (mergedStyle.fontSize.isSpecified) mergedStyle.fontSize.toPx() else 0f
            if (fontPx > 0f) ((sizePx + lineSpacing.toPx()) / fontPx).em else (sizePx + lineSpacing.toPx()).toSp()
        }
    }
    val emoticonInlineContent = remember(text, sizePx, emoticonSize, density) {
        EmoticonManager.getEmoticonInlineContent(EmoticonUtil.emoticonIds(text), sizePx * emoticonSize, density)
    }
    IconText(
        text,
        modifier,
        color,
        fontSize,
        fontStyle,
        fontWeight,
        fontFamily,
        letterSpacing,
        textDecoration,
        textAlign,
        spacingLineHeight,
        overflow,
        softWrap,
        maxLines,
        minLines,
        emoticonInlineContent + inlineContent,
        onTextLayout,
        style
    )
}

@Composable
fun IconText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {
    val textColor = color.takeOrElse {
        style.color.takeOrElse {
            LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
        }
    }
    val mergedStyle = style.merge(
        TextStyle(
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign ?: TextAlign.Unspecified,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing
        )
    )
    val sizePx = calcLineHeightPx(mergedStyle) * 9 / 10
    val sizeSp = sizePx.pxToSp(LocalContext.current).sp
    val sizeDp = sizePx.pxToDp().dp
    val iconInlineContent =
        remember(sizeSp) {
            mapOf(
                "link_icon" to InlineTextContent(
                    placeholder = Placeholder(
                        width = sizeSp,
                        height = sizeSp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    ),
                    children = {
                        Icon(
                            Symbols.Link,
                            contentDescription = stringResource(id = R.string.link),
                            modifier = Modifier.size(sizeDp),
                            tint = ExtendedTheme.colors.accent,
                        )
                    }
                ),
                "video_icon" to InlineTextContent(
                    placeholder = Placeholder(
                        width = sizeSp,
                        height = sizeSp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    ),
                    children = {
                        Icon(
                            Symbols.SlowMotionVideo,
                            contentDescription = stringResource(id = R.string.desc_video),
                            modifier = Modifier.size(sizeDp),
                            tint = ExtendedTheme.colors.accent,
                        )
                    }
                ),
                "photo_icon" to InlineTextContent(
                    placeholder = Placeholder(
                        width = sizeSp,
                        height = sizeSp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    ),
                    children = {
                        Icon(
                            Symbols.Photo,
                            contentDescription = stringResource(id = R.string.desc_photo),
                            modifier = Modifier.size(sizeDp),
                            tint = ExtendedTheme.colors.accent,
                        )
                    }
                ),
                "user_icon" to InlineTextContent(
                    placeholder = Placeholder(
                        width = sizeSp,
                        height = sizeSp,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
                    ),
                    children = {
                        Icon(
                            Symbols.AccountCircle,
                            contentDescription = stringResource(id = R.string.user),
                            modifier = Modifier.size(sizeDp),
                            tint = ExtendedTheme.colors.accent,
                        )
                    }
                ),
            )
        }
    Text(
        text,
        modifier.inlineContentSafe(),
        color,
        fontSize,
        fontStyle,
        fontWeight,
        fontFamily,
        letterSpacing,
        textDecoration,
        textAlign,
        lineHeight,
        overflow,
        softWrap,
        maxLines,
        minLines,
        iconInlineContent + inlineContent,
        onTextLayout,
        style
    )
}

/**
 * A [Text] composable that supports setting its minimum width to width of the specified number of Chinese characters
 */
@Composable
fun TextWithMinWidth(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    minLength: Int = 0,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
) {
    val textMeasurer = rememberTextMeasurer()
    val singleChar = stringResource(id = R.string.single_chinese_char)
    val singleCharWidth = remember(style) {
        textMeasurer.measure(
            text = singleChar,
            style = style
        ).size.width.pxToDp().dp
    }

    Text(
        text = text,
        modifier = Modifier
            .defaultMinSize(minWidth = singleCharWidth * minLength)
            .then(modifier),
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

/** Sizes of short texts measured on their own, kept for the whole app: each row of a list asks for the same ones. */
private val measuredTextSizes = LruCache<MeasuredTextKey, IntSize>(64)

private data class MeasuredTextKey(
    val text: String,
    val style: TextStyle,
    val density: Float,
    val fontScale: Float,
    val layoutDirection: LayoutDirection,
)

/** The size [text] takes alone in [style], measured once for all who ask. */
@Composable
fun rememberTextSize(text: String, style: TextStyle): IntSize {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    return remember(text, style, density, layoutDirection, fontFamilyResolver) {
        val key = MeasuredTextKey(text, style, density.density, density.fontScale, layoutDirection)
        measuredTextSizes.get(key) ?: TextMeasurer(fontFamilyResolver, density, layoutDirection, cacheSize = 0)
            .measure(text, style)
            .size
            .also { measuredTextSizes.put(key, it) }
    }
}

@Composable
fun buildChipInlineContent(
    text: String,
    padding: PaddingValues = PaddingValues(vertical = 2.dp, horizontal = 4.dp),
    textStyle: TextStyle = LocalTextStyle.current,
    chipTextStyle: TextStyle = LocalTextStyle.current,
    backgroundColor: Color = ExtendedTheme.colors.chip,
    color: Color = ExtendedTheme.colors.onChip
): InlineTextContent {
    val textSize = rememberTextSize(text, textStyle)
    val layoutDirection = LocalLayoutDirection.current
    // All in the density the text is measured and drawn in, whose text scale is the app's own
    val (heightSp, fontSize, widthSp) = with(LocalDensity.current) {
        val textHeightPx = textStyle.fontSize.toPx() -
            padding.calculateTopPadding().toPx() -
            padding.calculateBottomPadding().toPx()
        val widthPx = textSize.width +
            padding.calculateStartPadding(layoutDirection).toPx() +
            padding.calculateEndPadding(layoutDirection).toPx()
        Triple(textSize.height.toSp(), textHeightPx.toSp(), widthPx.toSp())
    }
    return InlineTextContent(
        placeholder = Placeholder(
            width = widthSp,
            height = heightSp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
        ),
        children = {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = it.takeIf { it.isNotBlank() && it != "\uFFFD" } ?: text,
                    style = chipTextStyle.copy(
                        fontSize = fontSize,
                        lineHeight = fontSize,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both
                        )
                    ),
                    textAlign = TextAlign.Center,
                    color = color,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Corners.full)
                        .background(backgroundColor)
                        .padding(padding)
                )
            }
        }
    )
}

@Composable
fun HighlightText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    lineSpacing: TextUnit = 0.sp,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    emoticonSize: Float = 0.9f,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    highlightKeywords: List<String> = emptyList(),
    highlightColor: Color = ExtendedTheme.colors.emphasis,
    highlightStyle: TextStyle = style,
) {
    HighlightText(
        text = remember(text) { text.emoticonString },
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        lineSpacing = lineSpacing,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        emoticonSize = emoticonSize,
        onTextLayout = onTextLayout,
        style = style,
        highlightKeywords = highlightKeywords,
        highlightColor = highlightColor,
        highlightStyle = highlightStyle,
    )
}

@Composable
fun HighlightText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    lineSpacing: TextUnit = 0.sp,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    emoticonSize: Float = 0.9f,
    inlineContent: Map<String, InlineTextContent> = emptyMap(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current,
    highlightKeywords: List<String> = emptyList(),
    highlightColor: Color = ExtendedTheme.colors.emphasis,
    highlightStyle: TextStyle = style,
) {
    val mergedHighlightStyle = remember(highlightStyle, highlightColor) {
        highlightStyle.copy(color = highlightColor)
    }
    val highlightText = remember(text, highlightKeywords) {
        if (highlightKeywords.isEmpty()) {
            text
        } else {
            buildAnnotatedString {
                append(text)
                highlightKeywords.forEach { keyword ->
                    val regexPattern = keyword.toPattern(Pattern.CASE_INSENSITIVE)
                    val matcher = regexPattern.matcher(text.text)
                    while (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        addStyle(
                            mergedHighlightStyle.toSpanStyle(),
                            start,
                            end
                        )
                    }
                }
            }
        }
    }
    PbContentText(
        text = highlightText,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        lineSpacing = lineSpacing,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        emoticonSize = emoticonSize,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        style = style,
    )
}

/**
 * Lays a text with inline content out alike in the lookahead and the approach pass, as the
 * lookahead found it, for a text whose pieces of inline content are measured in only one pass
 * crashes in the other.
 *
 * A text measures the inline content its layout keeps and leaves out what an ellipsis cuts. Where
 * the two passes give it different widths, as a page resized under a shared transition does when
 * the screen turns, a piece kept in the approach but cut in the lookahead is placed there unmeasured.
 */
fun Modifier.inlineContentSafe(): Modifier = this then LookaheadConstraintsElement

private data object LookaheadConstraintsElement : ModifierNodeElement<LookaheadConstraintsNode>() {
    override fun create() = LookaheadConstraintsNode()
    override fun update(node: LookaheadConstraintsNode) = Unit
    override fun InspectorInfo.inspectableProperties() {
        name = "inlineContentSafe"
    }
}

private class LookaheadConstraintsNode : Modifier.Node(), ApproachLayoutModifierNode {
    private var lookahead: Constraints? = null

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        lookahead = constraints
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

    override fun isMeasurementApproachInProgress(lookaheadSize: IntSize): Boolean = false

    override fun ApproachMeasureScope.approachMeasure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(lookahead ?: constraints)
        return layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
}
