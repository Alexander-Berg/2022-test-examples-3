package ru.yandex.market.mbi.orderservice.tms.service.logbroker.logistic.events

import com.google.protobuf.util.JsonFormat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.common.test.util.StringTestUtil
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.defaultTestMapper
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.common.enum.MerchantItemStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderSubstatus
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEditRequestEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEvent
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEventType
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.ProcessedLogisticEventEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEditRequestRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEventsRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.Clock
import java.time.Instant

/**
 * Функциональные тесты на [OrderEditRequestUpdatedProcessor]
 */
@DbUnitDataSet
@CleanupTables(
    [
        OrderEntity::class,
        OrderLineEntity::class,
        OrderEvent::class,
        OrderEditRequestEntity::class,
        ProcessedLogisticEventEntity::class
    ]
)
class OrderEditRequestUpdatedProcessorTest : FunctionalTest() {

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderLineEntityRepository: OrderLineEntityRepository

    @Autowired
    lateinit var orderEditRequestRepository: OrderEditRequestRepository

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
    fun `basic message persistence test`() {
        this::class.loadTestEntities<OrderEntity>("OrderEditRequestUpdatedProcessorTest.orders.before.json").let {
            orderEntityRepository.insertRows(it)
        }
        this::class.loadTestEntities<OrderLineEntity>("OrderEditRequestUpdatedProcessorTest.orderLines.before.json")
            .let {
                orderLineEntityRepository.insertRows(it)
            }

        this::class.loadTestEntities<OrderEditRequestEntity>("OrderEditRequestUpdatedProcessorTest.changeRequests.before.json")
            .let {
                orderEditRequestRepository.insertRows(it)
            }

        val messages = createLogisticEventsBatch(
            "OrderEditRequestUpdatedProcessorTest.event.before.json"
        ).map { createMessages(it) }.flatten()

        orderRepository.executeInTransaction({ tx -> logisticEventsMainProcessor.processMessages(messages, tx) })


        assertThat(
            orderEntityRepository.selectRows(
                "* from [${orderEntityRepository.tablePath}] where orderId = 79914680",
                true
            )
        ).allSatisfy {
            assertThat(it.status).isEqualTo(MerchantOrderStatus.PROCESSING)
            assertThat(it.substatus).isEqualTo(MerchantOrderSubstatus.PACKAGING)
        }

        assertThat(
            orderLineEntityRepository.selectRows(
                "* from [${orderLineEntityRepository.tablePath}] where orderId = 79914680",
                true
            )
        ).satisfies { list ->
            assertThat(list.firstOrNull { line -> line.key.orderLineId == 138268465L }).satisfies {
                assertThat(it!!.countInDelivery).isEqualTo(0)
                assertThat(it.itemStatuses!!.statuses[MerchantItemStatus.DELETED.name]).isEqualTo(3)
                assertThat(it.itemStatuses!!.statuses[MerchantItemStatus.CREATED.name]).isNull()
            }

            assertThat(list.firstOrNull { line -> line.key.orderLineId == 138268466L }).satisfies {
                assertThat(it!!.countInDelivery).isEqualTo(1)
                assertThat(it.itemStatuses!!.statuses[MerchantItemStatus.DELETED.name]).isEqualTo(1)
                assertThat(it.itemStatuses!!.statuses[MerchantItemStatus.CREATED.name]).isEqualTo(1)
            }
        }

        assertThat(
            orderEventEntityRepository.selectRows(
                "* from [${orderEventEntityRepository.tablePath}] where orderId = 79914680",
                true
            )
        ).allSatisfy {
            assertThat(it.type).isEqualTo(OrderEventType.ORDER_ITEMS_CHANGED)
        }

        assertThat(
            orderEntityRepository.selectRows(
                "* from [${orderEntityRepository.tablePath}] where orderId = 79914681",
                true
            )
        ).allSatisfy {
            assertThat(it.status).isEqualTo(MerchantOrderStatus.CANCELLED_IN_PROCESSING)
            assertThat(it.substatus).isEqualTo(MerchantOrderSubstatus.MISSING_ITEM)
        }

        assertThat(
            orderLineEntityRepository.selectRows(
                "* from [${orderLineEntityRepository.tablePath}] where orderId = 79914681",
                true
            )
        ).satisfies { list ->
            assertThat(list.firstOrNull { line -> line.key.orderLineId == 138268467L }).satisfies {
                assertThat(it!!.countInDelivery).isEqualTo(0)
                assertThat(it.itemStatuses!!.statuses[MerchantItemStatus.DELETED.name]).isEqualTo(2)
                assertThat(it.itemStatuses!!.statuses[MerchantItemStatus.CREATED.name]).isNull()
            }
        }

        assertThat(
            orderEventEntityRepository.selectRows(
                "* from [${orderEventEntityRepository.tablePath}] where orderId = 79914681",
                true
            )
        ).hasSize(2)
    }

    private fun createLogisticEventsBatch(resourceName: String): List<LogisticEvent> {
        val s = StringTestUtil.getString(this.javaClass, resourceName)
        val tree = defaultTestMapper.readTree(s)
        return tree.asSequence()
            .map { defaultTestMapper.writeValueAsString(it) }
            .map {
                val builder = LogisticEvent.newBuilder()
                JsonFormat.parser().merge(it, builder)
                builder.build()
            }.toList()
    }
}
