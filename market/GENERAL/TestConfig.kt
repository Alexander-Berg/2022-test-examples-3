package ru.yandex.market.mdm.service.functional.config

import org.mockito.Mockito
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import ru.yandex.passport.tvmauth.TvmClient

@Configuration
open class TestConfig {

    @Bean
    @Primary
    open fun tvmClient(): TvmClient {
        return Mockito.mock(TvmClient::class.java)
    }
}
