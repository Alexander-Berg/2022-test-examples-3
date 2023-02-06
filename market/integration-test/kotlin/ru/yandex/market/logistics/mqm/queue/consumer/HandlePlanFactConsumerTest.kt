package ru.yandex.market.logistics.mqm.queue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.queue.dto.HandlePlanFactDto
import ru.yandex.market.logistics.mqm.utils.gatherHibernateStatistic
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.persistence.EntityManagerFactory

internal class HandlePlanFactConsumerTest: AbstractContextualTest() {

    @Autowired
    private lateinit var consumer: HandlePlanFactConsumer

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @BeforeEach
    private fun setUp() {
        clock.setFixed(Instant.parse("2021-07-01T09:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DatabaseSetup("/queue/consumer/before/handle_plan_fact/setup.xml")
    @DisplayName("Не обновлять qualityRule")
    fun doNotUpdateQualityRule() {
        val startSchedulingTime = clock.instant().minus(10, ChronoUnit.MINUTES)
        val dto = HandlePlanFactDto(listOf(1L), startSchedulingTime)
        val statistics = gatherHibernateStatistic(entityManagerFactory) {
            consumer.processPayload(dto)
        }
        statistics.entityUpdateCount shouldBe 2
    }
}

