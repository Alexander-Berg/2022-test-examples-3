package ru.yandex.market.wms.replenishment.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import ru.yandex.market.wms.common.spring.utils.uuid.FixedListTestUuidGenerator
import ru.yandex.market.wms.common.spring.utils.uuid.UuidGenerator
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@TestConfiguration
open class OrderReplenishmentTestConfig {
    @Primary
    @Bean
    open fun uuidGenerator(): UuidGenerator = FixedListTestUuidGenerator(
        listOf(
            "6d809e60-d707-11ea-9550-a9553a7b0571",
            "6d809e60-d707-11ea-9550-a9553a7b0572",
            "6d809e60-d707-11ea-9550-a9553a7b0573"
        )
    )

    @Bean
    open fun fixedUuidGenerator(): FixedListTestUuidGenerator = uuidGenerator() as FixedListTestUuidGenerator

    @Bean
    @Primary
    open fun orderReplenishmentClock(): Clock = Clock.fixed(Instant.parse("2020-03-17T12:34:56.789Z"), ZoneOffset.UTC)
}
