package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.PartnerService
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.createWaybillSegmentWithCheckpoint
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Instant

class CreatedPlanFactProcessorTest {

    private val partnerService: PartnerService = Mockito.mock(PartnerService::class.java)
    private val settingsService = TestableSettingsService()
    private val processor = CreatedPlanFactProcessor(partnerService, settingsService)

    @Test
    @DisplayName("Проверка расчета ожидаемого времени")
    fun calculateExpectedDatetime() {
        val expectedDeadline = FIXED_TIME.plus(CreatedPlanFactProcessor.DEADLINE_INTERVAL)
        processor.calculateExpectedDatetime(createSegment()) shouldBe expectedDeadline
    }

    @Test
    @DisplayName("Процессор применим, если partnerType сегмента не DROPSHIP")
    fun isEligibleIfDropshipPartnerType() {
        processor.isEligible(createSegment(partnerType = PartnerType.DELIVERY)) shouldBe true
    }

    @Test
    @DisplayName("Процессор применим, если WaybillSegment не первый")
    fun isEligibleIfWaybillSegmentNotFirst() {
        processor.isEligible(createSegment(hasPreviousSegment = true)) shouldBe true
    }

    @Test
    @DisplayName("Процессор неприменим если сегмент isOnlyReturnFulfillment")
    fun isNonEligibleIfIsOnlyReturnFulfillment() {
        processor.isEligible(createSegment(segmentType = SegmentType.FULFILLMENT, withTag = true)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим если сегмент isCreated")
    fun isNonEligibleIfIsCreated() {
        processor.isEligible(createSegment(externalId = "id")) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["YANDEX_DELIVERY"]
    )
    @DisplayName("Процессор применим для всех клиентов платформы, кроме ЯДо")
    fun isEligibleForPlatformClient(platformClient: PlatformClient) {
        val waybillSegment = createSegment(partnerType = PartnerType.DELIVERY)
        waybillSegment.order?.platformClientId = platformClient.id
        processor.isEligible(waybillSegment) shouldBe true
    }

    private fun createSegment(
        segmentType: SegmentType = SegmentType.COURIER,
        partnerType: PartnerType = PartnerType.DROPSHIP,
        hasPreviousSegment: Boolean = false,
        externalId: String? = null,
        withTag: Boolean = true
    ): WaybillSegment {
        val waybillSegment = createWaybillSegmentWithCheckpoint(segmentType, SegmentStatus.OUT)
        waybillSegment.apply {
            this.externalId = externalId
            this.partnerType = partnerType
        }

        if (withTag) waybillSegment.apply {
            waybillSegmentTags = mutableSetOf(WaybillSegmentTag.RETURN)
        }

        val segments = mutableListOf(waybillSegment)
        if (hasPreviousSegment) {
            segments.add(0, createWaybillSegmentWithCheckpoint(segmentType, SegmentStatus.OUT))
        }

        joinInOrder(segments).apply {
            created = FIXED_TIME
            id = 10L
        }

        return waybillSegment
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2021-01-01T15:00:00.00Z")
    }
}
