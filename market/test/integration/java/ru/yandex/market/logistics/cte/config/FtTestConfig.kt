package ru.yandex.market.logistics.cte.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import ru.yandex.market.logistics.cte.service.FeatureToggleService

@Configuration
class FtTestConfig {

    @Bean
    @Primary
    fun ftService(): FeatureToggleService {
        return object : FeatureToggleService {
            override fun matrixSeparationEnabled(): Boolean = false
        }
    }
}
