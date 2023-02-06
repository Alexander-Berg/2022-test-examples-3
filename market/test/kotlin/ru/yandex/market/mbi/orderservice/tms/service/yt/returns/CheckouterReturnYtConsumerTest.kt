package ru.yandex.market.mbi.orderservice.tms.service.yt.returns

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.common.enum.CheckouterReturnReasonType
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
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.LogisticReturnManagementEventEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.CheckouterOrderIdIndexRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.CheckouterReturnLineRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.CheckouterReturnOrderIdxRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.CheckouterReturnRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtReturnRepository
import ru.yandex.market.mbi.orderservice.common.service.pg.EnvironmentService
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.TableBindingHolder
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.tms.persistence.repository.yt.CheckouterReturnImportFilter
import ru.yandex.market.mbi.orderservice.tms.persistence.repository.yt.CheckouterReturnYt
import ru.yandex.market.mbi.orderservice.tms.persistence.repository.yt.CheckouterReturnYtDao
import ru.yandex.market.mbi.orderservice.tms.service.yt.events.CheckouterReturnYtConsumer
import ru.yandex.market.mbi.orderservice.tms.service.yt.events.ReturnExecutorsHelper
import ru.yandex.market.yt.client.YtClientProxy

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
class CheckouterReturnYtConsumerTest : FunctionalTest() {

    @Autowired
    lateinit var environmentService: EnvironmentService

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var checkouterReturnRepository: CheckouterReturnRepository

    @Autowired
    lateinit var checkouterReturnLineEntityRepository: CheckouterReturnLineRepository

    @Autowired
    lateinit var returnRepository: YtReturnRepository

    @Autowired
    lateinit var readWriteClient: YtClientProxy

    @Autowired
    lateinit var tableBindingHolder: TableBindingHolder

    @Autowired
    lateinit var orderIdIndexRepository: CheckouterOrderIdIndexRepository

    @Autowired
    lateinit var checkouterReturnOrderIdxRepository: CheckouterReturnOrderIdxRepository

    @Autowired
    lateinit var returnExecutorsHelper: ReturnExecutorsHelper

    @Autowired
    lateinit var returnLineIdSequence: DataFieldMaxValueIncrementer

    private var checkouterReturnYtConsumer: CheckouterReturnYtConsumer? = null
    private var mockedDao: CheckouterReturnYtDao? = null

    @BeforeEach
    fun init() {
        mockedDao = mock {}
        checkouterReturnYtConsumer = CheckouterReturnYtConsumer(
            orderIdIndexRepository,
            mockedDao!!,
            orderRepository,
            returnRepository,
            returnExecutorsHelper,
            returnLineIdSequence
        )
    }

    @Test
    fun `verify simple import from checkouter data`() {
        val last = returnLineIdSequence.nextLongValue()
        whenever(mockedDao!!.readCheckouterReturnsAsStream(any()))
            .thenReturn(createCheckouterReturnsYtData("checkouter-data-0.json").stream())

        val orders = createOrdersBatch("orders-batch-0.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-0.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        checkouterReturnYtConsumer!!.importCheckouterReturns(CheckouterReturnImportFilter())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                79914680,
                setOf(1223579)
            )

        assertThat(
            checkouterReturnOrderIdxRepository.lookupRow(
                OrderKey(1223579, 79914680)
            )
        ).satisfies { assertThat(it!!.checkouterReturns).hasSize(1) }

        assertThat(returnLines).hasSize(1).satisfies { lines ->
            assertThat(lines.find { it.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(1223579)
                    assertThat(it.key.orderId).isEqualTo(79914680)
                    assertThat(it.key.orderLineId).isEqualTo(138268465)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(1)
                    assertThat(it.logisticReturnId).isNull()
                    assertThat(it.logisticReturnLineStatus).isNull()
                    assertThat(it.logisticReturnLineEventId).isNull()
                    assertThat(it.count).isEqualTo(1)
                }
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 1223579,
                    orderId = 79914680
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.PARTIALLY_RETURNED)

            val line = it.lines.find { line -> line.orderLineId == 138268465L }!!
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(1)
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.RETURN_CREATED.name]).isEqualTo(1)
        }

        assertThat(
            checkouterReturnRepository.lookupRows(
                setOf(
                    CheckouterReturnKey(
                        partnerId = 1223579,
                        checkouterReturnId = 1
                    )
                )
            )
        ).satisfies { lines ->
            assertThat(lines.find { it.key.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)

                    assertThat(it.orderId).isEqualTo(79914680)
                    assertThat(it.fastReturn).isEqualTo(true)
                    assertThat(it.returnLineIds.size).isEqualTo(1)
                }
        }

        assertThat(
            checkouterReturnLineEntityRepository.lookupRows(
                setOf(
                    CheckouterReturnLineKey(
                        returnLineId = last + 1,
                        returnItemId = 1
                    )
                )
            )
        ).satisfies { lines ->
            assertThat(lines.find { it.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)

                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.checkouterReturnId).isEqualTo(1)
                    assertThat(it.reasonType).isEqualTo(CheckouterReturnReasonType.DOES_NOT_FIT)
                }
        }
    }

    @Test
    fun `verify import 2 different returns from checkouter data`() {
        val last = returnLineIdSequence.nextLongValue()
        whenever(mockedDao!!.readCheckouterReturnsAsStream(any()))
            .thenReturn(createCheckouterReturnsYtData("checkouter-data-1.json").stream())

        val orders = createOrdersBatch("orders-batch-1.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-1.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        checkouterReturnYtConsumer!!.importCheckouterReturns(CheckouterReturnImportFilter())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                79914680,
                setOf(1223579)
            )

        assertThat(
            checkouterReturnOrderIdxRepository.lookupRow(
                OrderKey(1223579, 79914680)
            )
        ).satisfies { assertThat(it!!.checkouterReturns).hasSize(2) }

        assertThat(returnLines).hasSize(2).satisfies { lines ->
            assertThat(lines.find { it.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(1223579)
                    assertThat(it.key.orderId).isEqualTo(79914680)
                    assertThat(it.key.orderLineId).isEqualTo(138268465)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(1)
                    assertThat(it.logisticReturnId).isNull()
                    assertThat(it.logisticReturnLineStatus).isNull()
                    assertThat(it.logisticReturnLineEventId).isNull()
                    assertThat(it.count).isEqualTo(1)
                }
            assertThat(lines.find { it.checkouterReturnId == 2L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(1223579)
                    assertThat(it.key.orderId).isEqualTo(79914680)
                    assertThat(it.key.orderLineId).isEqualTo(138268465)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(2)
                    assertThat(it.logisticReturnId).isNull()
                    assertThat(it.logisticReturnLineStatus).isNull()
                    assertThat(it.logisticReturnLineEventId).isNull()
                    assertThat(it.count).isEqualTo(1)
                }
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 1223579,
                    orderId = 79914680
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.RETURNED)

            val line = it.lines.find { line -> line.orderLineId == 138268465L }!!
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.RETURN_CREATED.name]).isEqualTo(2)
        }

        assertThat(
            checkouterReturnRepository.lookupRows(
                setOf(
                    CheckouterReturnKey(
                        partnerId = 1223579,
                        checkouterReturnId = 1
                    )
                )
            )
        ).satisfies { lines ->
            assertThat(lines.find { it.key.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)

                    assertThat(it.orderId).isEqualTo(79914680)
                    assertThat(it.fastReturn).isEqualTo(true)
                    assertThat(it.returnLineIds.size).isEqualTo(1)
                }
        }

        assertThat(
            checkouterReturnLineEntityRepository.lookupRows(
                setOf(
                    CheckouterReturnLineKey(
                        returnLineId = last + 1,
                        returnItemId = 1
                    )
                )
            )
        ).satisfies { lines ->
            assertThat(lines.find { it.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)

                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.checkouterReturnId).isEqualTo(1)
                    assertThat(it.reasonType).isEqualTo(CheckouterReturnReasonType.DOES_NOT_FIT)
                }
        }
    }

    @Test
    fun `verify import 2 same returns from checkouter data`() {
        val last = returnLineIdSequence.nextLongValue()
        whenever(mockedDao!!.readCheckouterReturnsAsStream(any()))
            .thenReturn(createCheckouterReturnsYtData("checkouter-data-2.json").stream())

        val orders = createOrdersBatch("orders-batch-2.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-2.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        checkouterReturnYtConsumer!!.importCheckouterReturns(CheckouterReturnImportFilter())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                79914680,
                setOf(1223579)
            )

        assertThat(
            checkouterReturnOrderIdxRepository.lookupRow(
                OrderKey(1223579, 79914680)
            )
        ).satisfies { assertThat(it!!.checkouterReturns).hasSize(1) }

        assertThat(returnLines).hasSize(1).satisfies { lines ->
            assertThat(lines.find { it.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(1223579)
                    assertThat(it.key.orderId).isEqualTo(79914680)
                    assertThat(it.key.orderLineId).isEqualTo(138268465)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(1)
                    assertThat(it.logisticReturnId).isNull()
                    assertThat(it.logisticReturnLineStatus).isNull()
                    assertThat(it.logisticReturnLineEventId).isNull()
                    assertThat(it.count).isEqualTo(1)
                }
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 1223579,
                    orderId = 79914680
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.PARTIALLY_RETURNED)

            val line = it.lines.find { line -> line.orderLineId == 138268465L }!!
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(1)
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.RETURN_CREATED.name]).isEqualTo(1)
        }

        assertThat(
            checkouterReturnRepository.lookupRows(
                setOf(
                    CheckouterReturnKey(
                        partnerId = 1223579,
                        checkouterReturnId = 1
                    )
                )
            )
        ).satisfies { lines ->
            assertThat(lines.find { it.key.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)

                    assertThat(it.orderId).isEqualTo(79914680)
                    assertThat(it.fastReturn).isEqualTo(true)
                    assertThat(it.returnLineIds.size).isEqualTo(1)
                }
        }

        assertThat(
            checkouterReturnLineEntityRepository.lookupRows(
                setOf(
                    CheckouterReturnLineKey(
                        returnLineId = last + 1,
                        returnItemId = 1
                    )
                )
            )
        ).satisfies { lines ->
            assertThat(lines.find { it.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)

                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.checkouterReturnId).isEqualTo(1)
                    assertThat(it.reasonType).isEqualTo(CheckouterReturnReasonType.WRONG_ITEM)
                }
        }
    }

    @Test
    fun `verify import returns from checkouter data after logistic data`() {
        whenever(mockedDao!!.readCheckouterReturnsAsStream(any()))
            .thenReturn(createCheckouterReturnsYtData("checkouter-data-3.json").stream())

        val orders = createOrdersBatch("orders-batch-3.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-3.json")

        val returnLinesToInsert = createReturnLines("return-line-3.json")
            .map {
                it.copy(
                    ReturnLineKey(
                        it.key.partnerId,
                        it.key.orderId,
                        it.key.orderLineId,
                        returnLineIdSequence.nextLongValue()
                    )
                )
            }

        readWriteClient.execInTransaction {
            it.insertRows(
                tableBindingHolder[ReturnLineEntity::class.java].table,
                tableBindingHolder[ReturnLineEntity::class.java].binder,
                returnLinesToInsert
            )
        }

        readWriteClient.execInTransaction {
            it.insertRows(
                tableBindingHolder[ReturnLineOrderLineIdIndex::class.java].table,
                tableBindingHolder[ReturnLineOrderLineIdIndex::class.java].binder,
                returnLinesToInsert
                    .groupBy { line -> line.key.orderLineId }
                    .map { entry ->
                        ReturnLineOrderLineIdIndex(
                            ReturnLineOrderLineIdIndex.Key(
                                entry.key
                            ),
                            entry.value.map { returnLine -> returnLine.key.returnLineId }.toSet()
                        )
                    }
            )
        }

        readWriteClient.execInTransaction {
            it.insertRows(
                tableBindingHolder[ReturnLineIdIndex::class.java].table,
                tableBindingHolder[ReturnLineIdIndex::class.java].binder,
                returnLinesToInsert.map { line ->
                    ReturnLineIdIndex(
                        ReturnLineIdIndex.Key(
                            line.key.returnLineId
                        ),
                        line.key.partnerId,
                        line.key.orderId,
                        line.key.orderLineId
                    )
                }
            )
        }

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        checkouterReturnYtConsumer!!.importCheckouterReturns(CheckouterReturnImportFilter())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                79914680,
                setOf(1223579)
            )

        assertThat(
            checkouterReturnOrderIdxRepository.lookupRow(
                OrderKey(1223579, 79914680)
            )
        ).satisfies { assertThat(it!!.checkouterReturns).hasSize(2) }

        assertThat(returnLines).hasSize(2).satisfies { lines ->
            assertThat(lines.find { it.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(1223579)
                    assertThat(it.key.orderId).isEqualTo(79914680)
                    assertThat(it.key.orderLineId).isEqualTo(138268465)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(1)
                    assertThat(it.logisticReturnId).isEqualTo(1)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.IN_TRANSIT)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(1)
                    assertThat(it.count).isEqualTo(1)
                }
            assertThat(lines.find { it.checkouterReturnId == 2L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(1223579)
                    assertThat(it.key.orderId).isEqualTo(79914680)
                    assertThat(it.key.orderLineId).isEqualTo(138268465)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(2)
                    assertThat(it.logisticReturnId).isNull()
                    assertThat(it.logisticReturnLineStatus).isNull()
                    assertThat(it.logisticReturnLineEventId).isNull()
                    assertThat(it.count).isEqualTo(1)
                }
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 1223579,
                    orderId = 79914680
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.PARTIALLY_RETURNED)

            val line = it.lines.find { line -> line.orderLineId == 138268465L }!!
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.RETURN_IN_TRANSIT.name]).isEqualTo(1)
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.RETURN_CREATED.name]).isEqualTo(1)
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(3)
        }

        assertThat(
            checkouterReturnRepository.lookupRows(
                setOf(
                    CheckouterReturnKey(
                        partnerId = 1223579,
                        checkouterReturnId = 1
                    )
                )
            )
        ).satisfies { lines ->
            assertThat(lines.find { it.key.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)

                    assertThat(it.orderId).isEqualTo(79914680)
                    assertThat(it.fastReturn).isEqualTo(true)
                    assertThat(it.returnLineIds.size).isEqualTo(1)
                }
        }

        assertThat(
            checkouterReturnLineEntityRepository.lookupRows(
                setOf(
                    CheckouterReturnLineKey(
                        returnLineId = returnLinesToInsert[0].key.returnLineId,
                        returnItemId = 1
                    )
                )
            )
        ).satisfies { lines ->
            assertThat(lines.find { it.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)

                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.checkouterReturnId).isEqualTo(1)
                    assertThat(it.reasonType).isEqualTo(CheckouterReturnReasonType.WRONG_ITEM)
                }
        }
    }

    @Test
    fun `verify simple non-approved import from checkouter data`() {
        val last = returnLineIdSequence.nextLongValue()
        whenever(mockedDao!!.readCheckouterReturnsAsStream(any()))
            .thenReturn(createCheckouterReturnsYtData("checkouter-data-4.json").stream())

        val orders = createOrdersBatch("orders-batch-4.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-4.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        checkouterReturnYtConsumer!!.importCheckouterReturns(CheckouterReturnImportFilter())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                79914680,
                setOf(1223579)
            )

        assertThat(
            checkouterReturnOrderIdxRepository.lookupRow(
                OrderKey(1223579, 79914680)
            )
        ).satisfies { assertThat(it!!.checkouterReturns).hasSize(1) }

        assertThat(returnLines).hasSize(1).satisfies { lines ->
            assertThat(lines.find { it.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(1223579)
                    assertThat(it.key.orderId).isEqualTo(79914680)
                    assertThat(it.key.orderLineId).isEqualTo(138268465)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(1)
                    assertThat(it.logisticReturnId).isNull()
                    assertThat(it.logisticReturnLineStatus).isNull()
                    assertThat(it.logisticReturnLineEventId).isNull()
                    assertThat(it.count).isEqualTo(1)
                }
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 1223579,
                    orderId = 79914680
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.DELIVERED)

            val line = it.lines.find { line -> line.orderLineId == 138268465L }!!
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(5)
        }

        assertThat(
            checkouterReturnRepository.lookupRows(
                setOf(
                    CheckouterReturnKey(
                        partnerId = 1223579,
                        checkouterReturnId = 1
                    )
                )
            )
        ).satisfies { lines ->
            assertThat(lines.find { it.key.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)

                    assertThat(it.orderId).isEqualTo(79914680)
                    assertThat(it.fastReturn).isEqualTo(true)
                    assertThat(it.returnLineIds.size).isEqualTo(1)
                }
        }

        assertThat(
            checkouterReturnLineEntityRepository.lookupRows(
                setOf(
                    CheckouterReturnLineKey(
                        returnLineId = last + 1,
                        returnItemId = 1
                    )
                )
            )
        ).satisfies { lines ->
            assertThat(lines.find { it.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)

                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.checkouterReturnId).isEqualTo(1)
                    assertThat(it.reasonType).isEqualTo(CheckouterReturnReasonType.DOES_NOT_FIT)
                }
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

    private fun createCheckouterReturnsYtData(resourceName: String): List<CheckouterReturnYt> {
        return this::class.loadTestEntities<CheckouterReturnYt>(
            resourceName
        ).sortedBy { it.returnId }
    }

    private fun createReturnLines(resourceName: String): List<ReturnLineEntity> {
        return this::class.loadTestEntities<ReturnLineEntity>(
            resourceName
        ).sortedBy { it.key.returnLineId }
    }
}
