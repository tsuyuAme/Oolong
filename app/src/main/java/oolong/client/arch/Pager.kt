package oolong.client.arch

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One page of a list. [next] and [prev] are the keys of the neighbouring pages, or null when the
 * list ends in that direction. What a key is (a page number, an offset, the ids still to load…)
 * is up to whoever loads the pages.
 */
data class Page<K : Any, T>(
    val items: List<T>,
    val next: K? = null,
    val prev: K? = null,
)

@Immutable
sealed interface LoadStatus {
    data object Idle : LoadStatus

    data object Loading : LoadStatus

    data class Error(val error: Throwable) : LoadStatus
}

@Immutable
data class PagerState<T>(
    val items: ImmutableList<T> = persistentListOf(),
    val refresh: LoadStatus = LoadStatus.Idle,
    val append: LoadStatus = LoadStatus.Idle,
    val prepend: LoadStatus = LoadStatus.Idle,
    val hasMore: Boolean = false,
    val hasPrevious: Boolean = false,
)

/** What a [Pager] is loading a page for, told to its load. */
class PageLoad internal constructor(
    /** Whether the page replaces the list, or for a feed goes in front of it, rather than extends it. */
    val refreshing: Boolean,
)

enum class RefreshMode {
    /** A refresh replaces the list. */
    Replace,

    /** A refresh puts the new items in front of the ones already loaded, like a feed. */
    Prepend,
}

/**
 * Loads a list page by page in either direction and keeps it as one [PagerState].
 *
 * Items are told apart by [itemKey]. A page loaded at either end never adds or changes an item
 * already in the list (use [update] for that); a [RefreshMode.Prepend] refresh does replace them
 * with the new copies.
 *
 * A page that adds nothing new ends the list in that direction, even if it names another page:
 * a wrong cursor then stops the list early instead of requesting the same page over and over.
 *
 * Only one load runs per direction, and a refresh cancels the loads in progress, whose keys belong
 * to the list being replaced. Loading starts when the pager is created.
 */
class Pager<K : Any, T>(
    private val scope: CoroutineScope,
    private val initialKey: K,
    private val itemKey: (T) -> Any,
    private val refreshMode: RefreshMode = RefreshMode.Replace,
    private val load: suspend PageLoad.(key: K) -> Page<K, T>,
) {
    private val _state = MutableStateFlow(PagerState<T>())
    val state: StateFlow<PagerState<T>> = _state.asStateFlow()

    private var nextKey: K? = null
    private var prevKey: K? = null

    private var refreshJob: Job? = null
    private var appendJob: Job? = null
    private var prependJob: Job? = null

    init {
        refresh()
    }

    /**
     * Loads the list again from [from], which also serves to jump to another page. With [clear],
     * the items go at once, for a list that stands for something else now, such as another filter:
     * the old ones would pass for the answer while it loads.
     */
    fun refresh(from: K = initialKey, clear: Boolean = false) {
        refreshJob?.cancel()
        appendJob?.cancel()
        prependJob?.cancel()
        _state.update {
            val loading = it.copy(refresh = LoadStatus.Loading, append = LoadStatus.Idle, prepend = LoadStatus.Idle)
            if (clear) loading.copy(items = persistentListOf(), hasMore = false, hasPrevious = false) else loading
        }
        refreshJob = scope.launch {
            val page = loadOrNull(from, refreshing = true) { error -> copy(refresh = LoadStatus.Error(error)) }
                ?: return@launch
            nextKey = page.next
            prevKey = page.prev
            _state.update { state ->
                val items = when (refreshMode) {
                    RefreshMode.Replace -> page.items
                    RefreshMode.Prepend -> page.items + state.items
                }
                state.copy(
                    items = items.distinctBy(itemKey).toImmutableList(),
                    refresh = LoadStatus.Idle,
                    hasMore = page.next != null,
                    hasPrevious = page.prev != null,
                )
            }
        }
    }

    /**
     * Loads the page after the list. [from] overrides the key of that page, for lists that can
     * grow at the end after reaching it, such as a thread receiving new replies.
     */
    fun loadMore(from: K? = null) {
        val key = from ?: nextKey ?: return
        if (refreshJob?.isActive == true || appendJob?.isActive == true) return
        _state.update { it.copy(append = LoadStatus.Loading) }
        appendJob = scope.launch {
            val page = loadOrNull(key, refreshing = false) { error -> copy(append = LoadStatus.Error(error)) }
                ?: return@launch
            val newItems = _state.value.newItems(page.items)
            nextKey = page.next.takeIf { newItems.isNotEmpty() }
            _state.update { state ->
                state.copy(
                    items = (state.items + newItems).toImmutableList(),
                    append = LoadStatus.Idle,
                    hasMore = nextKey != null,
                )
            }
        }
    }

    /** Loads the page before the list; [from] overrides its key like in [loadMore]. */
    fun loadPrevious(from: K? = null) {
        val key = from ?: prevKey ?: return
        if (refreshJob?.isActive == true || prependJob?.isActive == true) return
        _state.update { it.copy(prepend = LoadStatus.Loading) }
        prependJob = scope.launch {
            val page = loadOrNull(key, refreshing = false) { error -> copy(prepend = LoadStatus.Error(error)) }
                ?: return@launch
            val newItems = _state.value.newItems(page.items)
            prevKey = page.prev.takeIf { newItems.isNotEmpty() }
            _state.update { state ->
                state.copy(
                    items = (newItems + state.items).toImmutableList(),
                    prepend = LoadStatus.Idle,
                    hasPrevious = prevKey != null,
                )
            }
        }
    }

    /** Changes the loaded items in place, e.g. to mark one as liked or to remove a deleted one. */
    fun update(transform: (ImmutableList<T>) -> List<T>) {
        _state.update { it.copy(items = transform(it.items).toImmutableList()) }
    }

    private fun PagerState<T>.newItems(page: List<T>): List<T> {
        val loaded = items.mapTo(HashSet(), itemKey)
        return page.distinctBy(itemKey).filterNot { itemKey(it) in loaded }
    }

    private suspend fun loadOrNull(
        key: K,
        refreshing: Boolean,
        onError: PagerState<T>.(Throwable) -> PagerState<T>,
    ): Page<K, T>? = try {
        // A load that swallowed its cancellation must not write a page of a list since replaced
        PageLoad(refreshing).load(key).also { currentCoroutineContext().ensureActive() }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        _state.update { it.onError(e) }
        null
    }
}

fun <K : Any, T> ViewModel.pager(
    initialKey: K,
    itemKey: (T) -> Any,
    refreshMode: RefreshMode = RefreshMode.Replace,
    load: suspend (key: K) -> Page<K, T>,
): Pager<K, T> = Pager(viewModelScope, initialKey, itemKey, refreshMode) { key -> load(key) }
