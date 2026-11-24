package oolong.client.ui.page.picker

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import com.github.panpf.zoomimage.CoilZoomAsyncImage
import com.github.panpf.zoomimage.CoilZoomState
import com.github.panpf.zoomimage.rememberCoilZoomState
import com.github.panpf.zoomimage.zoom.Edge
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import oolong.client.R
import oolong.client.model.DeviceImage
import oolong.client.ui.common.theme.compose.LocalReduceMotion
import oolong.client.ui.common.theme.compose.Motion
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.common.theme.compose.Symbols
import oolong.client.ui.page.editor.EditorPlace
import oolong.client.ui.page.photoview.offsetTo
import oolong.client.ui.widgets.compose.Button
import oolong.client.ui.widgets.compose.MediaGlassButton
import oolong.client.ui.widgets.compose.MediaGlassIconButton
import oolong.client.ui.widgets.compose.MediaGlassPill
import oolong.client.ui.widgets.compose.ProvideMediaGlass
import oolong.client.ui.widgets.compose.mediaGlass
import kotlin.math.abs

private enum class Phase { Opening, Open, Closing }

/**
 * [pictures] large, from [start], to swipe through, zoom into and close with a drag down. The
 * picture grows out of its thumbnail in [places] and shrinks back into the thumbnail of the one
 * showing, where it is on screen; [onPage] says which that is, for the grid to keep it there.
 * The circle picks the picture shown; 编辑 opens it in the picture editor; the tray along the
 * bottom shows the picks, and its button says what it gives back: the picture shown when nothing
 * else is picked, or with one to pick, otherwise every pick.
 */
@Composable
internal fun PickerViewer(
    pictures: List<DeviceImage>,
    start: Int,
    state: ImagePickerState,
    places: Map<Uri, ThumbnailPlace>,
    onPage: (Uri) -> Unit,
    onToggle: (Uri) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onEdit: (Uri, EditorPlace?) -> Unit,
    onDone: (List<Uri>) -> Unit,
    onClose: () -> Unit,
) {
    if (pictures.isEmpty()) {
        BackHandler(onBack = onClose)
        return
    }
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = start.coerceIn(0, pictures.lastIndex)) { pictures.size }
    val scope = rememberCoroutineScope()
    val glass = rememberHazeState()
    val zoomStates = remember { mutableStateMapOf<Int, CoilZoomState>() }
    // With motion reduced the picture fades in and out in place instead of flying
    val reduced = LocalReduceMotion.current
    // Where the viewer lies in the window, which the thumbnails' places are measured in
    var origin by remember { mutableStateOf<Offset?>(null) }
    // 0 at the thumbnail, 1 open; closing runs it back to 0
    val progress = remember { Animatable(0f) }
    var phase by remember { mutableStateOf(Phase.Opening) }
    // The thumbnail flying between its place (near; null to fade in place) and the viewer's (far)
    var flying by remember { mutableStateOf<Uri?>(null) }
    var near by remember { mutableStateOf<ThumbnailPlace?>(null) }
    var far by remember { mutableStateOf(Rect.Zero) }
    // The flown thumbnail, for the picture to show until it is in
    var flownKey by remember { mutableStateOf<MemoryCache.Key?>(null) }
    var closingFade by remember { mutableStateOf(1f) }
    // How far a drag down has carried the picture
    var drag by remember { mutableStateOf(Offset.Zero) }
    var dragging by remember { mutableStateOf(false) }
    val picture = pictures[pagerState.currentPage.coerceIn(0, pictures.lastIndex)]
    val order = state.selected.indexOf(picture.uri).takeIf { it >= 0 }?.plus(1)
    LaunchedEffect(picture.uri) { onPage(picture.uri) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().onPlaced { origin = it.positionInRoot() }) {
        val container = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val height = container.height
        val dragScale = 1f - (drag.y.coerceAtLeast(0f) / height).coerceIn(0f, 1f) * 0.45f
        val dragFade = 1f - (abs(drag.y) / (height * 0.5f)).coerceIn(0f, 1f)

        /** The thumbnail of [image] on screen, in the viewer's space, or null to fade in place. */
        fun placeOf(image: DeviceImage): ThumbnailPlace? {
            val at = origin ?: return null
            val place = places[image.uri]?.takeIf { it.visible && !reduced } ?: return null
            return ThumbnailPlace(place.bounds.translate(-at), place.corner, visible = true)
        }

        LaunchedEffect(origin != null) {
            if (origin == null) return@LaunchedEffect
            val first = pictures[pagerState.currentPage]
            far = fitted(container, first.aspect)
            // Without its size the picture cannot fly into place, and fades in where it shows
            near = placeOf(first).takeIf { first.aspect > 0f }
            flying = state.edits[first.uri] ?: first.uri
            progress.animateTo(1f, Motion.present(Motion.ProgressThreshold))
            phase = Phase.Open
        }

        fun close() {
            if (phase == Phase.Closing) return
            val shown = zoomStates[pagerState.currentPage]?.zoomable?.contentDisplayRectF?.takeIf { !it.isEmpty }
                ?: fitted(container, picture.aspect)
            far = shown.dragged(drag, dragScale, container)
            near = placeOf(picture)
            flying = state.edits[picture.uri] ?: picture.uri
            closingFade = if (phase == Phase.Open) dragFade else progress.value.coerceIn(0f, 1f)
            drag = Offset.Zero
            dragging = false
            phase = Phase.Closing
            scope.launch {
                progress.snapTo(1f)
                progress.animateTo(0f, Motion.dismiss(Motion.ProgressThreshold))
                onClose()
            }
        }
        BackHandler(enabled = phase != Phase.Closing) { close() }

        val p = progress.value
        val black = if (phase == Phase.Closing) p * closingFade else p.coerceIn(0f, 1f) * dragFade
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = black }
                .background(Color.Black)
                // Taps do not fall through to the grid under the viewer
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(glass)
                .graphicsLayer {
                    // Kept while it flies, so the picture is in by the time it lands
                    alpha = if (phase == Phase.Open) 1f else 0f
                    translationX = drag.x
                    translationY = drag.y
                    scaleX = dragScale
                    scaleY = dragScale
                    transformOrigin = TransformOrigin.Center
                }
                .pointerInput(pagerState) {
                    val slop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        if (phase != Phase.Open || !zoomStates[pagerState.currentPage].atRest()) return@awaitEachGesture
                        val tracker = VelocityTracker()
                        var total = Offset.Zero
                        var active = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            // A second finger is a pinch, for the picture to zoom
                            if (event.changes.count { it.pressed } > 1) break
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) {
                                if (active) {
                                    val velocity = tracker.calculateVelocity()
                                    if (drag.y > 96.dp.toPx() || velocity.y > 900.dp.toPx()) {
                                        close()
                                    } else {
                                        scope.launch {
                                            Animatable(drag, Offset.VectorConverter).animateTo(Offset.Zero, Motion.settle()) { drag = value }
                                            dragging = false
                                        }
                                    }
                                }
                                break
                            }
                            val delta = change.positionChange()
                            if (!active) {
                                total += delta
                                if (total.y > slop && abs(total.y) > abs(total.x) * 1.5f) {
                                    active = true
                                    dragging = true
                                } else if (abs(total.x) > slop || total.y < -slop) {
                                    break
                                }
                            }
                            if (active) {
                                change.consume()
                                drag += delta
                                tracker.addPosition(change.uptimeMillis, change.position)
                            }
                        }
                    }
                },
        ) {
            HorizontalPager(
                state = pagerState,
                pageSpacing = 16.dp,
                key = { pictures[it].uri.toString() },
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val zoomState = rememberCoilZoomState()
                DisposableEffect(page, zoomState) {
                    zoomStates[page] = zoomState
                    onDispose { if (zoomStates[page] === zoomState) zoomStates.remove(page) }
                }
                val uri = pictures[page].uri
                val shown = state.edits[uri] ?: uri
                // The thumbnail it flew in as shows until the picture is in
                val placeholder = flownKey.takeIf { page == start }
                val request = remember(shown, placeholder) {
                    ImageRequest.Builder(context).data(shown).placeholderMemoryCacheKey(placeholder).build()
                }
                CoilZoomAsyncImage(
                    model = request,
                    contentDescription = null,
                    zoomState = zoomState,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        // The thumbnail, flying between its place in the grid or the tray and the viewer's
        val flown = flying
        if (phase != Phase.Open && flown != null) {
            val from = near
            val rect = from?.let { lerp(it.bounds, far, p) } ?: far.scaledAround(if (reduced) 1f else lerp(0.9f, 1f, p))
            val corner = from?.let { lerp(it.corner.value, 0f, p.coerceIn(0f, 1f)) } ?: 0f
            Box(
                modifier = Modifier
                    .offsetTo(rect)
                    .graphicsLayer {
                        alpha = if (from == null) p.coerceIn(0f, 1f) * (if (phase == Phase.Closing) closingFade else 1f) else 1f
                        shape = SmoothCornerShape(corner.dp)
                        clip = true
                    },
            ) {
                AsyncImage(
                    model = rememberThumbnailRequest(flown),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    onSuccess = { if (phase == Phase.Opening) flownKey = it.result.memoryCacheKey },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        val chrome by animateFloatAsState(
            targetValue = if (phase == Phase.Open && !dragging) 1f else 0f,
            animationSpec = if (phase == Phase.Open && !dragging) Motion.fadeIn() else Motion.fadeOut(),
            label = "chrome",
        )
        val chromeAlpha = if (dragging) dragFade else chrome
        ProvideMediaGlass(glass) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .graphicsLayer { alpha = chromeAlpha }
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .fillMaxWidth(),
            ) {
                MediaGlassIconButton(
                    icon = Symbols.ArrowBack,
                    description = stringResource(id = R.string.button_back),
                    onClick = { close() },
                )
                if (pictures.size > 1) {
                    MediaGlassPill {
                        Text(
                            text = stringResource(id = R.string.title_picker_index, pagerState.currentPage + 1, pictures.size),
                            color = Color.White,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
                MediaGlassButton(
                    onClick = { onToggle(picture.uri) },
                    onClickLabel = stringResource(id = R.string.desc_picker_select),
                ) {
                    PickCircle(order = order, size = 26.dp, filled = Color.White, onFilled = Color.Black)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer { alpha = chromeAlpha }
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .mediaGlass(SmoothCornerShape.Capsule)
                    .padding(horizontal = 8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .height(44.dp)
                        .clickable(role = Role.Button) {
                            // Out of the picture as it shows, and back into it
                            val shown = zoomStates[pagerState.currentPage]?.zoomable?.contentDisplayRectF?.takeIf { !it.isEmpty }
                            val at = origin
                            onEdit(picture.uri, if (shown != null && at != null) EditorPlace(shown.translate(at)) else null)
                        }
                        .padding(horizontal = 12.dp),
                ) {
                    Icon(imageVector = Symbols.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(text = stringResource(id = R.string.editor_edit_picture), color = Color.White, fontSize = 14.sp)
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (state.multiple) {
                        PickedStrip(
                            state = state,
                            onOpen = { index ->
                                val uri = state.selected[index]
                                val page = pictures.indexOfFirst { it.uri == uri }
                                if (page >= 0) scope.launch { pagerState.animateScrollToPage(page) }
                            },
                            onMove = onMove,
                            current = picture.uri,
                            filled = Color.White,
                            onFilled = Color.Black,
                        )
                    }
                }
                val shownOne = listOf(state.edits[picture.uri] ?: picture.uri)
                Button(
                    onClick = {
                        // Says what it gives back: the picture shown, unless others are picked
                        onDone(if (state.multiple && state.selected.isNotEmpty()) state.result else shownOne)
                    },
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color.White, contentColor = Color.Black),
                ) {
                    Text(
                        text = when {
                            !state.multiple -> stringResource(id = R.string.button_picker_use_this)
                            state.selected.isEmpty() -> stringResource(id = R.string.button_picker_add_this)
                            else -> stringResource(id = R.string.button_picker_add, state.selected.size)
                        },
                    )
                }
            }
        }
    }
}

/** Whether the picture shows whole and unzoomed, so a drag down may close it. */
private fun CoilZoomState?.atRest(): Boolean {
    val zoomable = this?.zoomable ?: return true
    if (zoomable.userTransform.scaleX > 1.01f) return false
    // A long picture scrolled down drags back up first
    return zoomable.scrollEdge.vertical.let { it == Edge.START || it == Edge.BOTH }
}

/** Height over width, or 0 when the size is not known. */
private val DeviceImage.aspect: Float
    get() = if (width > 0 && height > 0) height.toFloat() / width else 0f

/** Where a picture of height over width [aspect] shows whole and centred; the whole viewer if unknown. */
private fun fitted(container: Size, aspect: Float): Rect {
    if (aspect <= 0f || container.width <= 0f || container.height <= 0f) return Rect(Offset.Zero, container)
    return if (aspect > container.height / container.width) {
        val width = container.height / aspect
        Rect((container.width - width) / 2, 0f, (container.width + width) / 2, container.height)
    } else {
        val height = container.width * aspect
        Rect(0f, (container.height - height) / 2, container.width, (container.height + height) / 2)
    }
}

/** This rect as a drag of [offset], scaled by [scale] about the container's centre, leaves it. */
private fun Rect.dragged(offset: Offset, scale: Float, container: Size): Rect {
    val centre = Offset(container.width / 2, container.height / 2)
    val topLeft = centre + (topLeft - centre) * scale + offset
    return Rect(topLeft, Size(width * scale, height * scale))
}

private fun Rect.scaledAround(scale: Float): Rect =
    Rect(center - Offset(width * scale / 2, height * scale / 2), Size(width * scale, height * scale))
