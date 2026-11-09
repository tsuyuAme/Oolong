package oolong.client.ui.widgets.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.Motion
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.common.theme.compose.TextStyles
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Choosing one of a few [options] side by side (design-system.md §9.1): a capsule track with the
 * chosen one on a pill that moves with `snap`. Held, the pill grows a little past the track and
 * turns to clear glass, as a switch's knob does, and follows the finger across the options; it
 * picks the one it is let go on.
 */
@Composable
fun SegmentedControl(
    options: ImmutableList<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trackColor: Color = ExtendedTheme.colors.chip,
) {
    val colors = ExtendedTheme.colors
    // The page's own colour; on the translucent theme, which has none, that of an opaque surface
    val pillColor = if (colors.background.alpha < 1f) colors.sheet else colors.background
    // Where the pill is, in options; follows the finger while held
    val position = remember { Animatable(selected.toFloat(), Motion.ProgressThreshold) }
    var holding by remember { mutableStateOf(false) }
    LaunchedEffect(selected, holding) {
        if (!holding) position.animateTo(selected.toFloat(), Motion.snap())
    }
    val held by animateFloatAsState(
        targetValue = if (holding) 1f else 0f,
        animationSpec = if (holding) Motion.press() else Motion.release(),
        label = "segmentHeld",
    )
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val currentSelected by rememberUpdatedState(selected)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val glassState = rememberHazeState()
    val glassAvailable = isGlassAvailable()
    var trackSize by remember { mutableStateOf(IntSize.Zero) }
    val inset = with(LocalDensity.current) { TrackInset.toPx() }
    val count = options.size
    // The option under the pill, so the labels follow it while it is dragged
    val highlighted = position.value.roundToInt()

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .onSizeChanged { trackSize = it }
                .clip(Corners.full)
                // What the held pill sees: the track, its labels, and the pill as it fades, so what
                // it bends from past its edge is the track rather than loose labels
                .hazeSource(glassState)
                .background(trackColor)
                .drawBehind {
                    val width = (size.width - inset * 2) / count
                    val height = size.height - inset * 2
                    drawRoundRect(
                        // The pill gives way to the glass while held, where glass is drawn
                        color = pillColor.copy(alpha = if (glassAvailable) 1f - held else 1f),
                        topLeft = Offset(inset + width * position.value, inset),
                        size = Size(width, height),
                        cornerRadius = CornerRadius(height / 2),
                    )
                }
                .pointerInput(count) {
                    val width = (size.width - inset * 2) / count
                    fun optionAt(x: Float) = ((x - inset) / width - 0.5f).coerceIn(0f, (count - 1).toFloat())
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        holding = true
                        scope.launch { position.animateTo(optionAt(down.position.x).roundToInt().toFloat(), Motion.snap()) }
                        var dragging = false
                        var last = position.value.roundToInt()
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                val target = if (dragging) position.value.roundToInt() else optionAt(change.position.x).roundToInt()
                                holding = false
                                if (target != currentSelected) currentOnSelect(target)
                                scope.launch { position.animateTo(target.toFloat(), Motion.snap()) }
                                return@awaitEachGesture
                            }
                            if (change.isConsumed) break
                            val moved = change.position - down.position
                            if (!dragging && (abs(moved.x) > touchSlop || abs(moved.y) > touchSlop)) {
                                // Up and down is the page's to scroll
                                if (abs(moved.y) > abs(moved.x)) break
                                dragging = true
                            }
                            if (dragging) {
                                change.consume()
                                val at = optionAt(change.position.x)
                                scope.launch { position.snapTo(at) }
                                if (at.roundToInt() != last) {
                                    last = at.roundToInt()
                                    haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                }
                            }
                        }
                        // Taken by a scroll, or let go elsewhere: the pill goes back
                        holding = false
                    }
                }
                .padding(TrackInset),
        ) {
            options.forEachIndexed { index, option ->
                val chosen = index == selected
                val shown = index == highlighted
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 32.dp)
                        .semantics(mergeDescendants = true) {
                            role = Role.Tab
                            this.selected = chosen
                            onClick {
                                if (!chosen) onSelect(index)
                                true
                            }
                        }
                        .padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = option,
                        style = TextStyles.label,
                        fontWeight = if (shown) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (shown) colors.text else colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (glassAvailable && held > 0f && trackSize != IntSize.Zero) {
            // The held pill, over the labels so it magnifies them; outside the track's size, which
            // it grows past
            Box(modifier = Modifier.matchParentSize()) {
                val density = LocalDensity.current
                val width = (trackSize.width - inset * 2) / count
                val height = trackSize.height - inset * 2
                val growWidth = with(density) { HeldGrowthWidth.toPx() } * held
                val growHeight = with(density) { HeldGrowthHeight.toPx() } * held
                val optics = remember(height, density) { liquidLensOptics(with(density) { (height.toFloat()).toDp() + HeldGrowthHeight }) }
                Box(
                    modifier = Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .offset {
                            IntOffset(
                                (inset + width * position.value - growWidth / 2).roundToInt(),
                                (inset - growHeight / 2).roundToInt(),
                            )
                        }
                        .requiredSize(with(density) { (width + growWidth).toDp() }, with(density) { (height + growHeight).toDp() })
                        // Bent as the navigation bar's lens is, for its size
                        .controlLens(glassState, PillShape, held, optics)
                )
            }
        }
    }
}

private val TrackInset = 3.dp

private val PillShape = SmoothCornerShape.Capsule

/** How much the pill grows while held, as a switch's knob does. */
private val HeldGrowthWidth = 16.dp
private val HeldGrowthHeight = 14.dp
