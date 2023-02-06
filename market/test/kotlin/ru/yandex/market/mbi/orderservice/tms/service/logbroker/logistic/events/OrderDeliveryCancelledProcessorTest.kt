package ru.yandex.market.mbi.orderservice.tms.service.logbroker.logistic.events

import com.google.protobuf.util.JsonFormat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent
import ru.yandex.market.logistics.logistics4shops.event.model.OrderDeliveryCancelled
import ru.yandex.market.logistics.logistics4shops.event.model.OrderDeliveryCancelled.CancelReason
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadResourceAsString
import ru.yandex.market.mbi.helpers.loadTestEntity
import ru.yandex.market.mbi.orderservice.common.dsl.Actor
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderSubstatus
import ru.yandex.market.mbi.orderservice.common.enum.OrderStockFreezeStatus
import ru.yandex.market.mbi.orderservice.common.model.yt.EventSource
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEvent
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEventKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEventType
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderStockFreezeStatusEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderStockFreezeStatusKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OutboundOrderEventEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.ProcessedLogisticEventEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderStockFreezeStatusRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.OrderEventService
import ru.yandex.market.mbi.orderservice.common.util.toProtoTimestamp
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent.PayloadCase
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.yt.client.YtClientProxy
import java.time.Instant
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent as ProtoEvent

/**
 * Функциональные тесты на [OrderDeliveryCancelledEventProcessor]
 */
@DbUnitDataSet
@CleanupTables(
    [
        OrderEntity::class,
        OrderLineEntity::class,
        OrderEvent::class,
        ProcessedLogisticEventEntity::class,
        OutboundOrderEventEntity::class
    ]
)
class OrderDeliveryCancelledProcessorTest : FunctionalTest() {

    @Autowired
    lateinit var rwClient: YtClientProxy

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var orderStockFreezeStatusRepository: OrderStockFreezeStatusRepository

    @Autowired
    lateinit var orderEventService: OrderEventService

    @Autowired
    lateinit var logisticEventsMainProcessor: LogisticEventsMainProcessor

    @BeforeEach
    fun setUp() {
        val order = this::class.loadTestEntity<OrderEntity>("OrderDeliveryCancelledProcessorTest.orders.before.json")
        orderEntityRepository.insertRow(order)
        orderStockFreezeStatusRepository.insertRow(
            OrderStockFreezeStatusEntity(
                OrderStockFreezeStatusKey(partnerId = order.key.partnerId, orderId = order.key.orderId),
                orderCreatedAt = order.createdAt,
                stockFreezeStatus = OrderStockFreezeStatus.FROZEN
            )
        )
    }

    @Test
    fun `verify that order is transitioned to cancelled status`() {
        // given
        val messages = createMessages(createEvent(1, 100, 1, CancelReason.SHOP_CANCELLED))

        // when
        rwClient.execInTransaction { tx ->
            logisticEventsMainProcessor.processMessages(messages, tx)
        }

        // then
        val actualOrder = orderEntityRepository.lookupRow(OrderKey(100, 1))
        assertThat(actualOrder).isNotNull.satisfies { order ->
            if (order == null) fail { "Order is expected to be non-null" }
            assertThat(order.status).isEqualTo(MerchantOrderStatus.CANCELLED_IN_DELIVERY)
            assertThat(order.substatus).isEqualTo(MerchantOrderSubstatus.USER_CHANGED_MIND)
        }

        val events = orderEventService.findEventsByOrderKey(OrderKey(100, 1))
        assertThat(events).singleElement().satisfies { event ->
            assertThat(event).extracting(
                "key",
                "actor",
                "type",
                "status",
                "substatus",
                "eventSource"
            ).containsExactly(
                OrderEventKey(100, 1, 1),
                Actor.MERCHANT_PI,
                OrderEventType.STATUS_CHANGE,
                MerchantOrderStatus.CANCELLED_IN_DELIVERY,
                MerchantOrderSubstatus.USER_CHANGED_MIND,
                EventSource.LOGISTICS
            )
        }

        assertThat(orderStockFreezeStatusRepository.lookupRows(setOf(OrderStockFreezeStatusKey(100, 1))))
            .allSatisfy {
                assertThat(it.stockFreezeStatus).isEqualTo(OrderStockFreezeStatus.UNFREEZE_PENDING)
            }

        val outboundEvents = orderRepository.findUnprocessedOutboundEvents(0)
        assertThat(outboundEvents).singleElement().satisfies { event ->
            assertThat(event.requestId).isEqualTo("abc/1")
            assertThat(event.orderId).isEqualTo(1L)
            assertThat(event.eventPayloadType).isEqualTo(PayloadCase.ORDER_STATE_CHANGED_PAYLOAD.name)
            assertThat(ProtoEvent.parseFrom(event.eventPayload))
                .satisfies { envelope ->
                    assertThat(envelope.traceId).isEqualTo("abc/1")
                    JSONAssert.assertEquals(
                        this::class.loadResourceAsString("OrderDeliveryCancelledProcessorTest.payload.expected.1.json"),
                        JsonFormat.printer().print(envelope.orderStateChangedPayload),
                        JSONCompareMode.STRICT_ORDER
                    )
                }
        }
    }

    private fun createEvent(
        eventId: Long,
        partnerId: Long,
        orderId: Long,
        reason: CancelReason
    ): LogisticEvent {
        return LogisticEvent.newBuilder()
            .setId(eventId)
            .setRequestId("abc/1")
            .setCreated(Instant.parse("2022-01-01T10:00:10Z").toProtoTimestamp())
            .setOrderDeliveryCancelled(
                OrderDeliveryCancelled.newBuilder()
                    .setShopId(partnerId)
                    .setOrderId(orderId)
                    .setReason(reason)
                    .build()
            ).build()
    }
}
