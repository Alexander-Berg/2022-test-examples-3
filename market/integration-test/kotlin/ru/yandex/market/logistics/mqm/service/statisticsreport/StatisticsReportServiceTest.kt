package ru.yandex.market.logistics.mqm.service.statisticsreport

import com.github.springtestdbunit.annotation.DatabaseSetup
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.util.DateTimeUtils.MOSCOW_ZONE
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.service.statisticsreport.querybuilder.AssemblyStatisticsReportQueryBuilder
import ru.yandex.market.logistics.mqm.service.statisticsreport.querybuilder.IntakeStatisticsReportQueryBuilder
import java.time.LocalDateTime

class StatisticsReportServiceTest : AbstractContextualTest() {

    @Autowired
    private lateinit var statisticsReportService: PlanFactStatisticsReportService

    @Autowired
    private lateinit var ffAssemblyQueryBuilder: AssemblyStatisticsReportQueryBuilder

    @Autowired
    private lateinit var ffDsIntakeQueryBuilder: IntakeStatisticsReportQueryBuilder

    @Test
    @DatabaseSetup("/service/statisticsreport/before/setup.xml")
    fun getMaxOverduePlanFacts() {
        val result = statisticsReportService.getMaxOverduePlanFactsForPeriod(
            LocalDateTime.of(2021, 8, 15, 16, 16, 16).atZone(MOSCOW_ZONE).toInstant(),
            LocalDateTime.of(2021, 8, 18, 18, 18, 18).atZone(MOSCOW_ZONE).toInstant(),
            0.4,
            ffAssemblyQueryBuilder
        )

        assertSoftly {
            result shouldBe MaxOverdue(
                3,
                LocalDateTime.of(2021, 8, 17, 18, 0, 0).atZone(MOSCOW_ZONE).toInstant(),
                2,
                LocalDateTime.of(2021, 8, 17, 18, 30, 0).atZone(MOSCOW_ZONE).toInstant()
            )
        }
    }

    @Test
    @DatabaseSetup("/service/statisticsreport/before/setup.xml")
    fun findPartnerWithMaxPlanFacts() {
        val result = statisticsReportService.findPartnerWithMaxOverduePlanFactsAtInstant(
            LocalDateTime.of(2021, 8, 17, 18, 0, 0).atZone(MOSCOW_ZONE).toInstant(),
            ffAssemblyQueryBuilder
        )

        assertSoftly {
            result shouldContainExactlyInAnyOrder  listOf(
                Pair("[id=101] Какой-то ФФ", 1),
                Pair("[id=301] Какой-то ФФ 3", 1),
                Pair("[id=401] Какой-то ФФ 4", 1)
            )
        }
    }

    @Test
    @DatabaseSetup("/service/statisticsreport/before/setup.xml")
    fun findReceivingPartnerWithMaxPlanFacts() {
        val result = statisticsReportService.findReceivingPartnerWithMaxOverduePlanFactsAtInstant(
            LocalDateTime.of(2021, 8, 17, 18, 0, 0).atZone(MOSCOW_ZONE).toInstant(),
            ffAssemblyQueryBuilder
        )

        assertSoftly {
            result shouldContainExactlyInAnyOrder  listOf(
                Pair("[id=102] Какая-то СД", 1),
                Pair("[id=302] Какая-то СД 3", 1),
                Pair("[id=402] Какая-то СД 4", 1)
            )
        }
    }

    @Test
    @DatabaseSetup("/service/statisticsreport/before/setup.xml")
    fun getZeroOverduePlanFacts() {
        val result = statisticsReportService.getMaxOverduePlanFactsForPeriod(
            LocalDateTime.of(2021, 8, 15, 16, 16, 16).atZone(MOSCOW_ZONE).toInstant(),
            LocalDateTime.of(2021, 8, 18, 18, 18, 18).atZone(MOSCOW_ZONE).toInstant(),
            0.2,
            ffDsIntakeQueryBuilder
        )

        assertSoftly {
            result shouldBe MaxOverdue(0, null)
        }
    }

    @Test
    @DatabaseSetup("/service/statisticsreport/before/setup.xml")
    fun findNoPartnerWithMaxPlanFacts() {
        val result = statisticsReportService.findPartnerWithMaxOverduePlanFactsAtInstant(
            LocalDateTime.of(2021, 8, 17, 18, 0, 0).atZone(MOSCOW_ZONE).toInstant(),
            ffDsIntakeQueryBuilder
        )

        assertSoftly {
            result shouldBe emptyList()
        }
    }
}
