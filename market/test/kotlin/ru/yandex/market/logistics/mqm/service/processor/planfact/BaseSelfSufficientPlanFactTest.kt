package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.lom.ChangeOrderRequest
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillSegmentStatusAddedListener
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderCombinatorRouteWasUpdatedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderCombinatorRouteWasUpdatedListener
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderLastMileChangedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderLastMileChangedListener
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedListener

@ExtendWith(MockitoExtension::class)
open class BaseSelfSufficientPlanFactTest: AbstractTest() {
    protected val clock = TestableClock()

    @Mock
    protected lateinit var planFactService: PlanFactService

    @BeforeEach
    fun setUpClock() {
        clock.setFixed(CURRENT_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    protected fun processNewSegmentStatus(
        processor: BaseSelfSufficientPlanFactProcessor,
        order: LomOrder,
        planFact: PlanFact? = null,
        checkpoint: WaybillSegmentStatusHistory? = null,
    ) {
        val existingPlanFacts = planFact?.let { mutableListOf(it) } ?: mutableListOf()
        val context = mockSegmentStatusAddedContext(
            lomOrder = order,
            checkpoint = checkpoint,
            existingPlanFacts = existingPlanFacts,
        )
        (processor as LomWaybillSegmentStatusAddedListener).waybillSegmentStatusAdded(context)
    }

    protected fun processOrderStatusChanged(
        processor: BaseSelfSufficientPlanFactProcessor,
        order: LomOrder,
        orderStatus: OrderStatus,
        planFact: PlanFact? = null,
    ) {
        order.status = orderStatus
        val existingPlanFacts = planFact?.let { mutableListOf(it) } ?: mutableListOf()
        val context = mockLomOrderStatusChangedContext(
            lomOrder = order,
            orderStatus = orderStatus,
            existingPlanFacts = existingPlanFacts,
        )
        (processor as LomOrderStatusChangedListener).lomOrderStatusChanged(context)
    }

    protected fun processOrderLastMileChanged(
        processor: BaseSelfSufficientPlanFactProcessor,
        order: LomOrder,
        planFact: PlanFact? = null,
    ) {
        val existingPlanFacts = planFact?.let { mutableListOf(it) } ?: mutableListOf()
        val context = LomOrderLastMileChangedContext(
            order = order,
            changeOrderRequest = null,
            orderPlanFacts = existingPlanFacts,
        )
        (processor as LomOrderLastMileChangedListener).lomOrderLastMileChanged(context)
    }

    protected fun processCombinatorRouteWasUpdated(
        processor: BaseSelfSufficientPlanFactProcessor,
        order: LomOrder,
        changeOrderRequest: ChangeOrderRequest? = null,
        planFact: PlanFact? = null,
    ) {
        val existingPlanFacts = planFact?.let { mutableListOf(it) } ?: mutableListOf()
        val context = LomOrderCombinatorRouteWasUpdatedContext(
            order = order,
            changeOrderRequest = changeOrderRequest,
            orderPlanFacts = existingPlanFacts,
        )
        (processor as LomOrderCombinatorRouteWasUpdatedListener).combinatorRouteWasUpdated(context)
    }

    protected fun captureSavedPlanFact(): PlanFact {
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        return planFactsCaptor.firstValue.single()
    }

    protected fun mockSegmentStatusAddedContext(
        lomOrder: LomOrder,
        checkpoint: WaybillSegmentStatusHistory? = null,
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
    ): LomWaybillStatusAddedContext {
        val newCheckpoint = checkpoint ?: (lomOrder.waybill
            .flatMap { it.waybillSegmentStatusHistory }
            .maxWithOrNull(Comparator.naturalOrder())
            ?: error("Order has no checkpoints"))
        return LomWaybillStatusAddedContext(newCheckpoint, lomOrder, existingPlanFacts)
    }

    protected fun mockLomOrderStatusChangedContext(
        lomOrder: LomOrder,
        orderStatus: OrderStatus = OrderStatus.CANCELLED,
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
    ): LomOrderStatusChangedContext {
        return LomOrderStatusChangedContext(lomOrder, orderStatus, existingPlanFacts)
    }

    protected fun mockLomOrderCombinatorRouteWasUpdatedContext(
        lomOrder: LomOrder,
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
    ) = LomOrderCombinatorRouteWasUpdatedContext(lomOrder, null, existingPlanFacts)

    companion object {
        @JvmStatic
        protected val CURRENT_TIME: Instant = Instant.parse("2022-03-18T11:00:00.00Z")
    }
}
