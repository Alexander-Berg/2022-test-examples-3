package ru.yandex.market.logistics.mqm.monitoringevent.consumer

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mockito
import org.mockito.Mockito.only
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.checker.MonitoringEventTaskChecker
import ru.yandex.market.logistics.mqm.logging.MonitoringEventTskvLogger
import ru.yandex.market.logistics.mqm.monitoringevent.base.consumer.CallPartnerWithStartrekCommentEventConsumer
import ru.yandex.market.logistics.mqm.monitoringevent.event.EventType
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CallPartnerWithStartrekCommentPayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.UpdateStartrekIssuePayload
import ru.yandex.market.logistics.mqm.ow.OwClient
import ru.yandex.market.logistics.mqm.ow.dto.CreateCallTicketRequest
import ru.yandex.market.logistics.mqm.ow.dto.CreateCallTicketResponse
import ru.yandex.market.logistics.mqm.service.processor.qualityrule.StartrekProcessorTest
import ru.yandex.market.logistics.mqm.utils.TskvLogCaptor

class CallPartnerWithStartrekCommentEventConsumerTest : StartrekProcessorTest() {

    @Autowired
    private lateinit var consumer: CallPartnerWithStartrekCommentEventConsumer

    @Autowired
    private lateinit var owClient: OwClient

    @Autowired
    private lateinit var eventTaskChecker: MonitoringEventTaskChecker

    @RegisterExtension
    @JvmField
    final val tskvLogCaptor = TskvLogCaptor(MonitoringEventTskvLogger.getLoggerName())

    @Test
    fun callPartnerTest() {
        val testCallTicket = "call_ticket_url"
        val testPayload = CallPartnerWithStartrekCommentPayload(
            orderId = "TEST_ORDER_ID",
            ticketTitle = "Test call ticket title",
            ticketDescription = "Test call ticket description",
            clientEmail = "test@mail",
            clientPhone = "+7123",
            issueKey = "TEST_MQM_TICKET-1",
        )
        val callRequest = CreateCallTicketRequest(
            orderId = testPayload.orderId,
            ticketTitle = testPayload.ticketTitle,
            ticketDescription = testPayload.ticketDescription,
            clientEmail = testPayload.clientEmail,
            clientPhone = testPayload.clientPhone,
        )

        Mockito.doReturn(CreateCallTicketResponse(testCallTicket)).`when`(owClient).createCallTicket(callRequest)


        consumer.processPayload(testPayload, null)

        Mockito.verify(owClient, only()).createCallTicket(callRequest)
        assertSoftly {
            tskvLogCaptor.results.toString() shouldContain
                "level=INFO\t" +
                "eventType=CALL_PARTNER_WITH_STARTREK_COMMENT\t" +
                "eventPayload=CallPartnerWithStartrekCommentPayload(" +
                "orderId=TEST_ORDER_ID, " +
                "ticketTitle=Test call ticket title, " +
                "ticketDescription=Test call ticket description, " +
                "clientEmail=test@mail, " +
                "clientPhone=+7123, " +
                "issueKey=TEST_MQM_TICKET-1" +
                ")\t" +
                "message=Call to partner was requested\t" +
                "extraKeys=orderId,phone\t" +
                "extraValues=TEST_ORDER_ID,+7123\n"
        }
        eventTaskChecker.assertExactlyQueueTask(
            EventType.UPDATE_STARTREK_ISSUE,
            UpdateStartrekIssuePayload(
                issueKey = testPayload.issueKey,
                comment = String.format(CallPartnerWithStartrekCommentEventConsumer.COMMENT_TEMPLATE, testCallTicket),
            )
        )
    }
}
