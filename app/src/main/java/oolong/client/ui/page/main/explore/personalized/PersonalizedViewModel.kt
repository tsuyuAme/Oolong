package oolong.client.ui.page.main.explore.personalized

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import oolong.client.R
import oolong.client.arch.Actions
import oolong.client.arch.LoadStatus
import oolong.client.arch.MessageController
import oolong.client.arch.Page
import oolong.client.arch.Pager
import oolong.client.arch.PagerState
import oolong.client.arch.RefreshMode
import oolong.client.arch.query.QueryClient
import oolong.client.arch.query.pager
import oolong.client.model.DislikeOption
import oolong.client.model.RecommendedThread
import oolong.client.model.ThreadSummary
import oolong.client.repository.BlockMemo
import oolong.client.repository.BlockRuleSource
import oolong.client.repository.ExploreRepository
import oolong.client.repository.ThreadActions
import oolong.client.repository.Threads
import oolong.client.ui.page.main.explore.FeedThread
import oolong.client.ui.page.main.explore.feedThread
import oolong.client.ui.page.main.explore.withItems
import javax.inject.Inject

/** A recommended thread, whether the block rules hide it, and why the user may not want it. */
@Immutable
data class RecommendedItem(
    val feed: FeedThread,
    val dislikeOptions: ImmutableList<DislikeOption> = persistentListOf(),
)

/** What a refresh of a list already shown brought. */
@Immutable
data class FeedRefresh(
    /** How many threads it added in front of the list. */
    val count: Int,
    /** Counts the refreshes, so that one bringing as many threads as the last still shows. */
    val id: Int,
    /** Whether it failed, leaving the list as it was. */
    val failed: Boolean = false,
)

@Immutable
data class PersonalizedState(
    val list: PagerState<RecommendedItem> = PagerState(refresh = LoadStatus.Loading),
    /** The threads the user said they don't want, which the list hides. */
    val disliked: ImmutableSet<Long> = persistentSetOf(),
    /**
     * Threads just disliked, which can still be taken back: each shows a row in its place saying
     * what the server will do, by thread id, or null when the server says nothing.
     */
    val undoable: ImmutableMap<Long, String?> = persistentMapOf(),
    /**
     * The thread that was first before the last refresh: the threads above it are new, and the
     * list marks where it begins. Null before any refresh of a list already shown.
     */
    val seenFrom: Long? = null,
    /**
     * The refresh that just ended, until [PersonalizedViewModel.refreshShown] says it was shown;
     * null while one is still loading, so it comes together with the threads it brought.
     */
    val lastRefresh: FeedRefresh? = null,
)

/**
 * Recommended threads, loaded the first time the tab shows. A pull-to-refresh replaces the list;
 * scrolling loads more pages after it.
 */
@HiltViewModel
class PersonalizedViewModel @Inject constructor(
    private val repository: ExploreRepository,
    private val client: QueryClient,
    private val threads: ThreadActions,
    private val messages: MessageController,
    blockRuleSource: BlockRuleSource,
) : ViewModel() {
    private val actions = Actions(viewModelScope)
    private val pager = MutableStateFlow<Pager<Int, RecommendedThread>?>(null)

    /** Block flags for each thread, which most state changes leave as they are. */
    private val blocked = BlockMemo<RecommendedThread, RecommendedItem>()
    private val local = MutableStateFlow(Local())

    private val pendingDislikes = mutableMapOf<Long, Job>()

    /** Whether the list last loaded has been looked for, which is done once, before the first load. */
    private var keptTried = false

    /** Remember list scroll so leaving the tab / opening a thread does not jump back to the top. */
    var scrollIndex: Int = 0
    var scrollOffset: Int = 0

    private data class Local(
        val disliked: Set<Long> = emptySet(),
        val undoable: Map<Long, String?> = emptyMap(),
        val seenFrom: Long? = null,
        val lastRefresh: FeedRefresh? = null,
        /** The id of the last refresh shown. */
        val shownRefresh: Int = 0,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<PersonalizedState> = combine(
        pager.flatMapLatest { it?.state ?: flowOf(PagerState(refresh = LoadStatus.Loading)) },
        local,
        blockRuleSource.rules,
    ) { list, local, rules ->
        PersonalizedState(
            list = list.withItems(blocked.map(rules, list.items) { RecommendedItem(rules.feedThread(it.thread), it.dislikeOptions) }),
            disliked = local.disliked.toImmutableSet(),
            undoable = local.undoable.toImmutableMap(),
            seenFrom = local.seenFrom,
            lastRefresh = local.lastRefresh
                ?.takeIf { it.id != local.shownRefresh && list.refresh != LoadStatus.Loading },
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PersonalizedState())

    /** Starts loading the list unless it already has. */
    fun open() {
        pager()
    }

    fun refresh() = pager().refresh()

    fun loadMore() = pager().loadMore()

    /** Marks refresh [id] shown, so that coming back to the list doesn't show it again. */
    fun refreshShown(id: Int) {
        local.update { it.copy(shownRefresh = maxOf(it.shownRefresh, id)) }
    }

    fun agree(thread: ThreadSummary) = actions.launch("agree-${thread.id}") {
        runCatching { threads.agree(thread, !thread.agree.agreed) }
            .onFailure { messages.showError(R.string.toast_agree_failed, it) }
    }

    /**
     * Puts a row that can take it back in place of [thread], then after a while hides it and tells
     * the server why the user doesn't want it; it stays hidden if that fails.
     */
    fun dislike(thread: ThreadSummary, reasons: List<DislikeOption>, clickTime: Long) {
        val threadId = thread.id
        if (threadId in pendingDislikes || threadId in local.value.disliked) return
        local.update { it.copy(undoable = it.undoable + (threadId to reasons.firstNotNullOfOrNull { r -> r.toast })) }
        pendingDislikes[threadId] = viewModelScope.launch {
            delay(UNDO_WINDOW)
            pendingDislikes.remove(threadId)
            local.update { it.copy(undoable = it.undoable - threadId, disliked = it.disliked + threadId) }
            runCatching { repository.dislike(thread, reasons, clickTime) }
        }
    }

    /** Takes back disliking thread [threadId] while its row still shows, before the server hears of it. */
    fun undoDislike(threadId: Long) {
        pendingDislikes.remove(threadId)?.cancel() ?: return
        local.update { it.copy(undoable = it.undoable - threadId) }
    }

    private fun pager(): Pager<Int, RecommendedThread> {
        pager.value?.let { return it }
        // Page 1 asks for new threads, the pages after it continue the list
        return client.pager(
            scope = viewModelScope,
            initialKey = 1,
            itemKey = { item: RecommendedThread -> item.thread.id },
            normalize = { it.copy(thread = Threads(it.thread)) },
            refreshMode = RefreshMode.Replace,  // 刷新清空列表，与官方一致
        ) { page ->
            val loaded = pager.value?.state?.value?.items.orEmpty()
            // Without loading anew as the app opens, the list shows as it was last loaded
            if (page == 1 && loaded.isEmpty() && !keptTried) {
                keptTried = true
                repository.keptRecommended()?.let { kept ->
                    local.update { it.copy(disliked = it.disliked + kept.hidden) }
                    return@pager Page(kept.items, next = kept.next ?: 2)
                }
            }
            val threads = try {
                repository.recommended(refresh = page == 1, page = page, newList = loaded.isEmpty())
            } catch (e: Exception) {
                if (page == 1 && loaded.isNotEmpty() && e !is CancellationException) onRefreshFailed()
                throw e
            }
            if (page == 1 && loaded.isNotEmpty()) onRefreshed(loaded, threads)
            Page(threads, next = page + 1)
        }.also { pager.value = it }
    }

    private fun onRefreshed(loaded: List<RecommendedThread>, threads: List<RecommendedThread>) {
        val loadedIds = loaded.mapTo(HashSet()) { it.thread.id }
        val count = threads.distinctBy { it.thread.id }.count { it.thread.id !in loadedIds }
        // Replace mode: no "seen here" divider; still toast how many new ids vs previous list
        local.update {
            it.copy(
                seenFrom = null,
                lastRefresh = FeedRefresh(count, (it.lastRefresh?.id ?: 0) + 1),
            )
        }
        scrollIndex = 0
        scrollOffset = 0
    }

    private fun onRefreshFailed() {
        local.update { it.copy(lastRefresh = FeedRefresh(0, (it.lastRefresh?.id ?: 0) + 1, failed = true)) }
    }

    private companion object {
        /** How long a disliked thread can be taken back. */
        const val UNDO_WINDOW = 5_000L
    }
}
