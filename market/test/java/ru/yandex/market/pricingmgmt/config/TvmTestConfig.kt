package ru.yandex.market.pricingmgmt.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ru.yandex.market.pricingmgmt.config.tvm.DummyTvmClient
import ru.yandex.passport.tvmauth.TvmClient

@Configuration
open class TvmTestConfig {

    @Bean
    open fun tvmClient(): TvmClient {
        return DummyTvmClient()
    }
}
