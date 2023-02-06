package ru.yandex.market.mbi.orderservice.tms.service.yt.events

import com.google.protobuf.util.JsonFormat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer
import ru.yandex.market.common.test.util.StringTestUtil
import ru.yandex.market.logistics.logistics4shops.event.model.FulfilmentBoxItemsReceived
import ru.yandex.market.logistics.logistics4shops.event.model.LogisticEvent
import ru.yandex.market.logistics.logistics4shops.event.model.RegularReturnStatusData
import ru.yandex.market.logistics.logistics4shops.event.model.ReturnStatusChangedPayload
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.defaultTestMapper
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.orderservice.common.dsl.Actor
import ru.yandex.market.mbi.orderservice.common.enum.LogisticReturnStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantItemStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderStatus
import ru.yandex.market.mbi.orderservice.common.enum.ReturnType
import ru.yandex.market.mbi.orderservice.common.model.yt.CheckouterEventEntities
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEvent
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEventType
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineKey
import ru.yandex.market.mbi.orderservice.common.model.yt.ProcessedLogisticEventEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.ReturnLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.ReturnLineIdIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.ReturnLineKey
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.ReturnLineOrderLineIdIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.checkouter.CheckouterReturnCreatedAtIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.checkouter.CheckouterReturnEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.checkouter.CheckouterReturnLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.checkouter.CheckouterReturnOrderIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.HistoricalReturnEventEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.InvalidLogisticEventEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.InvalidLogisticEventKey
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.LogisticReturnLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.LogisticReturnLineKey
import ru.yandex.market.mbi.orderservice.common.model.yt.returns.logistic.LogisticReturnManagementEventEntity
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.DLQRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.ProcessedLogisticEventRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtReturnRepository
import ru.yandex.market.mbi.orderservice.common.service.pg.EnvironmentService
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.OrderEventService
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.TableBindingHolder
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import ru.yandex.market.mbi.orderservice.tms.service.logbroker.logistic.events.LogisticEventsMainProcessor
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
        HistoricalReturnEventEntity::class,
        ProcessedLogisticEventEntity::class
    ]
)
class LogisticReturnEventsProcessorTest : FunctionalTest() {

    @Autowired
    lateinit var environmentService: EnvironmentService

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var returnRepository: YtReturnRepository

    @Autowired
    lateinit var processedLogisticEventRepository: ProcessedLogisticEventRepository

    @Autowired
    lateinit var logisticDlqRepository: DLQRepository<InvalidLogisticEventKey, InvalidLogisticEventEntity>

    @Autowired
    lateinit var readWriteClient: YtClientProxy

    @Autowired
    lateinit var tableBindingHolder: TableBindingHolder

    @Autowired
    lateinit var logisticReturnEventsProcessor: LogisticReturnEventsProcessor

    @Autowired
    lateinit var logisticEventsMainProcessor: LogisticEventsMainProcessor

    @Autowired
    lateinit var orderEventService: OrderEventService

    @Autowired
    lateinit var returnLineIdSequence: DataFieldMaxValueIncrementer

    @BeforeEach
    fun `init`() {
        environmentService.set(LRM_IMPORT_INVALID_EVENTS_ENABLED, "true")
        environmentService.set(LRM_IMPORT_ENABLED, "true")
        environmentService.set(LOGISTIC_IMPORT_IGNORE_RETURNS, "0")
        environmentService.set(ROUTE_INVALID_EVENTS_TO_DLQ, "true")
    }

    @Test
    fun `verify consistent enums`() {
        ReturnStatusChangedPayload.ReturnStatus.values()
            .filter { it != ReturnStatusChangedPayload.ReturnStatus.UNRECOGNIZED }
            .forEach { logisticReturnEventsProcessor.mapStatus(it) }

        ReturnStatusChangedPayload.ReturnSource.values()
            .filter { it != ReturnStatusChangedPayload.ReturnSource.UNRECOGNIZED }
            .forEach { logisticReturnEventsProcessor.mapReturnSource(it) }

        FulfilmentBoxItemsReceived.BoxItem.UnitCountType.values()
            .filter { it != FulfilmentBoxItemsReceived.BoxItem.UnitCountType.UNRECOGNIZED }
            .forEach { logisticReturnEventsProcessor.mapStock(it) }

        RegularReturnStatusData.Box.RecipientType.values()
            .filter { it != RegularReturnStatusData.Box.RecipientType.UNRECOGNIZED }
            .forEach { logisticReturnEventsProcessor.mapRecipientType(it) }
    }

    @Test
    fun `verify that return line has not been inserted (0 events)`() {
        val returnEvents = createLogisticReturnEventsBatch("return-events-batch-0.json")

        val orders = createOrdersBatch("orders-batch-1.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-1.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        logisticEventsMainProcessor.processLogisticEvents(returnEvents.asSequence())

        assertThat(
            returnRepository.findReturnLinesByOrder(
                79914680,
                setOf(1223579)
            )
        ).hasSize(0)
    }

    @Test
    fun `verify that return line has been inserted`() {
        val last = returnLineIdSequence.nextLongValue()

        val returnEvents = createLogisticReturnEventsBatch("return-events-batch-1.json")
        val orders = createOrdersBatch("orders-batch-1.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-1.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        logisticEventsMainProcessor.processLogisticEvents(returnEvents.asSequence())

        assertThat(
            returnRepository.findReturnLinesByOrder(
                79914680,
                setOf(1223579)
            )
        ).hasSize(1).satisfies { lines ->
            assertThat(lines.find { it.key.orderLineId == 138268465L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(1223579)
                    assertThat(it.key.orderId).isEqualTo(79914680)
                    assertThat(it.key.orderLineId).isEqualTo(138268465)
                    assertThat(it.key.returnLineId).isEqualTo(last + 1)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(6)
                    assertThat(it.logisticReturnId).isEqualTo(5)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.RECEIVED)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(1)
                    assertThat(it.count).isEqualTo(1)
                }
        }

        assertThat(
            returnRepository.findLogisticReturnLinesByKeys(
                setOf(
                    LogisticReturnLineKey(
                        returnLineId = last + 1,
                        eventId = 1
                    )
                )
            )
        ).hasSize(1).satisfies { lines ->
            assertThat(lines.find { it.key.returnLineId == last + 1 && it.key.eventId == 1L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.returnLineId).isEqualTo(last + 1)
                    assertThat(it.key.eventId).isEqualTo(1)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.RECEIVED)
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
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isNull()
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.RETURN_RECEIVED.name]).isEqualTo(1)
        }
    }

    @Test
    fun `verify that return lines has been inserted for several sskus and partners`() {
        val last = returnLineIdSequence.nextLongValue()

        val returnEvents = createLogisticReturnEventsBatch("return-events-batch-2.json")

        val orders = createOrdersBatch("orders-batch-2.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-2.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        logisticEventsMainProcessor.processLogisticEvents(returnEvents.asSequence())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                81545127,
                setOf(543900, 472311, 546711)
            )

        assertThat(returnLines).hasSize(4).satisfies { lines ->
            assertThat(lines.find { it.key.orderLineId == 140430843L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(546711)
                    assertThat(it.key.orderId).isEqualTo(81545127)
                    assertThat(it.key.orderLineId).isEqualTo(140430843)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(6)
                    assertThat(it.logisticReturnId).isEqualTo(5)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.RECEIVED)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(1)
                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.boxes.map { box -> box.boxExternalId }).containsAll(listOf("VOZ_FBS_1771685"))
                }
            assertThat(lines.find { it.key.orderLineId == 140430844L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(543900)
                    assertThat(it.key.orderId).isEqualTo(81545127)
                    assertThat(it.key.orderLineId).isEqualTo(140430844)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(6)
                    assertThat(it.logisticReturnId).isEqualTo(5)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.RECEIVED)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(1)
                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.boxes.map { box -> box.boxExternalId }).containsAll(listOf("VOZ_FBS_1771685"))
                }
            assertThat(lines.find { it.key.orderLineId == 140430845L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(472311)
                    assertThat(it.key.orderId).isEqualTo(81545127)
                    assertThat(it.key.orderLineId).isEqualTo(140430845)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(6)
                    assertThat(it.logisticReturnId).isEqualTo(5)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.RECEIVED)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(1)
                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.boxes.map { box -> box.boxExternalId }).containsAll(listOf("VOZ_FBS_1771685"))
                }
            assertThat(lines.find { it.key.orderLineId == 140430846L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(472311)
                    assertThat(it.key.orderId).isEqualTo(81545127)
                    assertThat(it.key.orderLineId).isEqualTo(140430846)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(6)
                    assertThat(it.logisticReturnId).isEqualTo(5)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.RECEIVED)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(1)
                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.boxes.map { box -> box.boxExternalId }).containsAll(listOf("VOZ_FBS_1771685"))
                }
        }

        assertThat(
            returnRepository.findLogisticReturnLinesByKeys(
                setOf(
                    LogisticReturnLineKey(
                        returnLineId = last + 1,
                        eventId = 1
                    ),
                    LogisticReturnLineKey(
                        returnLineId = last + 2,
                        eventId = 1
                    ),
                    LogisticReturnLineKey(
                        returnLineId = last + 3,
                        eventId = 1
                    ),
                    LogisticReturnLineKey(
                        returnLineId = last + 4,
                        eventId = 1
                    )
                )
            )
        ).hasSize(4).satisfies { lines ->
            assertThat(lines)
                .allSatisfy {
                    require(it != null)
                    assertThat(it.key.eventId).isEqualTo(1)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.RECEIVED)
                }
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 543900,
                    orderId = 81545127
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.PARTIALLY_RETURNED)

            val line = it.lines.find { line -> line.orderLineId == 140430844L }!!
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(2)
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.RETURN_RECEIVED.name]).isEqualTo(1)
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 472311,
                    orderId = 81545127
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.PARTIALLY_RETURNED)

            val line1 = it.lines.find { line -> line.orderLineId == 140430846L }!!
            assertThat(line1.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(2)
            assertThat(line1.itemStatuses.statuses[MerchantItemStatus.RETURN_RECEIVED.name]).isEqualTo(1)

            val line2 = it.lines.find { line -> line.orderLineId == 140430845L }!!
            assertThat(line2.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(3)
            assertThat(line2.itemStatuses.statuses[MerchantItemStatus.RETURN_RECEIVED.name]).isEqualTo(1)
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 546711,
                    orderId = 81545127
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.RETURNED)

            val line = it.lines.find { line -> line.orderLineId == 140430843L }!!
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.CANCELLED.name]).isEqualTo(1)
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.RETURN_RECEIVED.name]).isEqualTo(1)
        }

        assertThat(
            orderEventService.findEventsByOrderKey(
                OrderKey(
                    partnerId = 546711,
                    orderId = 81545127
                )
            )
        ).filteredOn { it.status == MerchantOrderStatus.RETURNED }.hasSize(1).allSatisfy {
            require(it != null)

            assertThat(it.key.orderId).isEqualTo(81545127)
            assertThat(it.actor).isEqualTo(Actor.OTHER)
            assertThat(it.type).isEqualTo(OrderEventType.STATUS_CHANGE)
        }
    }

    @Test
    fun `verify that return lines has been inserted for several events`() {
        val last = returnLineIdSequence.nextLongValue()

        val returnEvents = createLogisticReturnEventsBatch("return-events-batch-3.json")
        val orders = createOrdersBatch("orders-batch-2.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-2.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        logisticEventsMainProcessor.processLogisticEvents(returnEvents.asSequence())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                81545127,
                setOf(543900, 472311, 546711)
            )

        assertThat(returnLines).hasSize(4).satisfies { lines ->
            assertThat(lines.find { it.key.orderLineId == 140430843L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(546711)
                    assertThat(it.key.orderId).isEqualTo(81545127)
                    assertThat(it.key.orderLineId).isEqualTo(140430843)
                    assertThat(it.returnType).isEqualTo(ReturnType.UNREDEEMED)
                    assertThat(it.checkouterReturnId).isNull()
                    assertThat(it.logisticReturnId).isEqualTo(5)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.IN_TRANSIT)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(2)
                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.boxes.map { box -> box.boxExternalId }).containsAll(listOf("VOZ_FBS_1771685"))
                }
            assertThat(lines.find { it.key.orderLineId == 140430844L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(543900)
                    assertThat(it.key.orderId).isEqualTo(81545127)
                    assertThat(it.key.orderLineId).isEqualTo(140430844)
                    assertThat(it.returnType).isEqualTo(ReturnType.UNREDEEMED)
                    assertThat(it.checkouterReturnId).isNull()
                    assertThat(it.logisticReturnId).isEqualTo(5)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.IN_TRANSIT)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(2)
                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.boxes.map { box -> box.boxExternalId }).containsAll(listOf("VOZ_FBS_1771685"))
                }
            assertThat(lines.find { it.key.orderLineId == 140430845L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(472311)
                    assertThat(it.key.orderId).isEqualTo(81545127)
                    assertThat(it.key.orderLineId).isEqualTo(140430845)
                    assertThat(it.returnType).isEqualTo(ReturnType.UNREDEEMED)
                    assertThat(it.checkouterReturnId).isNull()
                    assertThat(it.logisticReturnId).isEqualTo(5)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.IN_TRANSIT)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(2)
                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.boxes.map { box -> box.boxExternalId }).containsAll(listOf("VOZ_FBS_1771685"))
                }
            assertThat(lines.find { it.key.orderLineId == 140430846L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(472311)
                    assertThat(it.key.orderId).isEqualTo(81545127)
                    assertThat(it.key.orderLineId).isEqualTo(140430846)
                    assertThat(it.returnType).isEqualTo(ReturnType.UNREDEEMED)
                    assertThat(it.checkouterReturnId).isNull()
                    assertThat(it.logisticReturnId).isEqualTo(5)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.IN_TRANSIT)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(2)
                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.boxes.map { box -> box.boxExternalId }).containsAll(listOf("VOZ_FBS_1771685"))
                }
        }

        assertThat(
            returnRepository.findLogisticReturnLinesByKeys(
                setOf(
                    LogisticReturnLineKey(
                        returnLineId = last + 1,
                        eventId = 1
                    ),
                    LogisticReturnLineKey(
                        returnLineId = last + 2,
                        eventId = 1
                    ),
                    LogisticReturnLineKey(
                        returnLineId = last + 3,
                        eventId = 1
                    ),
                    LogisticReturnLineKey(
                        returnLineId = last + 4,
                        eventId = 1
                    )
                )
            )
        ).hasSize(4).satisfies { lines ->
            assertThat(lines)
                .allSatisfy {
                    require(it != null)
                    assertThat(it.key.eventId).isEqualTo(1)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.CREATED)
                }
        }

        assertThat(
            returnRepository.findLogisticReturnLinesByKeys(
                setOf(
                    LogisticReturnLineKey(
                        returnLineId = last + 1,
                        eventId = 2
                    ),
                    LogisticReturnLineKey(
                        returnLineId = last + 2,
                        eventId = 2
                    ),
                    LogisticReturnLineKey(
                        returnLineId = last + 3,
                        eventId = 2
                    ),
                    LogisticReturnLineKey(
                        returnLineId = last + 4,
                        eventId = 2
                    )
                )
            )
        ).hasSize(4).satisfies { lines ->
            assertThat(lines)
                .allSatisfy {
                    require(it != null)
                    assertThat(it.key.eventId).isEqualTo(2)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.IN_TRANSIT)
                }
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 472311,
                    orderId = 81545127
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.PARTIALLY_DELIVERED)

            val line1 = it.lines.find { line -> line.orderLineId == 140430846L }!!
            assertThat(line1.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(3)
            assertThat(line1.itemStatuses.statuses[MerchantItemStatus.UNREDEEMED_IN_TRANSIT.name]).isEqualTo(1)

            val line2 = it.lines.find { line -> line.orderLineId == 140430845L }!!
            assertThat(line2.itemStatuses.statuses[MerchantItemStatus.UNREDEEMED_IN_TRANSIT.name]).isEqualTo(1)
            assertThat(line2.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(4)
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 543900,
                    orderId = 81545127
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.PARTIALLY_DELIVERED)

            val line1 = it.lines.find { line -> line.orderLineId == 140430844L }!!
            assertThat(line1.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(3)
            assertThat(line1.itemStatuses.statuses[MerchantItemStatus.UNREDEEMED_IN_TRANSIT.name]).isEqualTo(1)
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 546711,
                    orderId = 81545127
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.PARTIALLY_DELIVERED)

            val line1 = it.lines.find { line -> line.orderLineId == 140430843L }!!
            assertThat(line1.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(1)
            assertThat(line1.itemStatuses.statuses[MerchantItemStatus.UNREDEEMED_IN_TRANSIT.name]).isEqualTo(1)
        }

        assertThat(
            orderEventService.findEventsByOrderKey(
                OrderKey(
                    partnerId = 546711,
                    orderId = 81545127
                )
            )
        ).filteredOn { it.status == MerchantOrderStatus.PARTIALLY_DELIVERED }.hasSize(1).allSatisfy {
            require(it != null)

            assertThat(it.key.orderId).isEqualTo(81545127)
            assertThat(it.actor).isEqualTo(Actor.OTHER)
            assertThat(it.type).isEqualTo(OrderEventType.STATUS_CHANGE)
        }
    }

    @Test
    fun `verify update for partially unredeemed in partially by partner`() {
        val last = returnLineIdSequence.nextLongValue()

        val returnEvents = createLogisticReturnEventsBatch("return-events-batch-4.json")

        val orders = createOrdersBatch("orders-batch-3.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-3.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        logisticEventsMainProcessor.processLogisticEvents(returnEvents.asSequence())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                81545127,
                setOf(543900, 472311, 546711)
            )

        assertThat(returnLines).hasSize(1).satisfies { lines ->
            assertThat(lines.find { it.key.orderLineId == 140430844L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(543900)
                    assertThat(it.key.orderId).isEqualTo(81545127)
                    assertThat(it.key.orderLineId).isEqualTo(140430844)
                    assertThat(it.returnType).isEqualTo(ReturnType.UNREDEEMED)
                    assertThat(it.checkouterReturnId).isNull()
                    assertThat(it.logisticReturnId).isEqualTo(5)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.RECEIVED)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(1)
                    assertThat(it.count).isEqualTo(1)
                }
        }

        assertThat(
            returnRepository.findLogisticReturnLinesByKeys(
                setOf(
                    LogisticReturnLineKey(
                        returnLineId = last + 1,
                        eventId = 1
                    )
                )
            )
        ).hasSize(1).satisfies { lines ->
            assertThat(lines)
                .allSatisfy {
                    require(it != null)
                    assertThat(it.key.eventId).isEqualTo(1)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.RECEIVED)
                }
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 543900,
                    orderId = 81545127
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.PARTIALLY_DELIVERED)

            val line = it.lines.find { line -> line.orderLineId == 140430844L }!!
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(2)
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.UNREDEEMED_RECEIVED.name]).isEqualTo(1)
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 472311,
                    orderId = 81545127
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.DELIVERED)

            val line1 = it.lines.find { line -> line.orderLineId == 140430846L }!!
            assertThat(line1.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(3)
        }

        assertThat(
            orderEventService.findEventsByOrderKey(
                OrderKey(
                    partnerId = 543900,
                    orderId = 81545127
                )
            )
        ).filteredOn { it.status == MerchantOrderStatus.PARTIALLY_DELIVERED }.hasSize(1).allSatisfy {
            require(it != null)

            assertThat(it.key.orderId).isEqualTo(81545127)
            assertThat(it.actor).isEqualTo(Actor.OTHER)
            assertThat(it.type).isEqualTo(OrderEventType.STATUS_CHANGE)
        }
    }

    @Test
    fun `verify update for full unredeemed in partially by partner`() {
        val last = returnLineIdSequence.nextLongValue()

        val returnEvents = createLogisticReturnEventsBatch("return-events-batch-4.json")

        val orders = createOrdersBatch("orders-batch-4.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-4.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        logisticEventsMainProcessor.processLogisticEvents(returnEvents.asSequence())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                81545127,
                setOf(543900, 472311, 546711)
            )

        assertThat(returnLines).hasSize(1).satisfies { lines ->
            assertThat(lines.find { it.key.orderLineId == 140430844L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(543900)
                    assertThat(it.key.orderId).isEqualTo(81545127)
                    assertThat(it.key.orderLineId).isEqualTo(140430844)
                    assertThat(it.returnType).isEqualTo(ReturnType.UNREDEEMED)
                    assertThat(it.checkouterReturnId).isNull()
                    assertThat(it.logisticReturnId).isEqualTo(5)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.RECEIVED)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(1)
                    assertThat(it.count).isEqualTo(1)
                }
        }

        assertThat(
            returnRepository.findLogisticReturnLinesByKeys(
                setOf(
                    LogisticReturnLineKey(
                        returnLineId = last + 1,
                        eventId = 1
                    )
                )
            )
        ).hasSize(1).satisfies { lines ->
            assertThat(lines)
                .allSatisfy {
                    require(it != null)
                    assertThat(it.key.eventId).isEqualTo(1)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.RECEIVED)
                }
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 543900,
                    orderId = 81545127
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.CANCELLED_IN_DELIVERY)

            val line = it.lines.find { line -> line.orderLineId == 140430844L }!!
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.UNREDEEMED_RECEIVED.name]).isEqualTo(1)
            assertThat(line.itemStatuses.statuses.size).isEqualTo(1)
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 472311,
                    orderId = 81545127
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.DELIVERED)

            val line1 = it.lines.find { line -> line.orderLineId == 140430846L }!!
            assertThat(line1.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(1)
            assertThat(line1.itemStatuses.statuses.size).isEqualTo(1)
        }

        assertThat(
            orderEventService.findEventsByOrderKey(
                OrderKey(
                    partnerId = 543900,
                    orderId = 81545127
                )
            )
        ).filteredOn { it.status == MerchantOrderStatus.CANCELLED_IN_DELIVERY }.hasSize(1).allSatisfy {
            require(it != null)

            assertThat(it.key.orderId).isEqualTo(81545127)
            assertThat(it.actor).isEqualTo(Actor.OTHER)
            assertThat(it.type).isEqualTo(OrderEventType.STATUS_CHANGE)
        }
    }

    @Test
    fun `verify update for return to partially return`() {
        val last = returnLineIdSequence.nextLongValue()

        val returnEvents = createLogisticReturnEventsBatch("return-events-batch-5.json")
        val orders = createOrdersBatch("orders-batch-5.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-5.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        logisticEventsMainProcessor.processLogisticEvents(returnEvents.asSequence())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                81545127,
                setOf(543900)
            )

        assertThat(returnLines).hasSize(1).satisfies { lines ->
            assertThat(lines.find { it.key.orderLineId == 140430844L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(543900)
                    assertThat(it.key.orderId).isEqualTo(81545127)
                    assertThat(it.key.orderLineId).isEqualTo(140430844)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(1)
                    assertThat(it.logisticReturnId).isEqualTo(5)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.IN_TRANSIT)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(2)
                    assertThat(it.count).isEqualTo(1)
                }
        }

        assertThat(
            returnRepository.findLogisticReturnLinesByKeys(
                setOf(
                    LogisticReturnLineKey(
                        returnLineId = last + 1,
                        eventId = 1
                    )
                )
            )
        ).hasSize(1).satisfies { lines ->
            assertThat(lines)
                .allSatisfy {
                    require(it != null)
                    assertThat(it.key.eventId).isEqualTo(1)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.RECEIVED)
                }
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 543900,
                    orderId = 81545127
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.PARTIALLY_RETURNED)

            val line = it.lines.find { line -> line.orderLineId == 140430844L }!!
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(1)
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.RETURN_IN_TRANSIT.name]).isEqualTo(1)
        }

        assertThat(
            orderEventService.findEventsByOrderKey(
                OrderKey(
                    partnerId = 543900,
                    orderId = 81545127
                )
            )
        ).filteredOn { it.status == MerchantOrderStatus.RETURNED }.hasSize(1).allSatisfy {
            require(it != null)

            assertThat(it.key.orderId).isEqualTo(81545127)
            assertThat(it.actor).isEqualTo(Actor.OTHER)
            assertThat(it.type).isEqualTo(OrderEventType.STATUS_CHANGE)
        }
    }

    @Test
    fun `verify update after checkouter data`() {
        val returnEvents = createLogisticReturnEventsBatch("return-events-batch-6.json")

        val orders = createOrdersBatch("orders-batch-6.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-6.json")

        val returnLinesToInsert = createReturnLines("checkouter-lines-6.json")
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

        logisticEventsMainProcessor.processLogisticEvents(returnEvents.asSequence())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                81545127,
                setOf(543900)
            )

        assertThat(returnLines).hasSize(2).satisfies { lines ->
            assertThat(lines.find { it.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(543900)
                    assertThat(it.key.orderId).isEqualTo(81545127)
                    assertThat(it.key.orderLineId).isEqualTo(140430844)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(1)
                    assertThat(it.logisticReturnId).isEqualTo(5)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.IN_TRANSIT)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(2)
                    assertThat(it.count).isEqualTo(1)
                }
            assertThat(lines.find { it.checkouterReturnId == 2L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(543900)
                    assertThat(it.key.orderId).isEqualTo(81545127)
                    assertThat(it.key.orderLineId).isEqualTo(140430844)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(2)
                    assertThat(it.logisticReturnId).isNull()
                    assertThat(it.logisticReturnLineStatus).isNull()
                    assertThat(it.logisticReturnLineEventId).isNull()
                    assertThat(it.count).isEqualTo(1)
                }
        }

        assertThat(
            returnRepository.findLogisticReturnLinesByKeys(
                setOf(
                    LogisticReturnLineKey(
                        returnLineId = returnLinesToInsert[0].key.returnLineId,
                        eventId = 1
                    )
                )
            )
        ).hasSize(1).satisfies { lines ->
            assertThat(lines)
                .allSatisfy {
                    require(it != null)
                    assertThat(it.key.eventId).isEqualTo(1)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.RECEIVED)
                }
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 543900,
                    orderId = 81545127
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.RETURNED)

            val line = it.lines.find { line -> line.orderLineId == 140430844L }!!
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.RETURN_CREATED.name]).isEqualTo(1)
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.RETURN_IN_TRANSIT.name]).isEqualTo(1)
        }

        assertThat(
            orderEventService.findEventsByOrderKey(
                OrderKey(
                    partnerId = 543900,
                    orderId = 81545127
                )
            )
        ).filteredOn { it.status == MerchantOrderStatus.RETURNED }.hasSize(1).allSatisfy {
            require(it != null)

            assertThat(it.key.orderId).isEqualTo(81545127)
            assertThat(it.actor).isEqualTo(Actor.OTHER)
            assertThat(it.type).isEqualTo(OrderEventType.STATUS_CHANGE)
        }
    }

    @Test
    fun `verify that non-approved return lines not change order and items`() {
        val last = returnLineIdSequence.nextLongValue()

        val returnEvents = createLogisticReturnEventsBatch("return-events-batch-7.json")

        val orders = createOrdersBatch("orders-batch-7.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-7.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        logisticEventsMainProcessor.processLogisticEvents(returnEvents.asSequence())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                81545127,
                setOf(543900)
            )

        assertThat(returnLines).hasSize(1).satisfies { lines ->
            assertThat(lines.find { it.checkouterReturnId == 1L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.key.partnerId).isEqualTo(543900)
                    assertThat(it.key.orderId).isEqualTo(81545127)
                    assertThat(it.key.orderLineId).isEqualTo(140430844)
                    assertThat(it.returnType).isEqualTo(ReturnType.RETURN)
                    assertThat(it.checkouterReturnId).isEqualTo(1)
                    assertThat(it.logisticReturnId).isEqualTo(5)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.CREATED)
                    assertThat(it.logisticReturnLineEventId).isEqualTo(2)
                    assertThat(it.count).isEqualTo(1)
                }
        }

        assertThat(
            returnRepository.findLogisticReturnLinesByKeys(
                setOf(
                    LogisticReturnLineKey(
                        returnLineId = last + 1,
                        eventId = 1
                    )
                )
            )
        ).hasSize(1).satisfies { lines ->
            assertThat(lines)
                .allSatisfy {
                    require(it != null)
                    assertThat(it.key.eventId).isEqualTo(1)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.CREATED)
                }
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 543900,
                    orderId = 81545127
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.DELIVERED)

            val line = it.lines.find { line -> line.orderLineId == 140430844L }!!
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.DELIVERED_TO_BUYER.name]).isEqualTo(2)
        }

        assertThat(
            orderEventService.findEventsByOrderKey(
                OrderKey(
                    partnerId = 543900,
                    orderId = 81545127
                )
            )
        ).hasSize(0)
    }

    @Test
    fun `verify that invalid events stored to special table`() {
        val returnEvents = createLogisticReturnEventsBatch("return-events-batch-8.json")

        val orders = createOrdersBatch("orders-batch-8.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-8.json")


        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        logisticEventsMainProcessor.processLogisticEvents(returnEvents.asSequence())

        val events = logisticDlqRepository.getInvalidEvents()
        assertThat(events.size).isEqualTo(2)
    }

    @Test
    fun `verify that return import corrected`() {
        val last = returnLineIdSequence.nextLongValue()

        val returnEvents = createLogisticReturnEventsBatch("return-events-batch-9.json")
        val orders = createOrdersBatch("orders-batch-9.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-9.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        logisticEventsMainProcessor.processLogisticEvents(returnEvents.asSequence())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                92166654,
                setOf(1736487)
            )

        assertThat(returnLines).hasSize(1).satisfies { lines ->
            assertThat(lines.find { it.key.orderLineId == 154218359L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.count).isEqualTo(1)
                }
        }

        assertThat(
            returnRepository.findLogisticReturnLinesByKeys(
                setOf(
                    LogisticReturnLineKey(
                        returnLineId = last + 1,
                        eventId = 92639
                    )
                )
            )
        ).hasSize(1).satisfies { lines ->
            assertThat(lines)
                .allSatisfy {
                    require(it != null)
                    assertThat(it.key.eventId).isEqualTo(92639)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.RECEIVED)
                }
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 1736487,
                    orderId = 92166654
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.CANCELLED_IN_DELIVERY)

            val line = it.lines.find { line -> line.orderLineId == 154218359L }!!
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.UNREDEEMED_RECEIVED.name]).isEqualTo(1)
        }
    }

    @Test
    fun `verify simple fashion create case`() {
        val last = returnLineIdSequence.nextLongValue()

        val returnEvents = createLogisticReturnEventsBatch("return-events-batch-10.json")
        val orders = createOrdersBatch("orders-batch-10.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-10.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        logisticEventsMainProcessor.processLogisticEvents(returnEvents.asSequence())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                93967965,
                setOf(1745788)
            )

        assertThat(returnLines).hasSize(1).satisfies { lines ->
            assertThat(lines.find { it.key.orderLineId == 156545493L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.count).isEqualTo(1)
                }
        }

        assertThat(
            returnRepository.findLogisticReturnLinesByKeys(
                setOf(
                    LogisticReturnLineKey(
                        returnLineId = last + 1,
                        eventId = 109188
                    )
                )
            )
        ).hasSize(1).satisfies { lines ->
            assertThat(lines)
                .allSatisfy {
                    require(it != null)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.CREATED)
                }
        }

        assertThat(
            orderRepository.findOrderWithLinesByKey(
                OrderKey(
                    partnerId = 1745788,
                    orderId = 93967965
                )
            )
        ).satisfies {
            require(it != null)

            assertThat(it.order.status).isEqualTo(MerchantOrderStatus.CANCELLED_IN_DELIVERY)

            val line = it.lines.find { line -> line.orderLineId == 156545493L }!!
            assertThat(line.itemStatuses.statuses[MerchantItemStatus.DELETED.name]).isEqualTo(1)
        }
    }

    @Test
    fun `verify events breaking order fixing`() {
        val last = returnLineIdSequence.nextLongValue()

        val returnEvents = createLogisticReturnEventsBatch("return-events-batch-11.json")
        val orders = createOrdersBatch("orders-batch-11.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-11.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        logisticEventsMainProcessor.processLogisticEvents(returnEvents.asSequence())

        val returnLines =
            returnRepository.findReturnLinesByOrder(
                93967965,
                setOf(1745788)
            )

        assertThat(returnLines).hasSize(1).satisfies { lines ->
            assertThat(lines.find { it.key.orderLineId == 156545493L }).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.PICKED)
                }
        }

        assertThat(
            returnRepository.findLogisticReturnLinesByKeys(
                setOf(
                    LogisticReturnLineKey(
                        returnLineId = last + 1,
                        eventId = 2
                    )
                )
            )
        ).hasSize(1).satisfies { lines ->
            assertThat(lines)
                .allSatisfy {
                    require(it != null)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.PICKED)
                }
        }
    }

    @Test
    fun `verify unredeemed duplication`() {
        val last = returnLineIdSequence.nextLongValue()

        val returnEvents = createLogisticReturnEventsBatch("return-events-batch-12.json")
        val orders = createOrdersBatch("orders-batch-12.json")
        val orderLines = createOrderLinesBatch("order-lines-batch-12.json")

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )

        logisticEventsMainProcessor.processLogisticEvents(sequenceOf(returnEvents[0]))

        var returnLines =
            returnRepository.findReturnLinesByOrder(
                109997848,
                setOf(1365595)
            )

        assertThat(returnLines).hasSize(1).satisfies { lines ->
            assertThat(lines.firstOrNull()).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.IN_TRANSIT)
                    assertThat(it.approved).isTrue()
                }
        }

        assertThat(
            orderRepository.orderLineEntityRepository.lookupRows(
                setOf(
                    OrderLineKey(
                        1365595,
                        109997848,
                        176187275
                    )
                )
            )
        ).hasSize(1).satisfies {
            assertThat(it[0]).satisfies { line ->
                assertThat(line.itemStatuses!!.statuses[MerchantItemStatus.UNREDEEMED_IN_TRANSIT.name]).isEqualTo(1)
            }
        }

        orderRepository.storeNewEventEntities(
            CheckouterEventEntities(
                orders,
                orderLines,
                logisticsEntities = emptyList()
            )
        )


        logisticEventsMainProcessor.processLogisticEvents(sequenceOf(returnEvents[1]))

        returnLines =
            returnRepository.findReturnLinesByOrder(
                109997848,
                setOf(1365595)
            )

        assertThat(returnLines).hasSize(1).satisfies { lines ->
            assertThat(lines.firstOrNull()).isNotNull
                .satisfies {
                    require(it != null)
                    assertThat(it.count).isEqualTo(1)
                    assertThat(it.logisticReturnLineStatus).isEqualTo(LogisticReturnStatus.IN_TRANSIT)
                    assertThat(it.approved).isTrue()
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

    private fun createLogisticReturnEventsBatch(resourceName: String): List<LogisticEvent> {
        val s = StringTestUtil.getString(this.javaClass, resourceName)
        val tree = defaultTestMapper.readTree(s)
        return tree.asSequence()
            .map { defaultTestMapper.writeValueAsString(it) }
            .map {
                val builder = ReturnStatusChangedPayload.newBuilder()
                JsonFormat.parser().merge(it, builder)
                LogisticEvent
                    .newBuilder()
                    .setId(builder.returnEventId)
                    .setReturnStatusChangedPayload(builder.build())
                    .build()
            }.toList()
    }

    private fun createReturnLines(resourceName: String): List<ReturnLineEntity> {
        return this::class.loadTestEntities<ReturnLineEntity>(
            resourceName
        ).sortedBy { it.key.returnLineId }
    }
}
