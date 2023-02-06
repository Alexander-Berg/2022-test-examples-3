package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.createWaybillSegmentWithCheckpoint

@Deprecated("Заменен на CourierLastMileRecipientPlanFactProcessorTest")
class RecipientCourierPlanFactProcessorTest: AbstractRecipientPlanFactProcessorTest() {

    @Test
    @DisplayName("Процессор неприменим если тип неверный")
    fun isNonEligibleIfRecipientTypeIsWrong() {
        val segment = createWaybillSegmentWithCheckpoint(
            SegmentType.PICKUP,
            SegmentStatus.IN
        )
        getProcessor().isEligible(segment) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим если нет 10 или 110 чекпоинта в истории")
    fun isNonEligibleWithoutCheckpointInHistory() {
        val segment = createWaybillSegmentWithCheckpoint(
            SegmentType.COURIER,
            SegmentStatus.TRANSIT_PICKUP
        )
        getProcessor().isEligible(segment) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим, если это Экспресс")
    fun isNonEligibleIfExpress() {
        val segment = createValidSegment()
            .apply { waybillSegmentTags!!.add(WaybillSegmentTag.CALL_COURIER) }
        getProcessor().isEligible(segment) shouldBe false
    }

    override fun createValidSegment(): WaybillSegment = createWaybillSegmentWithCheckpoint(
        SegmentType.COURIER,
        SegmentStatus.IN
    )

    override fun getProcessor(): AbstractRecipientPlanFactProcessor = RecipientCourierPlanFactProcessor(
        lmsPartnerService,
        logService
    )

    override fun getParamType() = PartnerExternalParamType.LAST_MILE_RECIPIENT_DEADLINE
}
