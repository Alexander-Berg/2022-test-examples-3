package ru.yandex.direct.gemini

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.apache.http.HttpStatus
import org.assertj.core.api.Assertions
import org.junit.Test

class GeminiClientGetMainMirrorsFailedTest : GeminiClientTestBase() {

    companion object {
        val URLS = listOf("https://onelovebox-shop.ru")
    }

    @Test
    fun getMainMirrors() {
        Assertions.assertThatThrownBy {
            geminiClient.getMainMirrors(URLS)
        }.isInstanceOf(GeminiClientException::class.java)
    }

    override fun dispatcher(): Dispatcher {
        return object : Dispatcher() {
            override fun dispatch(request: RecordedRequest?): MockResponse {
                return MockResponse().setResponseCode(HttpStatus.SC_BAD_REQUEST).setBody("Failed")
            }
        }
    }
}
