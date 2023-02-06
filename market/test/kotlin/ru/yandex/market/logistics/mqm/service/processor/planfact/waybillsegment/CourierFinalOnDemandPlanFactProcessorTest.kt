package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint
import java.time.Duration
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class CourierFinalOnDemandPlanFactProcessorTest: BaseSelfSufficientOnDemandPlanFactProcessorTest() {

    private val clock = TestableClock()

    private val settingsService = TestableSettingsService()

    @BeforeEach
    fun setUp() {
        clock.setFixed(getFixedTime(), DateTimeUtils.MOSCOW_ZONE)
    }

    // Проверки создания ПФ, при различных условиях.

    @DisplayName("ПФ не создается, если не МК")
    @Test
    fun planFactNotCreateIfSegmentNotMk() {
        val context = createPlanFactNotCreateIfSegmentNotMkLomWaybillStatusAddedContext()
        val processor = createProcessor()

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    @DisplayName("ПФ не создается, если есть закрывающие статусы")
    @Test
    fun planFactNotCreateIfNextSegmentsWithCloseStatuses() {
        val context = createPlanFactNotCreateIfNextSegmentsWithCloseStatuses()
        val processor = createProcessor()

        processor.waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())
    }

    override fun createProcessor() = CourierFinalOnDemandPlanFactProcessor(
        settingService = settingsService,
        clock = clock,
        planFactService = planFactService,
    )

    override fun getProducerName() = CourierFinalOnDemandPlanFactProcessor::class.simpleName!!

    override fun getFixedTime(): Instant = FIXED_TIME

    override fun getExpectedTime(): Instant = EXPECTED_TIME

    override fun createSuccessPlanFact() =
        PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            entityId = 100_102,
            expectedStatus = SegmentStatus.OUT.name,
            expectedStatusDatetime = getExpectedTime(),
            producerName = getProducerName(),
            planFactStatus = PlanFactStatus.CREATED,
            processingStatus = ProcessingStatus.ENQUEUED,
            scheduleTime = getExpectedTime(),
        )

    override fun createPlanFactCreationSuccessLomWaybillStatusAddedContext(): LomWaybillStatusAddedContext =
        createSegmentStatusAddedContext()

    override fun createDoNotSaveNewIfExistsSameLomWaybillStatusAddedContext(): LomWaybillStatusAddedContext =
        createSegmentStatusAddedContext(withExistingPlanFact = true)

    override fun createMarkExistsPlanFactOutdatedLomWaybillStatusAddedContext(): LomWaybillStatusAddedContext =
        createSegmentStatusAddedContext(
            withExistingPlanFact = true,
            expectedTime = getExpectedTime().minusSeconds(1),
        )

    override fun createSetPlanFactInTimeLomWaybillStatusAddedContext(): LomWaybillStatusAddedContext =
        createSegmentStatusAddedContext(
            withExistingPlanFact = true,
            newSegmentStatus = SegmentStatus.OUT,
            newCheckpointTime = IN_TIME_CHECKPOINT_TIME,
        )

    override fun createSetPlanFactNotActualLomWaybillStatusAddedContext(): LomWaybillStatusAddedContext =
        createSegmentStatusAddedContext(
            withExistingPlanFact = true,
            newSegmentStatus = SegmentStatus.OUT,
            newCheckpointTime = NOT_ACTUAL_TIME_CHECKPOINT_TIME,
        )

    override fun createClosePlanFactIfNextSegmentStatusLomWaybillStatusAddedContext(): LomWaybillStatusAddedContext =
        createSegmentStatusAddedContext(
            withExistingPlanFact = true,
            newSegmentStatus = SegmentStatus.RETURN_PREPARING,
            newCheckpointTime = NOT_ACTUAL_TIME_CHECKPOINT_TIME,
        )

    override fun createLomOrderStatusChangedContext(
        orderStatus: OrderStatus,
        expectedTime: Instant,
    ): LomOrderStatusChangedContext {
        val secondSegment = createSecondSegment()
        val lomOrder = createLomOrder(secondSegment = secondSegment)
        val existingPlanFact = createExistsPlanFact(waybillSegment = secondSegment, expectedTime = expectedTime)
        val existingPlanFacts = mutableListOf(existingPlanFact)
        return LomOrderStatusChangedContext(lomOrder, orderStatus, existingPlanFacts)
    }

    private fun createPlanFactNotCreateIfSegmentNotMkLomWaybillStatusAddedContext(): LomWaybillStatusAddedContext =
        createSegmentStatusAddedContext(segmentType = SegmentType.FULFILLMENT)

    private fun createPlanFactNotCreateIfNextSegmentsWithCloseStatuses(): LomWaybillStatusAddedContext =
        createSegmentStatusAddedContext(withCloseStatusInNextSegment = true)

    // Вспомогательные методы.

    private fun createSegmentStatusAddedContext(
        withExistingPlanFact: Boolean = false,
        expectedTime: Instant = getExpectedTime(),
        newSegmentStatus: SegmentStatus = SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT,
        newCheckpointTime: Instant = CHECKPOINT_TIME,
        segmentType: SegmentType = SegmentType.MOVEMENT,
        withCloseStatusInNextSegment: Boolean = false,
    ): LomWaybillStatusAddedContext {
        val firstSegment = createFirstSegment(segmentType = segmentType)
        val secondSegment = createSecondSegment()
        val thirdSegment = createThirdSegment()
        val lomOrder =
            createLomOrder(firstSegment = firstSegment, secondSegment = secondSegment, thirdSegment = thirdSegment)
        val checkPoint = createNewCheckpoint(
            newSegmentStatus = newSegmentStatus,
            newCheckpointTime = newCheckpointTime,
            waybillSegment = secondSegment,
        )
        val existingPlanFacts = mutableListOf<PlanFact>()
        if (withExistingPlanFact) {
            val existingPlanFact =
                createExistsPlanFact(waybillSegment = secondSegment, expectedTime = expectedTime)
            existingPlanFacts.add(existingPlanFact)
        }
        if (withCloseStatusInNextSegment) {
            writeWaybillSegmentCheckpoint(thirdSegment, SegmentStatus.RETURN_PREPARING, CHECKPOINT_TIME)
        }
        return createSegmentStatusAddedContext(
            lomOrder = lomOrder,
            newCheckpoint = checkPoint,
            existingPlanFacts = existingPlanFacts,
        )
    }

    private fun createLomOrder(
        firstSegment: WaybillSegment = createFirstSegment(),
        secondSegment: WaybillSegment = createSecondSegment(),
        thirdSegment: WaybillSegment = createThirdSegment(),
    ) = joinInOrder(listOf(firstSegment, secondSegment, thirdSegment)).apply { id = 1 }

    private fun createFirstSegment(
        segmentType: SegmentType = SegmentType.MOVEMENT,
    ) =
        WaybillSegment(
            id = 100_101,
            partnerType = PartnerType.DELIVERY,
            segmentType = segmentType,
            partnerSubtype = PartnerSubtype.MARKET_COURIER,
        ).apply {
            waybillSegmentTags = mutableSetOf(WaybillSegmentTag.ON_DEMAND)
        }

    private fun createSecondSegment(
        segmentType: SegmentType = SegmentType.COURIER,
    ) =
        WaybillSegment(
            id = 100_102,
            partnerType = PartnerType.DELIVERY,
            segmentType = segmentType,
        )

    private fun createThirdSegment() =
        WaybillSegment(
            id = 100_103,
        )

    private fun createExistsPlanFact(
        waybillSegment: WaybillSegment,
        expectedTime: Instant,
    ): PlanFact {
        return PlanFact(
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            id = 1000_102,
            entityId = 100_102,
            expectedStatus = SegmentStatus.OUT.name,
            planFactStatus = PlanFactStatus.CREATED,
            expectedStatusDatetime = expectedTime,
            producerName = getProducerName(),
        ).apply { entity = waybillSegment }
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2021-01-03T15:00:00.00Z")
        private val CHECKPOINT_TIME = FIXED_TIME
        private val DEFAULT_TIMEOUT = Duration.ofHours(24)
        private val EXPECTED_TIME = FIXED_TIME.plus(DEFAULT_TIMEOUT)
        private val IN_TIME_CHECKPOINT_TIME = FIXED_TIME
        private val NOT_ACTUAL_TIME_CHECKPOINT_TIME = EXPECTED_TIME
    }
}
