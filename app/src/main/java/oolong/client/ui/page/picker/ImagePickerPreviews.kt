package oolong.client.ui.page.picker

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import oolong.client.model.DeviceAlbum
import oolong.client.model.DeviceImage
import oolong.client.ui.common.theme.compose.TiebaLiteTheme

/** Sample pictures, shared by the previews below and the screenshot tests. */
internal object ImagePickerPreviewData {
    private fun uri(id: Int) = "content://media/external/images/media/$id".toUri()

    private val images = (1..14).map { id ->
        DeviceImage(
            uri(id),
            albumId = if (id % 3 == 0) 2 else 1,
            albumName = "",
            mimeType = if (id == 4) "image/gif" else "image/jpeg",
            width = 1080,
            height = if (id == 7) 5400 else 1440,
        )
    }

    val multiple = ImagePickerState(
        loaded = true,
        albums = persistentListOf(
            DeviceAlbum(id = null, name = "", cover = uri(1), count = 14),
            DeviceAlbum(id = 1, name = "Camera", cover = uri(1), count = 10),
            DeviceAlbum(id = 2, name = "Screenshots", cover = uri(3), count = 4),
        ),
        images = images.toImmutableList(),
        selected = persistentListOf(uri(5), uri(2)),
        maxItems = 9,
    )
}

@Preview("ImagePickerScreen", showBackground = true)
@Composable
private fun PreviewImagePickerScreen() {
    TiebaLiteTheme { ImagePickerPreviewScreen(ImagePickerPreviewData.multiple, PickerAccess.All) }
}

@Preview("ImagePickerScreen denied", showBackground = true)
@Composable
private fun PreviewImagePickerScreenDenied() {
    TiebaLiteTheme { ImagePickerPreviewScreen(ImagePickerState(maxItems = 9), PickerAccess.Denied) }
}

@Composable
internal fun ImagePickerPreviewScreen(state: ImagePickerState, access: PickerAccess) {
    ImagePickerScreen(
        state = state,
        access = access,
        onBack = {},
        onShowAlbum = {},
        onToggle = {},
        onSelect = {},
        onMove = { _, _ -> },
        onDone = {},
        onTakePhoto = {},
        onEdit = { _, _ -> },
        onRequestAccess = {},
        onOpenSettings = {},
    )
}
