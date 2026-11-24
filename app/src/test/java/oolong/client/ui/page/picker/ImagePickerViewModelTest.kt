package oolong.client.ui.page.picker

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import oolong.client.model.DeviceImage
import oolong.client.repository.DeviceImageRepository
import oolong.client.testing.MainDispatcherRule
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class ImagePickerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = mockk<DeviceImageRepository>()

    private fun uri(id: Int) = Uri.parse("content://media/external/images/media/$id")

    private fun image(id: Int, album: Long) = DeviceImage(uri(id), album, "album $album", "image/jpeg")

    private fun viewModel(maxItems: Int, saved: SavedStateHandle = SavedStateHandle(mapOf("max_items" to maxItems))) =
        ImagePickerViewModel(repository, saved)

    @Test
    fun `lists every picture first, then the folders by size`() = runTest {
        coEvery { repository.images() } returns listOf(image(1, 10), image(2, 20), image(3, 20), image(4, 10), image(5, 20))
        val viewModel = viewModel(maxItems = 1)

        viewModel.load()
        runCurrent()

        val state = viewModel.state.value
        assertEquals(listOf(null, 20L, 10L), state.albums.map { it.id })
        assertEquals(listOf(5, 3, 2), state.albums.map { it.count })
        assertEquals(listOf(uri(1), uri(2), uri(1)), state.albums.map { it.cover })
        assertEquals(5, state.images.size)

        viewModel.showAlbum(20)

        assertEquals(listOf(uri(2), uri(3), uri(5)), viewModel.state.value.images.map { it.uri })
    }

    @Test
    fun `keeps the picks in order and refuses one over the limit`() = runTest {
        coEvery { repository.images() } returns (1..4).map { image(it, 10) }
        val viewModel = viewModel(maxItems = 2)
        viewModel.load()
        runCurrent()

        viewModel.toggle(uri(3))
        viewModel.toggle(uri(1))
        viewModel.toggle(uri(2))

        assertEquals(listOf(uri(3), uri(1)), viewModel.state.value.selected)
        assertEquals(2, viewModel.limitReached.first())

        viewModel.toggle(uri(3))

        assertEquals(listOf(uri(1)), viewModel.state.value.selected)
    }

    @Test
    fun `keeps the picks across process death and drops those no longer readable`() = runTest {
        coEvery { repository.images() } returns (1..4).map { image(it, 10) }
        val saved = SavedStateHandle(mapOf("max_items" to 9))
        viewModel(maxItems = 9, saved).apply {
            load()
            runCurrent()
            toggle(uri(2))
            toggle(uri(4))
        }

        coEvery { repository.images() } returns (1..3).map { image(it, 10) }
        val restored = viewModel(maxItems = 9, saved)
        assertEquals(listOf(uri(2), uri(4)), restored.state.value.selected)
        restored.load()
        runCurrent()

        assertEquals(listOf(uri(2)), restored.state.value.selected)
    }

    @Test
    fun `with one to pick, another pick takes the first one's place`() = runTest {
        coEvery { repository.images() } returns (1..3).map { image(it, 10) }
        val viewModel = viewModel(maxItems = 1)
        viewModel.load()
        runCurrent()

        viewModel.toggle(uri(1))
        viewModel.toggle(uri(2))

        assertEquals(listOf(uri(2)), viewModel.state.value.selected)
    }

    @Test
    fun `a drag picks as many as fit, and the picks can be put in another order`() = runTest {
        coEvery { repository.images() } returns (1..5).map { image(it, 10) }
        val viewModel = viewModel(maxItems = 3)
        viewModel.load()
        runCurrent()

        viewModel.select(listOf(uri(1), uri(2), uri(3), uri(4)))

        assertEquals(listOf(uri(1), uri(2), uri(3)), viewModel.state.value.selected)
        assertEquals(true, viewModel.state.value.full)
        assertEquals(3, viewModel.limitReached.first())

        viewModel.move(from = 2, to = 0)

        assertEquals(listOf(uri(3), uri(1), uri(2)), viewModel.state.value.selected)
    }

    @Test
    fun `a photo taken comes first and is picked, and an edit is given back in its place`() = runTest {
        coEvery { repository.images() } returns (1..3).map { image(it, 10) }
        val viewModel = viewModel(maxItems = 9)
        viewModel.load()
        runCurrent()
        val photo = Uri.parse("content://app/picker/photo.jpg")
        val edit = Uri.parse("content://app/picker/edit.jpg")

        viewModel.toggle(uri(1))
        viewModel.captured(photo)
        viewModel.edited(uri(2), edit)

        val state = viewModel.state.value
        assertEquals(photo, state.images.first().uri)
        assertEquals(listOf(uri(1), photo, uri(2)), state.selected)
        assertEquals(listOf(uri(1), photo, edit), state.result)
        // A photo taken lists no folder of its own
        assertEquals(listOf(null, 10L), state.albums.map { it.id })
    }

    @Test
    fun `shows nothing when the pictures cannot be read`() = runTest {
        coEvery { repository.images() } throws SecurityException()
        val viewModel = viewModel(maxItems = 1)

        viewModel.load()
        runCurrent()

        assertEquals(true, viewModel.state.value.loaded)
        assertEquals(0, viewModel.state.value.images.size)
    }
}
