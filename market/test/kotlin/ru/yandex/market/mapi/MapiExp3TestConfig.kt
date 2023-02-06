package ru.yandex.market.mapi

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.yandex.market.experiments3.client.Experiments3Client
import ru.yandex.market.mapi.configprovider.ClientConfigProviderImpl
import ru.yandex.market.mapi.configprovider.InfraConfigProviderImpl
import ru.yandex.market.mapi.core.MockContext

@Configuration
@Profile("junit")
open class MapiExp3TestConfig {

    @Bean
    open fun exp3Client() = MockContext.registerMock<Experiments3Client>()

    @Bean
    open fun clientConfigProvider() = MockContext.registerMock<ClientConfigProviderImpl>()

    @Bean
    open fun infraConfigProvider() = MockContext.registerMock<InfraConfigProviderImpl>()
}
