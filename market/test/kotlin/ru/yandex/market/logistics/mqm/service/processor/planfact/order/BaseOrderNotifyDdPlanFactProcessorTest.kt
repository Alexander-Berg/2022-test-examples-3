package ru.yandex.market.logistics.mqm.service.processor.planfact.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.common.util.date.TestableClock
import ru.yandex.geobase.beans.GeobaseRegionData
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.configuration.properties.NotifyDeliveryDateProperties
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.ChangeOrderRequest
import ru.yandex.market.logistics.mqm.entity.lom.ChangeOrderRequestPayload
import ru.yandex.market.logistics.mqm.entity.lom.LomOrder
import ru.yandex.market.logistics.mqm.entity.lom.PartnerSettings
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.DeliveryInterval
import ru.yandex.market.logistics.mqm.entity.lom.embedded.Recipient
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.ChangeOrderRequestReason
import ru.yandex.market.logistics.mqm.entity.lom.enums.ChangeOrderRequestType
import ru.yandex.market.logistics.mqm.entity.lom.enums.DeliveryType
import ru.yandex.market.logistics.mqm.entity.lom.enums.OrderStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.ChangeOrderRequestService
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderCombinatorRouteWasUpdatedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderDeliveryDateChangedContext
import ru.yandex.market.logistics.mqm.service.event.order.LomOrderStatusChangedContext
import ru.yandex.market.logistics.mqm.service.geobase.GeoBaseClientService
import ru.yandex.market.logistics.mqm.service.processor.settings.PlanFactProcessorSettingService
import ru.yandex.market.logistics.mqm.utils.TEST_ORDER_ID
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.createFFSegment
import ru.yandex.market.logistics.mqm.utils.createMkSegment
import ru.yandex.market.logistics.mqm.utils.createPvzOrder
import ru.yandex.market.logistics.mqm.utils.createPvzSegment
import ru.yandex.market.logistics.mqm.utils.createScMkSegment
import ru.yandex.market.logistics.mqm.utils.getMkSegment
import ru.yandex.market.logistics.mqm.utils.getPreviousSegment
import ru.yandex.market.logistics.mqm.utils.overrideOrderWaybill
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

@ExtendWith(MockitoExtension::class)
abstract class BaseOrderNotifyDdPlanFactProcessorTest {
    private val settingsService = TestableSettingsService()

    private val clock = TestableClock()

    @Mock
    protected lateinit var planFactService: PlanFactService

    @Mock
    private lateinit var geoBaseClientService: GeoBaseClientService

    @Mock
    private lateinit var changeOrderRequestService: ChangeOrderRequestService

    private var objectMapper: ObjectMapper = ObjectMapper()

    @BeforeEach
    fun setUp() {
        clock.setFixed(getFixedTime(), DateTimeUtils.MOSCOW_ZONE)
    }

    // Создание ПФ.

    @Test
    @DisplayName("Проверка создания план-факта")
    fun planFactCreationSuccess() {
        val context = mockLomOrderEventContext()

        mockServices()

        val expectedStatusDatetime = expectedStatusDatetime().plusSeconds(EXPECTED_TIME_SHIFT_SEC)

        setupProcessor().lomOrderStatusChanged(context)

        val planFact = getSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_ORDER
            planFact.entityId shouldBe TEST_ORDER_ID
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe expectedStatusDatetime
            planFact.producerName shouldBe getProducerName()
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe expectedStatusDatetime
        }
    }

    // Проверки обработки при существующих ПФ.

    @Test
    @DisplayName("Если существует план-факт с таким-же планом, то новый план-факт не сохраняется")
    fun doNotSaveNewIfExistsSame() {
        val lomOrder = createLomOrder()
        val expectedStatusDatetime = expectedStatusDatetime().plusSeconds(EXPECTED_TIME_SHIFT_SEC)
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = expectedStatusDatetime,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        mockServices()

        val context = mockLomOrderEventContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        setupProcessor().lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.CREATED
    }

    @Test
    @DisplayName("Если существует план-факт с другим планом, то он помечается как OUTDATED")
    fun markOldPlanFactOutdated() {
        val lomOrder = createLomOrder()
        val expectedStatusDatetime = expectedStatusDatetime().minusSeconds(1)
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = expectedStatusDatetime,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        mockServices()

        val context = mockLomOrderEventContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        setupProcessor().lomOrderStatusChanged(context)

        verify(planFactService).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED
    }

    @Test
    @DisplayName("Если план нового пф в прошлом, то новый пф не сохранится, а старый не закроется")
    fun doNotSaveNewIfPlanInThePast() {
        val lomOrder = createLomOrder()
        val expectedStatusDatetime = expectedStatusDatetime()
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = expectedStatusDatetime,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)
        clock.setFixed(expectedStatusDatetime.plusSeconds(1), DateTimeUtils.MOSCOW_ZONE)
        mockServices()

        val context = mockLomOrderEventContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        setupProcessor().lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.CREATED
    }

    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        names = [
            "ENQUEUED", "PROCESSING"
        ],
        mode = EnumSource.Mode.EXCLUDE
    )
    @DisplayName("План-факт не создается, если статус заказа не [ENQUEUED, PROCESSING]")
    fun planFactNotCreationIfNotOrderStatus(orderStatus: OrderStatus) {
        val lomOrder = createLomOrder(orderStatus = orderStatus)

        val context = mockLomOrderEventContext(lomOrder = lomOrder)

        setupProcessor().lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())
    }

    // Проверки создания ПФ, при различных условиях.

    @Test
    @DisplayName("План-факт не создается, если нет ДД")
    fun planFactNotCreationIfNoDd() {
        val lomOrder = createLomOrder()
        lomOrder.deliveryInterval = DeliveryInterval()
        val context = mockLomOrderEventContext(lomOrder = lomOrder)

        assertThrows<IllegalStateException> { setupProcessor().lomOrderStatusChanged(context) }

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("План-факт создается, если изменилась ДД")
    fun planFactCreateIfDdChanged() {
        val lomOrder = createLomOrder()
        val expectedStatusDatetime = expectedStatusDatetime().plusSeconds(EXPECTED_TIME_SHIFT_SEC)
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = expectedStatusDatetime().minusSeconds(1),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val changeOrderRequest = createChangeOrderRequest(lomOrder)

        mockServices()

        val context = mockLomOrderDeliveryDateChangedContext(
            lomOrder = lomOrder,
            existingPlanFacts = existingPlanFacts,
            changeOrderRequests = listOf(changeOrderRequest),
        )

        setupProcessor().lomOrderDeliveryDateChanged(context)

        existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED

        val planFact = getSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_ORDER
            planFact.entityId shouldBe TEST_ORDER_ID
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe expectedStatusDatetime
            planFact.producerName shouldBe getProducerName()
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe expectedStatusDatetime
        }
    }

    @Test
    @DisplayName("План-факт не создается, если измениние ДД вызвало изменение маршрута")
    fun planFactNotCreateIfDdChanged() {
        val lomOrder = createLomOrder()
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = expectedStatusDatetime().minusSeconds(1),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val changeOrderRequest = createChangeOrderRequest(
            order = lomOrder,
            requestType = ChangeOrderRequestType.RECALCULATE_ROUTE_DATES,
        )

        val context = mockLomOrderDeliveryDateChangedContext(
            lomOrder = lomOrder,
            existingPlanFacts = existingPlanFacts,
            changeOrderRequests = listOf(changeOrderRequest),
        )

        setupProcessor().lomOrderDeliveryDateChanged(context)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("План-факт не создается, если экспресс")
    fun planFactNotCreationIfExpress() {
        val lomOrder = createLomOrder()
        lomOrder.waybill.first().partnerSettings =  PartnerSettings(dropshipExpress = true)
        val context = mockLomOrderEventContext(lomOrder = lomOrder)

        setupProcessor().lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("План-факт не создается, если был in на МК")
    fun planFactNotCreationIfHasInOnMc() {
        val lomOrder = createLomOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.getMkSegment(),
            status = SegmentStatus.IN,
            checkpointReceivedDatetime = getFixedTime()
        )

        val context = mockLomOrderEventContext(lomOrder = lomOrder)

        setupProcessor().lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())
    }

    // Закрытие ПФ при закрывающих статусах заказа.

    @DisplayName("Проставлять план-факт в EXPIRED, если пришел один из закрывающих статусов заказа до плана")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["CANCELLED", "RETURNING", "RETURNED", "LOST", "DELIVERED"]
    )
    fun setPlanFactExpiredIfCloseStatusCameOnTime(orderStatus: OrderStatus) {
        val lomOrder = createLomOrder(orderStatus = orderStatus)
        val expectedStatusDatetime = expectedStatusDatetime()
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = expectedStatusDatetime,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockLomOrderEventContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        setupProcessor().lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())

        assertSoftly {
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.EXPIRED
        }
    }

    @DisplayName("Проставлять план-факт в NOT_ACTUAL, если опоздал один из закрывающих статусов заказа")
    @ParameterizedTest
    @EnumSource(
        value = OrderStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["CANCELLED", "RETURNING", "RETURNED", "LOST", "DELIVERED"]
    )
    fun setPlanFactNotActualIfCloseStatusCameLate(orderStatus: OrderStatus) {
        val lomOrder = createLomOrder(orderStatus = orderStatus)
        val expectedStatusDatetime = getFixedTime().minusSeconds(1)
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = expectedStatusDatetime,
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockLomOrderEventContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        setupProcessor().lomOrderStatusChanged(context)

        verify(planFactService, never()).save(any())

        assertSoftly {
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @DisplayName("Проставлять план-факт в NOT_ACTUAL, если пришел финальный чекпоинт")
    @Test
    fun setPlanFactNotActualIfFinalCheckpointCame() {
        val lomOrder = createLomOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.maxByOrNull { it.waybillSegmentIndex }!!,
            status = SegmentStatus.TRANSIT_TRANSMITTED_TO_RECIPIENT,
            checkpointReceivedDatetime = getFixedTime()
        )
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = getFixedTime(),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockLomWaybillStatusAddedContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        setupProcessor().waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())

        assertSoftly {
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @DisplayName("Проставлять план-факт в NOT_ACTUAL, если пришел IN на последний сегмент")
    @Test
    fun setPlanFactNotActualIfInOnLastSegment() {
        val lomOrder = createLomOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill.maxByOrNull { it.waybillSegmentIndex }!!,
            status = SegmentStatus.IN,
            checkpointReceivedDatetime = getFixedTime()
        )
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = getFixedTime(),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockLomWaybillStatusAddedContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        setupProcessor().waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())

        assertSoftly {
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @DisplayName("Проставлять план-факт в NOT_ACTUAL, если пришел IN на МК")
    @Test
    fun setPlanFactNotActualIfInOnMc() {
        val lomOrder = createLomOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.getMkSegment(),
            status = SegmentStatus.IN,
            checkpointReceivedDatetime = getFixedTime()
        )
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = getFixedTime(),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockLomWaybillStatusAddedContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        setupProcessor().waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())

        assertSoftly {
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @DisplayName("Проставлять план-факт в NOT_ACTUAL, если пришел IN на СЦ МК")
    @Test
    fun setPlanFactNotActualIfInOnScMc() {
        val lomOrder = createLomOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.getMkSegment().getPreviousSegment(),
            status = SegmentStatus.IN,
            checkpointReceivedDatetime = getFixedTime()
        )
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = getFixedTime(),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockLomWaybillStatusAddedContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        setupProcessor().waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())

        assertSoftly {
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @DisplayName("Проставлять план-факт в NOT_ACTUAL, если пришел OUT на СЦ до СЦ МК")
    @Test
    fun setPlanFactNotActualIfOutOnScBeforeScMc() {
        val lomOrder = createLomOrder()
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.getMkSegment().getPreviousSegment().getPreviousSegment(),
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = getFixedTime()
        )
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = getFixedTime(),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockLomWaybillStatusAddedContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        setupProcessor().waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())

        assertSoftly {
            existingPlanFact.factStatusDatetime shouldBe null
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    @DisplayName("Закрывать план-факт, если пришел OUT на ФФ до СЦ МК")
    @Test
    fun setPlanFactNotActualIfOutOnFfBeforeScMc() {
        val lomOrder = createLomOrder()
        val newWaybill = listOf(createFFSegment(), createScMkSegment(), createMkSegment(), createPvzSegment())
        overrideOrderWaybill(lomOrder, newWaybill)
        writeWaybillSegmentCheckpoint(
            segment = lomOrder.waybill[0],
            status = SegmentStatus.OUT,
            checkpointReceivedDatetime = getFixedTime()
        )
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = getFixedTime(),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = mockLomWaybillStatusAddedContext(lomOrder = lomOrder, existingPlanFacts = existingPlanFacts)

        setupProcessor().waybillSegmentStatusAdded(context)

        verify(planFactService, never()).save(any())

        assertSoftly {
            existingPlanFact.planFactStatus shouldBe PlanFactStatus.NOT_ACTUAL
        }
    }

    // Проверки при изменении маршрута.

    @Test
    @DisplayName("Проверка создания план-факта, если изменился маршрут и есть нужный Cor")
    fun planFactCreationSuccessIfCorExists() {
        val lomOrder = createLomOrder()
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = expectedStatusDatetime().minusSeconds(1),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        mockServices()

        mockChangeOrderRequestService(order = lomOrder)

        val context = createLomOrderCombinatorRouteWasUpdatedContext(
            lomOrder = lomOrder,
            existingPlanFacts = existingPlanFacts,
        )

        setupProcessor().combinatorRouteWasUpdated(context)

        verify(planFactService).save(any())
        existingPlanFact.planFactStatus shouldBe PlanFactStatus.OUTDATED

        val planFact = getSavedPlanFact()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_ORDER
            planFact.entityId shouldBe TEST_ORDER_ID
            planFact.expectedStatus shouldBe EXPECTED_STATUS
            planFact.expectedStatusDatetime shouldBe expectedStatusDatetime().plusSeconds(EXPECTED_TIME_SHIFT_SEC)
            planFact.producerName shouldBe getProducerName()
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe expectedStatusDatetime().plusSeconds(EXPECTED_TIME_SHIFT_SEC)
        }
    }

    @Test
    @DisplayName("План-факт не создается, если изменился маршрут и нет Cor")
    fun planFactNotCreationIfNoCor() {
        val lomOrder = createLomOrder()
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = expectedStatusDatetime().minusSeconds(1),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = createLomOrderCombinatorRouteWasUpdatedContext(
            lomOrder = lomOrder,
            existingPlanFacts = existingPlanFacts,
        )

        assertThrows<IllegalStateException> { setupProcessor().combinatorRouteWasUpdated(context) }

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("План-факт не создается, если изменился маршрут и Cor невидим")
    fun planFactNotCreationIfCorWithOtherType() {
        val lomOrder = createLomOrder()
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = expectedStatusDatetime().minusSeconds(1),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = createLomOrderCombinatorRouteWasUpdatedContext(
            lomOrder = lomOrder,
            existingPlanFacts = existingPlanFacts,
        )

        mockChangeOrderRequestService(order = lomOrder, visible = false)

        setupProcessor().combinatorRouteWasUpdated(context)

        verify(planFactService, never()).save(any())
    }

    @Test
    @DisplayName("План-факт не создается, если изменился маршрут и Cor без нужного идентификатора маршрута")
    fun planFactNotCreationIfCorWithOtherRouteId() {
        val lomOrder = createLomOrder()
        val existingPlanFact = createPlanFact(
            lomOrder = lomOrder,
            expectedTime = expectedStatusDatetime().minusSeconds(1),
        )
        val existingPlanFacts = mutableListOf(existingPlanFact)

        val context = createLomOrderCombinatorRouteWasUpdatedContext(
            lomOrder = lomOrder,
            existingPlanFacts = existingPlanFacts,
        )

        mockChangeOrderRequestService(order = lomOrder, visible = false, orderRouteUuid = LOM_ORDER_ROUTE_UUID_2)

        assertThrows<IllegalStateException> { setupProcessor().combinatorRouteWasUpdated(context) }

        verify(planFactService, never()).save(any())
    }

    // Вспомогательные методы.

    protected abstract fun mockProcessor(
        settingService: PlanFactProcessorSettingService,
        clock: Clock,
        planFactService: PlanFactService,
        geoBaseClientService: GeoBaseClientService,
        properties: NotifyDeliveryDateProperties,
        changeOrderRequestService: ChangeOrderRequestService,
    ): BaseOrderNotifyDdPlanFactProcessor

    protected abstract fun expectedStatusDatetime(): Instant

    protected abstract fun getProducerName(): String

    protected abstract fun getFixedTime(): Instant

    protected abstract fun getPlannedShipmentDatetime(): Instant

    protected abstract fun getDeliveryDate(): LocalDate

    protected fun setupProcessor(): BaseOrderNotifyDdPlanFactProcessor =
        mockProcessor(
            settingsService,
            clock,
            planFactService,
            geoBaseClientService,
            NotifyDeliveryDateProperties(
                notificationInDdEnabled = true,
                notificationPrevDdEnabled = true,
            ),
            changeOrderRequestService,
        )

    private fun mockServices() =
        whenever(geoBaseClientService.getRegion(eq(GEO_ID))).thenReturn(mockGeobaseRegionData())

    private fun getSavedPlanFact(): PlanFact {
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        return planFactsCaptor.firstValue.single()
    }

    private fun createCurrentWaybillSegment(
        shipmentDateTime: Instant? = getPlannedShipmentDatetime(),
        partnerType: PartnerType = PartnerType.FULFILLMENT,
        isFromExpress: Boolean = false,
        isFromOnDemand: Boolean = false,
    ): WaybillSegment {
        val waybillShipment = WaybillShipment(dateTime = shipmentDateTime)
        val segment = WaybillSegment(
            id = 51,
            partnerId = 1,
            partnerType = partnerType,
            segmentType = SegmentType.FULFILLMENT,
            shipment = waybillShipment,
            partnerSettings = PartnerSettings(dropshipExpress = isFromExpress),
        )
        if (isFromOnDemand) {
            segment.apply {
                waybillSegmentTags = mutableSetOf(WaybillSegmentTag.ON_DEMAND)
            }
        }
        return segment
    }

    private fun createNextSegment(): WaybillSegment =
        WaybillSegment(
            id = 52,
            partnerId = 2,
            partnerType = PartnerType.DELIVERY,
            segmentType = SegmentType.COURIER
        )

    private fun createPreviousSegment(partnerType: PartnerType = PartnerType.SORTING_CENTER): WaybillSegment =
        WaybillSegment(
            id = 50,
            partnerId = 1,
            partnerType = partnerType
        )

    protected fun createLomOrder(
        orderStatus: OrderStatus = OrderStatus.ENQUEUED,
        addressGeoId: Int? = GEO_ID,
        deliveryDate: LocalDate? = getDeliveryDate(),
        orderCreatedDate: Instant = ORDER_CREATED_DATE,
        orderRouteUuid: UUID? = LOM_ORDER_ROUTE_UUID,
    ): LomOrder {
        val lomOrder = createPvzOrder()
        lomOrder.apply {
            status = orderStatus
            recipient = Recipient(
                addressGeoId = addressGeoId
            )
            deliveryInterval = DeliveryInterval(
                deliveryDateMax = deliveryDate
            )
            deliveryType = DeliveryType.COURIER
            created = orderCreatedDate
            routeUuid = orderRouteUuid
        }
        return lomOrder
    }

    protected fun mockLomOrderEventContext(
        lomOrder: LomOrder = createLomOrder(),
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
    ) = LomOrderStatusChangedContext(lomOrder, lomOrder.status!!, existingPlanFacts)

    protected fun mockLomWaybillStatusAddedContext(
        lomOrder: LomOrder,
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
    ): LomWaybillStatusAddedContext {
        val checkpoint = lomOrder.waybill.flatMap { it.waybillSegmentStatusHistory }.maxByOrNull { it.created!! }!!
        return LomWaybillStatusAddedContext(checkpoint, lomOrder, existingPlanFacts)
    }

    private fun mockGeobaseRegionData() =
        GeobaseRegionData().apply { tzname = TIME_ZONE }

    private fun createPlanFact(
        lomOrder: LomOrder,
        expectedTime: Instant,
    ): PlanFact {
        return PlanFact(
            entityType = EntityType.LOM_ORDER,
            entityId = lomOrder.id,
            expectedStatus = EXPECTED_STATUS,
            expectedStatusDatetime = expectedTime,
            producerName = getProducerName()
        )
            .markCreated(expectedTime)
            .apply { entity = lomOrder }
    }

    private fun mockLomOrderDeliveryDateChangedContext(
        lomOrder: LomOrder = createLomOrder(),
        changeOrderRequests: List<ChangeOrderRequest>,
        existingPlanFacts: MutableList<PlanFact> = mutableListOf()
    ) = LomOrderDeliveryDateChangedContext(
        order = lomOrder,
        changeOrderRequests = changeOrderRequests,
        existingPlanFacts,
    )

    private fun createChangeOrderRequest(
        order: LomOrder,
        requestType: ChangeOrderRequestType = ChangeOrderRequestType.DELIVERY_DATE,
        reason: ChangeOrderRequestReason = ChangeOrderRequestReason.PRE_DELIVERY_ROUTE_RECALCULATION,
        visible: Boolean? = null,
        orderRouteUuid: UUID? = LOM_ORDER_ROUTE_UUID,
    ): ChangeOrderRequest {
        val changeOrderRequest = ChangeOrderRequest(
            requestType = requestType,
            reason = reason,
        ).apply {
            this.order = order
            if (visible != null) {
                val payload = objectMapper.readTree(""" {"notifyUser": $visible, "routeUuid": "$orderRouteUuid"} """)
                changeOrderRequestPayloads = mutableSetOf(ChangeOrderRequestPayload(payload = payload))
            }
        }
        return changeOrderRequest
    }

    private fun mockChangeOrderRequestService(
        order: LomOrder,
        visible: Boolean = true,
        orderRouteUuid: UUID? = LOM_ORDER_ROUTE_UUID,
    ) {
        whenever(changeOrderRequestService.findByOrderIdWithTypes(eq(TEST_ORDER_ID), any()))
            .thenReturn(
                listOf(
                    createChangeOrderRequest(
                        order = order,
                        requestType = ChangeOrderRequestType.RECALCULATE_ROUTE_DATES,
                        visible = visible,
                        orderRouteUuid = orderRouteUuid,
                        )
                )
            )
    }

    private fun createLomOrderCombinatorRouteWasUpdatedContext(
        lomOrder: LomOrder = createLomOrder(),
        existingPlanFacts: MutableList<PlanFact> = mutableListOf(),
    ) = LomOrderCombinatorRouteWasUpdatedContext(
        order = lomOrder,
        changeOrderRequest = null,
        orderPlanFacts = existingPlanFacts,
    )

    companion object {
        private const val EXPECTED_STATUS = "UNKNOWN"
        private const val EXPECTED_TIME_SHIFT_SEC = ((TEST_ORDER_ID % 60) - 30) * 60
        private const val GEO_ID = 1
        private const val TIME_ZONE = "Asia/Yekaterinburg"
        private val ORDER_CREATED_DATE = Instant.parse("2021-09-29T10:00:00.00Z")
        private val LOM_ORDER_ROUTE_UUID = UUID.fromString("3b762674-a5fc-11ec-b909-0242ac120002")
        private val LOM_ORDER_ROUTE_UUID_2 = UUID.fromString("3b762674-a5fc-11ec-b910-0242ac120002")
    }
}
