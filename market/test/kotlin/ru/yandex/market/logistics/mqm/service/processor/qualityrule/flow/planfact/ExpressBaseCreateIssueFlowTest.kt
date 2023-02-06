package ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.planfact

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import freemarker.template.Configuration
import io.kotest.matchers.collections.shouldContain
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.lom.model.enums.CargoType
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.configuration.properties.StarterkQueueProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.PlanFactGroup
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.lom.LomItem
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.rules.payloads.StartrekPayload
import ru.yandex.market.logistics.mqm.service.PlanFactAnalyticsService
import ru.yandex.market.logistics.mqm.service.lms.LmsPartnerService
import ru.yandex.market.logistics.mqm.service.logging.LogService
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.group.TestAdditionalData
import ru.yandex.market.logistics.mqm.service.startrek.StartrekService
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate

@ExtendWith(MockitoExtension::class)
internal class ExpressBaseCreateIssueFlowTest : AbstractTest() {

    @Mock
    lateinit var logService: LogService

    @Mock
    lateinit var startrekService: StartrekService

    @Mock
    lateinit var freemarkerConfiguration: Configuration

    @Mock
    lateinit var planFactAnalyticsService: PlanFactAnalyticsService

    @Mock
    lateinit var lmsPartnerService: LmsPartnerService

    @Captor
    lateinit var issueCreateCaptor: ArgumentCaptor<IssueCreate>

    private val clock = TestableClock()
    private val rule = QualityRule(rule = TestStartrekPayload())
    private val issue = Issue(null, null, TICKET, null, 1, EmptyMap(), null)

    @BeforeEach
    private fun setUp() {
        clock.setFixed(DEFAULT_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DisplayName("Добавление тегов важных категорий")
    fun markImportantCategories() {
        val testGroup = PlanFactGroup(id = 1234)
        testGroup.planFacts.addAll(listOf(generatePlanFact(1, "jewelry", setOf(CargoType.JEWELRY))))

        whenever(startrekService.createIssue(any())).thenReturn(issue)
        doNothing().whenever(planFactAnalyticsService).updateIssueKey(TICKET, listOf(testGroup.planFacts.first()))

        prepareExpressBaseCreateIssueFlow().apply(rule, testGroup.planFacts.first())

        verify(startrekService).createIssue(issueCreateCaptor.capture())
        val ordersInTag = (issueCreateCaptor.value.values.getOrThrow("tags") as Array<String>)
        ordersInTag shouldContain "JEWELRY"
    }

    private fun prepareExpressBaseCreateIssueFlow(): ExpressBaseCreateIssueFlow {
        return object : ExpressBaseCreateIssueFlow(
            TestAdditionalData::class.java,
            startrekService,
            logService,
            freemarkerConfiguration,
            clock,
            planFactAnalyticsService,
            StarterkQueueProperties(backLogFieldId = mapOf(TEST_QUEUE to TEST_BACKLOG_KEY)),
            lmsPartnerService,
        ) {
            override fun isApplicable(rule: QualityRule, planFact: PlanFact) = true

            override fun createSummary(planFact: PlanFact) = "Summary"

            override fun createDescription(planFact: PlanFact) = "Description"
        }
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-10-05T10:00:00.00Z")
        private const val TICKET = "MONITORINGSNDBX123"
        private const val TEST_QUEUE = "MONITORINGSNDBX"
        private const val TEST_BACKLOG_KEY = "testKey"
    }

    private fun generatePlanFact(orderId: Int, categoryName: String, cargoTypes: Set<CargoType>?) =
        PlanFact(id = orderId.toLong())
            .apply {
                entity = WaybillSegment().apply {
                    order = LomOrder(
                        barcode = orderId.toString(),
                        items = listOf(LomItem("order$orderId", categoryName, cargoTypes))
                    )
                }
            }

    private class TestStartrekPayload : StartrekPayload(queue = TEST_QUEUE)
}
