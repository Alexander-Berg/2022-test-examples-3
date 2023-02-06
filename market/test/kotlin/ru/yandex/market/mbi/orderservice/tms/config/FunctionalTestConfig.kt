package ru.yandex.market.mbi.orderservice.tms.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.mockito.kotlin.mock
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.retry.support.RetryTemplate
import ru.yandex.market.logistics.logistics4shops.api.InternalOrderApi
import ru.yandex.market.mbi.orderservice.common.config.CommonFunctionalTestConfig
import ru.yandex.market.mbi.orderservice.common.dsl.OrderTransitionGraph
import ru.yandex.market.mbi.orderservice.common.persistence.repository.pg.KeyValueRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.pg.PartnerRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.ProcessedCheckouterEventRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.service.external.CheckouterApiService
import ru.yandex.market.mbi.orderservice.common.service.pg.EnvironmentService
import ru.yandex.market.mbi.orderservice.common.service.pg.EventIdGenerator
import ru.yandex.market.mbi.orderservice.common.service.pg.PgEnvironmentService
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.OrderEventService
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.OrderItemsService
import ru.yandex.market.mbi.orderservice.tms.service.logbroker.events.CheckouterEventProcessor
import ru.yandex.market.mbi.orderservice.tms.service.logbroker.events.PartnerInfoSnapshotProcessor
import ru.yandex.market.mbi.orderservice.tms.service.yt.YtClusterLivenessProbe
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
    open fun checkouterEventProcessor(
        @Qualifier("checkouterAnnotationObjectMapper") checkouterAnnotationObjectMapper: ObjectMapper,
        environmentService: EnvironmentService,
        orderTransitionGraph: OrderTransitionGraph,
        orderRepository: YtOrderRepository,
        orderEventService: OrderEventService,
        orderItemsService: OrderItemsService,
        eventIdGenerator: EventIdGenerator,
        processedCheckouterEventRepository: ProcessedCheckouterEventRepository,
        checkouterApiService: CheckouterApiService
    ): CheckouterEventProcessor {
        return CheckouterEventProcessor(
            checkouterAnnotationObjectMapper,
            environmentService,
            orderTransitionGraph,
            orderEventService,
            orderItemsService,
            eventIdGenerator,
            checkouterApiService,
            processedCheckouterEventRepository,
            orderRepository
        )
    }

    @Bean
    open fun partnerInfoSnapshotProcessor(
        partnerRepository: PartnerRepository
    ): PartnerInfoSnapshotProcessor {
        return PartnerInfoSnapshotProcessor(partnerRepository)
    }

    @Bean
    open fun environmentService(keyValueRepository: KeyValueRepository): EnvironmentService {
        return PgEnvironmentService(
            keyValueRepository,
            durationAdjuster = { Duration.ofNanos(1) }
        )
    }

    @Bean
    open fun ytClusterLivenessProbe(
        ytJdbcTemplate: NamedParameterJdbcTemplate,
        retryTemplate: RetryTemplate,
    ): YtClusterLivenessProbe = object : YtClusterLivenessProbe(ytJdbcTemplate, retryTemplate) {
        override fun checkClusterLiveness() {
            // do nothing
        }

        override fun preDestroy() {
            // do nothing
        }
    }

    @Bean
    open fun internalOrderApi(): InternalOrderApi {
        return mock {}
    }
}
