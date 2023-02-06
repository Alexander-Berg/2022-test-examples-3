package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.service.returns.LrmReturnService
import ru.yandex.market.logistics.mqm.service.returns.LrmReturnServiceImpl
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.createWaybillSegmentWithCheckpoint
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Clock
import java.time.Instant

class DsScIntakeReturnPlanFactProcessorTest {

    private val settingService = TestableSettingsService()

    @Mock
    private var lrmReturnService: LrmReturnService = Mockito.mock(LrmReturnServiceImpl::class.java)

    private val processor = DsScIntakeReturnPlanFactProcessor(
        settingService,
        lrmReturnService,
        Clock.fixed(Instant.parse("2020-02-14T11:15:25.00Z"), DateTimeUtils.MOSCOW_ZONE)
    )

    @Test
    @DisplayName("Расчет ожидаемого времени после получения 80 чекпоинта")
    fun calculateExpectedDatetime() {
        val expectedDeadline = FIXED_TIME.plus(DsScIntakeReturnPlanFactProcessor.TIMEOUT)
        processor.calculateExpectedDatetime(createSegment()) shouldBe expectedDeadline
    }

    @Test
    @DisplayName("Проверка применимости процессора")
    fun isEligible() {
        processor.isEligible(createSegment()) shouldBe true
    }

    @Test
    @DisplayName("Процессор неприменим если тип сегмента неверный")
    fun isNonEligibleIfSegmentTypeIsWrong() {
        processor.isEligible(createSegment(segmentType = SegmentType.PICKUP)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим, если partnerType неверный")
    fun isNotEligibleIfPartnerTypeIsWrong() {
        processor.isEligible(createSegment(partnerType = PartnerType.OWN_DELIVERY)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим, если partnerType следующего сегмента неверный")
    fun isNotEligibleIfNextSegmentPartnerTypeIsWrong() {
        processor.isEligible(createSegment(nextPartnerType = PartnerType.DROPSHIP)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим если нет 80 чекпоинта в истории")
    fun isNonEligibleWithoutCheckpointInHistory() {
        processor.isEligible(createSegment(nextSegmentStatus = SegmentStatus.IN)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим, если нет следующего сегмента")
    fun isNotEligibleIfNoNextSegment() {
        processor.isEligible(createSegment(hasNextSegment = false)) shouldBe false
    }

    private fun createSegment(
        partnerType: PartnerType = PartnerType.SORTING_CENTER,
        segmentType: SegmentType = SegmentType.SORTING_CENTER,
        nextPartnerType: PartnerType = PartnerType.DELIVERY,
        nextSegmentStatus: SegmentStatus = SegmentStatus.RETURNED,
        hasNextSegment: Boolean = true
    ): WaybillSegment {
        val currentSegment = createWaybillSegmentWithCheckpoint(segmentType, nextSegmentStatus)
        currentSegment.partnerType = partnerType

        if (hasNextSegment) {
            val nextSegment = createWaybillSegmentWithCheckpoint(segmentType, nextSegmentStatus)
            nextSegment.partnerType = nextPartnerType
            joinInOrder(listOf(currentSegment, nextSegment))
        }

        return currentSegment
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2021-01-02T09:00:00.00Z")
    }
}
