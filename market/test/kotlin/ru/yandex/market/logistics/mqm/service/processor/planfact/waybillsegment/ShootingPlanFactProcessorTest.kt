package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import io.kotest.assertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.Recipient
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.ShootingService
import ru.yandex.market.logistics.mqm.service.processor.planfact.BaseSelfSufficientPlanFactTest
import ru.yandex.market.logistics.mqm.service.processor.settings.payload.LomOrderSegmentsCreationAttributes
import ru.yandex.market.logistics.mqm.service.processor.settings.payload.ShootingPlanFactProcessorSettings
import ru.yandex.market.logistics.mqm.service.processor.settings.PlanFactProcessorSettingService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint
import ru.yandex.market.sdk.userinfo.service.UidConstants
import java.time.Duration
import java.time.Instant

class ShootingPlanFactProcessorTest: BaseSelfSufficientPlanFactTest() {
    private lateinit var processor: ShootingPlanFactProcessor

    @Mock
    private lateinit var settingService: PlanFactProcessorSettingService

    @Mock
    private lateinit var shootingService: ShootingService

    @BeforeEach
    fun setUp() {
        processor = ShootingPlanFactProcessor(
            clock = clock,
            planFactService = planFactService,
            settingService = settingService,
            shootingService = shootingService,
        )
    }

    // Создание ПФ и обработка ЧП.

    @Test
    @DisplayName("Создать план-факты, если пришел статус для создания - TRACK_RECEIVED")
    fun planFactsCreationSuccess() {
        val firstSegment = createFirstSegment()
        val lomOrder = createLomOrder(firstSegment = firstSegment)
        writeWaybillSegmentCheckpoint(
            segment = firstSegment,
            status = RECEIVED_STATUS,
            checkpointReceivedDatetime = CURRENT_TIME,
        )
        mockSettings()
        mockShootingService()

        processNewSegmentStatus(processor = processor, order = lomOrder)

        val planFact = captureSavedPlanFact()

        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.entityId shouldBe FIRST_SEGMENT_ID
            planFact.expectedStatus shouldBe EXPECTED_STATUS.name
            planFact.expectedStatusDatetime shouldBe EXPECTED_PLAN_TIME
            planFact.producerName shouldBe ShootingPlanFactProcessor::class.simpleName
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_SCHEDULE_TIME
        }
    }

    // Проверки при обработке заказа.

    @DisplayName("Проставлять план-факт в NOT_ACTUAL, если опоздал один из закрывающих статусов заказа")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["RETURNED", "DELIVERED", "LOST", "RETURNING"]
    )
    fun setPlanFactsNotActualIfCloseStatusCameLate(orderStatus: OrderStatus) {
        val firstSegment = createFirstSegment()
        val lomOrder = createLomOrder(firstSegment = firstSegment)
        val existingPlanFact = createPlanFact(
            waybillSegment = firstSegment,
            expectedTime = EXPECTED_PLAN_TIME.minusSeconds(60),
        )

        processOrderStatusChanged(processor, lomOrder, orderStatus, existingPlanFact)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
    }

    // Вспомогательные методы. Создание.

    private fun mockSettings() {
        whenever(
            settingService.getOrDefault(eq(processor.producerName()), eq(SETTINGS_KEY), any())
        ).thenReturn(
            createSettings()
        )
        whenever(
            settingService.getOrDefault(eq(processor.producerName()), eq(CREATE_PLAN_FACTS_KEY), any())
        ).thenReturn(
            true
        )
        whenever(
            settingService.getOrDefault(eq(processor.producerName()), eq(DELTA_KEY), any())
        ).thenReturn(
            DELTA
        )
    }

    private fun createSettings(): ShootingPlanFactProcessorSettings {
        return ShootingPlanFactProcessorSettings(
            lomOrderSegmentsCreationAttributes = listOf(
                LomOrderSegmentsCreationAttributes(
                    segmentType = SegmentType.FULFILLMENT.name,
                    count = 1,
                    expectedActiveCountFactor = 1.0,
                ),
                LomOrderSegmentsCreationAttributes(
                    segmentType = SegmentType.SORTING_CENTER.name,
                    count = 1,
                    expectedActiveCountFactor = 1.0,
                ),
                LomOrderSegmentsCreationAttributes(
                    segmentType = SegmentType.COURIER.name,
                    count = 1,
                    expectedActiveCountFactor = 1.0,
                ),
            ),
        )
    }

    private fun createLomOrder(
        firstSegment: WaybillSegment = createFirstSegment(),
        secondSegment: WaybillSegment = createSecondSegment(),
        thirdSegment: WaybillSegment = createThirdSegment(),
    ) = joinInOrder(listOf(firstSegment, secondSegment, thirdSegment)).apply {
        id = LOM_ORDER_ID
        recipient = Recipient(uid = UidConstants.NO_SIDE_EFFECT_UID)
    }

    private fun createFirstSegment(
        segmentType: SegmentType = SegmentType.FULFILLMENT,
        partnerType: PartnerType = PartnerType.FULFILLMENT,
    ) =
        WaybillSegment(
            id = FIRST_SEGMENT_ID,
            segmentType = segmentType,
            partnerType = partnerType,
        )

    private fun createSecondSegment(
        segmentType: SegmentType = SegmentType.SORTING_CENTER,
        partnerType: PartnerType = PartnerType.SORTING_CENTER,
    ) =
        WaybillSegment(
            id = SECOND_SEGMENT_ID,
            segmentType = segmentType,
            partnerType = partnerType,
        )

    private fun createThirdSegment(
        segmentType: SegmentType = SegmentType.COURIER,
        partnerType: PartnerType = PartnerType.DELIVERY,
    ) =
        WaybillSegment(
            id = THIRD_SEGMENT_ID,
            segmentType = segmentType,
            partnerType = partnerType,
        )

    private fun createPlanFact(
        expectedTime: Instant = EXPECTED_PLAN_TIME,
        waybillSegment: WaybillSegment,
    ): PlanFact {
        return PlanFact(
            id = FIRST_PLAN_FACT_ID,
            entityId = waybillSegment.id,
            entityType = EntityType.LOM_WAYBILL_SEGMENT,
            planFactStatus = PlanFactStatus.CREATED,
            expectedStatusDatetime = expectedTime,
            expectedStatus = EXPECTED_STATUS.name,
            producerName = ShootingPlanFactProcessor::class.simpleName,
        ).apply { entity = waybillSegment }
    }

    // Вспомогательные методы.

    private fun mockShootingService() {
        whenever(
            shootingService.canProcessShootingOrder(any())
        ).thenReturn(
            true
        )
    }

    companion object {
        private const val SETTINGS_KEY = "SETTINGS"
        private const val LOM_ORDER_ID = 1L
        private const val FIRST_SEGMENT_ID = 51L
        private const val SECOND_SEGMENT_ID = 52L
        private const val THIRD_SEGMENT_ID = 53L
        private const val FIRST_PLAN_FACT_ID = 101L
        private val RECEIVED_STATUS = SegmentStatus.TRACK_RECEIVED
        private val EXPECTED_STATUS = SegmentStatus.OUT
        private val EXPECTED_PLAN_TIME: Instant = Instant.parse("2022-03-18T11:01:00.00Z")
        private val EXPECTED_SCHEDULE_TIME: Instant = Instant.parse("2022-03-18T11:01:00.00Z")
        private const val CREATE_PLAN_FACTS_KEY = "CREATE_PLAN_FACTS"
        private const val DELTA_KEY = "DELTA"
        private val DELTA = Duration.ofMinutes(1)
    }
}
