package ru.yandex.market.logistics.mqm.queue.consumer

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.queue.dto.HandlePlanFactGroupDto
import ru.yandex.market.logistics.mqm.utils.gatherHibernateStatistic
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.persistence.EntityManagerFactory

internal class HandlePlanFactGroupConsumerTest: AbstractContextualTest() {

    @RegisterExtension
    @JvmField
    final val tskvLogCaptor = BackLogCaptor()

    @Autowired
    private lateinit var consumer: HandlePlanFactGroupConsumer

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @BeforeEach
    private fun setUp() {
        clock.setFixed(Instant.parse("2021-07-01T09:00:00.00Z"), DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DatabaseSetup("/queue/consumer/before/handle_plan_fact_group/setup.xml")
    @DisplayName("Не обновлять qualityRule")
    fun doNotUpdateQualityRule() {
        val startSchedulingTime = clock.instant().minus(10, ChronoUnit.MINUTES)
        val dto = HandlePlanFactGroupDto(listOf(1L), startSchedulingTime)
        val statistics = gatherHibernateStatistic(entityManagerFactory) {
            consumer.processPayload(dto)
        }
        statistics.entityUpdateCount shouldBe 1
    }

    @Test
    @DisplayName("Писать метрики обработки")
    @DatabaseSetup("/queue/consumer/before/handle_plan_fact_group/setup_for_metrics.xml")
    fun writeHandleMetrics() {
        val startSchedulingTime = clock.instant().minus(11, ChronoUnit.MINUTES)
        val dto = HandlePlanFactGroupDto(listOf(1L), startSchedulingTime)
        consumer.processPayload(dto)
        tskvLogCaptor.results.first {it.contains("timeSinceSchedulingInMs")} shouldContain "660000,"
    }
}
