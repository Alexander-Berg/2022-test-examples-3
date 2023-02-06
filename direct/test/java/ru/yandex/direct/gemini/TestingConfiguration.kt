package ru.yandex.direct.gemini

import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.DefaultAsyncHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.yandex.direct.asynchttp.FetcherSettings
import ru.yandex.direct.asynchttp.ParallelFetcherFactory

@Configuration
open class TestingConfiguration {
    @Bean
    open fun asyncHttpClient(): AsyncHttpClient {
        return DefaultAsyncHttpClient()
    }

    @Bean
    open fun fetcherFactory(asyncHttpClient: AsyncHttpClient): ParallelFetcherFactory {
        return ParallelFetcherFactory(asyncHttpClient, FetcherSettings())
    }
}
