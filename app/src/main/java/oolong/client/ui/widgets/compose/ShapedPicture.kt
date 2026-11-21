package oolong.client.ui.widgets.compose

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import android.graphics.Matrix as AndroidMatrix

/**
 * What a picture in a shape draws: the [bitmap] when there is one to draw, else whatever the
 * content draws, cut out by the shape when [masked], else [color].
 */
internal class ShapedPicture(
    val bitmap: Bitmap?,
    val masked: Boolean,
    val color: Color,
)

/**
 * Draws a picture in [shape] instead of the content under it. A clip to a shape drawn as a path is
 * anti-aliased coarsely, or not at all, on some GPUs, so the picture fills the shape as a paint
 * instead, which is anti-aliased everywhere. What is no single still picture, such as an animated
 * one, is cut out as [maskTo] does.
 */
internal fun Modifier.shapedPicture(
    shape: Shape,
    contentScale: ContentScale,
    colorFilter: ColorFilter?,
    picture: () -> ShapedPicture,
): Modifier = this
    .graphicsLayer {
        compositingStrategy = if (picture().drawsMasked()) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }
    .drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val shown = picture()
        val outside = if (shown.drawsMasked()) outside(outline) else null
        val bitmap = shown.bitmap?.takeIf { !shown.drawsMasked() }
        when {
            outside != null -> onDrawWithContent { drawMasked(outside) }

            bitmap == null -> onDrawWithContent {
                drawOutline(outline, shown.color, colorFilter = colorFilter)
            }

            else -> {
                val source = Size(bitmap.width.toFloat(), bitmap.height.toFloat())
                val scale = contentScale.computeScaleFactor(source, size)
                val drawn = Size(source.width * scale.scaleX, source.height * scale.scaleY)
                val at = Alignment.Center.align(
                    IntSize(drawn.width.roundToInt(), drawn.height.roundToInt()),
                    IntSize(size.width.roundToInt(), size.height.roundToInt()),
                    layoutDirection,
                )
                val brush = PictureBrush(bitmap, scale.scaleX, scale.scaleY, Offset(at.x.toFloat(), at.y.toFloat()))
                val covers = at.x <= 0 && at.y <= 0 && drawn.width >= size.width && drawn.height >= size.height
                onDrawWithContent {
                    if (covers) {
                        drawOutline(outline, brush, colorFilter = colorFilter)
                    } else {
                        // Only where the picture is, as the picture alone would be drawn
                        clipRect(at.x.toFloat(), at.y.toFloat(), at.x + drawn.width, at.y + drawn.height) {
                            drawOutline(outline, brush, colorFilter = colorFilter)
                        }
                    }
                }
            }
        }
    }

/**
 * Cuts what is drawn to [shape] with an anti-aliased edge: drawn offscreen, then everything outside
 * the shape is erased. For content that is no single still picture; a picture takes ContentImage's
 * shape instead. Unlike a clip, it limits drawing only; touch and layout keep the full bounds.
 */
fun Modifier.maskTo(shape: Shape): Modifier = this
    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    .drawWithCache {
        val outside = outside(shape.createOutline(size, layoutDirection, this))
        onDrawWithContent { drawMasked(outside) }
    }

/**
 * What lies outside [outline], a pixel beyond the bounds all round. A blend touches only what is
 * drawn, so the content is cut by erasing this rather than by filling the shape.
 */
private fun CacheDrawScope.outside(outline: Outline): Path = Path().apply {
    fillType = PathFillType.EvenOdd
    addRect(Rect(Offset.Zero, size).inflate(1f))
    addOutline(outline)
}

/** Draws the content, then erases [outside]; needs an offscreen layer. */
private fun ContentDrawScope.drawMasked(outside: Path) {
    drawContent()
    drawPath(outside, Color.Black, blendMode = BlendMode.DstOut)
}

/** True when the picture is cut out of the content rather than drawn as a paint. */
private fun ShapedPicture.drawsMasked(): Boolean =
    masked || (bitmap != null && !canPaintWith(bitmap))

/** A bitmap kept on the GPU can be a paint only on the renderer of Android 10 and later. */
private fun canPaintWith(bitmap: Bitmap): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
        bitmap.config != Bitmap.Config.HARDWARE

/** [bitmap] scaled by [scaleX] and [scaleY] and moved to [offset], as a paint. */
private class PictureBrush(
    private val bitmap: Bitmap,
    private val scaleX: Float,
    private val scaleY: Float,
    private val offset: Offset,
) : ShaderBrush() {
    override fun createShader(size: Size): Shader =
        ImageShader(bitmap.asImageBitmap(), TileMode.Clamp, TileMode.Clamp).apply {
            setLocalMatrix(
                AndroidMatrix().apply {
                    setScale(scaleX, scaleY)
                    postTranslate(offset.x, offset.y)
                }
            )
        }
}
