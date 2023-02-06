package ru.yandex.market.mbi.orderservice.api.config

import org.mockito.kotlin.mock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import ru.yandex.market.logistics.logistics4shops.api.InternalOrderApi
import ru.yandex.market.mbi.orderservice.api.persistence.dao.yt.summary.WeeklyCompensatedOrdersDao
import ru.yandex.market.mbi.orderservice.common.config.CommonFunctionalTestConfig
import ru.yandex.market.mbi.orderservice.common.persistence.repository.pg.KeyValueRepository
import ru.yandex.market.mbi.orderservice.common.service.external.Logistics4ShopsApiService
import ru.yandex.market.mbi.orderservice.common.service.pg.EnvironmentService
import ru.yandex.market.mbi.orderservice.common.service.pg.PgEnvironmentService
import java.time.Duration

@Configuration
@Import(CommonFunctionalTestConfig::class)
open class FunctionalTestConfig {

    companion object {
        @JvmStatic
        @Bean
        fun propertyConfigurer(): PropertySourcesPlaceholderConfigurer =
            PropertySourcesPlaceholderConfigurer().apply {
                order = -1
                setIgnoreUnresolvablePlaceholders(false)
            }
    }

    @Bean
    open fun environmentService(keyValueRepository: KeyValueRepository): EnvironmentService {
        return PgEnvironmentService(
            keyValueRepository,
            durationAdjuster = { Duration.ofNanos(1) }
        )
    }

    @Bean
    open fun logistics4ShopsApiService(): Logistics4ShopsApiService {
        return mock {}
    }

    @Bean
    open fun internalOrderApi(): InternalOrderApi {
        return mock {}
    }

    @Primary
    @Bean
    open fun mockWeeklyCompensatedOrdersDao(): WeeklyCompensatedOrdersDao = mock { }
}
