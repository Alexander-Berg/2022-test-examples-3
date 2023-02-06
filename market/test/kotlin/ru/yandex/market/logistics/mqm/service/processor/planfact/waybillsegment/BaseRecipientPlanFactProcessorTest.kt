package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamResponse
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.DeliveryInterval
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderDeliveryDateChangedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderLastMileChangedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.service.geobase.GeoBaseClientService
import ru.yandex.market.logistics.mqm.service.lms.LmsPartnerService
import ru.yandex.market.logistics.mqm.service.logging.LogService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

@ExtendWith(MockitoExtension::class)
abstract class BaseRecipientPlanFactProcessorTest {
    @Mock
    protected lateinit var planFactService: PlanFactService

    @Mock
    protected lateinit var lmsPartnerService: LmsPartnerService

    @Mock
    protected lateinit var geoBaseClientService: GeoBaseClientService

    @Mock
    protected lateinit var logService: LogService

    protected val clock = TestableClock()

    @BeforeEach
    fun setUp() {
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Успешное создание план-факта без дедлайна СД в LMS")
    @Test
    fun createPlanFactWithoutLmsSettings() {
        val context = createWaybillSegmentAddedContext()
        getProcessor().waybillSegmentStatusAdded(context)
        verify(lmsPartnerService).getPartnerExternalParam(
            PARTNER_ID,
            getParamType()
        )
        verifyPlanFactWithoutLmsSettingsSaved()
    }

    @DisplayName("Успешное создание план-факта без дедлайна СД в LMS по 20 чп")
    @Test
    fun createPlanFactWithoutLmsSettings20checkpoint() {
        val context = createWaybillSegmentAddedContext(SegmentStatus.TRANSIT_DELIVERY_AT_START_SORT)
        getProcessor().waybillSegmentStatusAdded(context)
        verify(lmsPartnerService).getPartnerExternalParam(
            PARTNER_ID,
            getParamType()
        )
        verifyPlanFactWithoutLmsSettingsSaved()
    }

    @DisplayName("Успешное создание план-факта без дедлайна СД в LMS по 30 чп")
    @Test
    fun createPlanFactWithoutLmsSettings30checkpoint() {
        val context = createWaybillSegmentAddedContext(SegmentStatus.TRANSIT_DELIVERY_TRANSPORTATION)
        getProcessor().waybillSegmentStatusAdded(context)
        verify(lmsPartnerService).getPartnerExternalParam(
            PARTNER_ID,
            getParamType()
        )
        verifyPlanFactWithoutLmsSettingsSaved()
    }


    @DisplayName("Успешное создание план-факта с дедлайном, когда 10 чп пришел до ПДД и до дедлайна СД в LMS")
    @Test
    fun createPlanFactIfCheckpointIsBeforeDeliveryDateAndBeforeLmsDeadline() {
        whenever(
            lmsPartnerService.getPartnerExternalParam(
                PARTNER_ID,
                getParamType()
            )
        ).thenReturn(PartnerExternalParamResponse.newBuilder().value("15:00:00").build())
        val context = createWaybillSegmentAddedContext(
            checkpointTime = FIXED_TIME.minusSeconds(10)
        )
        getProcessor().waybillSegmentStatusAdded(context)
        verifyPlanFactExpectedTime(Instant.parse("2021-12-25T12:00:00.00Z"))
    }

    @DisplayName("Успешное создание план-факта с дедлайном когда 10 чп пришел до ПДД и после дедлайна СД в LMS")
    @Test
    fun createPlanFactIfCheckpointIsBeforeDeliveryDateAndAfterLmsDeadline() {
        whenever(
            lmsPartnerService.getPartnerExternalParam(
                PARTNER_ID,
                getParamType()
            )
        ).thenReturn(PartnerExternalParamResponse.newBuilder().value("14:00:00").build())
        val context = createWaybillSegmentAddedContext(
            checkpointTime = FIXED_TIME.minusSeconds(10)
        )
        getProcessor().waybillSegmentStatusAdded(context)
        verifyPlanFactExpectedTime(Instant.parse("2021-12-25T11:00:00.00Z"))
    }

    @DisplayName("Успешное создание план-факта с дедлайном, когда 10 чп пришел в ПДД и после дедлайна СД в LMS")
    @Test
    fun createPlanFactIfCheckpointIsAtDeliveryDateAndAfterLmsDeadline() {
        whenever(
            lmsPartnerService.getPartnerExternalParam(
                PARTNER_ID,
                getParamType()
            )
        ).thenReturn(PartnerExternalParamResponse.newBuilder().value("15:00:00").build())
        val context = createWaybillSegmentAddedContext(
            checkpointTime = Instant.parse("2021-12-25T12:10:00.00Z")
        )
        getProcessor().waybillSegmentStatusAdded(context)
        verifyPlanFactExpectedTime(Instant.parse("2021-12-26T12:00:00.00Z"))
    }

    @DisplayName("Успешное создание план-факта с дедлайном, когда 10 чп пришел после ПДД и до дедлайна СД в LMS")
    @Test
    fun createPlanFactIfCheckpointIsAfterDeliveryDateAndBeforeLmsDeadline() {
        whenever(
            lmsPartnerService.getPartnerExternalParam(
                PARTNER_ID,
                getParamType()
            )
        ).thenReturn(PartnerExternalParamResponse.newBuilder().value("15:00:00").build())
        val context = createWaybillSegmentAddedContext(
            checkpointTime = Instant.parse("2021-12-26T11:50:00.00Z")
        )
        getProcessor().waybillSegmentStatusAdded(context)
        verifyPlanFactExpectedTime(Instant.parse("2021-12-26T12:00:00.00Z"))
    }

    @DisplayName("Успешное создание план-факта с дедлайном, когда 10 чп пришел после ПДД и после дедлайна СД в LMS")
    @Test
    fun createPlanFactIfCheckpointIsAfterDeliveryDateAndAfterLmsDeadline() {
        whenever(
            lmsPartnerService.getPartnerExternalParam(
                PARTNER_ID,
                getParamType()
            )
        ).thenReturn(PartnerExternalParamResponse.newBuilder().value("15:00:00").build())
        val context = createWaybillSegmentAddedContext(
            checkpointTime = Instant.parse("2021-12-26T12:10:00.00Z")
        )
        getProcessor().waybillSegmentStatusAdded(context)
        verifyPlanFactExpectedTime(Instant.parse("2021-12-27T12:00:00.00Z"))
    }

    @DisplayName("Не создавать план-факт если чекпоинт не IN")
    @Test
    fun doNotCreatePlanFactIfCheckpointIsInvalid() {
        val context = createWaybillSegmentAddedContext()
        context.checkpoint.status = SegmentStatus.OUT
        getProcessor().waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    @DisplayName("Пометить существующий план-факт как OUTDATED и сохранить новый")
    @Test
    fun markOutdatedAndSaveNewPlanFact() {
        val context = createWaybillSegmentAddedContext(withPlanFacts = true)
        val oldPlanFact = context.getPlanFactsFromProcessor(getProcessor().producerName()).first()
        oldPlanFact.expectedStatusDatetime = EXPECTED_TIME_NO_LMS_SETTINGS.minusSeconds(10)
        getProcessor().waybillSegmentStatusAdded(context)
        verifyPlanFactWithoutLmsSettingsSaved()
        assertSoftly {
            oldPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
        }
    }

    @DisplayName("Пометить план-факт IN_TIME")
    @Test
    fun markInTime() {
        val context = createWaybillSegmentAddedContext(
            checkpoint = getExpectedStatus(),
            withPlanFacts = true,
        )
        val planFact = context.getPlanFactsFromProcessor(getProcessor().producerName()).first()
        getProcessor().waybillSegmentStatusAdded(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.IN_TIME
        }
    }

    @DisplayName("Пометить план-факт NOT_ACTUAL")
    @Test
    fun markNotActual() {
        val context = createWaybillSegmentAddedContext(
            checkpoint = getExpectedStatus(),
            checkpointTime = EXPECTED_TIME_NO_LMS_SETTINGS.plusSeconds(10),
            withPlanFacts = true,
        )
        val planFact = context.getPlanFactsFromProcessor(getProcessor().producerName()).first()
        getProcessor().waybillSegmentStatusAdded(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @DisplayName("Пометить план-факт EXPIRED по приходу финального статуса заказа до плана")
    @ParameterizedTest
    @EnumSource(
        OrderStatus::class,
        names = ["RETURNED", "LOST", "DELIVERED", "RETURNING"]
    )
    fun markExpiredOnCloseOrderStatus(orderStatus: OrderStatus) {
        val context = createLomOrderEventContext(orderStatus = orderStatus)
        val planFact = context.getPlanFactsFromProcessor(getProcessor().producerName()).first()
        getProcessor().lomOrderStatusChanged(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
        }
    }

    @DisplayName("Пометить план-факт NOT_ACTUAL по приходу финального статуса заказа после плана")
    @ParameterizedTest
    @EnumSource(
        OrderStatus::class,
        names = ["RETURNED", "LOST", "DELIVERED", "RETURNING"]
    )
    fun markNotActualOnCloseOrderStatus(orderStatus: OrderStatus) {
        val context = createLomOrderEventContext(orderStatus = orderStatus)
        val planFact = context.getPlanFactsFromProcessor(getProcessor().producerName()).first()
        planFact.expectedStatusDatetime = FIXED_TIME.minusSeconds(10)
        getProcessor().lomOrderStatusChanged(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @DisplayName("Пересчитать план-факт по событию изменения даты доставки")
    @Test
    fun recalculatePlanFactOnOrderDeliveryDateChanged() {
        val context = createLomOrderDeliveryDateChangedContext()
        val planFact = context.getPlanFactsFromProcessor(getProcessor().producerName()).first()
        planFact.expectedStatusDatetime = EXPECTED_TIME_NO_LMS_SETTINGS.minusSeconds(10)
        getProcessor().lomOrderDeliveryDateChanged(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
        }
        verifyPlanFactWithoutLmsSettingsSaved()
    }

    @DisplayName("Не создавать план-факт, если есть ожидаемый статус")
    @Test
    fun doNotCreatePlanFactIfStatusExists() {
        val context = createWaybillSegmentAddedContext()
        val segment = context.order.waybill.last()
        writeWaybillSegmentCheckpoint(segment, getExpectedStatus(), FIXED_TIME)
        getProcessor().waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    @DisplayName("Пометить план-факт NOT_ACTUAL, если пришел статус CANCELLED")
    @Test
    fun markNotActualIfCancelled() {
        val context = createWaybillSegmentAddedContext(
            checkpoint = SegmentStatus.CANCELLED,
            checkpointTime = EXPECTED_TIME_NO_LMS_SETTINGS.plusSeconds(10),
            withPlanFacts = true,
        )
        val planFact = context.getPlanFactsFromProcessor(getProcessor().producerName()).first()
        getProcessor().waybillSegmentStatusAdded(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @DisplayName("Обработать план-факт по событию изменения типа последней мили")
    @Test
    fun processPlanFactOnOrderLastMileChanged() {
        val context = createLomOrderLastMileChangedContext()
        val planFact = context.getPlanFactsFromProcessor(getProcessor().producerName()).first()
        planFact.expectedStatusDatetime = FIXED_TIME.minusSeconds(10)
        getProcessor().lomOrderLastMileChanged(context)
        assertSoftly {
            planFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
        verifyPlanFactWithoutLmsSettingsSaved()
    }

    protected fun verifyPlanFactExpectedTime(expectedTime: Instant) {
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.expectedStatusDatetime shouldBe expectedTime
            planFact.scheduleTime shouldBe expectedTime
        }
    }

    protected fun verifyPlanFactWithoutLmsSettingsSaved() {
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.expectedStatus shouldBe getExpectedStatus().name
            planFact.expectedStatusDatetime shouldBe EXPECTED_TIME_NO_LMS_SETTINGS
            planFact.producerName shouldBe getProcessor().producerName()
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe EXPECTED_TIME_NO_LMS_SETTINGS
        }
    }

    protected fun createPreviousSegment(
        partnerSubtype: PartnerSubtype = PartnerSubtype.MARKET_COURIER,
        segmentType: SegmentType = SegmentType.MOVEMENT,
    ) = WaybillSegment(
        id = 1,
        partnerSubtype = partnerSubtype,
        segmentType = segmentType,
    )

    protected fun createLastMileSegment(
        lastMileType: SegmentType = SegmentType.COURIER,
    ) = WaybillSegment(
        id = 2,
        partnerId = PARTNER_ID,
        partnerType = PartnerType.DELIVERY,
        segmentType = lastMileType,
    )

    protected fun createOrder(
        lastMileType: SegmentType = SegmentType.COURIER,
        previousSegment: WaybillSegment = createPreviousSegment(),
        lastMileSegment: WaybillSegment = createLastMileSegment(lastMileType = lastMileType),
        platformClient: PlatformClient = PlatformClient.BERU,
    ): LomOrder {
        val order = joinInOrder(listOf(previousSegment, lastMileSegment))
            .apply {
                deliveryInterval = DEFAULT_INTERVAL
                platformClientId = platformClient.id
            }
        return order
    }

    protected fun createBasePlanFact(entityId: Long) = PlanFact(
        producerName = getProcessor().producerName(),
        expectedStatus = getExpectedStatus().name,
        planFactStatus = PlanFactStatus.CREATED,
        entityType = EntityType.LOM_WAYBILL_SEGMENT,
        entityId = entityId,
        expectedStatusDatetime = EXPECTED_TIME_NO_LMS_SETTINGS
    )

    protected abstract fun getProcessor(): BaseRecipientPlanFactProcessor

    protected abstract fun getParamType(): PartnerExternalParamType

    protected abstract fun getExpectedStatus(): SegmentStatus

    protected abstract fun createWaybillSegmentAddedContext(
        checkpoint: SegmentStatus = SegmentStatus.IN,
        checkpointTime: Instant = FIXED_TIME,
        withPlanFacts: Boolean = false,
    ): LomWaybillStatusAddedContext

    protected abstract fun createLomOrderEventContext(
        orderStatus: OrderStatus,
    ): LomOrderStatusChangedContext

    protected abstract fun createLomOrderDeliveryDateChangedContext(): LomOrderDeliveryDateChangedContext

    protected abstract fun createLomOrderLastMileChangedContext(): LomOrderLastMileChangedContext

    protected data class StorageContext(val lomOrder: LomOrder, val planFact: PlanFact)

    companion object {
        @JvmStatic
        protected val PARTNER_ID = 123L

        @JvmStatic
        protected val FIXED_TIME = Instant.parse("2021-12-21T12:00:00.00Z")

        @JvmStatic
        protected val DEFAULT_INTERVAL = DeliveryInterval(
            deliveryDateMax = LocalDate.of(2021, 12, 25),
        )

        @JvmStatic
        protected val EXPECTED_TIME_NO_LMS_SETTINGS = Instant.parse("2021-12-25T20:59:59Z")
    }
}
