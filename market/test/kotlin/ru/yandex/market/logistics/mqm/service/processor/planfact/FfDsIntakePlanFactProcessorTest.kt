package ru.yandex.market.logistics.mqm.service.processor.planfact

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType

class FfDsIntakePlanFactProcessorTest : AbstractIntakePlanFactProcessorTest() {
    @Test
    @DisplayName("Проверка применимости процессора")
    fun isEligible() {
        val waybillSegment = createSegments()
        Assertions.assertThat(getProcessor().isEligible(waybillSegment)).isTrue
    }

    @ParameterizedTest(name = AbstractTest.TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["BERU", "DBS", "FAAS"]
    )
    @DisplayName("Проверка неприменимости процессора для platformClient")
    fun isNonEligiblePlatformClient(platformClient: PlatformClient) {
        val waybillSegment = createSegments(platformClient = platformClient)
        Assertions.assertThat(getProcessor().isEligible(waybillSegment)).isFalse
    }

    override fun createCurrentSegment() =
        createWaybillSegment(SegmentType.COURIER, PartnerType.DELIVERY, 1, 101L)

    override fun createPreviousSegment() =
        createWaybillSegment(SegmentType.FULFILLMENT, PartnerType.FULFILLMENT, 0, 202L)

    override fun getProcessor(): AbstractIntakePlanFactProcessor =
        FfDsIntakePlanFactProcessor(partnerService, logService, settingsService)
}
