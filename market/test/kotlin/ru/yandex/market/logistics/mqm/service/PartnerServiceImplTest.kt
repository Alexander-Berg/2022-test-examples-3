package ru.yandex.market.logistics.mqm.service

import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.LocalDate
import java.time.Duration
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.management.client.LMSClient
import ru.yandex.market.logistics.management.entity.response.CutoffResponse
import ru.yandex.market.logistics.management.entity.response.partnerRelation.PartnerRelationEntityDto
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse
import ru.yandex.market.logistics.mqm.configuration.properties.PartnerServiceProperties
import ru.yandex.market.logistics.mqm.entity.lom.LomOrderCombinatorRoute
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PointType
import ru.yandex.market.logistics.mqm.entity.lom.enums.ServiceCodeName
import ru.yandex.market.logistics.mqm.service.lms.LmsPartnerService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.util.Optional

import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.service.management.PartnerRelationParamsService

@ExtendWith(MockitoExtension::class)
class PartnerServiceImplTest {

    @Mock
    private lateinit var combinatorRouteService: CombinatorRouteService

    @Mock
    private lateinit var lmsPartnerService: LmsPartnerService

    @Mock
    private lateinit var lmsClient: LMSClient

    @Mock
    private lateinit var partnerRelationParamsService: PartnerRelationParamsService

    @Test
    @DisplayName("Проверка корректного получения времени по расписанию")
    fun successFindScheduleWindowTime() {
        val service = prepareService(lmsPartnerServicePartnerRelationOptional = false)
        val segment = createWaybillSegment()
        joinInOrder(listOf(segment, createNextWaybillSegment())).apply { id = ORDER_ID }
        service.findScheduleWindowTime(segment, FIXED_LOCAL_DATE) shouldBe PartnerService.TimeWindow(
            startTime = LocalTime.ofSecondOfDay(SCHEDULE_START_SECONDS),
            endTime = LocalTime.ofSecondOfDay(SCHEDULE_END_SECONDS)
        )
    }

    @Test
    @DisplayName("Проверка корректного получения дельты")
    fun successFindIntakeDeadline() {
        val service = prepareService(lmsPartnerServicePartnerRelationOptional = true)
        val segment = createWaybillSegment()
        joinInOrder(listOf(segment, createNextWaybillSegment())).apply { id = ORDER_ID }
        service.findIntakeDeadline(segment) shouldBe Duration.ofMinutes(DURATION_DELTA.toLong())
    }

    @Test
    @DisplayName("Проверка корректного получения Cutoff, если партнер DROPSHIP")
    fun successFindCutoffTimeForDropship() {
        val service = prepareService(
            combinatorRoute = createCombinatorRouteForDsCutoff(
                movementTime = MOVEMENT_TIME,
            ),
            partnerRelationEntityDto = createPartnerRelationEntityDto(
                partnerIdFrom = 301L,
                partnerIdTo = PARTNER_TO_ID,
            ),
        )
        val segment = createWaybillSegment(
            partnerId = 301L,
            segmentId = 1001L,
            partnerType = PartnerType.DROPSHIP,
        )
        joinInOrder(listOf(segment, createNextWaybillSegment())).apply { id = ORDER_ID }
        service.findCutoffTime(segment) shouldBe LocalTime.ofSecondOfDay(DS_CUTOFF_IN_SECONDS)
    }

    @Test
    @DisplayName("Проверка корректного получения Cutoff, если партнер не DROPSHIP")
    fun successFindCutoffTimeForNoDropship() {
        val service = prepareService(
            combinatorRoute = createCombinatorRouteForScCutoff(
                movementTime = MOVEMENT_TIME,
            ),
            partnerRelationEntityDto = createPartnerRelationEntityDto(
                partnerIdFrom = 2001005,
                partnerIdTo = 2001006,
            ),
        )
        val prevSegment = createPrevWaybillSegment()
        val segment = createWaybillSegment(
            partnerId = 2001005,
            segmentId = 1002L,
            partnerType = PartnerType.SORTING_CENTER,
        )
        val nextSegment = createNextWaybillSegment(partnerId = 2001006, segmentId = 1004L)
        joinInOrder(listOf(prevSegment, segment, nextSegment)).apply { id = ORDER_ID }
        service.findCutoffTime(segment) shouldBe LocalTime.ofSecondOfDay(SC_CUTOFF_IN_SECONDS)
    }

    private fun prepareService(
        combinatorRoute: LomOrderCombinatorRoute = createCombinatorRoute(),
        partnerRelationEntityDto: PartnerRelationEntityDto = createPartnerRelationEntityDto(),
        lmsPartnerServicePartnerRelationOptional: Boolean = true,
    ): PartnerServiceImpl {
        whenever(combinatorRouteService.getByLomOrderId(ORDER_ID)).thenReturn(combinatorRoute)
        if (lmsPartnerServicePartnerRelationOptional) {
            whenever(
                lmsPartnerService.getPartnerRelationOptional(
                    partnerRelationEntityDto.fromPartnerId, partnerRelationEntityDto.toPartnerId
                )
            ).thenReturn(
                Optional.of(partnerRelationEntityDto)
            )
        } else {
            whenever(
                lmsPartnerService.getPartnerRelation(
                    partnerRelationEntityDto.fromPartnerId, partnerRelationEntityDto.toPartnerId
                )
            ).thenReturn(
                partnerRelationEntityDto
            )
        }
        return PartnerServiceImpl(
            combinatorRouteService = combinatorRouteService,
            lmsPartnerService = lmsPartnerService,
            lmsClient = lmsClient,
            partnerServiceProperties = PartnerServiceProperties().apply {
                scheduleWindowTimeFromRoute = true
                cutoffTimeFromRoute = true
                packagingDurationFromRoute = true
                intakeDeadlineFromRoute = true
            },
            partnerRelationParamsService = partnerRelationParamsService,
        )
    }

    private fun createPrevWaybillSegment(
        id: Long = 50L,
        partnerId: Long = PREV_PARTNER_ID,
        segmentId: Long = 1001L,
    ) =
        WaybillSegment(
            id = id,
            partnerId = partnerId,
        ).apply {
            combinatorSegmentIds = mutableListOf(segmentId)
        }

    private fun createWaybillSegment(
        id: Long = 51L,
        partnerId: Long = PARTNER_FROM_ID,
        segmentId: Long = 1003L,
        partnerType: PartnerType? = null,
    ) =
        WaybillSegment(
            id = id,
            partnerId = partnerId,
            partnerType = partnerType,
        ).apply {
            combinatorSegmentIds = mutableListOf(segmentId)
        }

    private fun createNextWaybillSegment(
        id: Long = 52L,
        partnerId: Long = PARTNER_TO_ID,
        segmentId: Long = 1005L,
    ) =
        WaybillSegment(
            id = id,
            partnerId = partnerId,
        ).apply {
            combinatorSegmentIds = mutableListOf(segmentId)
        }

    private fun createCombinatorRoute(): LomOrderCombinatorRoute =
        LomOrderCombinatorRoute(
            route = LomOrderCombinatorRoute.DeliveryRoute(
                paths = listOf(
                    LomOrderCombinatorRoute.Path(pointFrom = 0, pointTo = 1),
                    LomOrderCombinatorRoute.Path(pointFrom = 1, pointTo = 2),
                    LomOrderCombinatorRoute.Path(pointFrom = 2, pointTo = 3),
                    LomOrderCombinatorRoute.Path(pointFrom = 3, pointTo = 4),
                ),
                points = listOf(
                    LomOrderCombinatorRoute.Point(
                        segmentId = 1001L,
                        segmentType = PointType.WAREHOUSE,
                        ids = LomOrderCombinatorRoute.PointIds(partnerId = 301),
                    ),
                    LomOrderCombinatorRoute.Point(
                        segmentId = 1002L,
                        segmentType = PointType.MOVEMENT,
                        ids = LomOrderCombinatorRoute.PointIds(partnerId = PARTNER_FROM_ID),
                    ),
                    LomOrderCombinatorRoute.Point(
                        segmentId = 1003L,
                        segmentType = PointType.WAREHOUSE,
                        ids = LomOrderCombinatorRoute.PointIds(partnerId = PARTNER_FROM_ID),
                    ),
                    LomOrderCombinatorRoute.Point(
                        segmentId = 1005L,
                        segmentType = PointType.MOVEMENT,
                        ids = LomOrderCombinatorRoute.PointIds(partnerId = PARTNER_TO_ID),
                        services = listOf(
                            LomOrderCombinatorRoute.DeliveryService(
                                id = 38585628,
                                code = ServiceCodeName.INBOUND,
                            ),
                            LomOrderCombinatorRoute.DeliveryService(
                                id = 38585630,
                                code = ServiceCodeName.MOVEMENT,
                                workingSchedule = createWorkingSchedule(),
                                durationDelta = DURATION_DELTA,
                            ),
                        ),
                    ),
                ),
            )
        )

    private fun createCombinatorRouteForDsCutoff(movementTime: Instant): LomOrderCombinatorRoute =
        LomOrderCombinatorRoute(
            route = LomOrderCombinatorRoute.DeliveryRoute(
                paths = listOf(
                    LomOrderCombinatorRoute.Path(pointFrom = 0, pointTo = 1),
                ),
                points = listOf(
                    LomOrderCombinatorRoute.Point(
                        segmentId = 1001L,
                        segmentType = PointType.WAREHOUSE,
                        ids = LomOrderCombinatorRoute.PointIds(partnerId = 301),
                        services = listOf(
                            LomOrderCombinatorRoute.DeliveryService(
                                id = 5453607,
                                code = ServiceCodeName.CUTOFF,
                                scheduleEndTime = LomOrderCombinatorRoute.Timestamp(
                                    seconds = movementTime.epochSecond,
                                    nanos = 0,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

    private fun createCombinatorRouteForScCutoff(movementTime: Instant): LomOrderCombinatorRoute =
        LomOrderCombinatorRoute(
            route = LomOrderCombinatorRoute.DeliveryRoute(
                paths = listOf(
                    LomOrderCombinatorRoute.Path(pointFrom = 0, pointTo = 1),
                    LomOrderCombinatorRoute.Path(pointFrom = 1, pointTo = 2),
                    LomOrderCombinatorRoute.Path(pointFrom = 2, pointTo = 3),
                    LomOrderCombinatorRoute.Path(pointFrom = 3, pointTo = 4),
                ),
                points = listOf(
                    LomOrderCombinatorRoute.Point(
                        segmentId = 1001L,
                        segmentType = PointType.WAREHOUSE,
                        ids = LomOrderCombinatorRoute.PointIds(partnerId = 301),
                        services = listOf(
                            LomOrderCombinatorRoute.DeliveryService(
                                id = 5453607,
                                code = ServiceCodeName.PROCESSING,
                                duration = LomOrderCombinatorRoute.Timestamp(seconds = 1200, nanos = 0),
                            ),
                        ),
                    ),
                    LomOrderCombinatorRoute.Point(
                        segmentId = 1002L,
                        segmentType = PointType.MOVEMENT,
                        ids = LomOrderCombinatorRoute.PointIds(partnerId = 2001005),
                        services = listOf(
                            LomOrderCombinatorRoute.DeliveryService(
                                id = 40359273,
                                code = ServiceCodeName.INBOUND,
                                duration = LomOrderCombinatorRoute.Timestamp(seconds = 2400, nanos = 0),
                            ),
                            LomOrderCombinatorRoute.DeliveryService(
                                id = 40359280,
                                code = ServiceCodeName.MOVEMENT,
                                scheduleEndTime = LomOrderCombinatorRoute.Timestamp(
                                    seconds = movementTime.epochSecond,
                                    nanos = 0,
                                ),
                            ),
                        ),
                    ),
                    LomOrderCombinatorRoute.Point(
                        segmentId = 1003L,
                        segmentType = PointType.WAREHOUSE,
                        ids = LomOrderCombinatorRoute.PointIds(partnerId = 2001005),
                    ),
                    LomOrderCombinatorRoute.Point(
                        segmentId = 1004L,
                        segmentType = PointType.MOVEMENT,
                        ids = LomOrderCombinatorRoute.PointIds(partnerId = 1005450),
                    ),
                    LomOrderCombinatorRoute.Point(
                        segmentId = 1004L,
                        segmentType = PointType.LINEHAUL,
                        ids = LomOrderCombinatorRoute.PointIds(partnerId = 1005450),
                    ),
                ),
            ),
        )

    private fun createWorkingSchedule() = listOf(
        LomOrderCombinatorRoute.ScheduleDay(
            daysOfWeek = listOf(0, 6),
            timeWindows = listOf(
                LomOrderCombinatorRoute.TimeWindow(startTime = 36000, endTime = SCHEDULE_END_SECONDS.toInt()),
            ),
        ),
        LomOrderCombinatorRoute.ScheduleDay(
            daysOfWeek = listOf(1, 2, 3, 4, 5),
            timeWindows = listOf(
                LomOrderCombinatorRoute.TimeWindow(startTime = 28800, endTime = 82800),
            ),
        ),
    )

    private fun createPartnerRelationEntityDto(
        partnerIdFrom: Long = PARTNER_FROM_ID,
        partnerIdTo: Long = PARTNER_TO_ID,
        cutoffTime: LocalTime = LocalTime.of(5, 0),
    ) =
        PartnerRelationEntityDto.newBuilder()
            .fromPartnerId(partnerIdFrom)
            .toPartnerId(partnerIdTo)
            .intakeSchedule(createSchedule())
            .cutoffs(setOf(CutoffResponse.newBuilder().cutoffTime(cutoffTime).build()))
            .build()

    private fun createSchedule() = setOf(
        ScheduleDayResponse(
            100L,
            DayOfWeek.SUNDAY.value,
            LocalTime.of(10, 0),
            LocalTime.ofSecondOfDay(SCHEDULE_END_SECONDS),
            true,
        ),
    )

    companion object {
        private const val ORDER_ID = 1L
        private val FIXED_TIME = Instant.parse("2022-07-03T06:00:00.00Z")
        private val FIXED_LOCAL_DATE = LocalDate.ofInstant(FIXED_TIME, DateTimeUtils.MOSCOW_ZONE)
        private const val SCHEDULE_END_SECONDS = 79200L
        private const val SCHEDULE_START_SECONDS = 36000L
        private const val PARTNER_FROM_ID = 147687L
        private const val PARTNER_TO_ID = 147433L
        private const val DURATION_DELTA = 120
        private val MOVEMENT_TIME = Instant.parse("2022-07-05T06:00:00.00Z")
        private const val PREV_PARTNER_ID = 301L
        private const val SC_CUTOFF_IN_SECONDS = 18000L
        private const val DS_CUTOFF_IN_SECONDS = 21600L
    }
}
