package ru.yandex.market.logistics.mqm.tms.statistics

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.processor.planfact.customerorder.DeliveryToLomCustomerOrderPlanFactProcessor
import ru.yandex.market.logistics.mqm.service.tms.statistics.StatisticsCollectorRunner
import ru.yandex.market.logistics.mqm.utils.tskvGetByKey
import ru.yandex.market.logistics.mqm.utils.tskvGetExtra
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor

class StatisticsCollectorRunnerTest: AbstractContextualTest() {
    @Autowired
    private lateinit var executor: StatisticsCollectorRunner

    @RegisterExtension
    @JvmField
    final val tskvLogCaptor = BackLogCaptor()

    @Test
    @DisplayName("Успешное исполнение")
    @DatabaseSetup("/tms/statistics/StatisticsCollectorRunner/setup.xml")
    fun successTest() {
        executor.run()

        val log = tskvLogCaptor.results[0]

        tskvGetByKey(log, "code") shouldBe Pair("code", "PLAN_FACT_STATISTICS")
        tskvGetExtra(log) shouldContainAll listOf(
            Pair("producerName", DeliveryToLomCustomerOrderPlanFactProcessor::class.java.simpleName),
            Pair("activeCount", "1"),
            Pair("openIssuesCount", "1"),
        )
    }
}
