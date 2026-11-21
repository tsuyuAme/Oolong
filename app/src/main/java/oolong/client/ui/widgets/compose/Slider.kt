package oolong.client.ui.widgets.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.Motion
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import kotlin.math.roundToInt

/**
 * A slider (design-system.md §9.1): a thin track filled up to a white pill knob, the knob of
 * [Switch]. Held or dragged, the knob grows a little past the track and turns to clear glass.
 *
 * With [steps], the value moves between `steps + 2` evenly spaced values, giving a tick at each.
 * [onValueChangeFinished] runs when the finger lifts.
 */
@Composable
fun Slider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val dragged by source.collectIsDraggedAsState()
    val held by animateFloatAsState(
        targetValue = if (enabled && (pressed || dragged)) 1f else 0f,
        animationSpec = if (pressed || dragged) Motion.press() else Motion.release(),
        label = "held",
    )

    val haptics = LocalHapticFeedback.current
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)
    val span = valueRange.endInclusive - valueRange.start
    fun fractionOf(v: Float) = if (span == 0f) 0f else ((v - valueRange.start) / span).coerceIn(0f, 1f)
    fun snapped(fraction: Float): Float {
        if (steps <= 0) return fraction
        val intervals = steps + 1
        return (fraction * intervals).roundToInt().toFloat() / intervals
    }

    // Where the knob is drawn, 0 at the start; follows the finger while held, springs to the value
    // when it changes from outside
    val position = remember { Animatable(fractionOf(value), visibilityThreshold = Motion.ProgressThreshold) }
    var tracking by remember { mutableStateOf(false) }
    // The finger's unsnapped place, so a stepped slider moves when the finger passes halfway
    var raw by remember { mutableStateOf(fractionOf(value)) }
    LaunchedEffect(value, tracking) {
        if (!tracking) position.animateTo(fractionOf(value), Motion.snap())
    }
    val scope = rememberCoroutineScope()
    fun moveTo(fraction: Float) {
        raw = fraction.coerceIn(0f, 1f)
        val next = snapped(raw)
        if (next != position.value) {
            if (steps > 0) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
            scope.launch { position.snapTo(next) }
            currentOnValueChange(valueRange.start + next * span)
        }
    }

    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val colors = ExtendedTheme.colors
    val offTrack = colors.text.copy(alpha = if (colors.isNightMode) 0.22f else 0.12f)
    val glassState = rememberHazeState()
    val glassAvailable = isGlassAvailable()

    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(TouchHeight)
            .semantics {
                if (!enabled) disabled()
                progressBarRangeInfo = ProgressBarRangeInfo(value.coerceIn(valueRange), valueRange, steps)
                setProgress { target ->
                    val fraction = snapped(fractionOf(target))
                    currentOnValueChange(valueRange.start + fraction * span)
                    currentOnValueChangeFinished?.invoke()
                    true
                }
            }
            .graphicsLayer { alpha = if (enabled) 1f else DisabledAlpha },
        contentAlignment = Alignment.CenterStart,
    ) {
        val density = LocalDensity.current
        // The knob's centre travels between the knob's half widths, so it never leaves the track
        val travel = maxWidth - KnobWidth
        val travelPx = with(density) { travel.toPx() }
        val startPx = with(density) { (KnobWidth / 2).toPx() }
        val reachPx = with(density) { KnobReach.toPx() }
        fun fractionAt(x: Float): Float {
            val f = (x - startPx) / travelPx
            return if (rtl) 1f - f else f
        }
        val dragState = rememberDraggableState { delta ->
            moveTo(raw + (if (rtl) -delta else delta) / travelPx)
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(TouchHeight)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onPress = { offset ->
                            val press = PressInteraction.Press(offset)
                            source.emit(press)
                            val released = tryAwaitRelease()
                            source.emit(if (released) PressInteraction.Release(press) else PressInteraction.Cancel(press))
                        },
                        onTap = { offset ->
                            tracking = true
                            moveTo(fractionAt(offset.x))
                            tracking = false
                            currentOnValueChangeFinished?.invoke()
                        },
                    )
                }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = enabled,
                    interactionSource = source,
                    onDragStarted = { offset ->
                        tracking = true
                        raw = position.value
                        // Grabbing the track away from the knob brings the knob to the finger
                        val at = fractionAt(offset.x)
                        if (kotlin.math.abs(at - position.value) * travelPx > reachPx) moveTo(at)
                    },
                    onDragStopped = {
                        tracking = false
                        currentOnValueChangeFinished?.invoke()
                    },
                )
        )

        // What the held knob sees: the track, and clear space around it as far as the knob reaches
        Box(
            Modifier
                .fillMaxWidth()
                .height(KnobHeight + HeldGrowthHeight)
                .hazeSource(glassState),
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .padding(horizontal = KnobWidth / 2 - TrackHeight / 2)
                    .fillMaxWidth()
                    .height(TrackHeight)
                    .drawBehind {
                        val radius = CornerRadius(size.height / 2)
                        drawRoundRect(offTrack, cornerRadius = radius)
                        // Filled from the start to the knob's centre
                        val filled = size.height + (size.width - size.height) * position.value
                        drawRoundRect(
                            colors.emphasis,
                            topLeft = Offset(if (rtl) size.width - filled else 0f, 0f),
                            size = Size(filled, size.height),
                            cornerRadius = radius,
                        )
                    }
            )
        }

        val growWidth = HeldGrowthWidth * held
        val knobWidth = KnobWidth + growWidth
        val knobHeight = KnobHeight + HeldGrowthHeight * held
        val knobShape = SmoothCornerShape.Capsule
        Box(
            Modifier
                .offset {
                    // offset mirrors itself in right-to-left layouts, measuring from the start
                    val x = travelPx * position.value - growWidth.toPx() / 2
                    IntOffset(x.roundToInt(), 0)
                }
                .requiredSize(knobWidth, knobHeight)
                .shadow(KnobElevation, knobShape, clip = false, ambientColor = KnobAmbient, spotColor = KnobSpot)
                // The white knob fades out as the glass fades in, where glass is drawn
                .background(Color.White.copy(alpha = if (glassAvailable) 1f - held else 1f), knobShape)
                // Unlike the switch's, this knob mostly sits on the page, where white alone is lost
                .border(Dp.Hairline, KnobRing.copy(alpha = KnobRing.alpha * (1f - held)), knobShape)
                .controlLens(glassState, knobShape, held)
        )
    }
}

private val TouchHeight = 40.dp
private val TrackHeight = 6.dp
private val KnobWidth = 30.dp
private val KnobHeight = 20.dp

/** How much the knob grows while held, as the switch's does. */
private val HeldGrowthWidth = 16.dp
private val HeldGrowthHeight = 14.dp

/** How far from the knob a drag may start and still pick the knob up where it is. */
private val KnobReach = 24.dp

private val KnobElevation = 3.dp
private val KnobRing = Color.Black.copy(alpha = 0.12f)
private val KnobAmbient = Color.Black.copy(alpha = 0.12f)
private val KnobSpot = Color.Black.copy(alpha = 0.24f)

private const val DisabledAlpha = 0.38f
