package oolong.client.ui.widgets.compose

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.asImage
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import oolong.client.Incognito
import oolong.client.R
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.widgets.compose.placeholder.PlaceholderHighlight
import oolong.client.ui.widgets.compose.placeholder.fade
import oolong.client.ui.widgets.compose.placeholder.placeholder
import oolong.client.utils.ImageUtil

object Sizes {
    val Tiny = 24.dp
    val Small = 36.dp
    val Medium = 48.dp
    val Large = 56.dp
}

@Composable
fun AvatarIcon(
    icon: ImageVector,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    color: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    backgroundColor: Color = Color.Transparent,
    shape: Shape = CircleShape,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        tint = color,
        modifier = modifier
            .size(size)
            .background(color = backgroundColor, shape = shape)
            .padding((size - iconSize) / 2),
    )
}

@Composable
fun AvatarIcon(
    @DrawableRes
    resId: Int,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    color: Color = LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
    backgroundColor: Color = Color.Transparent,
    shape: Shape = CircleShape,
) {
    Icon(
        imageVector = ImageVector.vectorResource(id = resId),
        contentDescription = contentDescription,
        tint = color,
        modifier = modifier
            .size(size)
            .background(color = backgroundColor, shape = shape)
            .padding((size - iconSize) / 2),
    )
}

@Composable
fun AvatarPlaceholder(
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Avatar(
        data = ImageUtil.getPlaceHolder(LocalContext.current, 0),
        size = size,
        contentDescription = null,
        modifier = modifier.placeholder(
            visible = true,
            highlight = PlaceholderHighlight.fade(),
            shape = CircleShape
        )
    )
}

@Composable
fun Avatar(
    data: String?,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
) {
    Avatar(
        data = data,
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        shape = shape,
    )
}

/**
 * An avatar from [data]. With [decodedAt] it is decoded at that size whatever size it shows at,
 * so it can be the copy a list already has at hand rather than a new one, shown at once.
 */
@Composable
fun Avatar(
    data: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    decodedAt: Dp? = null,
) {
    val placeholder = rememberImagePlaceholder()
    val placeholderColor = (placeholder as ColorPainter).color
    val context = LocalContext.current
    val density = LocalDensity.current
    val name = remember(data) { data?.let { avatarName(it) ?: pictureName(it) } }
    val model = remember(data, decodedAt, density, placeholderColor) {
        ImageRequest.Builder(context)
            .data(data)
            // The same avatar, a user's or a forum's, drawn before at another size or address, shows until
            // this one is in
            .placeholderMemoryCacheKey(name?.let { ShownPictures[it] })
            .placeholder(ColorDrawable(placeholderColor.toArgb()).asImage())
            .apply {
                if (decodedAt != null) {
                    val px = with(density) { decodedAt.roundToPx() }
                    size(px, px)
                }
            }
            .build()
    }
    // A smooth shape takes the picture as a paint, as ContentImage does; a circle stays a clip
    val shaped = shape is SmoothCornerShape
    var shown by remember(data, shaped) {
        mutableStateOf(
            if (shaped) {
                name?.let { ShownPictures[it] }?.let { (SingletonImageLoader.get(context).memoryCache?.get(it)?.image as? BitmapImage)?.bitmap }
            } else {
                null
            }
        )
    }
    var animated by remember(data) { mutableStateOf(false) }
    AsyncImage(
        model = model,
        error = placeholder,
        onSuccess = { state ->
            name?.let { ShownPictures[it] = state.result.memoryCacheKey }
            if (shaped) {
                val image = state.result.image
                shown = (image as? BitmapImage)?.bitmap
                animated = image !is BitmapImage
            }
        },
        onError = if (shaped) {
            { shown = null }
        } else {
            null
        },
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = if (shaped) {
            modifier.shapedPicture(shape, ContentScale.Crop, null) { ShapedPicture(shown, animated, placeholderColor) }
        } else {
            modifier.clip(shape)
        },
    )
}

@Composable
fun Avatar(
    data: Int,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val placeholder = rememberImagePlaceholder()
    AsyncImage(
        model = data,
        placeholder = placeholder,
        error = placeholder,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}

@Composable
fun Avatar(
    data: Drawable,
    size: Dp,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = rememberDrawablePainter(drawable = data),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}

/**
 * The viewer's own avatar from [data], or, while browsing incognito, a mask in its place, so it
 * shows wherever the viewer's face would that nothing is being kept.
 */
@Composable
fun OwnAvatar(
    data: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val incognito by Incognito.on.collectAsState()
    if (incognito) {
        val colors = ExtendedTheme.colors
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .clip(CircleShape)
                .background(colors.text),
        ) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_me_incognito_on),
                contentDescription = contentDescription,
                tint = colors.background,
                modifier = Modifier.fillMaxSize(0.72f),
            )
        }
    } else {
        Avatar(data = data, contentDescription = contentDescription, modifier = modifier)
    }
}
