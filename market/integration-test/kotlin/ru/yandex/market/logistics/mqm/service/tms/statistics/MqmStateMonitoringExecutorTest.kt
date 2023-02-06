package ru.yandex.market.logistics.mqm.service.tms.statistics

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.utils.tskvGetByKey
import ru.yandex.market.logistics.mqm.utils.tskvGetEntity
import ru.yandex.market.logistics.mqm.utils.tskvGetExtra
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor

class MqmStateMonitoringExecutorTest: AbstractContextualTest() {

    @RegisterExtension
    @JvmField
    final val tskvLogCaptor = BackLogCaptor()

    @Autowired
    lateinit var executor: MqmStateMonitoringExecutor

    @Test
    @DisplayName("Создание метрик на основе данных в бд")
    @DatabaseSetup("/tms/MqmStateMonitoringExecutor/setup.xml")
    fun createNewGroup() {
        executor.run()

        val actualMap = tskvLogCaptor.results.map { log ->
            mutableListOf<Pair<String, String?>>().apply {
                this.addAll(tskvGetExtra(log))
                this.addAll(tskvGetEntity(log))
                this.add(tskvGetByKey(log, CODE_KEY))
            }
        }

        assertSoftly {
            actualMap shouldContainAll
                setOf(
                    preparePlanFactLog(),
                    preparePlanFactLog(
                        producerName = "producer_second",
                        count = "1",
                    ),
                    preparePlanFactGroupLog(),
                    preparePlanFactGroupLog(
                        aggregationType = "DROPOFF_DATE_PARTNER",
                        count = "2",
                    )
                )
        }
    }

    private fun preparePlanFactLog(
        processingStatus: String = "ENQUEUED",
        planFactStatus: String = "ACTIVE",
        producerName: String = "producer_first",
        count: String = "2",
        expectedStatus: String = "IN",
        code: String = PLAN_FACT_CODE_VALUE
    ) = listOf(
        Pair("processingStatus", processingStatus),
        Pair("planFactStatus", planFactStatus),
        Pair("producerName", producerName),
        Pair("count", count),
        Pair("expectedStatus", expectedStatus),
        Pair("code", code)
    )

    private fun preparePlanFactGroupLog(
        processingStatus: String = "ENQUEUED",
        aggregationType: String = "EXPRESS",
        count: String = "1",
        expectedStatus: String = "OUT",
        code: String = PLAN_FACT_GROUP_CODE_VALUE
    ) = listOf(
        Pair("processingStatus", processingStatus),
        Pair("aggregationType", aggregationType),
        Pair("count", count),
        Pair("expectedStatus", expectedStatus),
        Pair("code", code)
    )

    companion object {
        val CODE_KEY = "code"
        val PLAN_FACT_CODE_VALUE = "PLAN_FACT_COUNT"
        val PLAN_FACT_GROUP_CODE_VALUE = "PLAN_FACT_GROUPS_COUNT"
    }
}
