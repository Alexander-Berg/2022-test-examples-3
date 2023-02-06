package ru.yandex.market.pricingmgmt.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import ru.yandex.market.pricingmgmt.util.SynchronousExecutionService
import java.util.concurrent.ExecutorService

@Configuration
@Profile("unittest")
open class SyncExecutorConfig {

    @Bean
    open fun priceImportExecutor(): ExecutorService = SynchronousExecutionService()
}
