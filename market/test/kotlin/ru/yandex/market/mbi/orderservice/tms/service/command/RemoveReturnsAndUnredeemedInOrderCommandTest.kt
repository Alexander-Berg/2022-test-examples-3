package ru.yandex.market.mbi.orderservice.tms.service.command

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.common.commandInvocationTest
import ru.yandex.market.mbi.orderservice.common.dsl.OrderTransitionGraph
import ru.yandex.market.mbi.orderservice.common.enum.LogisticReturnStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantItemStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderStatus
import ru.yandex.market.mbi.orderservice.common.enum.ReturnType
import ru.yandex.market.mbi.orderservice.common.model.yt.CheckouterEventEntities
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEvent
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.ReturnLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.ReturnLineIdIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.ReturnLineKey
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.ReturnLineOrderLineIdIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.checkouter.CheckouterReturnCreatedAtIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.checkouter.CheckouterReturnEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.checkouter.CheckouterReturnKey
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.checkouter.CheckouterReturnLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.checkouter.CheckouterReturnLineKey
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.checkouter.CheckouterReturnOrderIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.HistoricalReturnEventEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.InvalidLogisticEventEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.LogisticReturnLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.LogisticReturnLineKey
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.LogisticReturnManagementEventEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.CheckouterOrderIdIndexRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.CheckouterReturnCreatedAtIdxRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.CheckouterReturnLineRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.CheckouterReturnOrderIdxRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.CheckouterReturnRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.LogisticReturnLineRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderLineEntityRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.ReturnLineIdIdxRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.ReturnLineOrderLineIdIdxRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.ReturnLineRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtReturnRepository
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.TableBindingHolder
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.yt.client.YtClientProxySource
import java.time.Instant

@CleanupTables(
    [
        OrderEntity::class,
        OrderLineEntity::class,
        OrderEvent::class,
        ReturnLineEntity::class,
        LogisticReturnLineEntity::class,
        LogisticReturnManagementEventEntity::class,
        CheckouterReturnEntity::class,
        CheckouterReturnLineEntity::class,
        CheckouterReturnOrderIndex::class,
        CheckouterReturnCreatedAtIndex::class,
        ReturnLineOrderLineIdIndex::class,
        ReturnLineIdIndex::class,
        InvalidLogisticEventEntity::class,
        HistoricalReturnEventEntity::class
    ]
)
class RemoveReturnsAndUnredeemedInOrderCommandTest : FunctionalTest() {

    @Autowired
    lateinit var roClient: YtClientProxySource

    @Autowired
    lateinit var tableBindingHolder: TableBindingHolder

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var returnRepository: YtReturnRepository

    @Autowired
    lateinit var orderEntityRepository: OrderEntityRepository

    @Autowired
    lateinit var orderIdIndexRepository: CheckouterOrderIdIndexRepository

    @Autowired
    lateinit var orderLineEntityRepository: OrderLineEntityRepository

    @Autowired
    lateinit var orderTransitionGraph: OrderTransitionGraph

    @Autowired
    lateinit var returnLineEntityRepository: ReturnLineRepository

    @Autowired
    lateinit var logisticReturnLineRepository: LogisticReturnLineRepository

    @Autowired
    lateinit var checkouterReturnLineRepository: CheckouterReturnLineRepository

    @Autowired
    lateinit var checkouterReturnRepository: CheckouterReturnRepository

    @Autowired
    lateinit var checkouterReturnCreatedAtIdxRepository: CheckouterReturnCreatedAtIdxRepository

    @Autowired
    lateinit var checkouterReturnOrderIdxRepository: CheckouterReturnOrderIdxRepository

    @Autowired
    lateinit var returnLineOrderLineIdIdxRepository: ReturnLineOrderLineIdIdxRepository

    @Autowired
    lateinit var returnLineIdIdxRepository: ReturnLineIdIdxRepository

    @Test
    fun commandTest() {
        val removeReturnsAndUnredeemedInOrderCommand =
            RemoveReturnsAndUnredeemedInOrderCommand(
                roClient,
                tableBindingHolder,
                orderRepository,
                returnRepository,
                returnLineEntityRepository,
                logisticReturnLineRepository,
                checkouterReturnLineRepository,
                checkouterReturnRepository,
                orderIdIndexRepository,
                checkouterReturnCreatedAtIdxRepository,
                checkouterReturnOrderIdxRepository,
                returnLineOrderLineIdIdxRepository,
                returnLineIdIdxRepository,
                orderEntityRepository,
                orderLineEntityRepository,
                orderTransitionGraph
            )

        val orders = createOrdersBatch("entities/orders-batch-1.json")
        val orderLines = createOrderLinesBatch("entities/order-lines-batch-1.json")

        returnLineEntityRepository.insertRows(
            listOf(
                ReturnLineEntity(
                    ReturnLineKey(
                        partnerId = 1223579,
                        orderId = 79914680,
                        orderLineId = 138268465,
                        returnLineId = 1
                    ),
                    returnType = ReturnType.UNREDEEMED
                ),
                ReturnLineEntity(
                    ReturnLineKey(
                        partnerId = 1223579,
                        orderId = 79914680,
                        orderLineId = 138268465,
                        returnLineId = 2
                    ),
                    returnType = ReturnType.UNREDEEMED
                )
            )
        )

        returnLineOrderLineIdIdxRepository.insertRows(
            listOf(
                ReturnLineOrderLineIdIndex(
                    ReturnLineOrderLineIdIndex.Key(
                        orderLineId = 138268465
                    ),
                    returnLineIds = setOf(1, 2)
                )
            )
        )

        logisticReturnLineRepository.insertRows(
            listOf(
                LogisticReturnLineEntity(
                    LogisticReturnLineKey(
                        returnLineId = 1,
                        eventId = 1
                    ),
                    count = 1,
                    logisticReturnLineStatus = LogisticReturnStatus.EXPIRED,
                    logisticReturnLineStatusCommittedAt = Instant.now()
                ),
                LogisticReturnLineEntity(
                    LogisticReturnLineKey(
                        returnLineId = 1,
                        eventId = 2
                    ),
                    count = 1,
                    logisticReturnLineStatus = LogisticReturnStatus.EXPIRED,
                    logisticReturnLineStatusCommittedAt = Instant.now()
                )
            )
        )

        checkouterReturnRepository.insertRows(
            listOf(
                CheckouterReturnEntity(
                    CheckouterReturnKey(
                        partnerId = 1223579,
                        checkouterReturnId = 1
                    ),
                    orderId = 79914680,
                    returnLineIds = setOf(1L)
                )
            )
        )

        checkouterReturnLineRepository.insertRows(
            listOf(
                CheckouterReturnLineEntity(
                    CheckouterReturnLineKey(
                        returnLineId = 1,
                        returnItemId = 1,
                    ),
                    checkouterReturnId = 1
                )
            )
        )

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        checkouterReturnOrderIdxRepository.insertRows(
            listOf(CheckouterReturnOrderIndex(OrderKey(1223579, 79914680), setOf(1)))
        )

        commandInvocationTest(
            removeReturnsAndUnredeemedInOrderCommand,
            """remove-returns-in-order-lines --orderIds=79914680 --unredeemedTargetStatus=CANCELLED"""
        )

        Assertions.assertThat(
            returnLineEntityRepository.lookupRows(
                setOf(
                    ReturnLineKey(
                        partnerId = 1223579,
                        orderId = 79914680,
                        orderLineId = 138268465,
                        returnLineId = 1
                    ),
                    ReturnLineKey(
                        partnerId = 1223579,
                        orderId = 79914680,
                        orderLineId = 138268465,
                        returnLineId = 2
                    )
                )
            )
        ).hasSize(0)

        Assertions.assertThat(
            logisticReturnLineRepository.lookupRows(
                setOf(
                    LogisticReturnLineKey(
                        returnLineId = 1,
                        eventId = 1
                    ),
                    LogisticReturnLineKey(
                        returnLineId = 1,
                        eventId = 2
                    )
                )
            )
        ).hasSize(0)

        Assertions.assertThat(
            checkouterReturnRepository.lookupRows(
                setOf(
                    CheckouterReturnKey(
                        partnerId = 1223579,
                        checkouterReturnId = 1
                    )
                )
            )
        ).hasSize(0)

        Assertions.assertThat(
            checkouterReturnLineRepository.lookupRows(
                setOf(
                    CheckouterReturnLineKey(
                        returnLineId = 1,
                        returnItemId = 1
                    )
                )
            )
        ).hasSize(0)

        Assertions.assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 1223579,
                    orderId = 79914680
                )
            )
        ).satisfies {
            require(it != null)

            Assertions.assertThat(it.order.status).isEqualTo(MerchantOrderStatus.PARTIALLY_DELIVERED)

            val line = it.lines.find { line -> line.orderLineId == 138268465L }!!
            Assertions.assertThat(line.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(2)
            Assertions.assertThat(line.itemStatuses.statuses[MerchantItemStatus.CANCELLED.name]).isEqualTo(1)
        }
    }

    private fun createOrdersBatch(resourceName: String): List<OrderEntity> {
        return this::class.loadTestEntities<OrderEntity>(resourceName)
            .sortedBy { it.key.orderId }
    }

    private fun createOrderLinesBatch(resourceName: String): List<OrderLineEntity> {
        return this::class.loadTestEntities<OrderLineEntity>(resourceName)
            .sortedBy { it.key.orderId }
    }
}
