package ru.yandex.market.mbi.orderservice.tms.transitions

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.checkout.checkouter.order.TaxSystem
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod
import ru.yandex.market.checkout.checkouter.pay.PaymentType
import ru.yandex.market.mbi.helpers.CleanupTables
import ru.yandex.market.mbi.orderservice.common.dsl.Actor
import ru.yandex.market.mbi.orderservice.common.dsl.EventDto
import ru.yandex.market.mbi.orderservice.common.dsl.OrderTransitionGraph
import ru.yandex.market.mbi.orderservice.common.enum.MerchantItemStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderStatus
import ru.yandex.market.mbi.orderservice.common.enum.MerchantOrderSubstatus
import ru.yandex.market.mbi.orderservice.common.enum.OrderSourcePlatform
import ru.yandex.market.mbi.orderservice.common.exceptions.TransitionNotFoundException
import ru.yandex.market.mbi.orderservice.common.model.dto.MerchantOrder
import ru.yandex.market.mbi.orderservice.common.model.dto.PlacementModel
import ru.yandex.market.mbi.orderservice.common.model.yt.EventSource
import ru.yandex.market.mbi.orderservice.common.model.yt.ItemStatuses
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEvent
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEventKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderEventType
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderKey
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderSummaryExtended
import ru.yandex.market.mbi.orderservice.common.model.yt.OrderSummaryExtendedKey
import ru.yandex.market.mbi.orderservice.common.persistence.repository.yt.OrderSummaryExtendedRepository
import ru.yandex.market.mbi.orderservice.common.service.yt.dynamic.OrderEventService
import ru.yandex.market.mbi.orderservice.tms.FunctionalTest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Currency
import java.util.TreeMap
import kotlin.math.abs

@CleanupTables([OrderEvent::class, OrderSummaryExtended::class])
class OrderTransitionGraphTest : FunctionalTest() {

    @Autowired
    lateinit var graph: OrderTransitionGraph

    @Autowired
    lateinit var orderEventService: OrderEventService

    @Autowired
    lateinit var orderSummaryExtendedRepository: OrderSummaryExtendedRepository

    private val statusesForDeliverySummary = listOf(
        MerchantOrderStatus.DELIVERY to MerchantOrderStatus.DELIVERED,
        MerchantOrderStatus.PICKUP to MerchantOrderStatus.DELIVERED,
        MerchantOrderStatus.DELIVERY to MerchantOrderStatus.PARTIALLY_DELIVERED,
        MerchantOrderStatus.PICKUP to MerchantOrderStatus.PARTIALLY_DELIVERED,
    )

    @Test
    fun `test invalid transitions`() {
        val order = getTestOrder(MerchantOrderStatus.PLACING, MerchantOrderSubstatus.UNKNOWN, emptyMap())

        Assertions.assertThatExceptionOfType(TransitionNotFoundException::class.java).isThrownBy {
            graph.doTransition(
                order = order,
                to = MerchantOrderStatus.PROCESSING,
                actor = Actor.MARKETPLACE
            )
        }

        Assertions.assertThatNoException().isThrownBy {
            graph.tryTransition(
                order = order,
                to = MerchantOrderStatus.PROCESSING,
                actor = Actor.MARKETPLACE,
                allowTransitive = false
            )
        }
    }

    @Test
    fun `test valid transition to cancelled`() {
        val order = getTestOrder(
            MerchantOrderStatus.PENDING,
            MerchantOrderSubstatus.UNKNOWN,
            mapOf(
                12345L to ItemStatuses(12345, TreeMap(mapOf(MerchantItemStatus.CREATED.name to 2))),
                12346L to ItemStatuses(12346, TreeMap(mapOf(MerchantItemStatus.CREATED.name to 3)))
            )
        )

        val orderAfterTransition = graph.doTransition(
            order = order,
            to = MerchantOrderStatus.PROCESSING,
            eventDto = EventDto(
                eventId = 9000,
                eventTimestamp = Instant.now(),
                source = EventSource.CHECKOUTER
            ),
            toSubstatus = MerchantOrderSubstatus.STARTED,
            actor = Actor.MERCHANT_API
        )
        assertThat(orderAfterTransition).satisfies {
            assertThat(it.status == MerchantOrderStatus.PROCESSING)
        }

        assertThat(
            graph.doTransitionToCancelled(
                order = orderAfterTransition,
                toSubstatus = MerchantOrderSubstatus.USER_CHANGED_MIND,
                actor = Actor.BUYER,
                eventDto = EventDto(
                    eventId = 9001,
                    eventTimestamp = Instant.now(),
                    source = EventSource.CHECKOUTER
                ),
            )
        )
            .satisfies {
                assertThat(it.status == MerchantOrderStatus.CANCELLED_IN_PROCESSING)
                assertThat(it.itemStatuses).containsKeys(12345, 12346)
                assertThat(it.itemStatuses).containsValues(
                    ItemStatuses(
                        orderLineId = 12345,
                        TreeMap(mapOf(MerchantItemStatus.CANCELLED.name to 2))
                    ),
                    ItemStatuses(
                        orderLineId = 12346,
                        TreeMap(mapOf(MerchantItemStatus.CANCELLED.name to 3))
                    )
                )
            }

        // assert side-effects
        val events = orderEventService.findEventsByOrderKey(OrderKey(774, 123))
        assertThat(events).satisfies {
            assertThat(it).hasSize(2)
            assertThat(it).usingElementComparatorIgnoringFields("eventSpawnTimestamp", "timestamp")
                .containsExactlyInAnyOrder(
                    OrderEvent(
                        OrderEventKey(774, 123, 9000),
                        eventSpawnTimestamp = Instant.now(),
                        actor = Actor.MERCHANT_API,
                        type = OrderEventType.STATUS_CHANGE,
                        status = MerchantOrderStatus.PROCESSING,
                        substatus = MerchantOrderSubstatus.STARTED,
                        eventSource = EventSource.CHECKOUTER,
                        details = ""
                    ),
                    OrderEvent(
                        OrderEventKey(774, 123, 9001),
                        eventSpawnTimestamp = Instant.now(),
                        actor = Actor.BUYER,
                        type = OrderEventType.STATUS_CHANGE,
                        status = MerchantOrderStatus.CANCELLED_IN_PROCESSING,
                        substatus = MerchantOrderSubstatus.USER_CHANGED_MIND,
                        eventSource = EventSource.CHECKOUTER,
                        details = ""
                    )
                )
        }
    }

    @Test
    fun `test valid transition to delivered`() {
        val order = getTestOrder(
            MerchantOrderStatus.DELIVERY,
            MerchantOrderSubstatus.DELIVERY_SERVICE_RECEIVED,
            mapOf(
                12345L to ItemStatuses(12345, TreeMap(mapOf(MerchantItemStatus.SHIPPED.name to 2))),
                12346L to ItemStatuses(
                    12346,
                    TreeMap(
                        mapOf(
                            MerchantItemStatus.SHIPPED.name to 2,
                            MerchantItemStatus.CANCELLED.name to 1
                        )
                    )
                )
            )
        )

        assertThat(
            graph.doTransition(
                order,
                MerchantOrderStatus.DELIVERED,
                MerchantOrderSubstatus.DELIVERY_SERVICE_DELIVERED,
                actor = Actor.DELIVERY_SERVICE,
                eventDto = EventDto(
                    eventId = 9000,
                    eventTimestamp = Instant.now(),
                    source = EventSource.CHECKOUTER
                )
            )
        ).satisfies {
            assertThat(it.status).isEqualTo(MerchantOrderStatus.DELIVERED)
            assertThat(it.itemStatuses).containsKeys(12345, 12346)
            assertThat(it.itemStatuses).containsValues(
                ItemStatuses(
                    orderLineId = 12345,
                    TreeMap(mapOf(MerchantItemStatus.DELIVERED_TO_BUYER.name to 2))
                ),
                ItemStatuses(
                    orderLineId = 12346,
                    TreeMap(
                        mapOf(
                            MerchantItemStatus.CANCELLED.name to 1,
                            MerchantItemStatus.DELIVERED_TO_BUYER.name to 2,
                        )
                    )
                )
            )
        }

        // assert side-effects
        val events = orderEventService.findEventsByOrderKey(OrderKey(774, 123))
        assertThat(events).satisfies {
            assertThat(it).hasSize(1)
            assertThat(it).usingElementComparatorIgnoringFields("eventSpawnTimestamp", "timestamp")
                .containsExactly(
                    OrderEvent(
                        OrderEventKey(774, 123, 9000),
                        eventSpawnTimestamp = Instant.now(),
                        actor = Actor.DELIVERY_SERVICE,
                        type = OrderEventType.STATUS_CHANGE,
                        status = MerchantOrderStatus.DELIVERED,
                        substatus = MerchantOrderSubstatus.DELIVERY_SERVICE_DELIVERED,
                        eventSource = EventSource.CHECKOUTER,
                        details = ""
                    )
                )
        }

        assertSummaryDeliveredHasUpdated()
    }

    @Test
    fun `test transitive status change to delivered`() {
        val order = getTestOrder(
            MerchantOrderStatus.PLACING,
            MerchantOrderSubstatus.UNKNOWN,
            mapOf(
                12345L to ItemStatuses(12345, TreeMap(mapOf(MerchantItemStatus.CREATED.name to 2)))
            )
        )

        assertThatCode {
            graph.doTransition(
                order,
                MerchantOrderStatus.PROCESSING,
                MerchantOrderSubstatus.STARTED,
                actor = Actor.MARKETPLACE,
                eventDto = EventDto(
                    eventId = 9000,
                    eventTimestamp = Instant.now(),
                    source = EventSource.CHECKOUTER
                ),
                allowTransitive = true
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `order_summary_extended record is created when transition to PROCESSING occurs`() {
        val order = getTestOrder(
            MerchantOrderStatus.PENDING,
            MerchantOrderSubstatus.UNKNOWN,
            mapOf(
                12345L to ItemStatuses(12345, TreeMap(mapOf(MerchantItemStatus.CREATED.name to 2)))
            )
        )
        graph.doTransition(
            order = order,
            to = MerchantOrderStatus.PROCESSING,
            actor = Actor.MERCHANT_API
        )
        assertThat(
            orderSummaryExtendedRepository.lookupRow(
                OrderSummaryExtendedKey(
                    partnerId = 774,
                    timestamp = Instant.parse("2021-12-31T12:00:00.00Z")
                        .truncatedTo(ChronoUnit.HOURS)
                )
            )!!
        ).extracting(OrderSummaryExtended::createdCount, OrderSummaryExtended::createdGmv)
            .containsExactly(1L, 57000L)
    }

    @Test
    fun `verify that summary is not updated for external orders`() {
        val order = getTestOrder(
            status = MerchantOrderStatus.PENDING,
            substatus = MerchantOrderSubstatus.UNKNOWN,
            itemStatuses = mapOf(
                12345L to ItemStatuses(12345, TreeMap(mapOf(MerchantItemStatus.CREATED.name to 2)))
            ),
            isExternal = true
        )
        graph.doTransition(
            order = order,
            to = MerchantOrderStatus.PROCESSING,
            actor = Actor.MERCHANT_API
        )

        assertThat(
            orderSummaryExtendedRepository.lookupRow(
                OrderSummaryExtendedKey(
                    partnerId = 774,
                    timestamp = Instant.parse("2021-12-31T12:00:00.00Z")
                        .truncatedTo(ChronoUnit.HOURS)
                )
            )
        ).isNull()
    }

    @TestFactory
    fun testSummaryDeliveredWasUpdated() = statusesForDeliverySummary
        .map { (fromStatus, toStatus) ->
            DynamicTest.dynamicTest(
                "test updating delivered summary" +
                    " on transition from $fromStatus to $toStatus"
            ) {
                val partnerId = abs(fromStatus.hashCode() + toStatus.hashCode().toLong())
                val order = getTestOrder(
                    fromStatus,
                    MerchantOrderSubstatus.UNKNOWN,
                    mapOf(),
                    partnerId = partnerId
                )
                graph.doTransition(
                    order = order,
                    to = toStatus,
                    actor = Actor.MERCHANT_API,
                    eventDto = EventDto(
                        eventId = 9000,
                        eventTimestamp = Instant.parse("2022-01-10T12:13:00.00Z"),
                        source = EventSource.CHECKOUTER
                    )
                )
                assertSummaryDeliveredHasUpdated(partnerId = partnerId)
            }
        }

    private fun assertSummaryDeliveredHasUpdated(partnerId: Long = 774) {
        assertThat(
            orderSummaryExtendedRepository.lookupRow(
                OrderSummaryExtendedKey(
                    partnerId = partnerId,
                    timestamp = Instant.parse("2021-12-31T12:00:00.00Z")
                        .truncatedTo(ChronoUnit.HOURS)
                )
            )!!
        ).extracting(OrderSummaryExtended::deliveredCount, OrderSummaryExtended::deliveredGmv)
            .containsExactly(1L, 57000L)
    }

    private fun getTestOrder(
        status: MerchantOrderStatus,
        substatus: MerchantOrderSubstatus,
        itemStatuses: Map<Long, ItemStatuses>,
        partnerId: Long = 774,
        isExternal: Boolean = false
    ): MerchantOrder {
        return MerchantOrder(
            partnerId = partnerId,
            orderId = 123,
            merchantOrderId = "123",
            createdAt = Instant.parse("2021-12-31T12:12:00.00Z"),
            updatedAt = Instant.parse("2021-12-31T12:12:00.00Z"),
            deliveredAt = Instant.parse("2022-01-10T12:13:00.00Z"),
            shipmentDeadline = null,
            shipmentDate = null,
            shipmentId = null,
            status = status,
            substatus = substatus,
            itemStatuses = itemStatuses,
            currency = Currency.getInstance("RUB"),
            buyerCurrency = Currency.getInstance("RUB"),
            price = 45000,
            subsidy = 12000,
            paymentType = PaymentType.PREPAID,
            paymentMethod = PaymentMethod.YANDEX,
            taxSystem = TaxSystem.USN,
            fake = false,
            express = false,
            hasCancellationRequest = false,
            cancellationRequest = null,
            note = "Test note",
            partnerNote = null,
            willExpireAt = null,
            sourcePlatform = if (isExternal) OrderSourcePlatform.OZON else null,
            partnerModel = PlacementModel.FBY
        )
    }
}
