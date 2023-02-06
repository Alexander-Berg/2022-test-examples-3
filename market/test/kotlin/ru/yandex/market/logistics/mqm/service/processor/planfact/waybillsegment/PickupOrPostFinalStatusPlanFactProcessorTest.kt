package ru.yandex.market.logistics.mqm.service.processor.planfact.waybillsegment

import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.common.util.date.TestableClock
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.enums.EntityType
import ru.yandex.market.logistics.mqm.entity.enums.PlanFactStatus
import ru.yandex.market.logistics.mqm.entity.enums.ProcessingStatus
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.DeliveryType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.service.PlanFactService
import ru.yandex.market.logistics.mqm.service.event.checkpoint.LomWaybillStatusAddedContext
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.stream.Stream

@ExtendWith(MockitoExtension::class)
class PickupOrPostFinalStatusPlanFactProcessorTest {
    @Mock
    private lateinit var planFactService: PlanFactService

    private val clock = TestableClock()

    lateinit var processor: PickupOrPostFinalStatusPlanFactProcessor

    @BeforeEach
    fun setUp() {
        processor = PickupOrPostFinalStatusPlanFactProcessor(planFactService, clock)
        clock.setFixed(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE)
    }

    @DisplayName("Успешное создание план-факта для почты")
    @ParameterizedTest(name = AbstractTest.TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    fun createPlanFactForPost(platformClient: PlatformClient) {
        val context = createLomWaybillStatusAddedContext(
            deliveryType = DeliveryType.POST,
            platformClient = platformClient
        )
        processor.waybillSegmentStatusAdded(context)
        verifyPlanFactSaved(FIXED_TIME.plus(30, ChronoUnit.DAYS))
    }

    @DisplayName("Успешное создание план-факта если подтип партнера валидный")
    @ParameterizedTest
    @MethodSource("getPartnerSubtypesDeadlinesMap")
    fun createPlanFactIfPartnerSubtypeIsValid(subtype: PartnerSubtype, deadline: Instant) {
        val context = createLomWaybillStatusAddedContext(
            partnerSubtype = subtype
        )
        processor.waybillSegmentStatusAdded(context)
        verifyPlanFactSaved(deadline)
    }

    @DisplayName("Успешное создание план-факта если идентификатор партнера валидный")
    @ParameterizedTest
    @MethodSource("getPartnerIdsDeadlinesMap")
    fun createPlanFactIfPartnerIdIsValid(partnerId: Long, deadline: Instant) {
        val context = createLomWaybillStatusAddedContext(
            partnerId = partnerId
        )
        processor.waybillSegmentStatusAdded(context)
        verifyPlanFactSaved(deadline)
    }

    @DisplayName("Успешное создание план-факта если был out на предыдущем сегменте")
    @Test
    fun createPlanFactIfWasOutOnPreviousSegment() {
        val context = createLomWaybillStatusAddedContext(
            deliveryType = DeliveryType.POST
        )
        writeWaybillSegmentCheckpoint(context.order.waybill.first(), SegmentStatus.OUT, FIXED_TIME)
        processor.waybillSegmentStatusAdded(context)
        verifyPlanFactSaved(FIXED_TIME.plus(30, ChronoUnit.DAYS))
    }


    @DisplayName("Не создавать план-факт если пришел не 45ый чекпоинт")
    @ParameterizedTest
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["TRANSIT_PICKUP"]
    )
    fun doNotCreatePlanFactIfCheckpointIsInvalid(segmentStatus: SegmentStatus) {
        val context = createLomWaybillStatusAddedContext(
            checkpoint = segmentStatus
        )
        processor.waybillSegmentStatusAdded(context)
        verifyNoMoreInteractions(planFactService)
    }

    private fun verifyPlanFactSaved(expectedTime: Instant = DEFAULT_EXPECTED_TIME) {
        val planFactsCaptor = argumentCaptor<List<PlanFact>>()
        verify(planFactService).save(planFactsCaptor.capture())
        val planFact = planFactsCaptor.firstValue.single()
        assertSoftly {
            planFact.entityType shouldBe EntityType.LOM_WAYBILL_SEGMENT
            planFact.expectedStatus shouldBe EXPECTED_STATUS.name
            planFact.expectedStatusDatetime shouldBe expectedTime
            planFact.producerName shouldBe processor.producerName()
            planFact.planFactStatus shouldBe PlanFactStatus.CREATED
            planFact.processingStatus shouldBe ProcessingStatus.ENQUEUED
            planFact.scheduleTime shouldBe expectedTime
        }
    }

    private fun createLomWaybillStatusAddedContext(
        checkpoint: SegmentStatus = SegmentStatus.TRANSIT_PICKUP,
        checkpointTime: Instant = FIXED_TIME,
        partnerSubtype: PartnerSubtype = PartnerSubtype.MARKET_OWN_PICKUP_POINT,
        partnerId: Long = DEFAULT_PARTNER_ID,
        deliveryType: DeliveryType = DeliveryType.COURIER,
        planFacts: List<PlanFact> = listOf(),
        platformClient: PlatformClient = PlatformClient.BERU,
    ): LomWaybillStatusAddedContext {
        val newCheckpoint = WaybillSegmentStatusHistory(status = checkpoint, date = checkpointTime)
        val previousSegment = WaybillSegment(id = 1)
        val lastMileSegment = WaybillSegment(
            id = 2,
            partnerId = partnerId,
            partnerType = PartnerType.DELIVERY,
            partnerSubtype = partnerSubtype
        ).apply {
            waybillSegmentStatusHistory = mutableSetOf(newCheckpoint)
            newCheckpoint.waybillSegment = this
            planFacts.forEach { pf -> pf.entity = this }
        }
        val order = joinInOrder(listOf(previousSegment, lastMileSegment))
            .apply {
                this.deliveryType = deliveryType
                platformClientId = platformClient.id
            }
        return LomWaybillStatusAddedContext(newCheckpoint, order, planFacts)
    }

    companion object {
        private const val DEFAULT_PARTNER_ID = 123L
        private const val DPD_PARTNER_ID = 1003939L
        private const val FIVEPOST_PARTNER_ID = 200L
        private val FIXED_TIME = Instant.parse("2021-12-21T12:00:00.00Z")
        private val DEFAULT_EXPECTED_TIME = FIXED_TIME.plus(1, ChronoUnit.DAYS)
        private val EXPECTED_STATUS = SegmentStatus.OUT

        @JvmStatic
        fun getPartnerSubtypesDeadlinesMap() = Stream.of(
            Arguments.of(
                PartnerSubtype.MARKET_OWN_PICKUP_POINT,
                FIXED_TIME.plus(20, ChronoUnit.DAYS)
            ),
            Arguments.of(
                PartnerSubtype.PARTNER_PICKUP_POINT_IP,
                FIXED_TIME.plus(20, ChronoUnit.DAYS)
            ),
            Arguments.of(
                PartnerSubtype.TAXI_LAVKA,
                FIXED_TIME.plus(7, ChronoUnit.DAYS)
            ),
            Arguments.of(
                PartnerSubtype.TAXI_EXPRESS,
                FIXED_TIME.plus(7, ChronoUnit.DAYS)
            ),
            Arguments.of(
                PartnerSubtype.MARKET_LOCKER,
                FIXED_TIME.plus(10, ChronoUnit.DAYS)
            ),
        )

        @JvmStatic
        fun getPartnerIdsDeadlinesMap() = Stream.of(
            Arguments.of(
                DPD_PARTNER_ID,
                FIXED_TIME.plus(18, ChronoUnit.DAYS)
            ),
            Arguments.of(
                FIVEPOST_PARTNER_ID,
                FIXED_TIME.plus(7, ChronoUnit.DAYS)
            )
        )
    }
}
