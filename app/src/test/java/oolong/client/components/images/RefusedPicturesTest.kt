package oolong.client.components.images

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RefusedPicturesTest {
    private val server = MockWebServer()
    private val client = OkHttpClient.Builder().addInterceptor(RefusedPictures).build()

    @Before
    fun start() = server.start()

    @After
    fun stop() = server.close()

    private fun fetch() = client.newCall(Request.Builder().url(server.url("/a.jpg")).build()).execute()

    @Test
    fun `the server's placeholder for a refused address is a failure`() {
        server.enqueue(MockResponse.Builder().addHeader("Error-Message", "img not auth").body("placeholder").build())
        fetch().use {
            assertFalse(it.isSuccessful)
            assertEquals(403, it.code)
        }
    }

    @Test
    fun `a picture it sends is kept`() {
        server.enqueue(MockResponse.Builder().body("picture").build())
        fetch().use {
            assertTrue(it.isSuccessful)
            assertEquals("picture", it.body.string())
        }
    }
}
