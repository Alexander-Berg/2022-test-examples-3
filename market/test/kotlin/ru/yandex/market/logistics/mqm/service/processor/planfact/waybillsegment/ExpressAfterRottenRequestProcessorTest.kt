package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.entity.enums.IssueLinkEntityType
import ru.yandex.market.logistics.mqm.entity.enums.IssueLinkReason
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.IssueLinkService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class ExpressAfterRottenRequestProcessorTest {
    private lateinit var processor: ExpressAfterRottenRequestProcessor

    @Mock
    private lateinit var issueLinkService: IssueLinkService

    @BeforeEach
    fun setUp() {
        processor = ExpressAfterRottenRequestProcessor(issueLinkService)
    }

    @AfterEach
    fun tearDown() {
        verifyNoMoreInteractions(issueLinkService)
    }

    @Test
    @DisplayName("Новый статус сегмента: успех")
    fun successWaybillStatusChange() {
        val (newCheckpoint, order) = prepareCheckpointAndOrder(isExpress = true, with47 = true)
        processor.waybillSegmentStatusAdded(LomWaybillStatusAddedContext(newCheckpoint, order, listOf()))

        verify(issueLinkService).close("extId", IssueLinkEntityType.ORDER, IssueLinkReason.RECALL_COURIER)
    }

    @DisplayName("Новый статус сегмента: заказ не экспрессный")
    @Test
    fun waybillStatusChangeNotExpress() {
        val prepareOrder = prepareCheckpointAndOrder(isExpress = false, with47 = true)
        processor.waybillSegmentStatusAdded(
            LomWaybillStatusAddedContext(prepareOrder.first, prepareOrder.second, listOf())
        )
    }

    @Test
    @DisplayName("Новый статус сегмента: заказ без 47чп")
    fun waybillStatusChangeWithout47() {
        val prepareOrder = prepareCheckpointAndOrder(isExpress = true, with47 = false)
        processor.waybillSegmentStatusAdded(
            LomWaybillStatusAddedContext(prepareOrder.first, prepareOrder.second, listOf())
        )
    }

    @Test
    @DisplayName("Новый статус сегмента: 47чп позже обрабатываемого")
    fun waybillStatusChange47After() {
        val prepareOrder = prepareCheckpointAndOrder(isExpress = true, with47 = true)

        prepareOrder.first.apply { date = TRANSIT_UPDATED_BY_DELIVERY_TIME.minusSeconds(10000) }

        processor.waybillSegmentStatusAdded(
            LomWaybillStatusAddedContext(prepareOrder.first, prepareOrder.second, listOf())
        )
    }

    @Test
    @DisplayName("Новый статус сегмента: пришел 47чп")
    fun waybillStatusChangeAnother47() {
        val prepareOrder = prepareCheckpointAndOrder(isExpress = true, with47 = true)
        prepareOrder.first.apply { status = SegmentStatus.TRANSIT_UPDATED_BY_DELIVERY }

        processor.waybillSegmentStatusAdded(
            LomWaybillStatusAddedContext(prepareOrder.first, prepareOrder.second, listOf())
        )
    }

    @Test
    @DisplayName("Новый статус сегмента: чп для другого сегмента")
    fun waybillStatusChangeAnotherSegment() {
        val prepareOrder = prepareCheckpointAndOrder(isExpress = true, with47 = true)

        prepareOrder.first.apply { waybillSegment = WaybillSegment(id = 1) }

        processor.waybillSegmentStatusAdded(
            LomWaybillStatusAddedContext(prepareOrder.first, prepareOrder.second, listOf())
        )
    }

    // Новый статус заказа

    @DisplayName("Новый статус заказа: успех")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST", "RETURNING"]
    )
    fun successOrderStatusChange(status: OrderStatus) {
        processor.lomOrderStatusChanged(
            LomOrderStatusChangedContext(
                prepareCheckpointAndOrder(isExpress = true, with47 = true).second,
                status,
                listOf()
            )
        )
        verify(issueLinkService).close("extId", IssueLinkEntityType.ORDER, IssueLinkReason.RECALL_COURIER)
    }

    @Test
    @DisplayName("Новый статус заказа: заказ не экспресс")
    fun orderStatusChangeNotExpress() {
        processor.lomOrderStatusChanged(
            LomOrderStatusChangedContext(
                prepareCheckpointAndOrder(isExpress = false, with47 = true).second,
                OrderStatus.LOST,
                listOf()
            )
        )
    }

    @Test
    @DisplayName("Новый статус заказа: не было 47чп")
    fun orderStatusChangeWithout47() {
        processor.lomOrderStatusChanged(
            LomOrderStatusChangedContext(
                prepareCheckpointAndOrder(isExpress = true, with47 = false).second,
                OrderStatus.LOST,
                listOf()
            )
        )
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST", "RETURNING"]
    )
    @DisplayName("Новый статус заказа: статус не финальный")
    fun orderStatusChangeNotFinalStatus(status: OrderStatus) {
        processor.lomOrderStatusChanged(
            LomOrderStatusChangedContext(
                prepareCheckpointAndOrder(isExpress = true, with47 = true).second,
                status,
                listOf()
            )
        )
    }

    private fun prepareCheckpointAndOrder(
        isExpress: Boolean,
        with47: Boolean
    ): Pair<WaybillSegmentStatusHistory, LomOrder> {
        val previousSegment = WaybillSegment(id = 1, segmentType = SegmentType.FULFILLMENT)
        val cp47 = WaybillSegmentStatusHistory(
            status = if (with47) SegmentStatus.TRANSIT_UPDATED_BY_DELIVERY else SegmentStatus.INFO_RECEIVED,
            date = TRANSIT_UPDATED_BY_DELIVERY_TIME
        )
        val not47Cp = WaybillSegmentStatusHistory(
            status = SegmentStatus.TRACK_RECEIVED,
            date = Instant.parse("2021-11-21T11:00:00.00Z")
        )
        val callCourierSegment = WaybillSegment(
            id = 2,
            segmentType = SegmentType.FULFILLMENT,
            waybillSegmentTags = if (isExpress) mutableSetOf(WaybillSegmentTag.CALL_COURIER) else null
        )
            .apply {
                waybillSegmentStatusHistory = mutableSetOf(cp47, not47Cp)
            }

        cp47.apply { waybillSegment = callCourierSegment }
        not47Cp.apply { waybillSegment = callCourierSegment }

        val newCheckpoint = WaybillSegmentStatusHistory(
            status = SegmentStatus.INFO_RECEIVED,
            date = TRANSIT_UPDATED_BY_DELIVERY_TIME.plusSeconds(10000)
        )
            .apply { waybillSegment = callCourierSegment }

        val order = LomOrder()
            .apply { waybill = mutableListOf(previousSegment, callCourierSegment); externalId = "extId" }
        return newCheckpoint to order
    }

    companion object {
        val TRANSIT_UPDATED_BY_DELIVERY_TIME: Instant = Instant.parse("2021-12-21T11:00:00.00Z")!!
    }
}
