package ru.yandex.market.partner.status

import org.mockito.kotlin.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.yandex.market.abo.api.client.AboAPI
import ru.yandex.market.ff4shops.client.FF4ShopsClient
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.tarificator.open.api.client.api.ShopDeliveryStateApi
import ru.yandex.market.mbi.api.billing.client.MbiBillingClient
import ru.yandex.market.mbi.api.client.MbiApiClient
import ru.yandex.market.mbi.datacamp.saas.SaasService
import ru.yandex.market.mbi.logprocessor.client.MbiLogProcessorClient
import ru.yandex.market.mbi.open.api.client.MbiOpenApiClient
import ru.yandex.market.partner.status.status.resolver.UpdateResolverService
import ru.yandex.market.partner.status.status.resolver.impl.TestUpdateResolverTaskQueue
import ru.yandex.market.partner.status.status.resolver.impl.UpdateResolverTaskQueueImpl
import ru.yandex.market.partner.status.yt.factory.YtTableReaderFactory
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Configuration
class FunctionalTestConfig {

    @Bean
    fun clock(): Clock {
        return Clock.fixed(
            Instant.from(
                LocalDateTime.of(2022, 2, 17, 11, 24).atZone(ZoneId.of("Europe/Moscow"))
            ),
            ZoneId.of("Europe/Moscow")
        )
    }

    @Bean
    fun mbiOpenApiClient(): MbiOpenApiClient {
        return mock { }
    }

    @Bean
    fun mbiBillingClient(): MbiBillingClient {
        return mock { }
    }

    @Bean
    fun mbiApiClient(): MbiApiClient {
        return mock { }
    }

    @Bean
    fun saasService(): SaasService {
        return mock { }
    }

    @Bean
    fun lmsClient(): LMSClient {
        return mock { }
    }

    @Bean
    fun mbiLogProcessorClient(): MbiLogProcessorClient {
        return mock { }
    }

    @Bean
    fun ff4shopsClient(): FF4ShopsClient {
        return mock { }
    }

    @Bean
    fun aboApi(): AboAPI {
        return mock { }
    }

    @Bean
    fun shopDeliveryStateApi(): ShopDeliveryStateApi {
        return mock { }
    }

    @Bean
    fun ytTableReaderFactory(): YtTableReaderFactory {
        return mock { }
    }

    @Bean
    fun testUpdateResolverTaskQueue(
        updateResolverServices: List<UpdateResolverService>
    ): TestUpdateResolverTaskQueue {
        return TestUpdateResolverTaskQueue(
            updateResolverServices,
            UpdateResolverTaskQueueImpl(
                updateResolverServices,
                10
            )
        )
    }
}
