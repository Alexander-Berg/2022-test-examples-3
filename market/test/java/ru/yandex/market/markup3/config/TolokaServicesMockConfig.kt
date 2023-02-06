package ru.yandex.market.markup3.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.yandex.market.markup3.yang.TolokaClientFactoryImplMock
import ru.yandex.market.markup3.yang.TolokaClientMock
import ru.yandex.market.markup3.yang.TolokaStagingClientFactoryMock
import ru.yandex.toloka.client.staging.TolokaStagingClientFactory
import ru.yandex.toloka.client.v1.impl.TolokaClientFactoryImpl

/**
 * @author york
 */
@Profile("test")
@Configuration
class TolokaClientServicesMockConfig : TolokaClientServicesConfig {

    @Bean
    fun tolokaClientMock(): TolokaClientMock = TolokaClientMock()

    @Bean
    override fun yangClientFactory(): TolokaClientFactoryImpl {
        return TolokaClientFactoryImplMock(tolokaClientMock())
    }

    @Bean
    override fun yangStagingClientFactory(): TolokaStagingClientFactory {
        return TolokaStagingClientFactoryMock(yangClientFactory() as TolokaClientFactoryImplMock)
    }

    @Bean
    override fun tolokaClientFactory(): TolokaClientFactoryImpl {
        return TolokaClientFactoryImplMock(tolokaClientMock())
    }

    @Bean
    override fun tolokaStagingClientFactory(): TolokaStagingClientFactory {
        return TolokaStagingClientFactoryMock(tolokaClientFactory() as TolokaClientFactoryImplMock)
    }
}
