package ru.yandex.market.mbi.orderservice.tms.service.logbroker.events

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import ru.yandex.kikimr.persqueue.compression.CompressionCodec
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData
import ru.yandex.market.checkout.checkouter.client.ClientRole
import ru.yandex.market.checkout.checkouter.event.HistoryEventReason
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent
import ru.yandex.market.checkout.checkouter.order.OrderItemInstance
import ru.yandex.market.checkout.checkouter.order.OrderItemInstances
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequest
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestStatus
import ru.yandex.market.checkout.checkouter.order.changerequest.ChangeRequestType
import ru.yandex.market.checkout.checkouter.order.changerequest.itemsremoval.ItemsRemovalChangeRequestPayload
import ru.yandex.market.common.test.db.DbUnitDataSet
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.helpers.loadResourceAsString
import ru.yandex.market.mbi.helpers.loadTestEntities
import ru.yandex.market.mbi.helpers.loadTestEntity
import ru.yandex.market.mbi.orderservice.common.dsl.Actor
import ru.yandex.market.mbi.orderservice.common.enum.MerchantItemStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderSubstatus
import ru.yandex.market.mbi.orderservice.common.enum.OrderSourcePlatform
import ru.yandex.market.mbi.orderservice.common.model.dto.ItemsChangedEventDetails
import ru.yandex.market.mbi.orderservice.common.model.dto.ItemsRemovedDto
import ru.yandex.market.mbi.orderservice.common.model.yt.CancellationRequest
import ru.yandex.market.mbi.orderservice.common.model.yt.CheckouterOrderIdIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.CreatedAtIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.EventSource
import ru.yandex.market.mbi.orderservice.common.model.yt.ItemStatuses
import ru.yandex.market.mbi.orderservice.common.model.yt.ItemsRemovalRequestPayload
import ru.yandex.market.mbi.orderservice.common.model.yt.MerchantOrderIdIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEditRequestEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEditRequestKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEvent
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEventKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEventType
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLineKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderLogisticsEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.OutboundOrderEventEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.ProcessedCheckouterEventEntity
import ru.yandex.market.mbi.orderservice.common.model.yt.PromoIndex
import ru.yandex.market.mbi.orderservice.common.model.yt.UpdatedItem
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.YtOrderRepository
import ru.yandex.market.mbi.orderservice.common.service.external.CheckouterApiService
import ru.yandex.market.mbi.orderservice.common.service.pg.EnvironmentService
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.OrderEditRequestService
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.OrderEventService
import ru.yandex.market.mbi.orderservice.common.util.toJsonString
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent.PayloadCase.MERCHANT_ORDER_SUBSTATUS_CHANGED_PAYLOAD
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent.PayloadCase.ORDER_ITEMS_REMOVED_PAYLOAD
import ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent.PayloadCase.ORDER_PARCEL_BOXES_CHANGED_PAYLOAD
import ru.yandex.market.mbi.orderservice.proto.event.model.Status
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.Instant

@DbUnitDataSet
@CleanupTables(
    [
        OrderEntity::class, OrderLineEntity::class, OrderLogisticsEntity::class,
        CreatedAtIndex::class, CheckouterOrderIdIndex::class,
        ProcessedCheckouterEventEntity::class,
        OrderEditRequestEntity::class, MerchantOrderIdIndex::class,
        OrderEvent::class, OutboundOrderEventEntity::class]
)
@ExperimentalCoroutinesApi
class CheckouterEventProcessorTest : FunctionalTest() {

    @Autowired
    @Qualifier("checkouterAnnotationObjectMapper")
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var checkouterEventProcessor: CheckouterEventProcessor

    @Autowired
    lateinit var orderRepository: YtOrderRepository

    @Autowired
    lateinit var orderEventService: OrderEventService

    @Autowired
    lateinit var environmentService: EnvironmentService

    @Autowired
    lateinit var checkouterApiService: CheckouterApiService

    @Autowired
    lateinit var orderEditRequestService: OrderEditRequestService

    @Test
    fun `verify that load testing events are not persisted`() {
        // given
        val batch = createBatch("load-testing-events-batch.json")

        // when
        checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
            checkouterEventProcessor.processMessages(batch.messageData, tx)
        }

        // then
        val persistedItems = orderRepository.orderEntityRepository.lookupRows(
            setOf(
                OrderKey(774, 1),
                OrderKey(774, 1),
                OrderKey(774, 1),
            )
        )

        assertThat(persistedItems).isEmpty()
    }

    @Test
    @DbUnitDataSet(before = ["ignored-events.before.csv"])
    fun `verify that ignored orders are not processed`() {
        // given
        val batch = createBatch("ignored-events-batch-1.json")

        // when
        checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
            checkouterEventProcessor.processMessages(batch.messageData, tx)
        }

        // then
        val actual = orderRepository.orderEntityRepository.lookupRows(
            setOf(
                OrderKey(620509, 62669778),
                OrderKey(620509, 62678235),
            )
        )
        assertThat(actual).hasSize(1)
        assertThat(actual[0].key).isEqualTo(OrderKey(620509, 62678235))
    }

    @Test
    fun `verify that that old events with no existing state are not persisted`() {
        // given
        val batch = createBatch("old-events-incomplete-batch.json")

        // when
        checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
            checkouterEventProcessor.processMessages(batch.messageData, tx)
        }

        // then
        assertThat(orderRepository.orderEntityRepository.lookupRow(OrderKey(1223579, 79914680))).isNull()
    }

    @Test
    fun `verify api fallback if state is missing`() {
        // given
        environmentService.set(ENABLE_CHECKOUTER_API_FALLBACK, "true")
        whenever(checkouterApiService.getEventsByOrderId(eq(79914680), any())).thenReturn(
            this::class.loadTestEntities<OrderHistoryEvent>("fbs-express-delivered-batch-1.json", objectMapper)
                .union(
                    this::class.loadTestEntities("fbs-express-delivered-batch-2.json", objectMapper)
                ).toList()
        )
        val batch = createBatch("fbs-express-delivered-batch-2.json")

        // when
        checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
            checkouterEventProcessor.processMessages(batch.messageData, tx)
        }

        // then
        val actualOrder = orderRepository.orderEntityRepository.lookupRow(OrderKey(1223579, 79914680))
        assertThat(actualOrder).isNotNull
            .usingRecursiveComparison()
            .isEqualTo(this::class.loadTestEntity<OrderEntity>("expected/fbs-express-delivered-order-entities.json"))
        environmentService.set(ENABLE_CHECKOUTER_API_FALLBACK, "false")
    }

    @Test
    fun `verify api fallback for invalid transitions`() {
        // given
        // пытаемся обработать два батча, у которых пропущена часть событий посередине
        // проверяем, что в случае фолбэка на API недостающие события будут забраны из ручки
        environmentService.set(ENABLE_CHECKOUTER_API_FALLBACK, "true")
        whenever(checkouterApiService.getEventsByOrderId(eq(79914680), any())).thenReturn(
            this::class.loadTestEntities<OrderHistoryEvent>("fbs-express-delivered-batch-1.json", objectMapper)
                .union(
                    this::class.loadTestEntities("fbs-express-delivered-batch-2.json", objectMapper)
                ).toList()
        )
        val batch1 = createBatch("fbs-express-api-fallback-batch-1.json")
        val batch2 = createBatch("fbs-express-api-fallback-batch-2.json")

        // when
        listOf(batch1, batch2).forEach {
            checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
                checkouterEventProcessor.processMessages(it.messageData, tx)
            }
        }

        // then
        val actualOrder = orderRepository.orderEntityRepository.lookupRow(OrderKey(1223579, 79914680))
        assertThat(actualOrder).isNotNull
            .usingRecursiveComparison()
            .isEqualTo(this::class.loadTestEntity<OrderEntity>("expected/fbs-express-delivered-order-entities.json"))
        verify(checkouterApiService, times(1)).getEventsByOrderId(eq(79914680), any())
        environmentService.set(ENABLE_CHECKOUTER_API_FALLBACK, "false")
    }

    @Test
    fun `FBS express delivered - 2 batches`() {
        // given
        val batch1 = createBatch("fbs-express-delivered-batch-1.json")
        val batch2 = createBatch("fbs-express-delivered-batch-2.json")

        // when
        listOf(batch1, batch2).forEach {
            checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
                checkouterEventProcessor.processMessages(it.messageData, tx)
            }
        }

        // then
        val actualOrder = orderRepository.orderEntityRepository.lookupRow(OrderKey(1223579, 79914680))
        assertThat(actualOrder).isNotNull
            .usingRecursiveComparison()
            .isEqualTo(this::class.loadTestEntity<OrderEntity>("expected/fbs-express-delivered-order-entities.json"))

        val actualLines =
            orderRepository.orderLineEntityRepository.lookupRow(OrderLineKey(1223579, 79914680, 138268465))
        assertThat(actualLines).isNotNull
            .satisfies { assertThat(it!!.orderLinePromo).hasSize(2) }
            .usingRecursiveComparison().ignoringFields("orderLinePromo")
            .isEqualTo(this::class.loadTestEntity<OrderLineEntity>("expected/fbs-express-delivered-order-lines.json"))

        val actualLogistics = orderRepository.orderLogisticsEntityRepository.lookupRow(OrderKey(1223579, 79914680))
        assertThat(actualLogistics).isNotNull
            .isEqualTo(
                this::class.loadTestEntity<OrderLogisticsEntity>(
                    "expected/fbs-express-delivered-logistic-entities.json"
                )
            )

        val events = orderEventService.findEventsByOrderKey(OrderKey(1223579, 79914680))
        assertThat(events)
            .isNotEmpty
            .usingElementComparatorIgnoringFields("eventId", "eventSpawnTimestamp", "timestamp")
            .containsExactlyInAnyOrderElementsOf(this::class.loadTestEntities<OrderEvent>("expected/fbs-express-delivered-events.json"))

        // index tables
        val createdAtIndex = orderRepository.createdAtIndexRepository.selectAll().firstOrNull()
        assertThat(createdAtIndex)
            .isNotNull
            .usingRecursiveComparison().ignoringFields("dummy")
            .isEqualTo(
                CreatedAtIndex(
                    CreatedAtIndex.Key(
                        Instant.ofEpochMilli(1638604474000),
                        1223579,
                        79914680
                    )
                )
            )

        val promoIndex = orderRepository.promoIndexRepository.selectAll()
        assertThat(promoIndex)
            .hasSize(1)
            .usingRecursiveComparison().ignoringFields("dummy")
            .isEqualTo(
                listOf(
                    PromoIndex(
                        PromoIndex.Key(
                            1223579,
                            "L174759",
                            79914680
                        )
                    )
                )
            )

        val merchantIndex = orderRepository.merchantOrderIdIndexRepository.selectAll()
        assertThat(merchantIndex)
            .hasSize(1)
            .isEqualTo(
                listOf(
                    MerchantOrderIdIndex(
                        MerchantOrderIdIndex.Key(
                            1223579,
                            "79914680",
                            OrderSourcePlatform.MARKET
                        ),
                        orderId = 79914680
                    )
                )
            )
        assertThat(orderRepository.findUnprocessedOutboundEvents(0)).hasSize(2)
    }

    @Test
    fun `FBS express - order items updated, order instances updated`() {
        // given
        val batch1 = createBatch("fbs-express-items-changed-1.json")
        val batch2 = createBatch("fbs-express-items-changed-2.json")

        // when
        listOf(batch1, batch2).forEach {
            checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
                checkouterEventProcessor.processMessages(it.messageData, tx)
            }
        }

        // then
        val actualOrder = orderRepository.orderEntityRepository.lookupRow(OrderKey(1872909, 91276558))
        assertThat(actualOrder!!).extracting("status", "substatus")
            .containsExactly(MerchantOrderStatus.DELIVERED, MerchantOrderSubstatus.DELIVERY_SERVICE_DELIVERED)
        val linesEntities = orderRepository.orderLineEntityRepository.lookupRows(actualOrder.toOrderLineKeys())
        assertThat(linesEntities).satisfies { lines ->
            assertThat(lines.size).isEqualTo(2)
            assertThat(lines.find { it.key.orderLineId == 153075646L }).isNotNull
                .extracting("initialCount", "countInDelivery", "itemStatuses", "cis")
                .containsExactly(
                    1, 0,
                    ItemStatuses(
                        orderLineId = 153075646L,
                        statuses = mapOf(MerchantItemStatus.DELETED.name to 1)
                    ),
                    emptyList<String>()
                )
            assertThat(lines.find { it.key.orderLineId == 153075647L }).isNotNull.satisfies {
                assertThat(it)
                    .extracting("initialCount", "countInDelivery", "itemStatuses", "cis")
                    .containsExactly(
                        1, 1,
                        ItemStatuses(
                            orderLineId = 153075647L,
                            statuses = mapOf(MerchantItemStatus.DELIVERED_TO_BUYER.name to 1)
                        ),
                        listOf("0104603619000087211096790147186")
                    )
                assertThat(it!!.identifiers!!.cisFull).singleElement().satisfies { cisFull ->
                    assertThat(cisFull.isFullCis()).isTrue
                    assertThat(cisFull.value).isEqualTo("0104603619000087211096790147186\u001D91EE06\u001D92gI47gJ+RXr57AszxcRTbbBueaPOmEgPKeEROpedGiWs=")
                    assertThat(cisFull.getCryptoTail()).isEqualTo("gI47gJ+RXr57AszxcRTbbBueaPOmEgPKeEROpedGiWs=")
                    assertThat(cisFull.getIdentity()).isEqualTo(it.cis!!.single())
                }
            }
        }

        val events = orderEventService.findEventsByOrderKey(OrderKey(1872909, 91276558))
        val itemsRemovedEvents = events.filter { it.type == OrderEventType.ORDER_ITEMS_CHANGED }
        assertThat(itemsRemovedEvents).hasSize(1)
        assertThat(itemsRemovedEvents[0])
            .usingRecursiveComparison().ignoringFields("eventSpawnTimestamp", "timestamp")
            .isEqualTo(
                OrderEvent(
                    key = OrderEventKey(
                        partnerId = 1872909,
                        orderId = 91276558,
                        eventId = 2484809186
                    ),
                    eventSpawnTimestamp = Instant.ofEpochMilli(1643533908000),
                    timestamp = Instant.now(),
                    actor = Actor.MERCHANT_API,
                    type = OrderEventType.ORDER_ITEMS_CHANGED,
                    status = MerchantOrderStatus.PROCESSING,
                    substatus = MerchantOrderSubstatus.STARTED,
                    details = ItemsChangedEventDetails(
                        reason = HistoryEventReason.ITEMS_NOT_SUPPLIED,
                        details = listOf(
                            ItemsRemovedDto(
                                key = OrderLineKey(
                                    partnerId = 1872909,
                                    orderId = 91276558,
                                    orderLineId = 153075646L
                                ),
                                count = 1
                            )
                        )
                    ).toJsonString()
                )
            )

        val outboundEvents = orderRepository.findUnprocessedOutboundEvents(0)
        assertThat(outboundEvents).hasSize(3).extracting("eventPayloadType")
            .containsExactly(
                MERCHANT_ORDER_SUBSTATUS_CHANGED_PAYLOAD.name,
                ORDER_ITEMS_REMOVED_PAYLOAD.name,
                MERCHANT_ORDER_SUBSTATUS_CHANGED_PAYLOAD.name
            )
    }

    @Test
    fun `FBS - order items updated, order instances updated, outbound event sending`() {
        // given
        val batch1 = createBatch("fbs-items-changed-1.json")
        val batch2 = createBatch("fbs-items-changed-2.json")

        // when
        listOf(batch1, batch2).forEach {
            checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
                checkouterEventProcessor.processMessages(it.messageData, tx)
            }
        }

        // then
        val actualOrder = orderRepository.orderEntityRepository.lookupRow(OrderKey(1872909, 91276558))
        assertThat(actualOrder!!).extracting("status", "substatus")
            .containsExactly(MerchantOrderStatus.DELIVERED, MerchantOrderSubstatus.DELIVERY_SERVICE_DELIVERED)
        val linesEntities = orderRepository.orderLineEntityRepository.lookupRows(actualOrder.toOrderLineKeys())
        assertThat(linesEntities).satisfies { lines ->
            assertThat(lines.size).isEqualTo(2)
            assertThat(lines.find { it.key.orderLineId == 153075646L }).isNotNull
                .extracting("initialCount", "countInDelivery", "itemStatuses", "cis")
                .containsExactly(
                    1, 0,
                    ItemStatuses(
                        orderLineId = 153075646L,
                        statuses = mapOf(MerchantItemStatus.DELETED.name to 1)
                    ),
                    emptyList<String>()
                )
            assertThat(lines.find { it.key.orderLineId == 153075647L }).isNotNull
                .extracting("initialCount", "countInDelivery", "itemStatuses", "cis")
                .containsExactly(
                    1, 1,
                    ItemStatuses(
                        orderLineId = 153075647L,
                        statuses = mapOf(MerchantItemStatus.DELIVERED_TO_BUYER.name to 1)
                    ),
                    listOf("0104603619000087211096790147186")
                )
        }
        val events = orderEventService.findEventsByOrderKey(OrderKey(1872909, 91276558))
        val itemsRemovedEvents = events.filter { it.type == OrderEventType.ORDER_ITEMS_CHANGED }
        assertThat(itemsRemovedEvents).hasSize(1)
        assertThat(itemsRemovedEvents[0])
            .usingRecursiveComparison().ignoringFields("eventSpawnTimestamp", "timestamp")
            .isEqualTo(
                OrderEvent(
                    key = OrderEventKey(
                        partnerId = 1872909,
                        orderId = 91276558,
                        eventId = 2484809186
                    ),
                    eventSpawnTimestamp = Instant.ofEpochMilli(1643533908000),
                    timestamp = Instant.now(),
                    actor = Actor.MERCHANT_API,
                    type = OrderEventType.ORDER_ITEMS_CHANGED,
                    status = MerchantOrderStatus.PROCESSING,
                    substatus = MerchantOrderSubstatus.STARTED,
                    details = ItemsChangedEventDetails(
                        reason = HistoryEventReason.ITEMS_NOT_SUPPLIED,
                        details = listOf(
                            ItemsRemovedDto(
                                key = OrderLineKey(
                                    partnerId = 1872909,
                                    orderId = 91276558,
                                    orderLineId = 153075646L
                                ),
                                count = 1
                            )
                        )
                    ).toJsonString()
                )
            )

        assertThat(orderRepository.findUnprocessedOutboundEvents(0)).satisfies { events ->
            assertThat(events.size).isEqualTo(6)

            val substatusEvents = events.filter {
                it.eventPayloadType == MERCHANT_ORDER_SUBSTATUS_CHANGED_PAYLOAD.name
            }
            assertThat(substatusEvents).hasSize(4).allSatisfy {
                assertThat(it.orderId).isEqualTo(91276558)
                assertThat(it.processed).isEqualTo(false)
                assertThat(
                    ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent.parseFrom(
                        it.eventPayload
                    )
                ).satisfies {
                    assertThat(it.merchantOrderSubstatusChangedPayload).satisfies {
                        assertThat(it.orderKey.orderId).isEqualTo(91276558)
                        assertThat(it.status).isEqualTo(Status.PROCESSING)
                    }
                }
            }

            val removedItemsEvents = events.filter {
                it.eventPayloadType == ORDER_ITEMS_REMOVED_PAYLOAD.name
            }
            assertThat(removedItemsEvents).hasSize(1).allSatisfy {
                assertThat(it.orderId).isEqualTo(91276558)
                assertThat(it.processed).isEqualTo(false)
                assertThat(
                    ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent.parseFrom(
                        it.eventPayload
                    )
                ).satisfies { parsedEvent ->
                    assertThat(parsedEvent.orderItemsRemovedPayload).satisfies { payload ->
                        assertThat(payload.orderKey.orderId).isEqualTo(91276558)
                        assertThat(payload.removedItemsList).hasSize(1)
                    }
                }
            }
            val parcelBoxesChangedEvents = events.filter {
                it.eventPayloadType == ORDER_PARCEL_BOXES_CHANGED_PAYLOAD.name
            }
            assertThat(parcelBoxesChangedEvents).singleElement().satisfies {
                assertThat(it.orderId).isEqualTo(91276558)
                assertThat(it.processed).isEqualTo(false)
                assertThat(
                    ru.yandex.market.mbi.orderservice.proto.event.model.OrderEvent.parseFrom(
                        it.eventPayload
                    )
                ).satisfies { parsedEvent ->
                    assertThat(parsedEvent.orderParcelBoxesChangedPayload).satisfies { payload ->
                        assertThat(payload.orderKey.orderId).isEqualTo(91276558)
                        assertThat(payload.orderKey.shopId).isEqualTo(1872909)
                    }
                }
            }
        }
    }

    @Test
    fun `FBY single merchant - order line is lost`() {
        // given
        val batch1 = createBatch("fby-items-changed.json")

        // when
        checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
            checkouterEventProcessor.processMessages(batch1.messageData, tx)
        }

        // then
        val actualOrder = orderRepository.orderEntityRepository.lookupRow(OrderKey(465852, 88950953))
        assertThat(actualOrder!!).extracting("status", "substatus")
            .containsExactly(MerchantOrderStatus.DELIVERED, MerchantOrderSubstatus.DELIVERY_SERVICE_DELIVERED)
        val linesEntities = orderRepository.orderLineEntityRepository.lookupRows(actualOrder.toOrderLineKeys())
        assertThat(linesEntities).satisfies { lines ->
            assertThat(lines.size).isEqualTo(3)
            assertThat(lines.find { it.key.orderLineId == 150029343L }).isNotNull
                .extracting("initialCount", "countInDelivery", "itemStatuses")
                .containsExactly(
                    1, 0,
                    ItemStatuses(
                        orderLineId = 150029343L,
                        statuses = mapOf(MerchantItemStatus.DELETED.name to 1)
                    )
                )
            assertThat(lines.find { it.key.orderLineId == 150029342L }).isNotNull
                .extracting("initialCount", "countInDelivery", "itemStatuses")
                .containsExactly(
                    1, 1,
                    ItemStatuses(
                        orderLineId = 150029342L,
                        statuses = mapOf(MerchantItemStatus.DELIVERED_TO_BUYER.name to 1)
                    )
                )
        }

        assertThat(orderRepository.findUnprocessedOutboundEvents(0).size).isEqualTo(0)
    }

    @Test
    fun `FBY multiple merchants - order line is lost`() {
        // given
        // батч содержит FBY заказ с позициями трех разных мерчантов, причем позиция одного из них
        // потеряна на складе. Ожидаем, что создадутся три партнерских заказа, причем один из них
        // перейдет в статус CANCELLED_IN_PROCESSING, а два других - в DELIVERED
        val batch1 = createBatch("fby-multiple-merchants-items-lost.json")
        val batch2 = createBatch("fby-multiple-merchants-items-lost-2.json")

        // when
        listOf(batch1, batch2).forEach {
            checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
                checkouterEventProcessor.processMessages(it.messageData, tx)
            }
        }

        // then
        val actualOrders = orderRepository.orderEntityRepository.selectAll()
        assertThat(actualOrders).hasSize(3).satisfies { orders ->
            assertThat(orders.find { it.key.partnerId == 594102L })
                .isNotNull
                .extracting("status", "substatus")
                .containsExactly(MerchantOrderStatus.CANCELLED_IN_PROCESSING, MerchantOrderSubstatus.MISSING_ITEM)
            assertThat(orders.filter { listOf(590476L, 620509L).contains(it.key.partnerId) }).hasSize(2)
                .allSatisfy {
                    assertThat(it.status).isEqualTo(MerchantOrderStatus.DELIVERED)
                    assertThat(it.substatus).isEqualTo(MerchantOrderSubstatus.DELIVERY_SERVICE_DELIVERED)
                }
        }
        val actualLines = orderRepository.orderLineEntityRepository.selectAll()
        assertThat(actualLines).hasSize(3).satisfies { lines ->
            assertThat(lines.find { it.key.orderLineId == 115008717L }).isNotNull
                .extracting("initialCount", "countInDelivery", "itemStatuses")
                .containsExactly(
                    1, 0,
                    ItemStatuses(
                        orderLineId = 115008717L,
                        statuses = mapOf(MerchantItemStatus.DELETED.name to 1)
                    )
                )
            assertThat(lines.find { it.key.orderLineId == 115008718L }).isNotNull
                .extracting("initialCount", "countInDelivery", "itemStatuses")
                .containsExactly(
                    1, 1,
                    ItemStatuses(
                        orderLineId = 115008718L,
                        statuses = mapOf(MerchantItemStatus.DELIVERED_TO_BUYER.name to 1)
                    )
                )
            assertThat(lines.find { it.key.orderLineId == 115008719L }).isNotNull
                .extracting("initialCount", "countInDelivery", "itemStatuses")
                .containsExactly(
                    5, 5,
                    ItemStatuses(
                        orderLineId = 115008719L,
                        statuses = mapOf(MerchantItemStatus.DELIVERED_TO_BUYER.name to 5)
                    )
                )
        }
        val createdAtIndex = orderRepository.createdAtIndexRepository.selectAll()
        assertThat(createdAtIndex).hasSize(3)
        val events = orderRepository.orderEventsRepository.selectAll().groupBy { it.key.partnerId }
        // события по доставленным линиям
        assertThat(events[590476L]).hasSize(11)
        assertThat(events[620509L]).hasSize(11)
        // события по отменившейся линии - после отмены событий нет
        assertThat(events[594102L])
            .hasSize(8)
            .filteredOn { it.key.eventId == 1L }
            .hasSize(1)
            .usingRecursiveComparison()
            .ignoringFields("eventSpawnTimestamp", "timestamp")
            .isEqualTo(
                listOf(
                    OrderEvent(
                        key = OrderEventKey(
                            partnerId = 594102,
                            orderId = 62669778,
                            eventId = 1
                        ),
                        eventSpawnTimestamp = Instant.now(),
                        actor = Actor.MARKETPLACE,
                        type = OrderEventType.STATUS_CHANGE,
                        status = MerchantOrderStatus.CANCELLED_IN_PROCESSING,
                        substatus = MerchantOrderSubstatus.MISSING_ITEM,
                        eventSource = EventSource.ORDER_SERVICE,
                        details = ""
                    )
                )
            )

        assertThat(orderRepository.findUnprocessedOutboundEvents(0).size).isEqualTo(0)
    }

    @Test
    fun `verify that already processed events do not cause any errors`() {
        // given
        val batch1 = createBatch("already-processed-event-batch-1.json")
        val batch2 = createBatch("already-processed-event-batch-2.json")

        // when
        listOf(batch1, batch2).forEach {
            checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
                checkouterEventProcessor.processMessages(it.messageData, tx)
            }
        }

        // then
        val actualOrder = orderRepository.orderEntityRepository.lookupRow(OrderKey(1223579, 79914680))
        assertThat(actualOrder).isNotNull
            .extracting("status").isEqualTo(MerchantOrderStatus.PENDING)
    }

    @Test
    fun `item partially not found - multiple order edits`() {
        // given
        val batch = createBatch("multiple-order-edits.json")

        // when
        checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
            checkouterEventProcessor.processMessages(batch.messageData, tx)
        }

        // then
        val actualLine = orderRepository.orderLineEntityRepository.lookupRow(
            OrderLineKey(465815, 86711022, 147179174)
        )
        assertThat(actualLine).isNotNull
            .extracting("initialCount", "countInDelivery", "itemStatuses")
            .containsExactly(
                3, 1,
                ItemStatuses(
                    orderLineId = 147179174,
                    statuses = mapOf(
                        MerchantItemStatus.DELIVERED_TO_BUYER.name to 1,
                        MerchantItemStatus.DELETED.name to 2
                    )
                )
            )
    }

    @Test
    fun `verify that events with already changed statuses do not cause any errors`() {
        val batch = createBatch("already-changed-status-substatus.events.before.json")
        checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
            checkouterEventProcessor.processMessages(batch.messageData, tx)
        }
        // смена статуса - для проверки события ORDER_STATUS_UPDATED
        changeStatus(1, MerchantOrderStatus.PROCESSING, MerchantOrderSubstatus.STARTED, EventSource.ORDER_SERVICE)
        // смена подстатуса - для проверки события ORDER_SUBSTATUS_UPDATED
        changeStatus(2, MerchantOrderStatus.PROCESSING, MerchantOrderSubstatus.PACKAGING, EventSource.LOGISTICS)
        // смена статуса - отправленные события из чекаутера не должны сломаться
        changeStatus(
            3,
            MerchantOrderStatus.DELIVERY,
            MerchantOrderSubstatus.DELIVERY_SERVICE_RECEIVED,
            EventSource.LOGISTICS
        )

        createBatch("already-changed-status-substatus.new-events.json").run {
            checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
                checkouterEventProcessor.processMessages(this.messageData, tx)
            }
        }

        val actualOrder = orderRepository.orderEntityRepository.lookupRow(OrderKey(1223579, 79914680))
        assertThat(actualOrder).isNotNull
            .extracting("status", "substatus")
            .containsExactly(MerchantOrderStatus.DELIVERY, MerchantOrderSubstatus.DELIVERY_SERVICE_RECEIVED)
    }

    @Test
    fun `verify that cancellation request processes correctly`() {
        // given
        val batch = createBatch("cancellation-request.json")

        // when
        checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
            checkouterEventProcessor.processMessages(batch.messageData, tx)
        }

        // then
        val actualOrder = orderRepository.orderEntityRepository.lookupRow(OrderKey(1223579, 79914680))
        assertThat(actualOrder).isNotNull
            .satisfies {
                assertThat(it!!.hasCancellationRequest).isEqualTo(true)
                assertThat(it.cancellationRequest).isEqualTo(
                    CancellationRequest(
                        requestId = 986997,
                        createdAt = Instant.parse("2021-12-04T17:03:26.433Z"),
                        substatus = MerchantOrderSubstatus.RESERVATION_EXPIRED,
                        notes = "some notes"
                    )
                )
            }
    }

    @Test
    fun `reject cancellation request`() {
        // given
        val batch1 = createBatch("cancellation-request.json")
        val batch2 = createBatch("cancellation-request-reject.json")

        // when
        listOf(batch1, batch2).forEach {
            checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
                checkouterEventProcessor.processMessages(it.messageData, tx)
            }
        }

        // then
        val actualOrder = orderRepository.orderEntityRepository.lookupRow(OrderKey(1223579, 79914680))
        assertThat(actualOrder).isNotNull
            .satisfies {
                assertThat(it!!.hasCancellationRequest).isEqualTo(false)
                assertThat(it.cancellationRequest).isEqualTo(CancellationRequest())
            }
    }

    @Test
    fun `verify that change-request status is changed correctly`() {
        createBatch("order-change-request.before.json").let {
            checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
                checkouterEventProcessor.processMessages(it.messageData, tx)
            }
        }

        val createdAt = Instant.parse("2022-01-30T05:38:37Z")
        orderEditRequestService.storeCheckouterEditRequest(
            orderKey = OrderKey(
                partnerId = 1872909,
                orderId = 91276558
            ),
            checkouterChangeRequest = ChangeRequest(
                12345,
                91276558,
                ItemsRemovalChangeRequestPayload(
                    listOf(
                        mock {
                            on(it.id).thenReturn(153075647L)
                            on(it.count).thenReturn(1)
                        },
                        mock {
                            on(it.id).thenReturn(153075648L)
                            on(it.count).thenReturn(3)
                        },
                    ),
                    listOf(mock()),
                    HistoryEventReason.ITEMS_NOT_FOUND,
                    setOf(
                        OrderItemInstances(
                            153075647L,
                            listOf(OrderItemInstance("153075647_cis"))
                        ),
                        OrderItemInstances(
                            153075648L,
                            listOf(OrderItemInstance("153075648_cis"))
                        )
                    )
                ),
                ChangeRequestStatus.NEW,
                createdAt,
                null,
                ClientRole.SHOP_USER
            ),
            requestStatus = ChangeRequestStatus.NEW
        )

        createBatch("order-change-request-created.json").let {
            checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
                checkouterEventProcessor.processMessages(it.messageData, tx)
            }
        }
        validateOrderEditRequest(ChangeRequestStatus.PROCESSING)

        createBatch("order-change-request-status-updated.json").let {
            checkouterEventProcessor.orderRepository.rwClient.execInTransaction { tx ->
                checkouterEventProcessor.processMessages(it.messageData, tx)
            }
        }
        validateOrderEditRequest(ChangeRequestStatus.APPLIED)
    }

    private fun validateOrderEditRequest(status: ChangeRequestStatus) {
        val key = OrderEditRequestKey(1872909, 91276558, 12345)
        val actualEditRequest = orderRepository.orderEditRequestRepository.lookupRow(
            key
        )
        assertThat(actualEditRequest).isNotNull
            .usingRecursiveComparison().ignoringFields("createdAt")
            .isEqualTo(
                OrderEditRequestEntity(
                    key = key,
                    type = ChangeRequestType.ITEMS_REMOVAL,
                    status = status,
                    itemsRemovalRequestPayload = ItemsRemovalRequestPayload(
                        updatedItems = listOf(
                            UpdatedItem(153075647L, 1, listOf("153075647_cis")),
                            UpdatedItem(153075648L, 3, listOf("153075648_cis")),
                        ),
                        reason = HistoryEventReason.ITEMS_NOT_FOUND
                    )
                )
            )
    }

    private fun changeStatus(
        eventId: Long,
        newStatus: MerchantOrderStatus,
        newSubstatus: MerchantOrderSubstatus?,
        source: EventSource,
        partnerId: Long = 1223579,
        orderId: Long = 79914680
    ) {
        orderRepository.storeOrderStatus(
            key = OrderKey(partnerId, orderId),
            newStatus = newStatus,
            newSubstatus = newSubstatus
        )
        orderRepository.storeOrderEvent(
            OrderEvent(
                key = OrderEventKey(partnerId, orderId, eventId),
                eventSpawnTimestamp = Instant.now(),
                actor = Actor.MERCHANT_PI,
                eventSource = EventSource.ORDER_SERVICE,
                type = OrderEventType.STATUS_CHANGE,
                status = newStatus,
                substatus = newSubstatus
            )
        )
    }

    private fun createBatch(eventsResourceName: String): MessageBatch {
        val events =
            objectMapper.readValue<List<OrderHistoryEvent>>(this::class.loadResourceAsString(eventsResourceName))
                .sortedBy { it.id }
        return MessageBatch(
            "checkouter-events", 0,
            events.map { ev ->
                MessageData(
                    objectMapper.writeValueAsBytes(ev),
                    0,
                    mock {
                        on(it.codec).thenReturn(CompressionCodec.RAW)
                    }
                )
            }
        )
    }
}
