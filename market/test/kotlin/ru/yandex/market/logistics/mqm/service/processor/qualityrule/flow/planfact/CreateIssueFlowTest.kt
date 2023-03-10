package ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.planfact

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
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.additionaldata.StartrekAdditionalData
import ru.yandex.market.logistics.mqm.entity.rules.payloads.StartrekPayload
import ru.yandex.market.logistics.mqm.service.PlanFactAnalyticsService
import ru.yandex.market.logistics.mqm.service.logging.LogService
import ru.yandex.market.logistics.mqm.service.startrek.StartrekService
import ru.yandex.startrek.client.error.ConflictException
import ru.yandex.startrek.client.error.ErrorCollection
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.SearchRequest
import java.time.Instant
import java.util.Optional

@ExtendWith(MockitoExtension::class)
internal class CreateIssueFlowTest: AbstractTest() {
    @Mock
    lateinit var logService: LogService

    @Mock
    lateinit var startrekService: StartrekService

    @Mock
    lateinit var planFactAnalyticsService: PlanFactAnalyticsService

    @Mock
    lateinit var freemarkerConfiguration: Configuration

    @Captor
    lateinit var issueCreateCaptor: ArgumentCaptor<IssueCreate>

    private val searchRequestCaptor = argumentCaptor<SearchRequest>()

    private val clock = TestableClock()
    private val rule = QualityRule(rule = TestStartrekPayload())
    private val planFact = PlanFact(id = 1234)
    private val issue = Issue(null, null, TICKET, null, 1, EmptyMap(), null)

    @BeforeEach
    private fun setUp() {
        clock.setFixed(DEFAULT_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DisplayName("???????????????????? ???????? unique")
    fun saveUnique() {
        whenever(startrekService.createIssue(any())).thenReturn(issue)
        doNothing().whenever(planFactAnalyticsService).updateIssueKey(TICKET, listOf(planFact))

        prepareCreateIssueFlow().apply(rule, planFact)

        verify(startrekService).createIssue(issueCreateCaptor.capture())
        val uniqueField = issueCreateCaptor.value.values.getOrThrow("unique")
        uniqueField shouldBe "MQM planFactId: 1234, queue: MONITORINGSNDBX"
        planFact.getData(TestAdditionalData::class.java)!!.issueKey shouldBe TICKET
        verify(planFactAnalyticsService).updateIssueKey(TICKET, listOf(planFact))
        issueCreateCaptor.value.values.getOrThrow(TEST_BACKLOG_KEY) shouldBe 1
    }

    @Test
    @DisplayName("???????????? ???????????????????????? ?????????? ???????? ?????????????? ConflictException ???? ?????????? ????????????????")
    fun searchForIssueWhenNotUnique() {
        val errorCollection = Mockito.mock(ErrorCollection::class.java)
        whenever(startrekService.createIssue(any())).thenThrow(ConflictException(errorCollection))
        whenever(startrekService.findIssues(any())).thenReturn(listOf(issue))

        prepareCreateIssueFlow().apply(rule, planFact)

        verify(startrekService).createIssue(any())
        verify(startrekService).findIssues(searchRequestCaptor.capture())
        val uniqueField = searchRequestCaptor.lastValue.filter.getOrThrow("unique")
        uniqueField shouldBe "MQM planFactId: 1234, queue: MONITORINGSNDBX"
        planFact.getData(TestAdditionalData::class.java)!!.issueKey shouldBe TICKET
    }

    @Test
    @DisplayName("???????????? ???????????????????????? ??????????, ???????? ?????????? ?? ?????????? summary ?? ???????? ?????????????? ?????? ????????")
    fun searchForIssueWhenIssueExistsByQueueAndSummary() {
        whenever(startrekService.findIssues(any())).thenReturn(listOf(issue))

        val ruleWithDuplicateCheck = QualityRule(rule = TestStartrekPayload().also { it.checkTicketDuplicate = true })
        prepareCreateIssueFlow().apply(ruleWithDuplicateCheck, planFact)

        verify(startrekService, times(2)).findIssues(searchRequestCaptor.capture())
        searchRequestCaptor.firstValue.filter.getOrThrow("queue") shouldBe TEST_QUEUE
        searchRequestCaptor.firstValue.filter.getOrThrow("summary") shouldBe "Summary"

        val uniqueField = searchRequestCaptor.lastValue.filter.getOrThrow("unique")
        uniqueField shouldBe "MQM planFactId: 1234, queue: MONITORINGSNDBX"
        planFact.getData(TestAdditionalData::class.java)!!.issueKey shouldBe TICKET

        verifyNoMoreInteractions(startrekService)
    }

    @Test
    @DisplayName("???????????? ???????????????????? ?????????? ???????? ????????????????, ???? ???? ???? ????????????")
    fun throwWhenNotUniqueButExistingNotFound() {
        val errorCollection = Mockito.mock(ErrorCollection::class.java)
        whenever(startrekService.createIssue(any())).thenThrow(ConflictException(errorCollection))
        whenever(startrekService.findIssues(any())).thenReturn(listOf())

        assertThrows<IllegalStateException> {
            prepareCreateIssueFlow().apply(rule, planFact)
        }
    }

    private fun prepareCreateIssueFlow(): CreateIssueFlow {
        return object: CreateIssueFlow(
            TestAdditionalData::class.java,
            startrekService,
            planFactAnalyticsService,
            logService,
            freemarkerConfiguration,
            clock,
            StarterkQueueProperties(backLogFieldId = mapOf(TEST_QUEUE to TEST_BACKLOG_KEY)),
        ) {
            override fun isApplicable(rule: QualityRule?, planFact: PlanFact?) = true

            override fun createSummary(planFact: PlanFact?) = "Summary"

            override fun createDescription(planFact: PlanFact?) = "Description"

            override fun getOrder(planFact: PlanFact?): Optional<String> = Optional.empty()
        }
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-10-05T10:00:00.00Z")
        private const val TICKET = "MONITORINGSNDBX123"
        private const val TEST_QUEUE = "MONITORINGSNDBX"
        private const val TEST_BACKLOG_KEY = "testKey"
    }

    private class TestAdditionalData: StartrekAdditionalData()

    private class TestStartrekPayload: StartrekPayload(queue = TEST_QUEUE)

}
