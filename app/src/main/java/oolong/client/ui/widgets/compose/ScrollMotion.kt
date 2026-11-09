package oolong.client.ui.widgets.compose

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import oolong.client.ui.common.theme.compose.Motion

/*
 * Scroll-driven motion. The content scrolls as it always does; what reacts to it reads how far it
 * has gone, never takes the scroll for itself. A [ScrollProgress] turns the scroll into 0..1, and
 * what moves reads it where it draws, so scrolling redraws but neither recomposes nor lays out.
 */

/**
 * How far along a scroll-driven change is, from 0 to 1. Read [value] or [fraction] in a draw
 * phase, such as a `graphicsLayer` block, so the scroll only redraws what follows it.
 */
@Stable
class ScrollProgress internal constructor(private val read: () -> Float) {
    val value: Float get() = read()

    /**
     * How far along [range] of the whole the progress is, eased by [easing]: 0 before it, 1 after.
     * Lets parts of one change keep their own pace, like a detail that fades in its first 40%.
     */
    fun fraction(range: ClosedFloatingPointRange<Float>, easing: Easing = LinearEasing): Float {
        val span = range.endInclusive - range.start
        val linear = if (span > 0f) ((value - range.start) / span).coerceIn(0f, 1f) else if (value >= range.start) 1f else 0f
        return easing.transform(linear)
    }
}

/**
 * Progress through [range] of the page's scroll: 0 with the content scrolled to [range]'s start
 * or less, 1 at its end or beyond. With [snapping], content let go inside the range scrolls on to
 * the nearer end.
 */
@Composable
fun rememberScrollProgress(
    range: ClosedRange<Dp>,
    snapping: Boolean = false,
    page: PageScrollState? = LocalPageScroll.current,
): ScrollProgress {
    val start = with(LocalDensity.current) { range.start.toPx() }
    val end = with(LocalDensity.current) { range.endInclusive.toPx() }
    return rememberScrollProgress(start..end, snapping, page)
}

/** [rememberScrollProgress] over a range in pixels. */
@Composable
internal fun rememberScrollProgress(
    range: ClosedFloatingPointRange<Float>,
    snapping: Boolean,
    page: PageScrollState? = LocalPageScroll.current,
): ScrollProgress {
    if (snapping && page != null) {
        DisposableEffect(page, range) {
            page.addSnapRange(range)
            onDispose { page.removeSnapRange(range) }
        }
    }
    return remember(page, range) {
        ScrollProgress {
            val scrolled = page?.scrolled ?: return@ScrollProgress 0f
            val span = range.endInclusive - range.start
            if (span > 0f) ((scrolled - range.start) / span).coerceIn(0f, 1f) else 1f
        }
    }
}

/**
 * Progress of the element marked with [matchedGeometryAnchor] under [anchor] into the top bar's
 * row: 0 while its middle is below the bar, 1 once it reaches the middle of the row. For something
 * that docks in the bar as it scrolls up to it, wherever it is on the page.
 */
@Composable
fun rememberScrollProgress(anchor: Any, page: PageScrollState? = LocalPageScroll.current): ScrollProgress {
    return remember(page, anchor) {
        ScrollProgress {
            val bounds = page?.anchors?.get(anchor) ?: return@ScrollProgress 0f
            val rowTop = page.barRowTop
            val rowBottom = page.barRowBottom
            if (rowTop.isNaN() || rowBottom.isNaN()) return@ScrollProgress 0f
            val rowCenter = (rowTop + rowBottom) / 2f
            ((rowBottom - bounds.center.y) / (rowBottom - rowCenter)).coerceIn(0f, 1f)
        }
    }
}

/**
 * Marks where something matched by [matchedGeometry] under [key] starts from. Draws nothing of
 * its own beyond its content; it only reports its bounds. An anchor that scrolls out and is
 * disposed is remembered where it was last seen.
 */
@Composable
fun Modifier.matchedGeometryAnchor(key: Any): Modifier {
    val page = LocalPageScroll.current ?: return this
    return onGloballyPositioned { page.anchors[key] = page.boundsOf(it) }
}

/**
 * Draws this where the [matchedGeometryAnchor] under [key] is at [progress] 0, and where it is
 * laid out at 1, moving and scaling in between, its height matched to the anchor's. Only how it is
 * drawn changes, so following the scroll redraws it but does not lay it out. Until its anchor has
 * been seen, it is not drawn.
 */
@Composable
fun Modifier.matchedGeometry(key: Any, progress: ScrollProgress): Modifier {
    val page = LocalPageScroll.current ?: return this
    val own = remember { mutableStateOf(Rect.Zero) }
    return onGloballyPositioned { own.value = page.boundsOf(it) }
        .graphicsLayer {
            val anchor = page.anchors[key]
            val self = own.value
            if (anchor == null || self.height <= 0f) {
                alpha = 0f
                return@graphicsLayer
            }
            val p = progress.value
            val scale = lerp(anchor.height / self.height, 1f, p)
            transformOrigin = TransformOrigin(0f, 0f)
            scaleX = scale
            scaleY = scale
            translationX = lerp(anchor.left - self.left, 0f, p)
            translationY = lerp(anchor.top - self.top, 0f, p)
        }
}

/**
 * Progress that follows which way the page's content is scrolling rather than where it is: it
 * goes towards 1 as the content scrolls on and back towards 0 as it scrolls back, and back to 0
 * when it is pulled past its top. For chrome that leaves as the content goes on and returns as
 * soon as it comes back.
 *
 * Over [away] of scroll it follows the scroll, and with [snapping] settles at whichever end is
 * nearer once the scroll stops. With [toggle], it instead flips to 1 once the content has gone on
 * by [away], and back once it has come back by as much, animating between: for a change that
 * should not stop halfway, like a bar folding to its icons; that state is kept while the page is
 * away, as under another page. While [enabled] is false it stays at 0. [page] is the page to
 * follow, by default the one this is part of; hoist it with [rememberPageScrollState] to use it
 * outside the page.
 */
@Composable
fun rememberScrollProgress(
    away: Dp,
    snapping: Boolean = true,
    toggle: Boolean = false,
    enabled: () -> Boolean = { true },
    page: PageScrollState? = LocalPageScroll.current,
): ScrollProgress {
    val distance = with(LocalDensity.current) { away.toPx() }
    val scope = rememberCoroutineScope()
    val currentEnabled by rememberUpdatedState(enabled)
    // Where a toggle was, as a fraction, to come back to after the page has been away
    var savedToggle by rememberSaveable { mutableFloatStateOf(0f) }
    val progress = remember(page, distance, snapping, toggle) {
        DirectedProgress(distance, snapping, toggle, scope, initial = if (toggle) savedToggle else 0f) { currentEnabled() }
    }
    if (toggle) {
        LaunchedEffect(progress) { snapshotFlow { progress.value }.collect { savedToggle = it } }
    }
    if (page != null) {
        DisposableEffect(page, progress) {
            page.add(progress)
            onDispose { page.remove(progress) }
        }
    }
    // Disabled by a scroll the page doesn't watch, such as one to the top by code, it still goes back
    LaunchedEffect(progress) {
        snapshotFlow { currentEnabled() }.collect { if (!it) progress.reset() }
    }
    return remember(progress) { ScrollProgress { progress.value } }
}

/** The state behind a [rememberScrollProgress] that follows the scroll's direction. */
internal class DirectedProgress(
    private val distance: Float,
    private val snapping: Boolean,
    private val toggle: Boolean,
    private val scope: CoroutineScope,
    /** Where it starts, as a fraction of [distance]. */
    initial: Float = 0f,
    private val enabled: () -> Boolean,
) {
    private var offset by mutableFloatStateOf(initial.coerceIn(0f, 1f) * distance)
    private var animating: Job? = null
    private var animatingTo = Float.NaN

    /** Scroll gone on (negative) or back (positive) since the last flip, in toggle mode. */
    private var run = 0f

    val value: Float get() = if (distance > 0f) offset / distance else 0f

    private fun animateTo(target: Float) {
        // Every scroll past the distance asks again; started over each time, it would never get
        // past its first frame until the scroll stopped
        if (animating?.isActive == true && animatingTo == target) return
        animating?.cancel()
        if (offset == target) return
        animatingTo = target
        animating = scope.launch {
            animate(offset, target, animationSpec = Motion.settle()) { value, _ -> offset = value }
        }
    }

    /** Goes back to 0, as while disabled. */
    fun reset() {
        run = 0f
        animateTo(0f)
    }

    fun onScroll(consumed: Float, available: Float) {
        if (!enabled()) {
            reset()
            return
        }
        if (consumed == 0f) {
            // Pulled on past the content's top
            if (available > 0f) {
                run = 0f
                animateTo(0f)
            }
            return
        }
        if (toggle) {
            if ((consumed > 0f) != (run > 0f)) run = 0f
            run += consumed
            if (run < -distance) animateTo(distance) else if (run > distance) animateTo(0f)
        } else {
            animating?.cancel()
            offset = (offset - consumed).coerceIn(0f, distance)
        }
    }

    fun onScrollEnd() {
        if (!toggle && snapping && offset > 0f && offset < distance) {
            animateTo(if (offset > distance / 2) distance else 0f)
        }
    }
}
