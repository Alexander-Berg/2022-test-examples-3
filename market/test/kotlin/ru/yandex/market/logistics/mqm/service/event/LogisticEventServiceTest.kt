package ru.yandex.market.logistics.mqm.service.event

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.time.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.delivery.tracker.domain.enums.OrderDeliveryCheckpointStatus
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.enums.courier.CourierStatus
import ru.yandex.market.logistics.mqm.entity.enums.lrm.ReturnSegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.DeliveryType
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnBoxEntity
import ru.yandex.market.logistics.mqm.entity.lrm.LrmReturnSegmentEntity
import ru.yandex.market.logistics.mqm.repository.PlanFactRepository
import ru.yandex.market.logistics.mqm.service.ChangeOrderRequestService
import ru.yandex.market.logistics.mqm.service.OrderLockService
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrder
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderAcceptMethod
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderBuyer
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderDelivery
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderDeliveryPartnerType
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderDeliveryType
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderHistoryEvent
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderHistoryEventType
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderItem
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderPaymentType
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderRgb
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderStatus
import ru.yandex.market.logistics.mqm.service.customerorder.CustomerOrderSubstatus
import ru.yandex.market.logistics.mqm.service.event.LogisticEventServiceImpl.Companion.LOG_CODE
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillSegmentStatusAddedListener
import ru.yandex.market.logistics.mqm.service.event.customerorder.CustomerOrderStatusChangedListener
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderLastMileChangedListener
import ru.yandex.market.logistics.mqm.service.event.returns.LrmCourierStatusChangedListener
import ru.yandex.market.logistics.mqm.service.event.returns.ReturnSegmentCreatedListener
import ru.yandex.market.logistics.mqm.service.event.returns.ReturnSegmentStatusChangedListener
import ru.yandex.market.logistics.mqm.service.lom.LomOrderService
import ru.yandex.market.logistics.mqm.service.returns.LrmReturnBoxService
import ru.yandex.market.logistics.mqm.utils.tskvGetCode
import ru.yandex.market.logistics.mqm.utils.tskvGetEntities
import ru.yandex.market.logistics.mqm.utils.tskvGetExtra
import ru.yandex.market.logistics.mqm.utils.tskvGetPayload
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor

@ExtendWith(MockitoExtension::class)
class LogisticEventServiceTest : AbstractTest() {

    @RegisterExtension
    @JvmField
    val backLogCaptor = BackLogCaptor()

    @Mock
    lateinit var listener1: LomWaybillSegmentStatusAddedListener

    @Mock
    lateinit var processor2: LomWaybillSegmentStatusAddedListener

    @Mock
    lateinit var lrmCourierStatusChangedListener: LrmCourierStatusChangedListener

    @Mock
    lateinit var returnSegmentStatusChangedListener1: ReturnSegmentStatusChangedListener

    @Mock
    lateinit var returnSegmentStatusChangedListener2: ReturnSegmentStatusChangedListener

    @Mock
    lateinit var listener3: CustomerOrderStatusChangedListener

    @Mock
    lateinit var returnSegmentCreatedListener: ReturnSegmentCreatedListener

    @Mock
    lateinit var planFactRepository: PlanFactRepository

    @Mock
    lateinit var planFactService: PlanFactService

    @Mock
    lateinit var lomOrderService: LomOrderService

    lateinit var service: LogisticEventServiceImpl

    @Mock
    lateinit var changeOrderRequestService: ChangeOrderRequestService

    @Mock
    lateinit var lomOrderLastMileChangedListener: LomOrderLastMileChangedListener

    @Mock
    lateinit var lrmReturnBoxService: LrmReturnBoxService

    @Mock
    lateinit var orderLockService: OrderLockService

    @BeforeEach
    fun setup() {
        service = LogisticEventServiceImpl(
            waybillSegmentStatusAddedListeners = listOf(listener1, processor2),
            lomOrderStatusChangedListeners = listOf(),
            returnSegmentStatusChangedListeners = listOf(
                returnSegmentStatusChangedListener2,
                returnSegmentStatusChangedListener1
            ),
            firstCteIntakeListeners = listOf(),
            secondCteIntakeListeners = listOf(),
            lrmCourierReturnEventListeners = listOf(lrmCourierStatusChangedListener),
            planFactRepository = planFactRepository,
            planFactService = planFactService,
            lomOrderService = lomOrderService,
            customerOrderStatusChangedListeners = listOf(listener3),
            lrmReturnCreatedListeners = listOf(),
            lomOrderDeliveryDateChangedListeners = listOf(),
            changeOrderRequestService = changeOrderRequestService,
            lomOrderCombinatorRouteWasUpdatedListeners = listOf(),
            returnSegmentCreatedListeners = listOf(returnSegmentCreatedListener),
            lomOrderLastMileChangedListeners = listOf(lomOrderLastMileChangedListener),
            lrmReturnBoxService = lrmReturnBoxService,
            orderLockService = orderLockService,
        )
    }

    // Проверка слушателей.

    @Test
    @DisplayName("Проверка успешного выполнения листенеров новых чекпоинтов")
    fun consumeWaybillStatus() {
        val testCheckpoint = mockCheckpoint()

        service.processNewWaybillStatus(testCheckpoint)

        verify(listener1).waybillSegmentStatusAdded(any())
        verify(processor2).waybillSegmentStatusAdded(any())

        val logStart = backLogCaptor.results[0]
        checkCheckpointLog(logStart)
        tskvGetPayload(logStart) shouldContain "Start to process event"

        val logEnd = backLogCaptor.results[1]
        checkCheckpointLog(logEnd)
        tskvGetPayload(logEnd) shouldContain "Event has been processed"
    }

    @Test
    @DisplayName("Проверка выполнения листенеров изменения статуса пользовательского заказа")
    fun consumeCustomerOrderStatusChanged() {
        val order = createCustomerOrder()
        val event = CustomerOrderHistoryEvent(
            eventType = CustomerOrderHistoryEventType.ORDER_STATUS_UPDATED,
            requestId = "requestId",
            orderBefore = null,
            orderAfter = order
        )
        service.processCustomerOrderStatusOrSubstatusChanged(event)
        val log = backLogCaptor.results[0]
        tskvGetPayload(log) shouldContain "Start to process event"
        val logEnd = backLogCaptor.results[1]
        tskvGetExtra(logEnd) shouldContainAll setOf(
            Pair("processor", listener3::class.simpleName),
            Pair("event", "CustomerOrderStatusChanged"),
        )
        tskvGetExtra(logEnd).toMap() shouldContainKey "durationMsec"
        tskvGetPayload(logEnd) shouldContain "Event has been processed"
    }

    @Test
    @DisplayName("Проверка выполнения листенеров, когда курьер забрал возвратную коробку.")
    fun processCourierReceivedPickup() {
        service.processCourierStatusChanged(
            LrmReturnBoxEntity(
                externalId = "box-ext-id",
            ),
            CourierStatus.RECEIVED_PICKUP
        )
        verify(lrmCourierStatusChangedListener).courierStatusChanged(any())
        val log = backLogCaptor.results[0]
        tskvGetPayload(log) shouldContain "Start to process event"
        val logEnd = backLogCaptor.results[1]
        tskvGetExtra(logEnd) shouldContainAll setOf(
            Pair("processor", lrmCourierStatusChangedListener::class.simpleName),
            Pair("event", "LrmCourierStatusChanged"),
        )
    }

    @Test
    @DisplayName("Проверка выполнения листенеров, когда статус курьера изменился.")
    fun processCourierStatusChanged() {
        service.processCourierStatusChanged(
            LrmReturnBoxEntity(
                externalId = "box-ext-id",
            ),
            CourierStatus.RECEIVED_PICKUP
        )
        verify(lrmCourierStatusChangedListener).courierStatusChanged(any())
        val log = backLogCaptor.results[0]
        tskvGetPayload(log) shouldContain "Start to process event"
        val logEnd = backLogCaptor.results[1]
        tskvGetExtra(logEnd) shouldContainAll setOf(
            Pair("processor", lrmCourierStatusChangedListener::class.simpleName),
            Pair("event", "LrmCourierStatusChanged"),
        )
    }

    @Test
    @DisplayName("Проверка выполнения листенеров, когда поменялся статус сегмента.")
    fun processSegmentStatusChanged() {
        service.processReturnSegmentStatusChanged(LrmReturnSegmentEntity(), ReturnSegmentStatus.OUT)
        verify(returnSegmentStatusChangedListener1).returnSegmentStatusWasChanged(any())
        verify(returnSegmentStatusChangedListener2).returnSegmentStatusWasChanged(any())
    }

    @Test
    @DisplayName("Проверка выполнения листенера, когда создался сегмент.")
    fun processSegmentCreated() {
        service.processReturnSegmentCreated(LrmReturnSegmentEntity())
        verify(returnSegmentCreatedListener).returnSegmentCreated(any())
        val log = backLogCaptor.results[0]
        tskvGetPayload(log) shouldContain "Start to process event"
    }

    @Test
    @DisplayName("Проверка выполнения листенеров изменения типа последней мили заказа")
    fun consumeLastMileChanged() {
        val deliveryType = DeliveryType.PICKUP
        val lomOrder = LomOrder(
            id = 1,
            deliveryType = deliveryType,
            status = OrderStatus.PROCESSING,
        )
        service.processLomOrderLastMileChanged(
            order = lomOrder,
            changeOrderRequestId = 1,
            deletedWaybillSegmentIds = setOf(),
        )
        val log = backLogCaptor.results[0]
        tskvGetPayload(log) shouldContain "Start to process event"
        val logEnd = backLogCaptor.results[1]
        tskvGetExtra(logEnd) shouldContainAll setOf(
            Pair("processor", lomOrderLastMileChangedListener::class.simpleName),
            Pair("event", "LomOrderLastMileChanged"),
        )
        tskvGetExtra(logEnd).toMap() shouldContainKey "durationMsec"
        tskvGetPayload(logEnd) shouldContain "Event has been processed"
    }

    // Системные проверки.

    @Test
    @DisplayName("Проверка, что ошибки в одном процессоре не влияет на другие")
    fun consumeWaybillStatusLogError() {
        val testCheckpoint = mockCheckpoint()
        whenever(listener1.waybillSegmentStatusAdded(any()))
            .thenThrow(RuntimeException("test_error"))

        service.processNewWaybillStatus(testCheckpoint)

        verify(listener1).waybillSegmentStatusAdded(any())
        verify(processor2).waybillSegmentStatusAdded(any())

        val logStart = backLogCaptor.results[0]
        checkCheckpointLog(logStart)
        tskvGetPayload(logStart) shouldContain "Start to process event"

        val logError = backLogCaptor.results[1]
        checkCheckpointLog(logError)
        tskvGetExtra(logError) shouldContainAll setOf(
            Pair("processor", listener1::class.simpleName),
            Pair("exception", RuntimeException::class.simpleName),
            Pair("event", "LomWaybillStatusAdded"),
        )
        tskvGetExtra(logError).toMap() shouldContainKey "durationMsec"
        tskvGetPayload(logError) shouldContain "Failed to apply processor to event"

        val logEnd = backLogCaptor.results[2]
        checkCheckpointLog(logEnd)
        tskvGetPayload(logEnd) shouldContain "Event has been processed"
    }

    private fun checkCheckpointLog(log: String) {
        tskvGetCode(log) shouldBe LOG_CODE
        tskvGetExtra(log) shouldContainAll setOf(
            Pair("statusId", CHECKPOINT_ID.toString()),
            Pair("statusTracker", TRACKER_STATUS),
        )
        tskvGetEntities(log) shouldContainAll setOf(
            Pair("waybillSegmentId", SEGMENT_ID.toString()),
            Pair("lomOrderId", ORDER_ID.toString()),
        )
    }

    private fun mockCheckpoint(): WaybillSegmentStatusHistory {
        val segment = WaybillSegment(
            id = SEGMENT_ID,
        ).apply { order = LomOrder(id = ORDER_ID) }
        return WaybillSegmentStatusHistory(
            id = CHECKPOINT_ID,
            trackerStatus = TRACKER_STATUS,
        ).apply { waybillSegment = segment }
    }

    private fun createCustomerOrder() =
        CustomerOrder(
            id = CUSTOMER_ORDER_ID,
            status = CustomerOrderStatus.DELIVERED,
            creationDate = CREATION_DATE,
            paymentType = CustomerOrderPaymentType.PREPAID,
            substatus = CustomerOrderSubstatus.STARTED,
            acceptMethod = CustomerOrderAcceptMethod.WEB_INTERFACE,
            rgb = CustomerOrderRgb.BLUE,
            delivery = CustomerOrderDelivery(
                marketBranded = true,
                deliveryPartnerType = CustomerOrderDeliveryPartnerType.YANDEX_MARKET,
                type = CustomerOrderDeliveryType.POST,
                deliveryServiceId = 0L,
            ),
            changeRequests = listOf(),
            items = listOf(
                CustomerOrderItem(1L, 2L, 3L)
            ),
            fulfilment = false,
            buyer = CustomerOrderBuyer(0L),
        )

    companion object {
        private const val SEGMENT_ID = 1L
        private const val ORDER_ID = 2L
        private const val CHECKPOINT_ID = 2L
        private const val CUSTOMER_ORDER_ID = 1001L
        private val CREATION_DATE = LocalDateTime
            .of(2021, 5, 20, 5, 0, 0)
            .atZone(DateTimeUtils.MOSCOW_ZONE)
            .toInstant()
        private var TRACKER_STATUS = OrderDeliveryCheckpointStatus.SENDER_SENT.name
    }
}
