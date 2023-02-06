package ru.yandex.market.mbi.orderservice.tms.service.logbroker.logistic.events

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent
import ru.yandex.market.logistics.logistics4shops.event.model.OrderItemInstance
import ru.yandex.market.logistics.logistics4shops.event.model.OrderItemInstances
import ru.yandex.market.logistics.logistics4shops.event.model.OrderItemsInstancesUpdatedPayload
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.common.model.yt.Cis
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEvent
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderStockFreezeStatusEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.ProcessedLogisticEventEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.util.toProtoTimestamp
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.Clock
import java.time.Instant

/**
 * Функциональные тесты на [OrderItemInstancesUpdatedProcessor]
 */
@DbUnitDataSet
@CleanupTables(
    [
        OrderEntity::class,
        OrderLineEntity::class,
        OrderEvent::class,
        OrderStockFreezeStatusEntity::class,
        ProcessedLogisticEventEntity::class
    ]
)
class OrderItemInstancesUpdatedProcessorTest : FunctionalTest() {

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderLineEntityRepository: OrderLineEntityRepository

    @Autowired
    lateinit var logisticEventsMainProcessor: LogisticEventsMainProcessor

    @Autowired
    lateinit var clock: Clock

    @BeforeEach
    fun setUp() {
        whenever(clock.instant()).thenReturn(Instant.now())
    }

    @Test
    fun `basic message persistence test`() {
        this::class.loadTestEntities<OrderEntity>("OrderItemInstancesUpdatedProcessorTest.orders.before.json").let {
            orderEntityRepository.insertRows(it)
            orderLineEntityRepository.insertRows(it.flatMap { order ->
                order.orderLineIds.map { lineId ->
                    OrderLineEntity(
                        key = OrderLineKey(
                            partnerId = order.key.partnerId,
                            orderId = order.key.orderId,
                            orderLineId = lineId
                        ),
                        shopSku = lineId.toString()
                    )
                }
            })
        }

        val messages = createMessages(
            createEvent(
                100, 1, 100,
                mapOf("140430845" to setOf("1" to "0104603619000087211096790147186\u001D91EE06\u001D92gI47gJ+RXr57AszxcRTbbBueaPOmEgPKeEROpedGiWs="))
            ),
            createEvent(
                200, 2, 101,
                mapOf("140430846" to setOf("2" to (null as String?)))
            )
        )

        orderRepository.executeInTransaction({ tx -> logisticEventsMainProcessor.processMessages(messages, tx) })

        assertThat(
            orderLineEntityRepository.selectRows(
                "* from [${orderLineEntityRepository.tablePath}] where partnerId = 100 and orderId = 1"
            )
        ).allSatisfy {
            assertThat(it.cis).contains("1")
            assertThat(it.identifiers!!.cisFull!!).contains(Cis("0104603619000087211096790147186\u001D91EE06\u001D92gI47gJ+RXr57AszxcRTbbBueaPOmEgPKeEROpedGiWs="))
        }

        assertThat(
            orderLineEntityRepository.selectRows(
                "* from [${orderLineEntityRepository.tablePath}] where partnerId = 200 and orderId = 2"
            )
        ).allSatisfy {
            assertThat(it.cis).contains("2")
            assertThat(it.identifiers!!.cisFull!!).isEmpty()
        }
    }

    private fun createEvent(
        partnerId: Long,
        orderId: Long,
        eventId: Long,
        instances: Map<String, Set<Pair<String?, String?>>>
    ): LogisticEvent {
        return LogisticEvent.newBuilder()
            .setId(eventId)
            .setRequestId("abc/1")
            .setCreated(Instant.parse("2022-01-01T10:00:10Z").toProtoTimestamp())
            .setOrderItemsInstancesUpdatedPayload(
                OrderItemsInstancesUpdatedPayload.newBuilder()
                    .setShopId(partnerId)
                    .setOrderId(orderId)
                    .addAllItemInstances(instances.map {
                        val builder = OrderItemInstances.newBuilder()
                        builder.ssku = it.key

                        builder.addAllInstances(
                            it.value.map { pair ->
                                val innerBuilder = OrderItemInstance.newBuilder()

                                pair.first?.let { value -> innerBuilder.cis = value }
                                pair.second?.let { value -> innerBuilder.cisFull = value }

                                innerBuilder.build()
                            }
                        )


                        builder.build()
                    })
                    .build()
            ).build()
    }
}
