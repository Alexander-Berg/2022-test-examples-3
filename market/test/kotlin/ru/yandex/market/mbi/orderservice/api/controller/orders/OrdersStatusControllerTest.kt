package ru.yandex.market.mbi.orderservice.api.controller.orders

import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.HttpClientBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyZeroInteractions
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import ru.yandex.market.checkout.checkouter.client.ClientRole
import ru.yandex.market.checkout.checkouter.order.Order
import ru.yandex.market.checkout.checkouter.order.OrderStatus
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus
import ru.yandex.market.checkout.checkouter.order.changerequest.CancellationRequestPayload
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.api.FunctionalTest
import ru.yandex.market.mbi.orderservice.common.dsl.Actor
import ru.yandex.market.mbi.orderservice.common.enum.MerchantItemStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderSubstatus
import ru.yandex.market.mbi.orderservice.common.model.yt.CancellationRequest
import ru.yandex.market.mbi.orderservice.common.model.yt.EventSource
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEvent
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEventType
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OutboundOrderEventEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.service.external.CheckouterApiService
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.OrderEventService
import ru.yandex.market.mbi.orderservice.model.ActorType
import ru.yandex.market.mbi.orderservice.model.ChangeOrderStatus
import ru.yandex.market.mbi.orderservice.model.ChangeRequestStatusType
import ru.yandex.market.mbi.orderservice.model.ConfirmationReasonType
import ru.yandex.market.mbi.orderservice.model.OrderSubStatus
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderCancelledPayload
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent.PayloadCase
import java.time.Clock
import java.time.Instant

@CleanupTables(
    [
        OrderEntity::class, OrderLineEntity::class, OrderEvent::class, OutboundOrderEventEntity::class
    ]
)
class OrdersStatusControllerTest : FunctionalTest() {

    @Autowired
    lateinit var ytOrderRepository: YtOrderRepository

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderLineEntityRepository: OrderLineEntityRepository

    @Autowired
    lateinit var orderEventService: OrderEventService

    @Autowired
    lateinit var checkouterApiService: CheckouterApiService

    @Autowired
    lateinit var clock: Clock

    private val instant = Instant.parse("2021-12-12T10:00:00Z")

    @BeforeEach
    internal fun setUp() {
        this::class.loadTestEntities<OrderEntity>("status/orders.json").let {
            orderEntityRepository.insertRows(it)
        }
        this::class.loadTestEntities<OrderLineEntity>("status/orderLines.json").let {
            orderLineEntityRepository.insertRows(it)
        }

        whenever(clock.instant()).thenReturn(instant)
    }

    @Test
    fun `verify that order without cancellation request id is processed correctly`() {
        changeCancellationRequestStatus(
            orderId = 81545127,
            changeRequestStatus = ChangeRequestStatusType.REJECTED,
            expectedHttpStatus = HttpStatus.SC_BAD_REQUEST
        ) {
            """{
                "errors": [
                  {
                    "code":"CHANGE_REQUEST_NOT_FOUND",
                    "message":"Change request was not found: partnerId = 543900, orderId = 81545127"
                  }
                ]
            }""".trimIndent()
        }

        verifyOrderStatus(
            orderId = 81545127,
            status = MerchantOrderStatus.PROCESSING,
            substatus = MerchantOrderSubstatus.STARTED,
            linesStatus = MerchantItemStatus.CREATED
        )
        verifyNoCancellationRequestExists(81545127)
    }

    @Test
    fun `reject cancellation request`() {
        mockCheckouterGetOrder(
            81545129,
            OrderStatus.DELIVERY,
            OrderSubstatus.DELIVERY_SERVICE_RECEIVED
        )

        changeCancellationRequestStatus(
            orderId = 81545129,
            changeRequestStatus = ChangeRequestStatusType.REJECTED
        ) {
            """{
                "result":{
                    "partnerId":543900,
                    "orderId":81545129,
                    "status":"DELIVERY",
                    "substatus":"DELIVERY_SERVICE_RECEIVED"
                 }
            }""".trimIndent()
        }

        verifyOrderStatus(
            orderId = 81545129,
            status = MerchantOrderStatus.DELIVERY,
            substatus = MerchantOrderSubstatus.DELIVERY_SERVICE_RECEIVED,
            linesStatus = MerchantItemStatus.SHIPPED
        )
        verifyNoCancellationRequestExists(81545129)
    }

    @Test
    fun `reject cancellation request and change status`() {
        mockCheckouterGetOrder(
            81545129,
            OrderStatus.DELIVERED,
            OrderSubstatus.DELIVERY_SERVICE_DELIVERED
        )

        changeCancellationRequestStatus(
            orderId = 81545129,
            changeRequestStatus = ChangeRequestStatusType.REJECTED,
            confirmationReason = ConfirmationReasonType.DELIVERED
        ) {
            """{
                "result":{
                    "partnerId":543900,
                    "orderId":81545129,
                    "status":"DELIVERED",
                    "substatus":"DELIVERY_SERVICE_DELIVERED"
                 }
            }""".trimIndent()
        }

        verifyOrderStatus(
            orderId = 81545129,
            status = MerchantOrderStatus.DELIVERED,
            substatus = MerchantOrderSubstatus.DELIVERY_SERVICE_DELIVERED,
            linesStatus = MerchantItemStatus.DELIVERED_TO_BUYER
        )
        verifyNoCancellationRequestExists(81545129)
    }

    @Test
    fun `apply cancellation request`() {
        changeCancellationRequestStatus(
            orderId = 81545129,
            changeRequestStatus = ChangeRequestStatusType.APPLIED
        ) {
            """{
                "result":{
                    "partnerId":543900,
                    "orderId":81545129,
                    "status":"CANCELLED_IN_DELIVERY",
                    "substatus":"USER_BOUGHT_CHEAPER"
                 }
            }""".trimIndent()
        }

        verifyOrderStatus(
            orderId = 81545129,
            status = MerchantOrderStatus.CANCELLED_IN_DELIVERY,
            substatus = MerchantOrderSubstatus.USER_BOUGHT_CHEAPER,
            linesStatus = MerchantItemStatus.CANCELLED
        )
    }

    private fun mockCheckouterGetOrder(
        orderId: Long,
        orderStatus: OrderStatus,
        orderSubstatus: OrderSubstatus
    ) {
        val order = mock<Order> {
            on(it.status).thenReturn(orderStatus)
            on(it.substatus).thenReturn(orderSubstatus)
        }
        whenever(checkouterApiService.getOrderById(eq(orderId))).thenReturn(order)
    }

    @Test
    @DisplayName("Проверка смены статуса, если заказ уже в нужном статусе")
    fun `change to same status`() {
        changeStatusRequest(
            81545127,
            ChangeOrderStatus.PROCESSING,
            OrderSubStatus.STARTED,
            """{
                "result":{
                    "partnerId":543900,
                    "orderId":81545127,
                    "status":"PROCESSING",
                    "substatus":"STARTED",
                    "cancellationRequestCreated":false
                 }
            }"""
        )

        verifyZeroInteractions(checkouterApiService)
        verifyOrderStatus(
            orderId = 81545127,
            status = MerchantOrderStatus.PROCESSING,
            substatus = MerchantOrderSubstatus.STARTED,
            linesStatus = MerchantItemStatus.CREATED
        )
    }

    @Test
    @DisplayName("Проверка смены статуса без подстатуса (чекатуер проставит дефолтный)")
    fun `change to new status`() {
        changeStatus(
            orderId = 81545127,
            toStatus = ChangeOrderStatus.DELIVERY,
            toSubstatus = null,
            fromCheckouterSubstatus = OrderSubStatus.DELIVERY_SERVICE_RECEIVED
        ) {
            """{
                "result":{
                    "partnerId":543900,
                    "orderId":81545127,
                    "status":"DELIVERY",
                    "substatus":"DELIVERY_SERVICE_RECEIVED",
                    "cancellationRequestCreated":false
                 }
            }"""
        }

        verifyOrderStatus(
            orderId = 81545127,
            status = MerchantOrderStatus.DELIVERY,
            substatus = MerchantOrderSubstatus.DELIVERY_SERVICE_RECEIVED,
            linesStatus = MerchantItemStatus.SHIPPED
        )
        verifyOrderEvent(81545127, MerchantOrderStatus.DELIVERY, MerchantOrderSubstatus.DELIVERY_SERVICE_RECEIVED)
    }

    @Test
    @DisplayName("Проверка смены подстатуса")
    fun `change to new substatus`() {
        changeStatus(
            orderId = 81545127,
            toStatus = ChangeOrderStatus.PROCESSING,
            toSubstatus = OrderSubStatus.READY_TO_SHIP
        ) {
            """{
                "result":{
                    "partnerId":543900,
                    "orderId":81545127,
                    "status":"PROCESSING",
                    "substatus":"READY_TO_SHIP",
                    "cancellationRequestCreated":false
                 }
            }"""
        }

        verifyOrderStatus(
            orderId = 81545127,
            status = MerchantOrderStatus.PROCESSING,
            substatus = MerchantOrderSubstatus.READY_TO_SHIP,
            linesStatus = MerchantItemStatus.CREATED
        )
        verifyOrderEvent(
            orderId = 81545127,
            status = MerchantOrderStatus.PROCESSING,
            substatus = MerchantOrderSubstatus.READY_TO_SHIP,
            type = OrderEventType.SUBSTATUS_CHANGE
        )
    }

    @Test
    @DisplayName("Проверка смены статусов у штук")
    fun `change lines to new status`() {
        changeStatus(
            orderId = 81545128,
            toStatus = ChangeOrderStatus.DELIVERED,
            toSubstatus = null,
            fromCheckouterSubstatus = OrderSubStatus.DELIVERY_SERVICE_DELIVERED
        ) {
            """{
                "result":{
                    "partnerId":543900,
                    "orderId":81545128,
                    "status":"DELIVERED",
                    "substatus":"DELIVERY_SERVICE_DELIVERED",
                    "cancellationRequestCreated":false
                 }
            }"""
        }

        verifyOrderStatus(
            orderId = 81545128,
            status = MerchantOrderStatus.DELIVERED,
            substatus = MerchantOrderSubstatus.DELIVERY_SERVICE_DELIVERED,
            linesStatus = MerchantItemStatus.DELIVERED_TO_BUYER
        )
        verifyOrderEvent(81545128, MerchantOrderStatus.DELIVERED, MerchantOrderSubstatus.DELIVERY_SERVICE_DELIVERED)
    }

    @Test
    @DisplayName("Проверка смены статуса на невалидный")
    fun `change to invalid status`() {
        changeStatus(
            orderId = 81545128,
            toStatus = ChangeOrderStatus.PROCESSING,
            toSubstatus = OrderSubStatus.STARTED,
            expectedHttpStatus = HttpStatus.SC_BAD_REQUEST
        ) {
            """{"className":"CommonApiErrorResponse",
                "actions":"partners/{partnerId}/common/orders/{orderId}/status",
                "errors":[{"code":"INVALID_STATUS_TRANSITION","message":"No transition found: DELIVERY -> PROCESSING","details":{}}]}
            """
        }

        verifyZeroInteractions(checkouterApiService)
        verifyOrderStatus(
            orderId = 81545128,
            status = MerchantOrderStatus.DELIVERY,
            substatus = MerchantOrderSubstatus.DELIVERY_SERVICE_RECEIVED,
            linesStatus = MerchantItemStatus.SHIPPED
        )
    }

    @Test
    @DisplayName("Проверка смены статуса с невалидным подстатусом")
    fun `change status with invalid substatus`() {
        changeStatus(
            orderId = 81545127,
            toStatus = ChangeOrderStatus.DELIVERY,
            toSubstatus = OrderSubStatus.STARTED,
            expectedHttpStatus = HttpStatus.SC_BAD_REQUEST
        ) {
            """{"className":"CommonApiErrorResponse",
                "actions":"partners/{partnerId}/common/orders/{orderId}/status",
                "errors":[{"code":"BUSINESS_VALIDATION_ERROR","message":"Substatus STARTED is not related to status DELIVERY",
                "details":{}}]}
            """
        }

        verifyZeroInteractions(checkouterApiService)
        verifyOrderStatus(
            orderId = 81545127,
            status = MerchantOrderStatus.PROCESSING,
            substatus = MerchantOrderSubstatus.STARTED,
            linesStatus = MerchantItemStatus.CREATED
        )
    }

    @Test
    @DisplayName("Проверка смены статуса на отмененный")
    fun `change status to cancelled`() {
        changeStatus(
            orderId = 81545127,
            toStatus = ChangeOrderStatus.CANCELLED,
            toSubstatus = OrderSubStatus.SHOP_FAILED,
            fromCheckouterRequestId = 12345,
            fromCheckouterRequestStatus = ChangeRequestStatus.APPLIED
        ) {
            """{
                "result":{
                    "partnerId":543900,
                    "orderId":81545127,
                    "status":"CANCELLED_IN_PROCESSING",
                    "substatus":"SHOP_FAILED",
                    "cancellationRequestCreated":true
                 }
            }"""
        }

        verifyOrderStatus(
            orderId = 81545127,
            status = MerchantOrderStatus.CANCELLED_IN_PROCESSING,
            substatus = MerchantOrderSubstatus.SHOP_FAILED,
            linesStatus = MerchantItemStatus.CANCELLED
        )
    }

    @Test
    @DisplayName("Проверка смены статуса на отмененный: смены нет, но создается заявка на отмену")
    fun `change status - verify that cancellation request created`() {
        changeStatus(
            orderId = 81545127,
            toStatus = ChangeOrderStatus.CANCELLED,
            toSubstatus = OrderSubStatus.SHOP_FAILED,
            fromCheckouterRequestId = 12345,
            fromCheckouterRequestStatus = ChangeRequestStatus.NEW
        ) {
            """{
                "result":{
                    "partnerId":543900,
                    "orderId":81545127,
                    "status":"PROCESSING",
                    "substatus":"STARTED",
                    "cancellationRequestCreated": true
                 }
            }"""
        }

        verifyOrderStatus(
            orderId = 81545127,
            status = MerchantOrderStatus.PROCESSING,
            substatus = MerchantOrderSubstatus.STARTED,
            linesStatus = MerchantItemStatus.CREATED
        )
        verifyCancellationRequest(81545127, 12345, MerchantOrderSubstatus.SHOP_FAILED)
    }

    @Test
    @DisplayName("Проверка смены статуса с невалидным подстатусом отмены")
    fun `change status with invalid cancel substatus`() {
        changeStatus(
            orderId = 81545127,
            toStatus = ChangeOrderStatus.CANCELLED,
            toSubstatus = OrderSubStatus.READY_TO_SHIP,
            fromCheckouterRequestId = 12345,
            fromCheckouterRequestStatus = ChangeRequestStatus.NEW,
            expectedHttpStatus = HttpStatus.SC_BAD_REQUEST
        ) {
            """{"className":"CommonApiErrorResponse",
                "actions":"partners/{partnerId}/common/orders/{orderId}/status",
                "errors":[{"code":"BUSINESS_VALIDATION_ERROR",
                "message":"Substatus READY_TO_SHIP is not related to status CANCELLED",
                "details":{}}]}
            """
        }

        verifyZeroInteractions(checkouterApiService)
        verifyOrderStatus(
            orderId = 81545127,
            status = MerchantOrderStatus.PROCESSING,
            substatus = MerchantOrderSubstatus.STARTED,
            linesStatus = MerchantItemStatus.CREATED
        )
    }

    @Test
    @DisplayName("Проверка смены статуса на CANCELLED 2 раза")
    fun `change status to cancelled twice`() {
        repeat(2) {
            changeStatus(
                orderId = 81545127,
                toStatus = ChangeOrderStatus.CANCELLED,
                toSubstatus = OrderSubStatus.SHOP_FAILED,
                fromCheckouterRequestId = 12345,
                fromCheckouterRequestStatus = ChangeRequestStatus.NEW
            ) {
                """{
                    "result":{
                        "partnerId":543900,
                        "orderId":81545127,
                        "status":"PROCESSING",
                        "substatus":"STARTED",
                        "cancellationRequestCreated": true
                     }
                }"""
            }
        }

        verifyOrderStatus(
            orderId = 81545127,
            status = MerchantOrderStatus.PROCESSING,
            substatus = MerchantOrderSubstatus.STARTED,
            linesStatus = MerchantItemStatus.CREATED
        )
        verifyCancellationRequest(81545127, 12345, MerchantOrderSubstatus.SHOP_FAILED)
    }

    @Test
    @DisplayName("Проверка отмены FaaS-заказа")
    fun `verify cancellation of FaaS order`() {
        changeStatus(
            91599999,
            ChangeOrderStatus.CANCELLED,
            OrderSubStatus.WRONG_ITEM,
            checkouterOrder = false
        ) {
            """{
                "result":{
                    "partnerId":543900,
                    "orderId":91599999,
                    "status":"DELIVERY",
                    "substatus":"DELIVERY_SERVICE_RECEIVED"
                 }
            }""".trimIndent()
        }

        val outboundEvents = ytOrderRepository.findUnprocessedOutboundEvents(0)
        assertThat(outboundEvents).singleElement().satisfies {
            assertThat(it.orderId).isEqualTo(91599999)
            assertThat(it.eventPayloadType).isEqualTo(PayloadCase.ORDER_CANCELLED_PAYLOAD.name)
            assertThat(it.eventPayload).isNotNull.isNotEqualTo(OrderCancelledPayload.getDefaultInstance())
        }

        verifyZeroInteractions(checkouterApiService)
    }

    private fun mockCheckouterCancellationRequest(
        orderId: Long,
        substatus: MerchantOrderSubstatus,
        fromCheckouterRequestId: Long,
        fromCheckouterRequestStatus: ChangeRequestStatus,
        partnerId: Long = 543900,
        notes: String? = null
    ) {
        val changeRequest = ChangeRequest(
            fromCheckouterRequestId,
            orderId,
            CancellationRequestPayload(OrderSubstatus.valueOf(substatus.name), notes, null, null),
            fromCheckouterRequestStatus,
            instant,
            null,
            ClientRole.SHOP_USER
        )
        whenever(
            checkouterApiService.editOrder(
                partnerId = eq(partnerId),
                orderId = eq(orderId),
                orderEditRequest = any(),
                actor = any(),
                actorId = any(),
                rgbs = any()
            )
        ).thenReturn(listOf(changeRequest))

    }

    private fun mockCheckouterUpdateStatus(
        orderId: Long,
        toStatus: MerchantOrderStatus,
        fromCheckouterSubstatus: OrderSubstatus?,
        toSubstatus: MerchantOrderSubstatus? = null,
        partnerId: Long = 543900
    ) {
        val order: Order = mock {
            on(it.status).thenReturn(OrderStatus.valueOf(toStatus.name))
            on(it.substatus).thenReturn(fromCheckouterSubstatus)
        }
        whenever(checkouterApiService.updateOrderStatus(
                eq(partnerId),
                eq(orderId),
                eq(toStatus),
                toSubstatus?.let { eq(it) } ?: anyOrNull(),
                any(),
                any()
        )).thenReturn(order)
    }

    private fun verifyOrderStatus(
        orderId: Long,
        status: MerchantOrderStatus,
        linesStatus: MerchantItemStatus,
        substatus: MerchantOrderSubstatus? = null,
        partnerId: Long = 543900
    ) {
        val order = ytOrderRepository.findOrderWithLinesByKey(OrderKey(partnerId, orderId))!!
        assertThat(order.order)
            .extracting("status", "substatus")
            .containsExactly(status, substatus)

        val linesStatuses = order.lines
            .map { line -> line.itemStatuses.statuses }
            .flatMap { statuses -> statuses.keys }
            .toSet()
        assertThat(linesStatuses).containsOnly(linesStatus.name)
    }

    private fun verifyOrderEvent(
        orderId: Long,
        status: MerchantOrderStatus,
        substatus: MerchantOrderSubstatus? = null,
        type: OrderEventType = OrderEventType.STATUS_CHANGE,
        partnerId: Long = 543900
    ) {
        orderEventService.findEventsByOrderKey(OrderKey(partnerId, orderId)).first {
            it.type == type && it.eventSource == EventSource.ORDER_SERVICE
        }.let {
            assertThat(it)
                .extracting("actor", "status", "substatus")
                .containsExactly(
                    Actor.MERCHANT_PI,
                    status,
                    substatus
                )
        }
    }

    private fun verifyCancellationRequest(
        orderId: Long,
        requestId: Long,
        cancellationRequestSubstatus: MerchantOrderSubstatus,
        partnerId: Long = 543900
    ) {
        val order = orderEntityRepository.lookupRow(OrderKey(partnerId, orderId))!!
        assertThat(order).isNotNull
            .satisfies {
                assertThat(order.hasCancellationRequest).isEqualTo(true)
                assertThat(order.cancellationRequest).isNotNull.isEqualTo(
                    CancellationRequest(
                        requestId,
                        instant,
                        cancellationRequestSubstatus
                    )
                )
            }
    }

    private fun verifyNoCancellationRequestExists(
        orderId: Long,
        partnerId: Long = 543900
    ) {
        val order = orderEntityRepository.lookupRow(OrderKey(partnerId, orderId))!!
        assertThat(order).isNotNull
            .satisfies {
                assertThat(order.hasCancellationRequest)
                    .satisfiesAnyOf(
                        { assertThat(it).isNull() },
                        { assertThat(it).isEqualTo(false) }
                    )
                assertThat(order.cancellationRequest)
                    .satisfiesAnyOf(
                        { assertThat(it).isNull() },
                        { assertThat(it).isEqualTo(CancellationRequest()) }
                    )
            }
    }

    private fun changeStatus(
        orderId: Long,
        toStatus: ChangeOrderStatus,
        toSubstatus: OrderSubStatus?,
        fromCheckouterSubstatus: OrderSubStatus? = toSubstatus,
        fromCheckouterRequestId: Long? = null,
        fromCheckouterRequestStatus: ChangeRequestStatus? = null,
        checkouterOrder: Boolean = true,
        partnerId: Long = 543900,
        expectedHttpStatus: Int = HttpStatus.SC_OK,
        expectedResponse: () -> String
    ) {
        if (checkouterOrder) {
            if (toStatus == ChangeOrderStatus.CANCELLED) {
                requireNotNull(fromCheckouterRequestId)
                requireNotNull(fromCheckouterRequestStatus)
                mockCheckouterCancellationRequest(
                    orderId,
                    MerchantOrderSubstatus.valueOf(toSubstatus!!.name),
                    fromCheckouterRequestId,
                    fromCheckouterRequestStatus,
                    partnerId = partnerId
                )
            } else {
                mockCheckouterUpdateStatus(
                    orderId,
                    MerchantOrderStatus.valueOf(toStatus.name),
                    fromCheckouterSubstatus?.let { OrderSubstatus.valueOf(it.name) },
                    toSubstatus = toSubstatus?.let { MerchantOrderSubstatus.valueOf(it.name) },
                    partnerId = partnerId
                )
            }
        }

        changeStatusRequest(
            orderId,
            toStatus,
            toSubstatus,
            expectedResponse(),
            expectedHttpStatus = expectedHttpStatus
        )
    }

    private fun changeStatusRequest(
        orderId: Long,
        status: ChangeOrderStatus,
        substatus: OrderSubStatus?,
        expected: String,
        partnerId: Long = 543900,
        expectedHttpStatus: Int = HttpStatus.SC_OK
    ) {
        val params = listOfNotNull(
            "status" to status.name,
            substatus?.let { "substatus" to it.name},
            "actor" to ActorType.MERCHANT_PI.name,
            "actorId" to "1234"
        ).toMap()
        val request = HttpPost(getUri("/partners/$partnerId/common/orders/$orderId/status", params))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")

        val response = HttpClientBuilder.create().build().execute(request)
        assertThat(response.statusLine.statusCode).isEqualTo(expectedHttpStatus)

        JSONAssert.assertEquals(
            expected,
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }

    private fun changeCancellationRequestStatus(
        orderId: Long,
        changeRequestStatus: ChangeRequestStatusType,
        confirmationReason: ConfirmationReasonType? = null,
        partnerId: Long = 543900,
        expectedHttpStatus: Int = HttpStatus.SC_OK,
        expectedResponse: () -> String
    ) {
        val params = listOfNotNull(
            "changeRequestStatus" to changeRequestStatus.name,
            confirmationReason?.let { "confirmationReason" to it.name },
            "actor" to ActorType.MERCHANT_PI.name,
            "actorId" to "1234"
        ).toMap()
        val request = HttpPost(getUri("/partners/$partnerId/common/orders/$orderId/cancellation-status", params))
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json")

        val response = HttpClientBuilder.create().build().execute(request)
        assertThat(response.statusLine.statusCode).isEqualTo(expectedHttpStatus)

        JSONAssert.assertEquals(
            expectedResponse(),
            IOUtils.toString(response.entity.content),
            JSONCompareMode.STRICT_ORDER
        )
    }
}
