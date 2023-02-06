package ru.yandex.market.logistics.mqm.service.event.processors

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.configuration.properties.SuspiciousReturnProcessorProperties
import ru.yandex.market.logistics.mqm.entity.lom.PartnerSettings
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.monitoringevent.base.producer.CreateStartrekIssueForClaimEventProducer
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssueForClaimPayload
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssueForClaimPayload.Fields.FIELD_DEFECTED_ORDERS
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssueForClaimPayload.Fields.FIELD_ORDER_ID
import ru.yandex.market.logistics.mqm.monitoringevent.payload.CreateStartrekIssueForClaimPayload.Fields.UNIQUE
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.money.common.dbqueue.api.EnqueueParams

@ExtendWith(MockitoExtension::class)
class ExpressSuspiciousReturnProcessorTest: AbstractTest() {

    @Mock
    lateinit var ticketProducer: CreateStartrekIssueForClaimEventProducer

    lateinit var processor: ExpressSuspiciousReturnProcessor

    @BeforeEach
    fun setup() {
        processor = ExpressSuspiciousReturnProcessor(
            ticketProducer,
            mockProperties(),
        )
    }

    @Test
    @DisplayName("Проверка создания тикета")
    fun process() {
        val testCheckpoint = mockCheckpoint()
        val context = createContext(testCheckpoint)
        val testPayload = mockPayload()
        whenever(ticketProducer.enqueue(mockPayload())).thenReturn(1)

        processor.waybillSegmentStatusAdded(context)

        verify(ticketProducer).enqueue(testPayload)
    }

    @Test
    @DisplayName("Проверка создания тикета, если подходят все партнёры")
    fun processEnabledForAllPartners() {
        val processorAllPartners = ExpressSuspiciousReturnProcessor(
            ticketProducer,
            mockProperties(partners = setOf()),
        )
        val testCheckpoint = mockCheckpoint()
        val context = createContext(testCheckpoint)
        val testPayload = mockPayload()
        whenever(ticketProducer.enqueue(mockPayload())).thenReturn(1)

        processorAllPartners.waybillSegmentStatusAdded(context)

        verify(ticketProducer).enqueue(testPayload)
    }

    @Test
    @DisplayName("Проверка, что процессор ничего не делает, если он выключен")
    fun processDoNothingIfDisabled() {
        val disabledProcessor = ExpressSuspiciousReturnProcessor(
            ticketProducer,
            mockProperties(enabled = false),
        )
        val testCheckpoint = mockCheckpoint()
        val context = createContext(testCheckpoint)

        disabledProcessor.waybillSegmentStatusAdded(context)
        verify(ticketProducer, never()).enqueue(any())
    }

    @Test
    @DisplayName("Проверка, что процессор не применяется, если не Express")
    fun processDoNothingIfNotExpress() {
        val testCheckpoint = mockCheckpoint(isExpress = false)
        val context = createContext(testCheckpoint)
        processor.waybillSegmentStatusAdded(context)
        verify(ticketProducer, never()).enqueue(any())
    }

    @Test
    @DisplayName("Проверка, что процессор не применяется, если обрабатывается неправильные чекпоинт")
    fun processDoNothingIfWrongCheckpoint() {
        val testCheckpoint = mockCheckpoint(status = OrderDeliveryCheckpointStatus.CANCELED)
        val context = createContext(testCheckpoint)
        processor.waybillSegmentStatusAdded(context)
        verify(ticketProducer, never()).enqueue(any())
    }

    @Test
    @DisplayName("Проверка, если партнёр не добавлен в обработку")
    fun processDoNothingIfWrongPartner() {
        val testCheckpoint = mockCheckpoint(partnerId = 123)
        val context = createContext(testCheckpoint)
        processor.waybillSegmentStatusAdded(context)
        verify(ticketProducer, never()).enqueue(any())
    }

    private fun mockCheckpoint(
        partnerId: Long = TEST_PARTNER_ID,
        status: OrderDeliveryCheckpointStatus = OrderDeliveryCheckpointStatus.RETURN_PREPARING,
        isExpress: Boolean = true,
    ): WaybillSegmentStatusHistory {
        val dropshipSegment = WaybillSegment(
            partnerId = partnerId,
            partnerName = TEST_PARTNER_NAME,
            segmentType = SegmentType.FULFILLMENT
        ).apply { partnerSettings = PartnerSettings(dropshipExpress = isExpress) }

        val taxiSegment = WaybillSegment()
        joinInOrder(listOf(dropshipSegment, taxiSegment)).apply {
            id = TEST_ORDER_ID
            barcode = TEST_BARCODE
        }

        return WaybillSegmentStatusHistory(
            trackerStatus = status.name,
        ).apply { waybillSegment = taxiSegment }
    }

    private fun mockPayload(): EnqueueParams<CreateStartrekIssueForClaimPayload> {
        return EnqueueParams.create(
            CreateStartrekIssueForClaimPayload(
                queue = TEST_QUEUE_NAME,
                summary = "Подозрительный возврат заказа $TEST_BARCODE партнера $TEST_PARTNER_NAME",
                description = "https://abo.market.yandex-team.ru/order/$TEST_BARCODE\n" +
                    "https://ow.market.yandex-team.ru/order/$TEST_BARCODE\n" +
                    "https://lms-admin.market.yandex-team.ru/lom/orders/$TEST_ORDER_ID",
                fields = mapOf(
                    "components" to listOf(TEST_COMPONENT),
                    FIELD_ORDER_ID.key to TEST_BARCODE,
                    FIELD_DEFECTED_ORDERS.key to 1,
                    UNIQUE.key to "${ExpressSuspiciousReturnProcessor::class.simpleName}_$TEST_BARCODE",
                ),
            ),
        )
    }

    private fun mockProperties(
        enabled: Boolean = true,
        partners: Set<Long> = setOf(TEST_PARTNER_ID),
    ) = SuspiciousReturnProcessorProperties(
        enabled = enabled,
        queueName = TEST_QUEUE_NAME,
        component = TEST_COMPONENT,
        partners = partners,
    )

    private fun createContext(testCheckpoint: WaybillSegmentStatusHistory) =
        LomWaybillStatusAddedContext(
            checkpoint = testCheckpoint,
            order = testCheckpoint.waybillSegment!!.order!!,
            orderPlanFacts = listOf()
        )

    companion object {
        private const val TEST_BARCODE = "test_order_id"
        private const val TEST_ORDER_ID = 1L
        private const val TEST_QUEUE_NAME = "TEST_QUEUE_NAME"
        private const val TEST_COMPONENT = 123L
        private const val TEST_PARTNER_ID = 2L
        private const val TEST_PARTNER_NAME = "TEST_PARTNER_NAME"
    }
}
