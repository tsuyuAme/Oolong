package oolong.client.ui.page.videoview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.withTimeoutOrNull
import oolong.client.R
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.Symbols
import oolong.client.ui.widgets.compose.ContentImage
import oolong.client.ui.widgets.compose.maskTo
import oolong.client.ui.widgets.compose.shouldLoadImage

/**
 * A video in a list or a post: its cover with a play button, how long it runs and how much of it
 * was watched. Pressing it starts loading the video; tapping opens it in the viewer, out of the
 * cover. Wide videos fill the width; tall ones take part of it, cropped to 3:4 at most, as single
 * pictures do.
 */
@Composable
fun VideoCover(
    video: ViewerVideo,
    modifier: Modifier = Modifier,
    shape: Shape = Corners.sm,
) {
    val viewer = LocalVideoViewer.current
    val context = LocalContext.current
    val current by rememberUpdatedState(video)
    var bounds by remember { mutableStateOf<Rect?>(null) }
    val description = stringResource(id = R.string.desc_video)
    DisposableEffect(viewer, video.key) {
        onDispose { viewer?.place(video.key, null) }
    }
    val ratio = video.aspect.takeIf { it > 0f }?.let { 1f / it } ?: (16f / 9)
    BoxWithConstraints(modifier = modifier) {
        val wide = ratio >= 1f
        val width = if (wide) maxWidth else maxWidth * TallWidthFraction
        val height = if (wide) width / ratio else minOf(width / ratio, width * 4 / 3)
        Box(
            modifier = Modifier
                .size(width, height)
                .onGloballyPositioned {
                    val rect = Rect(it.positionInWindow(), it.size.toSize())
                    bounds = rect
                    viewer?.place(current.key, rect, visible = it.boundsInWindow().height > rect.height * 0.3f)
                }
                .pointerInput(viewer) {
                    if (viewer == null) return@pointerInput
                    detectTapGestures(
                        onPress = {
                            // A fling that starts on the cover turns into a scroll before this; only
                            // a finger that stays loads ahead. A quick tap loads as it opens
                            val ended = withTimeoutOrNull(PreloadDelay) { tryAwaitRelease() }
                            if (ended == null) {
                                viewer.preload(current)
                                if (!tryAwaitRelease()) viewer.cancelPreload(current)
                            }
                        },
                        onTap = { viewer.open(current, bounds) },
                    )
                }
                .semantics {
                    role = Role.Button
                    contentDescription = description
                    onClick {
                        viewer?.open(current, bounds)
                        true
                    }
                },
        ) {
            // Held back like any picture when the data-saving setting says so; a tap still plays
            ContentImage(
                imageUri = video.cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                load = remember { shouldLoadImage(context, skipNetworkCheck = false) },
                modifier = Modifier.fillMaxSize(),
                shape = shape,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.35f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Symbols.PlayArrowFill,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
            if (video.duration > 0) {
                Text(
                    text = formatDuration(video.duration * 1000L),
                    color = Color.White,
                    style = TextStyle(fontSize = 11.5.sp, fontFeatureSettings = "tnum"),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
            val watched = viewer?.watched?.get(video.key)
            if (watched != null) {
                // Along the bottom edge, so cut to the cover's corners
                Box(Modifier.matchParentSize().maskTo(shape)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Color.White.copy(alpha = 0.3f)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(watched.coerceIn(0f, 1f))
                                .height(3.dp)
                                .background(Color.White),
                        )
                    }
                }
            }
        }
    }
}

private const val TallWidthFraction = 0.6f

/** How long a finger stays on a cover before the video starts loading. */
private const val PreloadDelay = 100L

/** `m:ss`, or `h:mm:ss` from an hour. */
internal fun formatDuration(millis: Long): String {
    val total = (millis / 1000).coerceAtLeast(0)
    val hours = total / 3600
    val minutes = total % 3600 / 60
    val seconds = total % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
