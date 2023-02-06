package ru.yandex.direct.core.testing.stub

import org.asynchttpclient.DefaultAsyncHttpClient
import ru.yandex.direct.asynchttp.FetcherSettings
import ru.yandex.direct.asynchttp.ParallelFetcherFactory
import ru.yandex.direct.gemini.GeminiClient
import ru.yandex.direct.utils.model.UrlParts

class GeminiClientStub : GeminiClient(
    "https://do-not-use-me.yandex.ru/", "test-user",
    ParallelFetcherFactory(DefaultAsyncHttpClient(), FetcherSettings()),
) {
    override fun getMainMirrors(urls: Collection<String>): Map<String, String> {
        return urls.associateWith { 
            val urlParts = UrlParts.fromUrl(it)
            "${urlParts.protocol}://${urlParts.domain}/"
        }
    }
}
