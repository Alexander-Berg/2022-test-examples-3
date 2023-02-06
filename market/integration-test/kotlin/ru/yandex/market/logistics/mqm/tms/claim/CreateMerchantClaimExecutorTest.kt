package ru.yandex.market.logistics.mqm.tms.claim

import com.github.springtestdbunit.annotation.DatabaseSetup
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import ru.yandex.bolts.collection.Cf
import ru.yandex.bolts.collection.Option
import ru.yandex.bolts.collection.impl.ArrayListF
import ru.yandex.bolts.collection.impl.EmptyMap
import ru.yandex.market.logistics.mqm.entity.enums.ClaimStatus
import ru.yandex.market.logistics.mqm.entity.enums.IssueLinkEntityType
import ru.yandex.market.logistics.mqm.monitoringevent.event.EventType
import ru.yandex.market.logistics.mqm.monitoringevent.payload.AbstractMonitoringEventPayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.BaseCreateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssueForClaimPayload
import ru.yandex.market.logistics.mqm.repository.ClaimRepository
import ru.yandex.market.logistics.mqm.service.monitoringevent.MonitoringEventService
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.StartrekProcessorTest
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor
import ru.yandex.startrek.client.model.CommentCreate
import ru.yandex.startrek.client.model.Issue
import ru.yandex.startrek.client.model.IssueCreate
import ru.yandex.startrek.client.model.Relationship
import ru.yandex.startrek.client.model.Transition

class CreateMerchantClaimExecutorTest : StartrekProcessorTest() {

    @Autowired
    private lateinit var merchantClaimExecutor: CreateMerchantClaimExecutor

    @SpyBean
    private lateinit var monitoringEventService: MonitoringEventService<AbstractMonitoringEventPayload>

    @Autowired
    private lateinit var claimRepository: ClaimRepository

    @RegisterExtension
    @JvmField
    final val backLogCaptor = BackLogCaptor()


    private val payloadCaptor: KArgumentCaptor<CreateStartrekIssueForClaimPayload> = KArgumentCaptor(
        ArgumentCaptor.forClass(CreateStartrekIssueForClaimPayload::class.java),
        CreateStartrekIssueForClaimPayload::class
    )

    @Test
    @DisplayName(
        "Успешное создание тасок на создание тикетов в очередь MQMMERCHANTCLAIM через событийный мониторинг"
    )
    @DatabaseSetup("/tms/claim/createClaimExecutor/before/setup_merchant_claim.xml")
    fun successMerchantPaidTicketCreation() {
        whenever(startrekSession.attachments()).thenReturn(attachments)
        val createClaimTicketPayload = createPayload()

        doReturn(Issue(null, null, "MQMMERCHANTCLAIM-1", null, 1, EmptyMap(), null))
            .whenever(issues)
            .create(ArgumentMatchers.any())
        whenever(startrekSession.issues()).thenReturn(issues)


        merchantClaimExecutor.run()

        verify(monitoringEventService, times(1)).pushEvent(
            eq(EventType.CREATE_STARTREK_ISSUE_FOR_CLAIM),
            payloadCaptor.capture()
        )

        val fbyClaim = claimRepository.findById(11).get()

        val logMessageExpected = createLogMessage(CreateClaimEventCode.CLAIM_MERCHANT)
        val log = backLogCaptor.results.toString()
        assertSoftly {
            payloadCaptor.firstValue shouldBe createClaimTicketPayload
            log shouldContain logMessageExpected
            fbyClaim.status shouldBe ClaimStatus.CLOSED

        }
    }

    private fun createLogMessage(code: CreateClaimEventCode): String {
        return "level=INFO\t" +
            "format=plain\t" +
            "code=$code\t" +
            "payload=Triggered ticket creation in MQMMERCHANTCLAIM queue for merchant\t" +
            "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
            "tags=CLAIM_MONITORING_EVENT"
    }

    private fun createPayload(): CreateStartrekIssueForClaimPayload = CreateStartrekIssueForClaimPayload(
        queue = "MQMMERCHANTCLAIM",
        summary = "",
        description = "Претензия включает 4 позиции товаров на сумму : 2501.02",
        fields = mapOf(
            "amountClaimed" to "2501.02 RUB"
        ),
        tags = setOf(CreateStartrekIssueForClaimPayload.Tags.MERCH_CLAIM.name),
        entities = setOf(BaseCreateStartrekIssuePayload.Entity("1", IssueLinkEntityType.CLAIM)),
    )

    /**
     * Этот метод нужен для того, чтобы замокать ответ для [ReopenIssueFlow].
     */
    private fun mockGetIssue() {
        val issueKey = "MQMMERCHANTCLAIM-1"
        whenever(issue.key).thenReturn(issueKey)
        whenever(issue.status).thenReturn(issueStatusRef)
        whenever(issue.assignee).thenReturn(Option.empty())
        whenever(issueStatusRef.key).thenReturn("open")
        val transition = Mockito.mock(Transition::class.java)
        whenever(issue.transitions).thenReturn(ArrayListF(listOf(transition, transition)))
        whenever(transition.id).thenReturn("closed").thenReturn("close")
        whenever(issues[issueKey]).thenReturn(issue)
    }

    private fun issueCreate(payload: CreateStartrekIssueForClaimPayload): IssueCreate {
        val issueBuilder = IssueCreate.builder()
        issueBuilder.queue("MQMMERCHANTCLAIM")
        issueBuilder.summary(payload.summary)
        issueBuilder.tags(Cf.toList(payload.tags))
        payload.description?.let { issueBuilder.description(it) }
        payload.fields?.let { it.forEach { entry -> issueBuilder.set(entry.key, entry.value) } }
        payload.issueLinks.forEach {
            issueBuilder.link(it, Relationship.RELATES)
        }
        payload.comments.forEach {
            val commentCreate = CommentCreate.builder().comment(it).build()
            issueBuilder.comment(commentCreate)
        }
        return issueBuilder.build()
    }
}
