package ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.group

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import freemarker.template.Configuration
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
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
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.group.BaseGroupFlow.FIELD_ORDER_ID
import ru.yandex.market.logistics.mqm.service.startrek.StartrekService
import ru.yandex.market.logistics.mqm.utils.takeFirst
import ru.yandex.startrek.client.error.ConflictException
import ru.yandex.startrek.client.error.ErrorCollection
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.SearchRequest
import java.time.Instant

@ExtendWith(MockitoExtension::class)
internal class CreateIssueFlowTest: AbstractTest() {
    @Mock
    lateinit var logService: LogService

    @Mock
    lateinit var startrekService: StartrekService

    @Mock
    lateinit var freemarkerConfiguration: Configuration

    @Mock
    lateinit var planFactAnalyticsService: PlanFactAnalyticsService

    @Captor
    lateinit var issueCreateCaptor: ArgumentCaptor<IssueCreate>

    private val searchRequestCaptor = argumentCaptor<SearchRequest>()

    private val clock = TestableClock()
    private val rule = QualityRule(rule = TestStartrekPayload())
    private val planFactGroup = PlanFactGroup(id = 1234)
    private val issue = Issue(null, null, TICKET, null, 1, EmptyMap(), null)

    @BeforeEach
    private fun setUp() {
        clock.setFixed(DEFAULT_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DisplayName("Сохранение поля unique")
    fun saveUnique() {
        whenever(startrekService.createIssue(any())).thenReturn(issue)
        doNothing().whenever(planFactAnalyticsService).updateIssueKey(TICKET, planFactGroup.planFacts)

        prepareCreateIssueFlow().apply(rule, planFactGroup)

        verify(startrekService).createIssue(issueCreateCaptor.capture())
        val uniqueField = issueCreateCaptor.value.values.getOrThrow("unique")
        uniqueField shouldBe "MQM planFactGroupId: 1234, queue: MONITORINGSNDBX"
        planFactGroup.getData(TestAdditionalData::class.java)!!.issueKey shouldBe TICKET
        verify(planFactAnalyticsService).updateIssueKey(TICKET, planFactGroup.planFacts)
    }

    @Test
    @DisplayName("Искать существующий тикет если брошено ConflictException во время создания")
    fun searchForIssueWhenNotUnique() {
        val errorCollection = Mockito.mock(ErrorCollection::class.java)
        whenever(startrekService.createIssue(any())).thenThrow(ConflictException(errorCollection))
        whenever(startrekService.findIssues(any())).thenReturn(listOf(issue))

        prepareCreateIssueFlow().apply(rule, planFactGroup)

        verify(startrekService).createIssue(any())
        verify(startrekService).findIssues(searchRequestCaptor.capture())
        val uniqueField = searchRequestCaptor.lastValue.filter.getOrThrow("unique")
        uniqueField shouldBe "MQM planFactGroupId: 1234, queue: MONITORINGSNDBX"
        planFactGroup.getData(TestAdditionalData::class.java)!!.issueKey shouldBe TICKET
    }

    @Test
    @DisplayName("Искать существующий тикет, если тикет с таким summary в этой очереди уже есть")
    fun searchForIssueWhenIssueExistsByQueueAndSummary() {
        whenever(startrekService.findIssues(any())).thenReturn(listOf(issue))

        val ruleWithDuplicateCheck = QualityRule(rule = TestStartrekPayload().also { it.checkTicketDuplicate = true })
        prepareCreateIssueFlow().apply(ruleWithDuplicateCheck, planFactGroup)

        verify(startrekService, times(2)).findIssues(searchRequestCaptor.capture())
        searchRequestCaptor.firstValue.filter.getOrThrow("queue") shouldBe TEST_QUEUE
        searchRequestCaptor.firstValue.filter.getOrThrow("summary") shouldBe "Summary"

        val uniqueField = searchRequestCaptor.lastValue.filter.getOrThrow("unique")
        uniqueField shouldBe "MQM planFactGroupId: 1234, queue: MONITORINGSNDBX"
        planFactGroup.getData(TestAdditionalData::class.java)!!.issueKey shouldBe TICKET

        verifyNoMoreInteractions(startrekService)
    }

    @Test
    @DisplayName("Кидать исключение когда есть дубликат, но он не найден")
    fun throwWhenNotUniqueButExistingNotFound() {
        val errorCollection = Mockito.mock(ErrorCollection::class.java)
        whenever(startrekService.createIssue(any())).thenThrow(ConflictException(errorCollection))
        whenever(startrekService.findIssues(any())).thenReturn(listOf())

        assertThrows<IllegalStateException> {
            prepareCreateIssueFlow().apply(rule, planFactGroup)
        }
    }

    @Test
    @DisplayName("Cоздавать тикет с тэгом cо списком заказов")
    fun createTicket() {
        val ordersToAddInTags = (1..2000).toList()
        val planFacts = ordersToAddInTags
            .map { generatePlanFact(it) }
        val testGroup = PlanFactGroup(id = 1234)
        testGroup.planFacts.addAll(planFacts)
        whenever(startrekService.createIssue(any())).thenReturn(issue)
        doNothing().whenever(planFactAnalyticsService).updateIssueKey(TICKET, testGroup.planFacts)

        prepareCreateIssueFlow().apply(rule, testGroup)

        verify(startrekService).createIssue(issueCreateCaptor.capture())
        val ordersInTag = (issueCreateCaptor.value.values.getOrThrow(FIELD_ORDER_ID) as String)
            .split(",")
            .map { it.trim() }
        ordersInTag shouldBe takeFirst(ordersToAddInTags.map { it.toString() }, 1000)
        issueCreateCaptor.value.values.getOrThrow(TEST_BACKLOG_KEY) shouldBe ordersToAddInTags.size
        verify(planFactAnalyticsService).updateIssueKey(TICKET, testGroup.planFacts)
    }

    private fun prepareCreateIssueFlow(): CreateIssueFlow {
        return object: CreateIssueFlow(
            TestAdditionalData::class.java,
            startrekService,
            logService,
            freemarkerConfiguration,
            clock,
            planFactAnalyticsService,
            StarterkQueueProperties(backLogFieldId = mapOf(TEST_QUEUE to TEST_BACKLOG_KEY)),
        ) {
            override fun isApplicable(rule: QualityRule?, planFactGroup: PlanFactGroup?) = true

            override fun createSummary(planFactGroup: PlanFactGroup?) = "Summary"

            override fun createDescription(planFactGroup: PlanFactGroup?) = "Description"
        }
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-10-05T10:00:00.00Z")
        private const val TICKET = "MONITORINGSNDBX123"
        private const val TEST_QUEUE = "MONITORINGSNDBX"
        private const val TEST_BACKLOG_KEY = "testKey"
    }

    private class TestAdditionalData: AggregatedStartrekAdditionalData()

    private class TestStartrekPayload: StartrekPayload(queue = TEST_QUEUE)

    private fun generatePlanFact(orderId: Int) = PlanFact(id = orderId.toLong())
        .apply { entity = WaybillSegment().apply { order = LomOrder(barcode = orderId.toString()) } }
}
