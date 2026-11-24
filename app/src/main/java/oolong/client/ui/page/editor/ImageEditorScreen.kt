package oolong.client.ui.page.editor

import android.graphics.Bitmap
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import oolong.client.R
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.Motion
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.common.theme.compose.Symbols
import oolong.client.ui.widgets.compose.Button
import oolong.client.ui.widgets.compose.GlassScaffold
import oolong.client.ui.widgets.compose.GlassTopBar
import oolong.client.ui.widgets.compose.TextButton
import oolong.client.ui.widgets.compose.floatingGlass
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** How far past filling the frame a picture can be zoomed. */
private const val MaxZoom = 8f

/** The frame's least gap to the sides and to the bars. */
private val FrameMargin = 20.dp

/**
 * The editor: [picture] under a frame of [aspect], width over height, moved and zoomed with the
 * fingers; outside the frame is dimmed. Cancel and done are on the bar above, the tools along the
 * bottom. Done gives where the picture lies in the frame.
 */
@Composable
fun ImageEditorScreen(
    picture: Bitmap?,
    aspect: Float,
    round: Boolean,
    saving: Boolean,
    onCancel: () -> Unit,
    onDone: (Crop) -> Unit,
    pictureHidden: Boolean = false,
    onFrame: (Rect) -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val state = remember { CropState(scope) }
    GlassScaffold(
        backgroundColor = Color.Black,
        topBar = {
            GlassTopBar(
                // No title: the picture is the page
                title = {},
                navigationIcon = {
                    TextButton(onClick = onCancel, color = ExtendedTheme.colors.text) {
                        Text(text = stringResource(id = R.string.button_cancel))
                    }
                },
                actionsInCapsule = false,
                actions = {
                    Button(
                        onClick = { state.crop()?.let(onDone) },
                        enabled = picture != null && !saving,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Text(text = stringResource(id = R.string.button_finish))
                    }
                },
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                EditorTools {
                    EditorTool(Symbols.RotateLeft, stringResource(id = R.string.button_rotate), enabled = picture != null) {
                        state.rotate()
                    }
                    EditorTool(Symbols.RestartAlt, stringResource(id = R.string.button_restore), enabled = state.changed) {
                        state.reset()
                    }
                }
            }
        },
    ) { padding ->
        val image = remember(picture) { picture?.asImageBitmap() }
        if (image != null) {
            CropArea(image = image, aspect = aspect, round = round, state = state, padding = padding, hidden = pictureHidden, onFrame = onFrame)
        }
    }
}

/** The editor's tools, side by side on one piece of glass. */
@Composable
private fun EditorTools(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .floatingGlass(SmoothCornerShape.Capsule, barButton = false, pressable = true)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) { content() }
}

@Composable
private fun EditorTool(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (enabled) 1f else 0.38f, Motion.snap(), label = "tool")
    Column(
        modifier = Modifier
            .widthIn(min = 64.dp)
            // The glass answers the press
            .clickable(interactionSource = null, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val color = ExtendedTheme.colors.text.copy(alpha = alpha)
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
        Text(text = label, color = color, style = MaterialTheme.typography.caption)
    }
}

/**
 * Where the picture lies: turned [turns] quarters anticlockwise, at [scale] screen pixels a
 * picture pixel, its centre [offset] from the frame's. It always covers the frame. Rotating and
 * resetting animate; the fingers move it directly.
 */
private class CropState(private val scope: CoroutineScope) {
    var turns by mutableIntStateOf(0)
        private set

    /** The turn drawn, which follows [turns]. */
    var degrees by mutableFloatStateOf(0f)
        private set
    var scale by mutableFloatStateOf(0f)
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    /** Whether the fingers are on it, which shows the grid. */
    var touching by mutableStateOf(false)

    private var frame = Size.Zero
    private var picture = IntSize.Zero

    /** Whether it has been moved, zoomed or turned since it opened. */
    private var moved = false
    private var animation: Job? = null

    /** Whether it is anywhere but as it opened. */
    val changed: Boolean
        get() = turns != 0 || abs(scale - fill(0)) > 0.001f || offset != Offset.Zero

    /** The scale at which the picture, turned [turns] quarters, just covers the frame. */
    private fun fill(turns: Int): Float {
        if (picture.width == 0 || frame.width == 0f) return 1f
        val (width, height) = if (turns % 2 == 0) picture.width to picture.height else picture.height to picture.width
        return max(frame.width / width, frame.height / height)
    }

    /**
     * The least scale at which the picture, turned [degrees] anticlockwise, covers the frame:
     * on the way between quarters a turned picture needs more than at either end.
     */
    private fun cover(degrees: Float): Float {
        if (picture.width == 0 || frame.width == 0f) return 0f
        val radians = Math.toRadians(degrees.toDouble())
        val cos = abs(cos(radians)).toFloat()
        val sin = abs(sin(radians)).toFloat()
        return max(
            (frame.width * cos + frame.height * sin) / picture.width,
            (frame.width * sin + frame.height * cos) / picture.height,
        )
    }

    /** [offset] held where the picture, at [scale], still covers the frame. */
    private fun clamp(offset: Offset, scale: Float, turns: Int): Offset {
        val (width, height) = if (turns % 2 == 0) picture.width to picture.height else picture.height to picture.width
        val x = ((width * scale - frame.width) / 2).coerceAtLeast(0f)
        val y = ((height * scale - frame.height) / 2).coerceAtLeast(0f)
        return Offset(offset.x.coerceIn(-x, x), offset.y.coerceIn(-y, y))
    }

    /** Lays the picture of [picture] size in a [frame] of this size, filling it, if either is new. */
    fun fit(frame: Size, picture: IntSize) {
        if (frame == this.frame && picture == this.picture) return
        val first = this.picture != picture
        this.frame = frame
        this.picture = picture
        // Untouched, it fills a frame that has changed, such as once the bars are measured
        if (first || !moved) {
            turns = 0
            degrees = 0f
            scale = fill(0)
            offset = Offset.Zero
        } else {
            scale = scale.coerceIn(fill(turns), fill(turns) * MaxZoom)
            offset = clamp(offset, scale, turns)
        }
    }

    /** Zooms by [zoom] about [centroid], from the frame's centre, and moves by [pan]. */
    fun transform(centroid: Offset, pan: Offset, zoom: Float) {
        animation?.cancel()
        moved = true
        val next = (scale * zoom).coerceIn(fill(turns), fill(turns) * MaxZoom)
        val by = next / scale
        offset = clamp((offset - centroid) * by + centroid + pan, next, turns)
        scale = next
    }

    fun rotate() {
        moved = true
        to(turns + 1, fill(turns + 1), Offset.Zero)
    }

    fun reset() {
        // Back the short way round
        val home = turns - turns.mod(4).let { if (it > 2) it - 4 else it }
        to(home, fill(0), Offset.Zero) {
            turns = 0
            degrees = 0f
            moved = false
        }
    }

    private fun to(turns: Int, scale: Float, offset: Offset, then: () -> Unit = {}) {
        animation?.cancel()
        val fromDegrees = degrees
        val fromScale = this.scale
        val fromOffset = this.offset
        this.turns = turns
        val toDegrees = turns * 90f
        animation = scope.launch {
            animate(0f, 1f, animationSpec = Motion.morph()) { t, _ ->
                degrees = lerp(fromDegrees, toDegrees, t)
                this@CropState.scale = max(lerp(fromScale, scale, t), cover(degrees))
                this@CropState.offset = lerp(fromOffset, offset, t)
            }
            then()
        }
    }

    /** Where the picture lies now, as the editor writes it; null before it is laid out. */
    fun crop(): Crop? = if (frame.width > 0f && scale > 0f) Crop(frame, turns.mod(4), scale, offset) else null
}

@Composable
private fun CropArea(
    image: ImageBitmap,
    aspect: Float,
    round: Boolean,
    state: CropState,
    padding: PaddingValues,
    hidden: Boolean,
    onFrame: (Rect) -> Unit,
) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    val grid by animateFloatAsState(if (state.touching) 1f else 0f, Motion.snap(), label = "grid")
    val dim = Color.Black.copy(alpha = 0.6f)
    val line = Color.White
    var frameRect by remember { mutableStateOf(Rect.Zero) }
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(state) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    state.touching = true
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    state.touching = false
                }
            }
            .pointerInput(state) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    state.transform(centroid - frameRect.center, pan, zoom)
                }
            },
    ) {
        // The frame: as large as fits between the bars with a margin, in the shape asked for
        val margin = with(density) { FrameMargin.toPx() }
        val area = with(density) {
            Rect(
                left = padding.calculateLeftPadding(direction).toPx() + margin,
                top = padding.calculateTopPadding().toPx() + margin,
                right = size.width - padding.calculateRightPadding(direction).toPx() - margin,
                bottom = size.height - padding.calculateBottomPadding().toPx() - margin,
            )
        }
        val width = minOf(area.width, area.height * aspect)
        val frame = Rect(Offset(area.center.x - width / 2, area.center.y - width / aspect / 2), Size(width, width / aspect))
        if (frameRect != frame) onFrame(frame)
        frameRect = frame
        if (hidden) return@Canvas
        state.fit(frame.size, IntSize(image.width, image.height))

        drawPicture(image, frame, state)

        val outline = Path().apply {
            if (round) addOval(frame) else addRect(frame)
        }
        val surround = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addPath(outline)
        }
        drawPath(surround, dim)
        drawPath(outline, line.copy(alpha = 0.9f), style = Stroke(width = 1.dp.toPx()))
        if (grid > 0f) drawGrid(frame, line.copy(alpha = 0.5f * grid))
    }
}

/** The picture as [state] lays it in [frame]: the steps [Crop.matrix] takes, on screen. */
private fun DrawScope.drawPicture(image: ImageBitmap, frame: Rect, state: CropState) {
    withTransform({
        translate(frame.center.x + state.offset.x, frame.center.y + state.offset.y)
        rotate(-state.degrees, pivot = Offset.Zero)
        scale(state.scale, state.scale, pivot = Offset.Zero)
        translate(-image.width / 2f, -image.height / 2f)
    }) {
        drawImage(image, filterQuality = FilterQuality.High)
    }
}

/** Lines in thirds across [frame], for lining the picture up. */
private fun DrawScope.drawGrid(frame: Rect, color: Color) {
    val stroke = 0.5.dp.toPx()
    for (i in 1..2) {
        val x = frame.left + frame.width * i / 3
        val y = frame.top + frame.height * i / 3
        drawLine(color, Offset(x, frame.top), Offset(x, frame.bottom), stroke)
        drawLine(color, Offset(frame.left, y), Offset(frame.right, y), stroke)
    }
}
