package ru.yandex.market.logistics.calendaring.config

import org.mockito.Mockito
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import ru.yandex.market.logistics.util.client.tvm.TvmSecurityConfiguration
import ru.yandex.market.logistics.util.client.tvm.TvmTicketChecker
import ru.yandex.market.logistics.util.client.tvm.TvmTicketCheckerImpl
import ru.yandex.market.logistics.util.client.tvm.client.TvmClientApi

@Import(TvmSecurityConfiguration::class)
open class SecurityTestConfig {
    @Bean
    @Primary
    open fun tvmClientApi(): TvmClientApi {
        return Mockito.mock(TvmClientApi::class.java)
    }

    @Bean
    @ConfigurationProperties("tvm.internal")
    open fun tvmTicketChecker(): TvmTicketChecker {
        return TvmTicketCheckerImpl()
    }
}
