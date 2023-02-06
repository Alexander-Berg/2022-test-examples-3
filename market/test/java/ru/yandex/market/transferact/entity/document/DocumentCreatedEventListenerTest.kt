package ru.yandex.market.transferact.entity.document

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mockito.`when`
import org.mockito.Mockito.anyString
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import ru.yandex.market.logistics.les.DiscrepancyActGeneratedEvent
import ru.yandex.market.logistics.les.DropshipScDiscrepancyActGeneratedEvent
import ru.yandex.market.logistics.les.ScScDiscrepancyActGeneratedEvent
import ru.yandex.market.transferact.AbstractTest
import ru.yandex.market.transferact.entity.actor.Actor
import ru.yandex.market.transferact.entity.actor.ActorType
import ru.yandex.market.transferact.entity.item.Item
import ru.yandex.market.transferact.entity.operation.Operation
import ru.yandex.market.transferact.entity.operation.OperationStatus
import ru.yandex.market.transferact.entity.operation.OperationType
import ru.yandex.market.transferact.entity.operation.item.OperationItemStatus
import ru.yandex.market.transferact.entity.transfer.Transfer
import ru.yandex.market.transferact.entity.transfer.TransferQueryService
import ru.yandex.market.transferact.entity.transfer.TransferStatus
import ru.yandex.market.transferact.sqs.SqsSender
import ru.yandex.market.transferact.utils.MockitoHelper.Companion.capture
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.stream.Stream

class DocumentCreatedEventListenerTest : AbstractTest() {

    @MockBean
    lateinit var transferQueryService: TransferQueryService

    @MockBean
    lateinit var discrepancyActSender: SqsSender<DiscrepancyActGeneratedEvent>

    @Autowired
    lateinit var documentCreatedEventListener: DocumentCreatedEventListener

    @Captor
    lateinit var eventCaptor: ArgumentCaptor<DiscrepancyActGeneratedEvent>

    @Captor
    lateinit var eventTypeCaptor: ArgumentCaptor<String>

    @MethodSource("source")
    @ParameterizedTest(name = ParameterizedTest.DISPLAY_NAME_PLACEHOLDER)
    fun `When transportation has discrepancy then send event discrepancy flag`(
        displayName: String,
        outboundActorTypeName: String,
        inboundActorTypeName: String,
        eventClass: Class<out DiscrepancyActGeneratedEvent>,
        eventType: String
    ) {
        val outboundActor = Actor(
            id = 1,
            name = "Кошелев Владимир Александрович",
            companyName = "ООО \"Воробушек\"",
            externalId = "123",
            actorType = ActorType(2, outboundActorTypeName, "apiKey1", "w", 2)
        )
        val inboundActor = Actor(
            id = 1,
            name = "Кошелев Владимир Александрович",
            companyName = "ООО \"Воробушек\"",
            externalId = "123",
            actorType = ActorType(2, inboundActorTypeName, "apiKey1", "w", 2)
        )
        val items = mutableSetOf(
            Item(
                id = 1,
                externalId = "7",
                declaredCost = BigDecimal.valueOf(1000),
                placeCount = 2,
                placeId = "1",
                status = OperationItemStatus.RECEIVED
            )
        )
        val operationReceiveInbound = Operation(
            id = 1,
            status = OperationStatus.CREATED,
            type = OperationType.RECEIVE,
            actor = outboundActor,
            operationItems = items,
            operationSignatures = listOf()
        )

        val operationReceiveOutbound = Operation(
            id = 2,
            status = OperationStatus.CREATED,
            type = OperationType.PROVIDE,
            actor = inboundActor,
            operationItems = setOf(),
            operationSignatures = listOf()
        )

        val inboundTransfer = Transfer(
            id = 1,
            status = TransferStatus.CLOSED,
            operationProvide = operationReceiveInbound,
            operationReceive = operationReceiveInbound,
            closedAt = OffsetDateTime.of(LocalDateTime.now().plusHours(10), ZoneOffset.of("+04:00")),
            localDate = LocalDate.of(2021, Month.DECEMBER, 3)
        )

        val outboundTransfer = Transfer(
            id = 2,
            status = TransferStatus.CLOSED,
            operationProvide = operationReceiveOutbound,
            operationReceive = operationReceiveOutbound,
            closedAt = OffsetDateTime.of(LocalDateTime.now().plusHours(10), ZoneOffset.of("+04:00")),
            localDate = LocalDate.of(2021, Month.DECEMBER, 3)
        )

        `when`(transferQueryService.getTransfersByTransportationId(anyString())).thenReturn(
            listOf(inboundTransfer, outboundTransfer)
        )

        val event = DocumentCreatedEvent(Document(1, "filename", DocumentType.DISCREPANCY_ACT, 1), "1", "bucket")

        documentCreatedEventListener.sendToSqs(event)

        verify(discrepancyActSender).sendSilently(capture(eventCaptor), capture(eventTypeCaptor))

        val eventResult = eventCaptor.value
        assertThat(eventResult.javaClass).isEqualTo(eventClass)
        assertThat(eventResult.transportationId).isEqualTo("1")
        assertThat(eventResult.bucket).isEqualTo("bucket")
        assertThat(eventResult.filename).isEqualTo("filename")
        assertThat(eventResult.isDiscrepancyExists).isTrue

        val eventTypeResult = eventTypeCaptor.value
        assertThat(eventTypeResult).isEqualTo(eventType)
    }

    companion object {
        @JvmStatic
        fun source() = Stream.of(
            Arguments.of(
                "SC_SC",
                "MARKET_SC",
                "MARKET_SC",
                ScScDiscrepancyActGeneratedEvent::class.java,
                ScScDiscrepancyActGeneratedEvent.EVENT_TYPE
            ),
            Arguments.of(
                "DS_SC",
                "MARKET_SHOP",
                "MARKET_SC",
                DropshipScDiscrepancyActGeneratedEvent::class.java,
                DropshipScDiscrepancyActGeneratedEvent.EVENT_TYPE
            )
        )
    }

}
