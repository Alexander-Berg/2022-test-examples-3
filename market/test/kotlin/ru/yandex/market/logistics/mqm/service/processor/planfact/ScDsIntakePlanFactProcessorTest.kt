package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import java.time.Instant
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint

class ScDsIntakePlanFactProcessorTest : AbstractIntakePlanFactProcessorTest() {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка применимости процессора")
    fun isEligible(platformClient: PlatformClient) {
        val waybillSegment = createSegments(platformClient = platformClient)
        getProcessor().isEligible(waybillSegment) shouldBe true
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = SegmentStatus::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["IN", "TRANSIT_DELIVERY_AT_START_SORT", "TRANSIT_DELIVERY_TRANSPORTATION", "OUT"]
    )
    @DisplayName("Не применим если на сегменте есть ожидаемый или следующие статусы")
    fun isNotEligibleIfHasExpectedOrNextCheckpoints(segmentStatus: SegmentStatus) {
        val waybillSegment = createSegments()
        writeWaybillSegmentCheckpoint(waybillSegment, segmentStatus, Instant.EPOCH)
        getProcessor().isEligible(waybillSegment) shouldBe false
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка неприменимости процессора для platformClient")
    fun isNonEligiblePlatformClient(platformClient: PlatformClient) {
        val waybillSegment = createSegments(platformClient = platformClient)
        getProcessor().isEligible(waybillSegment) shouldBe false
    }

    override fun createCurrentSegment() =
        createWaybillSegment(SegmentType.COURIER, PartnerType.DELIVERY, 1, 101L)

    override fun createPreviousSegment() =
        createWaybillSegment(SegmentType.SORTING_CENTER, PartnerType.SORTING_CENTER, 0, 202L)

    override fun getProcessor(): AbstractIntakePlanFactProcessor =
        ScDsIntakePlanFactProcessor(partnerService, logService, settingsService)
}
