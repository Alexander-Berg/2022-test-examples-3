package ru.yandex.market.mbi.orderservice.tms.service.logbroker.logistic.events

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistics4shops.event.model.ExpressOrderCreatedPayload
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.common.dsl.Actor
import ru.yandex.market.mbi.orderservice.common.model.yt.EventSource
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEvent
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEventKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEventType
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderKey
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.OrderEventService
import ru.yandex.market.mbi.orderservice.common.util.toProtoTimestamp
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.Instant

/**
 * Функциональные тесты на [ExpressOrderCreatedEventProcessor]
 */
@CleanupTables([OrderEntity::class, OrderEvent::class])
class ExpressOrderCreatedEventProcessorTest : FunctionalTest() {

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderEventService: OrderEventService

    @Autowired
    lateinit var logisticEventsMainProcessor: LogisticEventsMainProcessor

    @BeforeEach
    fun setUp() {
        this::class.loadTestEntities<OrderEntity>("ExpressOrderCreatedEventProcessorTest.orders.before.json").let {
            orderEntityRepository.insertRows(it)
        }
    }

    @Test
    fun `verify store orders deadline`() {
        val messages = createMessages(
            createEvent(1, 100, 1, Instant.parse("2022-01-01T10:10:10Z")),
            createEvent(2, 200, 2, Instant.parse("2022-10-01T00:00:00Z"))
        )

        orderRepository.executeInTransaction({ tx -> logisticEventsMainProcessor.processMessages(messages, tx) })

        val actualRows = orderEntityRepository.lookupRows(
            setOf(
                OrderKey(100, 1),
                OrderKey(200, 2)
            )
        )
        assertThat(actualRows)
            .extracting(OrderEntity::key, OrderEntity::shipmentDeadline, OrderEntity::orderLineIds)
            .containsExactlyInAnyOrder(
                Tuple(OrderKey(100, 1), Instant.parse("2022-01-01T10:10:10Z"), listOf(140430845L)),
                Tuple(OrderKey(200, 2), Instant.parse("2022-10-01T00:00:00Z"), listOf(140430846L)),
            )

        val events = orderEventService.findEventsByOrderKey(OrderKey(100, 1))
        assertThat(events).hasSize(1)
            .first()
            .usingRecursiveComparison().ignoringFields("timestamp")
            .isEqualTo(
                OrderEvent(
                    key = OrderEventKey(
                        partnerId = 100,
                        orderId = 1,
                        eventId = 1
                    ),
                    eventSpawnTimestamp = Instant.parse("2022-01-01T10:00:10Z"),
                    timestamp = Instant.now(),
                    actor = Actor.MARKETPLACE,
                    type = OrderEventType.SHIPMENT_DEADLINE_UPDATED,
                    status = null,
                    substatus = null,
                    eventSource = EventSource.LOGISTICS,
                    details = ""
                )
            )
    }

    private fun createEvent(
        eventId: Long,
        partnerId: Long,
        orderId: Long,
        deadline: Instant
    ): LogisticEvent {
        return LogisticEvent.newBuilder()
            .setId(eventId)
            .setRequestId("abc/1")
            .setCreated(Instant.parse("2022-01-01T10:00:10Z").toProtoTimestamp())
            .setExpressOrderCreatedPayload(
                ExpressOrderCreatedPayload.newBuilder()
                    .setShopId(partnerId)
                    .setOrderId(orderId)
                    .setPackagingDeadline(deadline.toProtoTimestamp())
                    .build()
            ).build()
    }
}
