package oolong.client.model

import android.net.Uri
import androidx.compose.runtime.Immutable

/** A picture stored on the device. */
@Immutable
data class DeviceImage(
    val uri: Uri,
    /** The folder it is in, which galleries show as an album. */
    val albumId: Long,
    val albumName: String,
    val mimeType: String,
    /** Its size as shown, upright; 0 where the device does not know it. */
    val width: Int = 0,
    val height: Int = 0,
    /** Marked a favourite in the device's gallery, where the gallery tells the system. */
    val favorite: Boolean = false,
) {
    /** Far taller than it is wide, as a screenshot of a long chat. */
    val isLong: Boolean get() = width > 0 && height >= width * LongRatio

    val isGif: Boolean get() = mimeType == "image/gif"
}

/** How many times taller than wide a picture is to count as long. */
private const val LongRatio = 3

/** A folder of pictures, with its newest one as the cover. */
@Immutable
data class DeviceAlbum(
    /** The folder, null for an album of every picture, or [Favorites]. */
    val id: Long?,
    val name: String,
    val cover: Uri,
    val count: Int,
) {
    companion object {
        /** The album of the pictures marked as favourites, which is no folder. */
        const val Favorites = Long.MIN_VALUE + 1
    }
}
