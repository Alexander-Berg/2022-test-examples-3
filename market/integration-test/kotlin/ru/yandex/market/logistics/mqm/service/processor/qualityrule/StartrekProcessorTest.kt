package ru.yandex.market.logistics.mqm.service.processor.qualityrule

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import java.io.ByteArrayInputStream
import java.time.Instant
import java.util.Optional
import org.junit.jupiter.api.BeforeEach
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import ru.yandex.bolts.collection.Option
import ru.yandex.bolts.collection.impl.ArrayListF
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter
import ru.yandex.market.logistics.management.entity.response.core.Phone
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse
import ru.yandex.market.logistics.management.entity.type.PhoneType
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.repository.PlanFactGroupRepository
import ru.yandex.market.logistics.mqm.repository.PlanFactRepository
import ru.yandex.market.logistics.mqm.service.handler.QualityRuleHandler
import ru.yandex.market.logistics.werewolf.client.WwClient
import ru.yandex.startrek.client.Attachments
import ru.yandex.startrek.client.Comments
import ru.yandex.startrek.client.Issues
import ru.yandex.startrek.client.Links
import ru.yandex.startrek.client.Session
import ru.yandex.startrek.client.Transitions
import ru.yandex.startrek.client.model.Attachment
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.StatusRef
import ru.yandex.startrek.client.model.Transition

abstract class StartrekProcessorTest: AbstractContextualTest() {
    @JvmField
    protected final val issues = Mockito.mock(Issues::class.java)
    @JvmField
    protected final val links = Mockito.mock(Links::class.java)
    @JvmField
    protected final val transitions = Mockito.mock(Transitions::class.java)
    @JvmField
    protected final val attachments = Mockito.mock(Attachments::class.java)
    @JvmField
    protected final val attachment = Mockito.mock(Attachment::class.java)
    @JvmField
    protected final val comments = Mockito.mock(Comments::class.java)
    @JvmField
    protected final val issue = Mockito.mock(Issue::class.java)
    @JvmField
    protected final val issueStatusRef = Mockito.mock(StatusRef::class.java)

    @Autowired
    protected lateinit var wwClient: WwClient

    @Autowired
    protected lateinit var startrekSession: Session

    @Autowired
    private lateinit var planFactRepository: PlanFactRepository

    @Autowired
    private lateinit var planFactGroupRepository: PlanFactGroupRepository

    @Autowired
    @Qualifier("planFactHandler")
    private lateinit var planFactHandler: QualityRuleHandler

    @Autowired
    @Qualifier("planFactGroupHandler")
    private lateinit var planFactGroupHandler: QualityRuleHandler

    @BeforeEach
    fun setUpSessionMocks() {
        whenever(startrekSession.issues()).thenReturn(issues)
        whenever(wwClient.generateClaim(any(), any())).thenReturn(byteArrayOf())
        whenever(startrekSession.transitions()).thenReturn(transitions)
        whenever(startrekSession.attachments()).thenReturn(attachments)
        whenever(attachments.upload(any(), any<ByteArrayInputStream>())).thenReturn(attachment)
        whenever(attachment.id).thenReturn(ATTACHMENT_ID)
        whenever(startrekSession.comments()).thenReturn(comments)
        whenever(startrekSession.links()).thenReturn(links)
        mockGetIssue()
    }

    protected fun LMSClient.mockLmsPartnerNumber(partnerId: Long, phone: String? = null, internalPhone: String = "") {
        val chosenLogisticPoint = 1L
        whenever(
            searchLogisticSegments(
                LogisticSegmentFilter
                    .builder()
                    .setPartnerIds(setOf(partnerId))
                    .build()
            )
        ).thenReturn(listOf(LogisticSegmentDto().setLogisticsPointId(chosenLogisticPoint)))

        if (phone != null) {
            whenever(getLogisticsPoint(chosenLogisticPoint)).thenReturn(
                Optional.of(
                    LogisticsPointResponse
                        .newBuilder()
                        .phones(setOf(Phone(phone, internalPhone, "", PhoneType.PRIMARY)))
                        .build()
                )
            )
        }
    }

    /**
     * Этот метод нужен для того, чтобы замокать ответ для [ReopenIssueFlow].
     */
    private fun mockGetIssue() {
        val issueKey = "MONITORINGSNDBX-1"
        whenever(issue.key).thenReturn(issueKey)
        whenever(issue.status).thenReturn(issueStatusRef)
        whenever(issue.assignee).thenReturn(Option.empty())
        whenever(issueStatusRef.key).thenReturn("open")
        val transition = Mockito.mock(Transition::class.java)
        whenever(issue.transitions).thenReturn(ArrayListF(listOf(transition, transition)))
        whenever(transition.id).thenReturn("closed").thenReturn("close")
        whenever(issues[issueKey]).thenReturn(issue)
    }

    protected fun handlePlanFacts() {
        val planFactsId = planFactRepository.findReadyToHandlePlanFactIds(
            minId = 0,
            processingStatus = ProcessingStatus.ENQUEUED,
            maxScheduleTime = Instant.parse("2100-01-01T12:00:00.00Z"),
            pageRequest = PageRequest.of(0, 1000, Sort.Direction.ASC, "id")
        )
        planFactHandler.handle(planFactsId, Instant.parse("2021-01-01T12:00:00.00Z"))
    }

    protected fun handleGroups() {
        val groupIds = planFactGroupRepository.findReadyForHandleGroupIds(
            minId = 0,
            processingStatus = ProcessingStatus.ENQUEUED,
            maxScheduleTime = Instant.parse("2100-01-01T12:00:00.00Z"),
            pageRequest = PageRequest.of(0, 1000, Sort.Direction.ASC, "id")
        )
        planFactGroupHandler.handle(groupIds, Instant.parse("2021-01-01T12:00:00.00Z"))
    }

    companion object {
        const val ATTACHMENT_ID = "ATTACHMENT_ID"
    }
}
