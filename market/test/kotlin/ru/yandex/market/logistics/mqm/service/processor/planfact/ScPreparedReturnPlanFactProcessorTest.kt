package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.mock
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.returns.LrmReturnService
import ru.yandex.market.logistics.mqm.service.returns.LrmReturnServiceImpl
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.createWaybillSegmentWithCheckpoint
import java.time.Clock
import java.time.Instant

class ScPreparedReturnPlanFactProcessorTest {

    private val settingsService = TestableSettingsService()

    @Mock
    private var lrmReturnService: LrmReturnService =Mockito.mock(LrmReturnServiceImpl::class.java)

    private val processor = ScPreparedReturnPlanFactProcessor(
        settingsService,
        Clock.fixed(Instant.parse("2020-02-14T11:15:25.00Z"), DateTimeUtils.MOSCOW_ZONE),
        mock()
    )

    @Test
    @DisplayName("Расчет ожидаемого времени после получения 170 чекпоинта")
    fun calculateExpectedDatetime() {
        val expectedDeadline = FIXED_TIME.plus(ScPreparedReturnPlanFactProcessor.TIMEOUT)
        processor.calculateExpectedDatetime(createSegment()) shouldBe expectedDeadline
    }

    @Test
    @DisplayName("Проверка применимости процессора")
    fun isEligible() {
        processor.isEligible(createSegment()) shouldBe true
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
    @DisplayName("Процессор неприменим если нет 170 чекпоинта в истории")
    fun isNonEligibleWithoutCheckpointInHistory() {
        processor.isEligible(createSegment(segmentStatus = SegmentStatus.IN)) shouldBe false
    }

    private fun createSegment(
        segmentType: SegmentType = SegmentType.SORTING_CENTER,
        segmentStatus: SegmentStatus = SegmentStatus.RETURN_ARRIVED,
        isReturnSortingCenterSegment: Boolean = true
    ): WaybillSegment {
        val waybillSegment = createWaybillSegmentWithCheckpoint(segmentType, segmentStatus)
        if (isReturnSortingCenterSegment) waybillSegment.apply {
            order!!.returnSortingCenterId = partnerId
        }

        return waybillSegment
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2021-01-02T09:00:00.00Z")
    }
}
