package oolong.client.ui.widgets.compose

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.Motion
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import kotlin.math.roundToInt

/**
 * A switch (design-system.md §9.1): a filled capsule track with a white pill knob. Held, the knob
 * grows a little past the track and turns to clear glass; it can be dragged across or tapped.
 *
 * It adds no touch area of its own: it sits in a row that toggles it when tapped.
 */
@Composable
fun Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val dragged by source.collectIsDraggedAsState()
    val held by animateFloatAsState(
        targetValue = if (enabled && (pressed || dragged)) 1f else 0f,
        animationSpec = if (pressed || dragged) Motion.press() else Motion.release(),
        label = "held",
    )

    val haptics = LocalHapticFeedback.current
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    val change: (Boolean) -> Unit = { value ->
        haptics.performHapticFeedback(if (value) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
        currentOnCheckedChange?.invoke(value)
    }

    // 0 off, 1 on; follows the finger while dragged
    val position = remember { Animatable(if (checked) 1f else 0f, visibilityThreshold = Motion.ProgressThreshold) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(checked, dragging) {
        if (!dragging) position.animateTo(if (checked) 1f else 0f, Motion.snap())
    }
    val scope = rememberCoroutineScope()
    val travelPx = with(LocalDensity.current) { KnobTravel.toPx() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val dragState = rememberDraggableState { delta ->
        val d = if (rtl) -delta else delta
        scope.launch { position.snapTo((position.value + d / travelPx).coerceIn(0f, 1f)) }
    }

    val colors = ExtendedTheme.colors
    val offTrack = colors.text.copy(alpha = if (colors.isNightMode) 0.22f else 0.12f)
    // The track shows the state, not where the knob is dragged to
    val trackOn by animateFloatAsState(if (checked) 1f else 0f, Motion.snap(), label = "track")
    val track = lerp(offTrack, colors.emphasis, trackOn)
    val glassState = rememberHazeState()
    val glassAvailable = isGlassAvailable()

    Box(
        modifier
            .then(
                if (onCheckedChange != null) {
                    Modifier
                        .toggleable(
                            value = checked,
                            onValueChange = change,
                            enabled = enabled,
                            role = Role.Switch,
                            interactionSource = source,
                            indication = null,
                        )
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Horizontal,
                            enabled = enabled,
                            interactionSource = source,
                            onDragStarted = { dragging = true },
                            onDragStopped = {
                                val on = position.value > 0.5f
                                dragging = false
                                if (on != checked) change(on)
                            },
                        )
                } else {
                    Modifier
                }
            )
            .size(TrackWidth, TrackHeight)
            .graphicsLayer { alpha = if (enabled) 1f else DisabledAlpha },
        contentAlignment = Alignment.CenterStart,
    ) {
        // What the held knob sees: the track, and clear space around it as far as the knob reaches
        Box(
            Modifier
                .requiredSize(TrackWidth + HeldGrowthWidth, TrackHeight + HeldGrowthHeight)
                .hazeSource(glassState),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(TrackWidth, TrackHeight)
                    // Filled in its shape rather than clipped: a clip's edge is anti-aliased coarsely, or not at all, on some GPUs
                    .background(track, Corners.full)
            )
        }
        val growWidth = HeldGrowthWidth * held
        val knobWidth = KnobWidth + growWidth
        val knobHeight = KnobHeight + HeldGrowthHeight * held
        val p = if (rtl) 1f - position.value else position.value
        val knobShape = SmoothCornerShape.Capsule
        Box(
            Modifier
                .offset {
                    val x = KnobInset.toPx() + KnobTravel.toPx() * p - growWidth.toPx() / 2
                    IntOffset(x.roundToInt(), 0)
                }
                .requiredSize(knobWidth, knobHeight)
                .shadow(KnobElevation, knobShape, clip = false, ambientColor = KnobAmbient, spotColor = KnobSpot)
                // The white knob fades out as the glass fades in, where glass is drawn
                .background(Color.White.copy(alpha = if (glassAvailable) 1f - held else 1f), knobShape)
                .controlLens(glassState, knobShape, held)
        )
    }
}

private val TrackWidth = 50.dp
private val TrackHeight = 26.dp
private val KnobWidth = 30.dp
private val KnobHeight = 20.dp
private val KnobInset = (TrackHeight - KnobHeight) / 2
private val KnobTravel = TrackWidth - KnobWidth - KnobInset * 2

/** How much the knob grows while held, standing out past the track like the navigation lens. */
private val HeldGrowthWidth = 16.dp
private val HeldGrowthHeight = 14.dp

private val KnobElevation = 2.dp
private val KnobAmbient = Color.Black.copy(alpha = 0.12f)
private val KnobSpot = Color.Black.copy(alpha = 0.24f)

private const val DisabledAlpha = 0.38f
