package ru.yandex.market.wms.inventory.config

import org.mockito.Mockito.mock
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import ru.yandex.market.wms.common.spring.config.settings.HttpClientSettings
import ru.yandex.market.wms.core.client.configuration.CoreWebClientConfig
import ru.yandex.market.wms.inventory.service.BalanceFetcherWms
import ru.yandex.passport.tvmauth.TvmClient

@TestConfiguration
open class InventoryTestConfig {
    @Bean
    @Primary
    @Qualifier(CoreWebClientConfig.CORE_CLIENT)
    open fun coreHttpClientSettings(): HttpClientSettings = mock(HttpClientSettings::class.java)

    @Bean
    @Primary
    open fun balanceFetcher(): BalanceFetcherWms = mock(BalanceFetcherWms::class.java)

    @Bean
    @Primary
    open fun tvmClient(): TvmClient = mock(TvmClient::class.java)
}
