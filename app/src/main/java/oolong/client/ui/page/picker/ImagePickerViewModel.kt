package oolong.client.ui.page.picker

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import oolong.client.model.DeviceAlbum
import oolong.client.model.DeviceImage
import oolong.client.repository.DeviceImageRepository
import javax.inject.Inject

@Immutable
data class ImagePickerState(
    /** Whether the pictures have been read at least once. */
    val loaded: Boolean = false,
    /** Every picture first, then each folder, the largest first. */
    val albums: ImmutableList<DeviceAlbum> = persistentListOf(),
    /** The album shown, null for every picture. */
    val albumId: Long? = null,
    /** The pictures of the album shown. */
    val images: ImmutableList<DeviceImage> = persistentListOf(),
    /** The pictures picked, in the order they were picked or put in since. */
    val selected: ImmutableList<Uri> = persistentListOf(),
    /** Pictures edited here, each to what the edit wrote, which is given back in its place. */
    val edits: ImmutableMap<Uri, Uri> = persistentMapOf(),
    val maxItems: Int = 1,
) {
    val multiple: Boolean get() = maxItems > 1

    /** Whether no more can be picked, so the rest are shown out of reach. */
    val full: Boolean get() = multiple && selected.size >= maxItems

    /** What is given back: the picks in order, each as edited. */
    val result: List<Uri> get() = selected.map { edits[it] ?: it }
}

/**
 * Picking pictures from the device's albums, for when the system photo picker is not there or
 * the user turned it off. Up to [EXTRA_MAX_ITEMS] pictures can be picked; the picks survive the
 * process being killed.
 */
@HiltViewModel
class ImagePickerViewModel @Inject constructor(
    private val repository: DeviceImageRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(
        ImagePickerState(
            selected = savedStateHandle.get<ArrayList<Uri>>(KEY_SELECTED).orEmpty().toImmutableList(),
            edits = savedStateHandle.get<ArrayList<Uri>>(KEY_EDITED).orEmpty()
                .zip(savedStateHandle.get<ArrayList<Uri>>(KEY_EDITS).orEmpty())
                .toMap().toImmutableMap(),
            maxItems = (savedStateHandle.get<Int>(EXTRA_MAX_ITEMS) ?: 1).coerceAtLeast(1),
        )
    )
    val state: StateFlow<ImagePickerState> = _state.asStateFlow()

    private val _limitReached = Channel<Int>(Channel.CONFLATED)

    /** The limit, each time a pick is refused for going over it. */
    val limitReached: Flow<Int> = _limitReached.receiveAsFlow()

    private var images: List<DeviceImage> = emptyList()

    /** Photos taken here, newest first, shown before the device's own. */
    private var captured: List<DeviceImage> =
        savedStateHandle.get<ArrayList<Uri>>(KEY_CAPTURED).orEmpty().map(::capturedImage)
    private var loading: Job? = null

    /** Reads the pictures again, as after the access to them changed. */
    fun load() {
        loading?.cancel()
        loading = viewModelScope.launch {
            images = try {
                repository.images()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Thrown when the access was taken away
                emptyList()
            }
            val readable = (captured + images).mapTo(HashSet()) { it.uri }
            _state.update { state ->
                val albums = albumsOf(captured + images)
                val albumId = state.albumId?.takeIf { id -> albums.any { it.id == id } }
                state.copy(
                    loaded = true,
                    albums = albums,
                    albumId = albumId,
                    images = imagesIn(albumId),
                    selected = state.selected.filter { it in readable }.toImmutableList(),
                )
            }
            saveSelection()
        }
    }

    fun showAlbum(albumId: Long?) = _state.update { it.copy(albumId = albumId, images = imagesIn(albumId)) }

    /** Picks [uri], or puts it back if picked already. With one to pick, it takes the other's place. */
    fun toggle(uri: Uri) {
        val state = _state.value
        val selected = when {
            uri in state.selected -> state.selected - uri
            !state.multiple -> listOf(uri)
            state.selected.size >= state.maxItems -> {
                _limitReached.trySend(state.maxItems)
                return
            }

            else -> state.selected + uri
        }
        _state.update { it.copy(selected = selected.toImmutableList()) }
        saveSelection()
    }

    /**
     * Makes [uris] the picks, as a drag across the grid does, keeping their order and as many as
     * fit; says so when some did not.
     */
    fun select(uris: List<Uri>) {
        val state = _state.value
        val kept = uris.distinct().take(state.maxItems)
        if (kept.size < uris.distinct().size) _limitReached.trySend(state.maxItems)
        if (kept == state.selected) return
        _state.update { it.copy(selected = kept.toImmutableList()) }
        saveSelection()
    }

    /** Moves the pick at [from] to [to], to change the order they are given back in. */
    fun move(from: Int, to: Int) {
        val selected = _state.value.selected.toMutableList()
        if (from !in selected.indices || to !in selected.indices || from == to) return
        selected.add(to, selected.removeAt(from))
        _state.update { it.copy(selected = selected.toImmutableList()) }
        saveSelection()
    }

    /** Adds a photo just taken at [uri] first, and picks it where there is room. */
    fun captured(uri: Uri) {
        captured = listOf(capturedImage(uri)) + captured
        savedStateHandle[KEY_CAPTURED] = ArrayList(captured.map { it.uri })
        _state.update { it.copy(albums = albumsOf(captured + images), images = imagesIn(it.albumId)) }
        val state = _state.value
        if (!state.multiple || !state.full) toggle(uri)
    }

    /** Gives back [edited] in place of [original], picking it where there is room. */
    fun edited(original: Uri, edited: Uri) {
        _state.update { it.copy(edits = (it.edits + (original to edited)).toImmutableMap()) }
        val state = _state.value
        if (original !in state.selected && (!state.multiple || !state.full)) toggle(original)
        saveSelection()
    }

    private fun imagesIn(albumId: Long?): ImmutableList<DeviceImage> = when (albumId) {
        null -> (captured + images).toImmutableList()
        DeviceAlbum.Favorites -> images.filter { it.favorite }.toImmutableList()
        else -> images.filter { it.albumId == albumId }.toImmutableList()
    }

    private fun saveSelection() {
        val state = _state.value
        savedStateHandle[KEY_SELECTED] = ArrayList(state.selected)
        savedStateHandle[KEY_EDITED] = ArrayList(state.edits.keys)
        savedStateHandle[KEY_EDITS] = ArrayList(state.edits.values)
    }

    companion object {
        const val EXTRA_MAX_ITEMS = "max_items"
        private const val KEY_SELECTED = "selected"
        private const val KEY_EDITED = "edited"
        private const val KEY_EDITS = "edits"
        private const val KEY_CAPTURED = "captured"
    }
}

/** A photo taken in the picker, which belongs to no folder of the device's. */
private fun capturedImage(uri: Uri) = DeviceImage(uri, albumId = CapturedAlbum, albumName = "", mimeType = "image/jpeg")

/** Where photos taken in the picker are, which lists no folder of its own. */
private const val CapturedAlbum = Long.MIN_VALUE

private fun albumsOf(images: List<DeviceImage>): ImmutableList<DeviceAlbum> {
    if (images.isEmpty()) return persistentListOf()
    val all = DeviceAlbum(id = null, name = "", cover = images.first().uri, count = images.size)
    // Grouping keeps each folder's pictures newest first, so the first is the cover
    val folders = images.filter { it.albumId != CapturedAlbum }.groupBy { it.albumId }.map { (id, pictures) ->
        DeviceAlbum(id = id, name = pictures.first().albumName, cover = pictures.first().uri, count = pictures.size)
    }
    // Only where the gallery marks any: many keep their favourites to themselves
    val favorites = images.filter { it.favorite }.takeIf { it.isNotEmpty() }?.let { pictures ->
        DeviceAlbum(id = DeviceAlbum.Favorites, name = "", cover = pictures.first().uri, count = pictures.size)
    }
    return (listOfNotNull(all, favorites) + folders.sortedByDescending { it.count }).toImmutableList()
}
