package ru.yandex.market.mbi.orderservice.common.config

import org.mockito.kotlin.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import ru.yandex.market.checkout.checkouter.config.CheckouterAnnotationJsonConfig
import ru.yandex.market.checkout.checkouter.config.CheckouterSerializationJsonConfig
import ru.yandex.market.mbi.orderservice.common.service.external.CheckouterApiService
import ru.yandex.market.mbi.orderservice.common.service.external.CombinatorApiService
import ru.yandex.market.mbi.orderservice.common.service.external.GeocoderApiService
import ru.yandex.market.mbi.orderservice.common.service.external.StockStorageApiService
import ru.yandex.market.personal_market.PersonalMarketService
import ru.yandex.market.yt.client.YtClientProxy
import ru.yandex.market.yt.client.YtClientProxySource
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

@Configuration
@Import(CheckouterSerializationJsonConfig::class, CheckouterAnnotationJsonConfig::class)
open class CommonFunctionalTestConfig {

    @Bean
    @Profile("!localYt")
    open fun readWriteClient(): YtClientProxy = mock {}

    @Bean
    @Profile("!localYt")
    open fun readOnlyClient(): YtClientProxySource = mock {}

    @Bean
    open fun testClock(): Clock = Clock.fixed(
        LocalDate.of(2021, 10, 1).atStartOfDay().toInstant(ZoneOffset.UTC),
        ZoneId.systemDefault()
    )

    @Bean
    open fun mockCheckouterApiService(): CheckouterApiService {
        return mock {}
    }

    @Bean
    open fun mockGeocoderApiService(): GeocoderApiService {
        return mock {}
    }

    @Bean
    open fun combinatorApiService(): CombinatorApiService {
        return mock {}
    }

    @Bean
    open fun stockStorageApiService(): StockStorageApiService {
        return mock {}
    }

    @Bean
    fun personalMarketService(): PersonalMarketService {
        return mock {}
    }

    @Bean
    open fun clock(): Clock = mock { }
}
