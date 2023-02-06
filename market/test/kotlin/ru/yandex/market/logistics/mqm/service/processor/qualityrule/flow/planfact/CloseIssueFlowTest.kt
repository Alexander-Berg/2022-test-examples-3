package ru.yandex.market.logistics.mqm.service.processor.qualityrule.flow.planfact;

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
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
import ru.yandex.bolts.collection.Option
import ru.yandex.bolts.collection.impl.ArrayListF
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.configuration.properties.StarterkQueueProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.QualityRule
import ru.yandex.market.logistics.mqm.entity.additionaldata.AggregatedStartrekAdditionalData
import ru.yandex.market.logistics.mqm.entity.rules.payloads.StartrekPayload
import ru.yandex.market.logistics.mqm.service.logging.LogService
import ru.yandex.market.logistics.mqm.service.startrek.StartrekService
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueUpdate
import ru.yandex.startrek.client.model.ScalarUpdate
import ru.yandex.startrek.client.model.StatusRef
import ru.yandex.startrek.client.model.Transition
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class CloseIssueFlowTest: AbstractTest() {

    @Mock
    lateinit var logService: LogService

    @Mock
    lateinit var startrekService: StartrekService

    @Mock
    lateinit var freemarkerConfiguration: Configuration

    @Captor
    lateinit var issueUpdateCaptor: ArgumentCaptor<IssueUpdate>

    private val clock = TestableClock()

    private val rule = QualityRule(rule = TestStartrekPayload())

    @Mock
    lateinit var issue: Issue

    @Mock
    lateinit var issueClosedStatus: StatusRef

    @BeforeEach
    private fun setUp() {
        clock.setFixed(DEFAULT_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    @Test
    @DisplayName("Проверка закрытия тикета")
    fun testClosedAutomatically() {
        val testPlanFact = PlanFact(id = 1234)
        testPlanFact.setData(TestAdditionalData().setIssueKey(clock, TICKET))
        setupOpenedIssue()

        prepareFlow().apply(rule, testPlanFact)

        verify(startrekService).executeTransition(eq(TICKET), any(), issueUpdateCaptor.capture())
        (issueUpdateCaptor.value.values.getOrThrow(TEST_BACKLOG_KEY) as ScalarUpdate).set.get() shouldBe 0
        issueUpdateCaptor.value.comment.get().comment.get() shouldBe TEST_COMMENT
    }

    private fun prepareFlow(): CloseIssueFlow {
        return object: CloseIssueFlow(
            startrekService,
            freemarkerConfiguration,
            TestAdditionalData::class.java,
            logService,
            StarterkQueueProperties(backLogFieldId = mapOf(TEST_QUEUE to TEST_BACKLOG_KEY)),
        ) {
            override fun getComment(): String = TEST_COMMENT
        }
    }

    private fun setupOpenedIssue() {
        whenever(issueClosedStatus.key).thenReturn("opened")
        whenever(issue.key).thenReturn(TICKET)
        whenever(issue.assignee).thenReturn(Option.empty())
        whenever(issue.status).thenReturn(issueClosedStatus)
        val testTransitions = ArrayListF<Transition?>()
        testTransitions.add(Transition("closed", null, null, null, null))
        whenever(issue.transitions).thenReturn(testTransitions)
        whenever(startrekService.getIssue(TICKET)).thenReturn(issue)
    }

    companion object {
        private val DEFAULT_TIME = Instant.parse("2021-10-05T10:00:00.00Z")
        private const val TICKET = "MONITORINGSNDBX123"
        private const val TEST_QUEUE = "MONITORINGSNDBX"
        private const val TEST_BACKLOG_KEY = "testKey"
        private const val TEST_COMMENT = "TEST_COMMENT"
    }

    private class TestAdditionalData: AggregatedStartrekAdditionalData()

    private class TestStartrekPayload: StartrekPayload(queue = TEST_QUEUE)
}