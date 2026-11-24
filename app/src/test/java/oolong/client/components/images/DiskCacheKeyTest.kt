package oolong.client.components.images

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DiskCacheKeyTest {
    @Test
    fun `drops the expiring signature and keeps the rest of the address`() {
        assertEquals(
            "v2:https://tiebapic.baidu.com/forum/pic/item/a.jpg?x=1",
            diskCacheKey("https://tiebapic.baidu.com/forum/pic/item/a.jpg?tbpicau=2026-11-13-05_0fb7&x=1"),
        )
    }

    @Test
    fun `addresses without it keep their own key`() {
        assertNull(diskCacheKey("https://tiebapic.baidu.com/forum/pic/item/a.jpg"))
    }
}
