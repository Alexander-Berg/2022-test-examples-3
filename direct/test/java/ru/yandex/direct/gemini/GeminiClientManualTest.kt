package ru.yandex.direct.gemini

import com.google.common.primitives.Ints
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.netty.util.HashedWheelTimer
import org.assertj.core.api.Assertions.assertThat
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.DefaultAsyncHttpClient
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import ru.yandex.direct.asynchttp.FetcherSettings
import ru.yandex.direct.asynchttp.ParallelFetcherFactory
import java.time.Duration

@Ignore("Ходит в сервис gemini с источником direct")
class GeminiClientManualTest {

    companion object {
        const val USER = "direct"

        val URLS_TO_MAIN_MIRRORS = mapOf(
            "https://onelovebox-shop.ru" to "https://www.onelovebox-shop.ru/",
            "https://yandex.ru" to "https://yandex.ru/",
            "incorrect_url" to null)
    }

    private lateinit var geminiClient: GeminiClient

    private fun getAsyncHttpClient(): AsyncHttpClient {
        val config = DefaultAsyncHttpClientConfig.Builder().run {
            setRequestTimeout(Ints.saturatedCast(Duration.ofSeconds(30).toMillis()))
            setReadTimeout(Ints.saturatedCast(Duration.ofSeconds(30).toMillis()))
            setConnectTimeout(Ints.saturatedCast(Duration.ofSeconds(10).toMillis()))
            setConnectionTtl(Ints.saturatedCast(Duration.ofMinutes(1).toMillis()))
            setPooledConnectionIdleTimeout(
                Ints.saturatedCast(Duration.ofSeconds(20).toMillis()))
            setIoThreadsCount(2)
            setNettyTimer(HashedWheelTimer(
                ThreadFactoryBuilder().setNameFormat("ahc-timer-%02d").setDaemon(true).build()))
            build()
        }

        return DefaultAsyncHttpClient(config)
    }

    @Before
    fun setUp() {
        val fetcherSettings = FetcherSettings()
        val parallelFetcherFactory = ParallelFetcherFactory(getAsyncHttpClient(), fetcherSettings)

        geminiClient = GeminiClient("http://gemini.search.yandex.net:9017", USER, parallelFetcherFactory)
    }

    @Test
    fun getMainMirror_success() {
        val urls = URLS_TO_MAIN_MIRRORS.keys
        val result = geminiClient.getMainMirrors(urls)
        val expectedResult = URLS_TO_MAIN_MIRRORS.filter { it.value != null }
        assertThat(result).isEqualTo(expectedResult)
    }

}
