package oolong.client.ui.page.picker

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import kotlinx.collections.immutable.ImmutableList
import oolong.client.R
import oolong.client.model.DeviceAlbum
import oolong.client.model.DeviceImage
import oolong.client.ui.common.theme.compose.Corners
import oolong.client.ui.common.theme.compose.ExtendedTheme
import oolong.client.ui.common.theme.compose.PressFeedback
import oolong.client.ui.common.theme.compose.SmoothCornerShape
import oolong.client.ui.common.theme.compose.Symbols
import oolong.client.ui.widgets.compose.BackNavigationIcon
import oolong.client.ui.widgets.compose.Button
import oolong.client.ui.widgets.compose.ContextMenuItem
import oolong.client.ui.widgets.compose.GlassScaffold
import oolong.client.ui.widgets.compose.GlassTopBar
import oolong.client.ui.widgets.compose.SecondaryButton
import oolong.client.ui.widgets.compose.TipScreen
import oolong.client.ui.widgets.compose.clickMenu
import oolong.client.ui.widgets.compose.maskTo

/** What the picker may read of the device's pictures. */
enum class PickerAccess {
    All,

    /** Only the pictures the user chose to share, on Android 14 and later. */
    Selected,
    Denied,

    /** Denied, and the system no longer asks, so it has to be granted in the settings. */
    DeniedForever,
}

/**
 * The picker: the pictures of one album in a grid under a bar that switches albums. With one
 * picture to pick, tapping it picks it; with more, taps mark them in order until done.
 */
@Composable
fun ImagePickerScreen(
    state: ImagePickerState,
    access: PickerAccess,
    onBack: () -> Unit,
    onShowAlbum: (Long?) -> Unit,
    onToggle: (Uri) -> Unit,
    onDone: (List<Uri>) -> Unit,
    onRequestAccess: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val readable = access == PickerAccess.All || access == PickerAccess.Selected
    GlassScaffold(
        topBar = {
            GlassTopBar(
                title = {
                    AlbumSwitcher(
                        albums = state.albums,
                        albumId = state.albumId,
                        enabled = readable,
                        onShowAlbum = onShowAlbum,
                    )
                },
                navigationIcon = { BackNavigationIcon(onBackPressed = onBack) },
                actionsInCapsule = false,
                actions = {
                    if (state.multiple && readable) {
                        Button(
                            onClick = { onDone(state.selected) },
                            enabled = state.selected.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 12.dp),
                        ) {
                            Text(
                                text = if (state.selected.isEmpty()) {
                                    stringResource(id = R.string.button_sure_default)
                                } else {
                                    stringResource(id = R.string.button_sure, state.selected.size)
                                }
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            !readable -> AccessTip(
                forever = access == PickerAccess.DeniedForever,
                onRequestAccess = onRequestAccess,
                onOpenSettings = onOpenSettings,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            state.loaded && state.images.isEmpty() && access == PickerAccess.All -> TipScreen(
                title = { Text(text = stringResource(id = R.string.title_picker_empty)) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )

            else -> ImageGrid(
                state = state,
                selectedOnly = access == PickerAccess.Selected,
                contentPadding = padding,
                onToggle = onToggle,
                onPick = { onDone(listOf(it)) },
                onSelectMore = onRequestAccess,
            )
        }
    }
}

@Composable
private fun AlbumSwitcher(
    albums: ImmutableList<DeviceAlbum>,
    albumId: Long?,
    enabled: Boolean,
    onShowAlbum: (Long?) -> Unit,
) {
    val current = albums.firstOrNull { it.id == albumId }
    val names = albums.map { albumName(it) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickMenu(
                    items = {
                        albums.mapIndexed { index, album ->
                            ContextMenuItem(
                                icon = null,
                                label = names[index],
                                detail = album.count.toString(),
                                leading = { AlbumCover(album) },
                                selected = album.id == albumId,
                            ) { onShowAlbum(album.id) }
                        }
                    },
                    enabled = enabled && albums.isNotEmpty(),
                    indication = PressFeedback.overlay(Corners.full),
                )
                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Text(
                text = current?.let { albumName(it) } ?: stringResource(id = R.string.title_picker_all_images),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (enabled && albums.isNotEmpty()) {
                Icon(imageVector = Symbols.ArrowDropDown, contentDescription = null)
            }
        }
    }
}

@Composable
private fun AlbumCover(album: DeviceAlbum) {
    DeviceThumbnail(
        uri = album.cover,
        modifier = Modifier
            .size(40.dp)
            .maskTo(SmoothCornerShape(6.dp)),
    )
}

@Composable
private fun albumName(album: DeviceAlbum): String = when {
    album.id == null -> stringResource(id = R.string.title_picker_all_images)
    album.name.isEmpty() -> stringResource(id = R.string.title_picker_unnamed_album)
    else -> album.name
}

@Composable
private fun ImageGrid(
    state: ImagePickerState,
    selectedOnly: Boolean,
    contentPadding: PaddingValues,
    onToggle: (Uri) -> Unit,
    onPick: (Uri) -> Unit,
    onSelectMore: () -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 104.dp),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (selectedOnly) {
            item(key = "selected_only", span = { GridItemSpan(maxLineSpan) }) {
                SelectedOnlyBanner(onSelectMore = onSelectMore)
            }
        }
        items(state.images, key = { it.uri.toString() }) { image ->
            val order = state.selected.indexOf(image.uri)
            ImageCell(
                image = image,
                multiple = state.multiple,
                order = order.takeIf { it >= 0 }?.plus(1),
                onClick = { if (state.multiple) onToggle(image.uri) else onPick(image.uri) },
            )
        }
    }
}

@Composable
private fun SelectedOnlyBanner(onSelectMore: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(id = R.string.tip_picker_selected_only),
            color = ExtendedTheme.colors.textSecondary,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.weight(1f),
        )
        SecondaryButton(onClick = onSelectMore, color = ExtendedTheme.colors.emphasis) {
            Text(text = stringResource(id = R.string.button_picker_select_more))
        }
    }
}

@Composable
private fun ImageCell(
    image: DeviceImage,
    multiple: Boolean,
    /** Where it is among the picked pictures, from 1, or null when not picked. */
    order: Int?,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
    ) {
        DeviceThumbnail(uri = image.uri, modifier = Modifier.fillMaxSize())
        if (order != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
        }
        if (image.mimeType == "image/gif") {
            Text(
                text = "GIF",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.5f), Corners.xs)
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
        if (multiple) {
            SelectionBadge(
                order = order,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            )
        }
    }
}

@Composable
private fun SelectionBadge(order: Int?, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .then(
                if (order != null) {
                    Modifier.background(ExtendedTheme.colors.emphasis)
                } else {
                    Modifier
                        .background(Color.Black.copy(alpha = 0.2f))
                        .border(1.5.dp, Color.White, CircleShape)
                }
            ),
    ) {
        if (order != null) {
            Text(
                text = order.toString(),
                color = ExtendedTheme.colors.onAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun AccessTip(
    forever: Boolean,
    onRequestAccess: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TipScreen(
        title = { Text(text = stringResource(id = R.string.title_picker_no_access)) },
        message = { Text(text = stringResource(id = R.string.tip_picker_no_access)) },
        actions = {
            Button(
                onClick = if (forever) onOpenSettings else onRequestAccess,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(
                    text = stringResource(
                        id = if (forever) R.string.button_picker_open_settings else R.string.button_picker_grant
                    )
                )
            }
        },
        modifier = modifier,
    )
}

/** About as large as a grid cell gets. */
private val ThumbnailSize = 160.dp

/** A picture on the device, cropped to fill [modifier]'s bounds. */
@Composable
private fun DeviceThumbnail(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val cellPx = with(LocalDensity.current) { ThumbnailSize.roundToPx() }
    AsyncImage(
        // A fixed size keeps the grid from flickering and decodes no more than a cell shows
        model = remember(uri, cellPx) { ImageRequest.Builder(context).data(uri).size(cellPx).build() },
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(ExtendedTheme.colors.chip),
    )
}
