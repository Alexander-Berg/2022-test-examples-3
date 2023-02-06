package ru.yandex.market.mbi.orderservice.tms.service.logbroker.logistic.events

import com.google.protobuf.util.JsonFormat
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.logistics.logistics4shops.event.model.ImportWarehouseType
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent
import ru.yandex.market.logistics.logistics4shops.event.model.OrderDeliveryNewStatus
import ru.yandex.market.logistics.logistics4shops.event.model.SortedAtScData
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadResourceAsString
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.helpers.loadTestEntity
import ru.yandex.market.mbi.orderservice.common.dsl.Actor
import ru.yandex.market.mbi.orderservice.common.enum.MerchantItemStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderSubstatus
import ru.yandex.market.mbi.orderservice.common.enum.OrderStockFreezeStatus
import ru.yandex.market.mbi.orderservice.common.enum.ShipmentType
import ru.yandex.market.mbi.orderservice.common.enum.TransitWarehouseType
import ru.yandex.market.mbi.orderservice.common.model.yt.EventSource
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEvent
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEventType
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLogisticsEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderStockFreezeStatusEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderStockFreezeStatusKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OutboundOrderEventEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.ProcessedLogisticEventEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEventsRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderStockFreezeStatusRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.util.toProtoTimestamp
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.Clock
import java.time.Instant
import ru.yandex.market.logistics.logistics4shops.event.model.ShipmentType as LogisticShipmentType
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent as ProtoOrderEvent

/**
 * Функциональные тесты на [OrderTrackingEventProcessor]
 */
@DbUnitDataSet
@CleanupTables(
    [
        OrderEntity::class,
        OrderEvent::class,
        OrderStockFreezeStatusEntity::class,
        ProcessedLogisticEventEntity::class,
        OrderLogisticsEntity::class,
        OutboundOrderEventEntity::class
    ]
)
class OrderTrackingEventProcessorTest : FunctionalTest() {

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderLineEntityRepository: OrderLineEntityRepository

    @Autowired
    lateinit var orderStockFreezeStatusRepository: OrderStockFreezeStatusRepository

    @Autowired
    lateinit var orderEventEntityRepository: OrderEventsRepository

    @Autowired
    lateinit var logisticEventsMainProcessor: LogisticEventsMainProcessor

    @Autowired
    lateinit var clock: Clock

    @BeforeEach
    fun setUp() {
        whenever(clock.instant()).thenReturn(Instant.now())
    }

    @Test
    fun enumCompatibilityTest() {
        assertThatCode {
            ImportWarehouseType.values()
                .filterNot { it == ImportWarehouseType.UNRECOGNIZED }
                .forEach {
                    TransitWarehouseType.valueOf(it.name)
                }
            LogisticShipmentType.values()
                .filterNot { it == LogisticShipmentType.UNRECOGNIZED }
                .forEach {
                    ShipmentType.valueOf(it.name)
                }
        }.doesNotThrowAnyException()
    }

    @Test
    fun `verify stock freezing`() {
        this::class.loadTestEntities<OrderEntity>("OrderTrackingEventProcessorTest.orders.before.json").let {
            orderEntityRepository.insertRows(it)

            it.filter { it.status == MerchantOrderStatus.PROCESSING && it.substatus == MerchantOrderSubstatus.STARTED }
                .forEach { order ->
                    orderStockFreezeStatusRepository.insertRow(
                        OrderStockFreezeStatusEntity(
                            key = OrderStockFreezeStatusKey(order.key.partnerId, order.key.orderId),
                            orderCreatedAt = order.createdAt,
                            stockFreezeStatus = OrderStockFreezeStatus.STARTED
                        )
                    )
                }

            it.filter { it.status == MerchantOrderStatus.PROCESSING && it.substatus != MerchantOrderSubstatus.STARTED }
                .forEach { order ->
                    orderStockFreezeStatusRepository.insertRow(
                        OrderStockFreezeStatusEntity(
                            key = OrderStockFreezeStatusKey(order.key.partnerId, order.key.orderId),
                            orderCreatedAt = order.createdAt,
                            stockFreezeStatus = OrderStockFreezeStatus.UNFREEZE_PENDING
                        )
                    )
                }
        }

        val messages = createMessages(
            createEvent(100, 1000000000001, 100, OrderDeliveryNewStatus.DeliveryStatus.SHIPPED),
            createEvent(200, 1000000000002, 101, OrderDeliveryNewStatus.DeliveryStatus.DELIVERED)
        )

        orderRepository.executeInTransaction({ tx -> logisticEventsMainProcessor.processMessages(messages, tx) })

        assertThat(
            orderEntityRepository.lookupRow(
                OrderKey(100, 1000000000001)
            )
        ).satisfies {
            assertThat(it!!.status).isEqualTo(MerchantOrderStatus.DELIVERY)
            assertThat(it.substatus).isEqualTo(MerchantOrderSubstatus.DELIVERY_SERVICE_RECEIVED)
        }

        assertThat(
            orderEntityRepository.lookupRow(
                OrderKey(200, 1000000000002)
            )
        ).satisfies {
            assertThat(it!!.status).isEqualTo(MerchantOrderStatus.DELIVERED)
            assertThat(it.substatus).isEqualTo(MerchantOrderSubstatus.DELIVERED_USER_RECEIVED)
        }

        assertThat(
            orderStockFreezeStatusRepository.lookupRows(
                setOf(
                    OrderStockFreezeStatusKey(100, 1000000000001),
                    OrderStockFreezeStatusKey(200, 1000000000002)
                )
            )
        ).allSatisfy {
            assertThat(it.stockFreezeStatus).isEqualTo(OrderStockFreezeStatus.UNFREEZE_PENDING)
        }

        assertThat(
            orderLineEntityRepository.selectRows(
                "* from [${orderLineEntityRepository.tablePath}] where partnerId = 100 and orderId = 1"
            )
        ).allSatisfy {
            assertThat(it.itemStatuses?.statuses?.keys).allSatisfy { status ->
                assertThat(status)
                    .isEqualTo(MerchantItemStatus.SHIPPED)
            }
        }

        assertThat(
            orderLineEntityRepository.selectRows(
                "* from [${orderLineEntityRepository.tablePath}] where partnerId = 200 and orderId = 2"
            )
        ).allSatisfy {
            assertThat(it.itemStatuses?.statuses?.keys).allSatisfy { status ->
                assertThat(status)
                    .isEqualTo(MerchantItemStatus.DELIVERED_TO_BUYER)
            }
        }

        assertThat(
            orderEventEntityRepository.selectRows(
                query = "* from [${orderEventEntityRepository.tablePath}]" +
                    " where orderId = 1000000000001 or orderId = 1000000000002",
                allowFullScan = true
            )
        )
            .satisfies { events ->
                assertThat(events.first { it.key.orderId == 1000000000001L }).satisfies { event ->
                    assertThat(event.key.partnerId).isEqualTo(100)
                    assertThat(event.key.orderId).isEqualTo(1000000000001)
                    assertThat(event.key.eventId).isEqualTo(100)
                    assertThat(event.status).isEqualTo(MerchantOrderStatus.DELIVERY)
                    assertThat(event.substatus).isEqualTo(MerchantOrderSubstatus.DELIVERY_SERVICE_RECEIVED)
                    assertThat(event.actor).isEqualTo(Actor.MARKETPLACE)
                    assertThat(event.eventSource).isEqualTo(EventSource.LOGISTICS)
                }
                assertThat(events.first { it.key.orderId == 1000000000002L }).satisfies { event ->
                    assertThat(event.key.partnerId).isEqualTo(200)
                    assertThat(event.key.orderId).isEqualTo(1000000000002)
                    assertThat(event.key.eventId).isEqualTo(101)
                    assertThat(event.status).isEqualTo(MerchantOrderStatus.DELIVERED)
                    assertThat(event.substatus).isEqualTo(MerchantOrderSubstatus.DELIVERED_USER_RECEIVED)
                    assertThat(event.actor).isEqualTo(Actor.MARKETPLACE)
                    assertThat(event.eventSource).isEqualTo(EventSource.LOGISTICS)
                }
            }

        val outboundEvents = orderRepository.findUnprocessedOutboundEvents(0)
        assertThat(outboundEvents).hasSize(2).satisfies { events ->
            assertThat(events).allSatisfy { assertThat(it.requestId).isEqualTo("abc/1") }
            assertThat(events).extracting("orderId").contains(1000000000001L, 1000000000002L)
            assertThat(events).allSatisfy {
                assertThat(it.eventPayloadType).isEqualTo(ProtoOrderEvent.PayloadCase.ORDER_STATE_CHANGED_PAYLOAD.name)
            }
            JSONAssert.assertEquals(
                this::class.loadResourceAsString("OrderTrackingEventProcessorTest.payload.expected.1.json"),
                JsonFormat.printer()
                    .includingDefaultValueFields()
                    .print(
                        ProtoOrderEvent.parseFrom(events.find { it.orderId == 1000000000001L }!!.eventPayload).orderStateChangedPayload
                    ),
                JSONCompareMode.STRICT
            )
            JSONAssert.assertEquals(
                this::class.loadResourceAsString("OrderTrackingEventProcessorTest.payload.expected.2.json"),
                JsonFormat.printer()
                    .includingDefaultValueFields()
                    .print(
                        ProtoOrderEvent.parseFrom(events.find { it.orderId == 1000000000002L }!!.eventPayload).orderStateChangedPayload
                    ),
                JSONCompareMode.STRICT
            )
        }
    }

    @Test
    fun `verify that non-faas events do not trigger status change`() {
        this::class.loadTestEntity<OrderEntity>("OrderTrackingEventProcessorTest.orders.non_faas.before.json").let {
            orderEntityRepository.insertRow(it)
        }

        val messages = createMessages(
            createEvent(
                partnerId = 1,
                orderId = 100,
                eventId = 300,
                status = OrderDeliveryNewStatus.DeliveryStatus.COURIER_ARRIVED
            ),
            createEvent(
                partnerId = 2,
                orderId = 200,
                eventId = 400,
                status = OrderDeliveryNewStatus.DeliveryStatus.SORTED_AT_SC
            )
        )

        assertThatCode {
            // заказа 100 нет в БД, но ошибка не бросается, т.к. заказ с данным статусом не должен обрабатываться
            orderRepository.executeInTransaction({ tx -> logisticEventsMainProcessor.processMessages(messages, tx) })
        }.doesNotThrowAnyException()

        val logisticsEntity = orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(2, 200))
        assertThat(logisticsEntity).isNotNull
            .extracting("transitWarehouseType", "shipmentType")
            .containsExactly(TransitWarehouseType.DROPOFF, ShipmentType.IMPORT)
        assertThat(
            orderEventEntityRepository.selectRows(
                query = "* from [${orderEventEntityRepository.tablePath}]" +
                    " where partnerId = 2 and orderId = 200",
            )
        ).singleElement().satisfies { event ->
            assertThat(event.key.eventId).isEqualTo(400)
            assertThat(event.eventSource).isEqualTo(EventSource.LOGISTICS)
            assertThat(event.type).isEqualTo(OrderEventType.SUBSTATUS_CHANGE)
            assertThat(event.eventSpawnTimestamp).isEqualTo("2022-01-01T09:30:00Z")
            assertThat(event.status).isEqualTo(MerchantOrderStatus.PROCESSING)
            assertThat(event.substatus).isEqualTo(MerchantOrderSubstatus.SHIPPED)
            assertThat(event.actor).isEqualTo(Actor.MARKETPLACE)
        }
    }

    @Test
    fun `verify sorted_at_sc event handling`() {
        this::class.loadTestEntity<OrderEntity>("OrderTrackingEventProcessorTest.orders.incorrect_status.before.json").let {
            orderEntityRepository.insertRow(it)
        }

        val messages = createMessages(
            createEvent(
                partnerId = 2,
                orderId = 200,
                eventId = 400,
                status = OrderDeliveryNewStatus.DeliveryStatus.SORTED_AT_SC
            )
        )

        assertThatCode {
            // заказ уже отменен в b2c, это штатная ситуация
            orderRepository.executeInTransaction({ tx -> logisticEventsMainProcessor.processMessages(messages, tx) })
        }.doesNotThrowAnyException()

        // проверяем, что мы все равно записали информацию об СЦ
        val logisticsEntity = orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(2, 200))
        assertThat(logisticsEntity).isNotNull
            .extracting("transitWarehouseType", "shipmentType")
            .containsExactly(TransitWarehouseType.DROPOFF, ShipmentType.IMPORT)
    }

    private fun createEvent(
        partnerId: Long,
        orderId: Long,
        eventId: Long,
        status: OrderDeliveryNewStatus.DeliveryStatus
    ): LogisticEvent {
        val payloadBuilder = OrderDeliveryNewStatus.newBuilder()
            .setShopId(partnerId)
            .setOrderId(orderId)
            .setStatus(status)
            .setStatusOriginalDate(Instant.parse("2022-01-01T09:30:00Z").toProtoTimestamp())

        if (status == OrderDeliveryNewStatus.DeliveryStatus.SORTED_AT_SC) {
            payloadBuilder.sortedAtScData = SortedAtScData.newBuilder()
                .setShipmentType(ru.yandex.market.logistics.logistics4shops.event.model.ShipmentType.IMPORT)
                .setImportWarehouseType(ImportWarehouseType.DROPOFF)
                .build()
        }

        return LogisticEvent.newBuilder()
            .setId(eventId)
            .setRequestId("abc/1")
            .setCreated(Instant.parse("2022-01-01T10:00:10Z").toProtoTimestamp())
            .setOrderDeliveryNewStatus(payloadBuilder.build()).build()
    }
}
