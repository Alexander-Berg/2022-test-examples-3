package ru.yandex.market.mbi.orderservice.tms.service.logbroker.logistic.events

import com.google.protobuf.Timestamp
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent
import ru.yandex.market.logistics.logistics4shops.event.model.OrderBindToShipmentPayload
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderShipment
import ru.yandex.market.mbi.orderservice.common.model.yt.ProcessedLogisticEventEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderShipmentsRepository
import ru.yandex.market.mbi.orderservice.common.util.epochToNull
import ru.yandex.market.mbi.orderservice.common.util.toProtoTimestamp
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.yt.client.YtClientProxy
import java.time.Instant

@CleanupTables([OrderShipment::class, OrderEntity::class, ProcessedLogisticEventEntity::class])
class OrderShipmentProcessorTest : FunctionalTest() {

    @Autowired
    lateinit var logisticEventsMainProcessor: LogisticEventsMainProcessor

    @Autowired
    lateinit var orderShipmentsRepository: OrderShipmentsRepository

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var rwClient: YtClientProxy

    @Test
    fun `basic message persistence test`() {
        val messages = createMessages(
            createEvent(
                eventId = 1,
                partnerId = 100,
                orderId = 1,
                shipmentId = 123,
                shipmentDateTime = Instant.parse("2022-01-05T10:11:12Z").toProtoTimestamp()
            ),
            createEvent(
                eventId = 2,
                partnerId = 200,
                orderId = 2,
                shipmentId = 321,
                shipmentDateTime = Timestamp.newBuilder().build() // empty
            ),
        )

        rwClient.execInTransaction {
            logisticEventsMainProcessor.processMessages(messages, it)
        }

        val actualRows = orderShipmentsRepository.selectAll()

        assertThat(actualRows).hasSize(2).containsExactlyInAnyOrder(
            OrderShipment(
                tableKey = OrderShipment.Key(
                    partnerId = 100,
                    orderId = 1,
                    shipmentId = 123
                ),
                shipmentDate = "2022-01-05"
            ),
            OrderShipment(
                tableKey = OrderShipment.Key(
                    partnerId = 200,
                    orderId = 2,
                    shipmentId = 321
                ),
                shipmentDate = ""
            )
        )

        val orders = orderEntityRepository.lookupRows(
            setOf(OrderKey(100, 1), OrderKey(200, 2))
        )
        assertThat(orders).extracting(OrderEntity::key, OrderEntity::shipmentId, { it.shipmentDate?.epochToNull() })
            .containsExactlyInAnyOrder(
                tuple(OrderKey(100, 1), 123L, Instant.parse("2022-01-05T10:11:12Z")),
                tuple(OrderKey(200, 2), 321L, null)
            )
    }

    private fun createEvent(
        eventId: Long,
        partnerId: Long,
        orderId: Long,
        shipmentId: Long,
        shipmentDateTime: Timestamp
    ): LogisticEvent {
        return LogisticEvent.newBuilder()
            .setId(eventId)
            .setRequestId("abc/1")
            .setCreated(Instant.parse("2022-01-01T10:00:10Z").toProtoTimestamp())
            .setOrderBindToShipmentPayload(
                OrderBindToShipmentPayload.newBuilder()
                    .setShopId(partnerId)
                    .setOrderId(orderId)
                    .setShipmentId(shipmentId)
                    .setShipmentDateTime(shipmentDateTime)
            ).build()
    }
}
