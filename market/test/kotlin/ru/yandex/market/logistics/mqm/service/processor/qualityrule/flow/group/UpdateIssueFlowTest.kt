package ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.group

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import freemarker.template.Configuration
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.TypeDescriptor
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.configuration.properties.StarterkQueueProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.PlanFactGroup
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.additionaldata.AggregatedStartrekAdditionalData
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.rules.payloads.StartrekPayload
import ru.yandex.market.logistics.mqm.service.PlanFactAnalyticsService
import ru.yandex.market.logistics.mqm.service.logging.LogService
import ru.yandex.market.logistics.mqm.service.startrek.StartrekService
import ru.yandex.market.logistics.mqm.utils.getTagAsInt
import ru.yandex.market.logistics.mqm.utils.getTagAsList
import ru.yandex.market.logistics.mqm.utils.takeFirst
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class UpdateIssueFlowTest: AbstractTest() {
    @Mock
    lateinit var logService: LogService

    @Mock
    lateinit var startrekService: StartrekService

    @Mock
    lateinit var planFactAnalyticsService: PlanFactAnalyticsService

    @Mock
    lateinit var freemarkerConfiguration: Configuration

    @Captor
    lateinit var issueUpdateCaptor: ArgumentCaptor<IssueUpdate>

    private val clock = TestableClock()
    private val rule = QualityRule(rule= TestStartrekPayload())
    private val issue = Issue(null, null, TICKET_NAME, null, 1, EmptyMap(), null)
    private val conversionService: TestConversionService = TestConversionService()

    @Mock
    lateinit var attachment: Attachment

    @BeforeEach
    private fun setUp() {
        clock.setFixed(NOW_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DisplayName("Обновление тикета")
    fun update() {
        val testOrdersId = (1..2000L).toList()
        val testGroup = mockGroup(testOrdersId)

        whenever(startrekService.updateIssue(any(), any())).thenReturn(issue)
        whenever(startrekService.uploadAttachments(any(), any())).thenReturn(attachment)
        doNothing().whenever(planFactAnalyticsService).updateIssueKey(TICKET_NAME, testGroup.planFacts)

        prepareUpdateIssueFlow().apply(rule, testGroup)

        verify(startrekService).updateIssue(any(), issueUpdateCaptor.capture())
        getTagAsList(issueUpdateCaptor.value, BaseGroupFlow.FIELD_ORDER_ID) shouldBe
            takeFirst(testOrdersId.map { it.toString() }, BaseGroupFlow.ORDERS_IN_TAG_LIMIT)
        getTagAsInt(issueUpdateCaptor.value, BaseGroupFlow.FIELD_DEFECTED_ORDERS) shouldBe testOrdersId.size
        issueUpdateCaptor.value.comment.get().comment.get() shouldBe TEST_COMMENT
        verify(planFactAnalyticsService).updateIssueKey(TICKET_NAME, testGroup.planFacts)
        (issueUpdateCaptor.value.values.getOrThrow(TEST_BACKLOG_KEY) as ScalarUpdate).set.get() shouldBe testOrdersId.size
    }

    private fun mockGroup(
        ordersId: List<Long>,
    ): PlanFactGroup {
        val testPlanFacts = ordersId.map { generatePlanFact(it) }
        val ordersIds = testPlanFacts
            .map { it.entity as WaybillSegment }
            .map { it.order!!.barcode!! }
            .toSet()
        val testData = TestAdditionalData().apply {
            setOrderIds(ordersIds)
            issueKey = TICKET_NAME
        }
        return PlanFactGroup(id = GROUP_ID)
            .apply {
                planFacts.addAll(testPlanFacts)
                setData(testData)
            }
    }

    private fun generatePlanFact(orderId: Long) = PlanFact(id = orderId)
        .apply { entity = WaybillSegment().apply { order = LomOrder(barcode = orderId.toString()) } }

    private fun prepareUpdateIssueFlow(): UpdateIssueFlow {
        return object: UpdateIssueFlow(
            startrekService,
            TestAdditionalData::class.java,
            freemarkerConfiguration,
            conversionService,
            TestRecord::class.java,
            clock,
            logService,
            planFactAnalyticsService,
            StarterkQueueProperties(backLogFieldId = mapOf(TEST_QUEUE to TEST_BACKLOG_KEY)),
        ) {
            override fun getComment(group: PlanFactGroup?): String = TEST_COMMENT
        }
    }

    companion object {
        private val NOW_TIME = Instant.parse("2021-10-05T10:00:00.00Z")
        private const val GROUP_ID = 124L
        private const val TICKET_NAME = "MONITORINGSNDBX123"
        private const val TEST_COMMENT = "TEST_COMMENT"
        private const val TEST_QUEUE = "MONITORINGSNDBX"
        private const val TEST_BACKLOG_KEY = "testKey"
    }

    private class TestStartrekPayload: StartrekPayload(queue = TEST_QUEUE)
}

open class TestAdditionalData: AggregatedStartrekAdditionalData()

open class TestRecord(
    var externalId: String? = null
)

class TestConversionService: ConversionService {
    override fun canConvert(sourceType: Class<*>?, targetType: Class<*>) = true

    override fun canConvert(sourceType: TypeDescriptor?, targetType: TypeDescriptor) = true

    override fun <T: Any?> convert(source: Any?, targetType: Class<T>): T? = TestRecord(
        externalId = ((source as PlanFact).entity as WaybillSegment).order!!.barcode
    ) as T?

    override fun convert(source: Any?, sourceType: TypeDescriptor?, targetType: TypeDescriptor): Any? = null
}
