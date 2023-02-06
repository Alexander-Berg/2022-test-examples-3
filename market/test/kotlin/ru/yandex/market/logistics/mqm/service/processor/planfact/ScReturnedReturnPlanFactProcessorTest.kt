package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.returns.LrmReturnService
import ru.yandex.market.logistics.mqm.service.returns.LrmReturnServiceImpl
import ru.yandex.market.logistics.mqm.service.tms.PartnerExpectedDateService
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.createWaybillSegmentWithCheckpoint
import java.time.Clock
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class ScReturnedReturnPlanFactProcessorTest {

    @Mock
    private lateinit var partnerExpectedDateService: PartnerExpectedDateService
    private val settingsService = TestableSettingsService()
    private lateinit var processor: ScReturnedReturnPlanFactProcessor
    @Mock
    private var lrmReturnService: LrmReturnService = Mockito.mock(LrmReturnServiceImpl::class.java)

    @BeforeEach
    fun setUp() {
        processor = ScReturnedReturnPlanFactProcessor(
            partnerExpectedDateService,
            settingsService,
            Clock.fixed(Instant.parse("2020-02-14T11:15:25.00Z"), DateTimeUtils.MOSCOW_ZONE),
            lrmReturnService,
        )
    }

    @Test
    @DisplayName("Расчет ожидаемого времени после получения 175 чекпоинта")
    fun calculateExpectedDatetime() {
        whenever(partnerExpectedDateService.calculateExpectedDays(ArgumentMatchers.anyLong()))
            .thenReturn(null)
        val waybillSegment = createSegment()
        val expectedDeadline = Instant.parse("2021-01-09T09:00:00Z")
        processor.calculateExpectedDatetime(waybillSegment) shouldBe expectedDeadline
    }

    @Test
    @DisplayName("Проверка неприменимости процессора, если партнер не Dropoff")
    fun isNotEligible() {
        processor.isEligible(createSegment()) shouldBe false
    }

    @Test
    @DisplayName("Проверка неприменимости процессора, если нет чекпоинта RETURN_PREPARING_SENDER")
    fun isNotEligibleWithoutReturnPreparingSender() {
        processor.isEligible(createSegment(isDropoff = true, segmentStatus = SegmentStatus.STARTED)) shouldBe false
    }

    @Test
    @DisplayName("Проверка применимости процессора, если партнер Dropoff, и есть 175 чекпоинт в истории")
    fun isEligibleInternalWhenPartnerDropoffAnd175() {
        processor.isEligible(createSegment(isDropoff = true)) shouldBe true
    }

    @Test
    @DisplayName("Процессор неприменим если тип сегмента не соответствует партнёру возвратного СЦ")
    fun isNonEligibleIfSegmentTypeIsNotReturnSCSegment() {
        processor.isEligible(createSegment(isReturnSortingCenterSegment = false)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим если тип сегмента неверный")
    fun isNonEligibleIfSegmentTypeIsWrong() {
        processor.isEligible(createSegment(segmentType = SegmentType.PICKUP)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим если нет 175 чекпоинта в истории")
    fun isNonEligibleWithoutCheckpointInHistory() {
        processor.isEligible(createSegment(segmentStatus = SegmentStatus.IN)) shouldBe false
    }

    private fun createSegment(
        segmentType: SegmentType = SegmentType.SORTING_CENTER,
        segmentStatus: SegmentStatus = SegmentStatus.RETURN_PREPARING_SENDER,
        isReturnSortingCenterSegment: Boolean = true,
        isDropoff: Boolean = false,
    ): WaybillSegment {
        val waybillSegment = createWaybillSegmentWithCheckpoint(segmentType, segmentStatus)
        if (isReturnSortingCenterSegment) waybillSegment.apply {
            order!!.returnSortingCenterId = partnerId
        }

        if (isDropoff) {
            waybillSegment.partnerType = PartnerType.DELIVERY
            waybillSegment.segmentType = SegmentType.SORTING_CENTER
        } else {
            waybillSegment.partnerType = PartnerType.SORTING_CENTER
        }

        return waybillSegment
    }
}
