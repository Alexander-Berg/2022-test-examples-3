package ru.yandex.market.logistics.mqm.service.processor.planfact

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.LocalDate
import java.time.LocalTime

class DropoffShipmentPlanFactProcessorTest : AbstractTest() {

    private val settingService = TestableSettingsService()
    private val processor = DropoffShipmentPlanFactProcessor(settingService)

    private val fixedDate = LocalDate.of(2021, 8, 17)

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка применимости процессора")
    fun isEligible(platformClient: PlatformClient) {
        val current = prepareSegments(platformClient = platformClient)
        Assertions.assertThat(processor.isEligible(current)).isTrue
    }

    @Test
    @DisplayName("Процессор не применим если партнет текущего сегмента не дропофф")
    fun isNonEligibleIfCurrentIsNotSortingCenter() {
        val current = prepareSegments(isDropoff = false)
        Assertions.assertThat(processor.isEligible(current)).isFalse
    }

    @Test
    @DisplayName("Процессор не применим если нет 110 чекпоинта")
    fun isNonEligibleIf110CheckpointIsMissing() {
        val current = prepareSegments(hasInCheckpoint = false)
        Assertions.assertThat(processor.isEligible(current)).isFalse
    }

    @Test
    @DisplayName("Процессор не применим если нет даты отгрузки")
    fun isNonEligibleIfShipmentDateIsMissing() {
        val current = prepareSegments(hasShipmentDate = false)
        Assertions.assertThat(processor.isEligible(current)).isFalse
    }

    @Test
    @DisplayName("Расчет ожидаемого времени отгрузки заказа на СЦ от дропоффа")
    fun calculateDateTime() {
        val expectedDeadline = fixedDate.atTime(LocalTime.MAX).atZone(DateTimeUtils.MOSCOW_ZONE).toInstant()
        val current = prepareSegments()
        Assertions.assertThat(processor.calculateExpectedDatetime(current)).isEqualTo(expectedDeadline)
    }

    private fun prepareSegments(
        isDropoff: Boolean = true,
        hasInCheckpoint: Boolean = true,
        hasShipmentDate: Boolean = true,
        platformClient: PlatformClient = PlatformClient.BERU
    ): WaybillSegment {

        val currentSegment = WaybillSegment(
            externalId = "123",
            partnerId = 2L,
            segmentType = SegmentType.SORTING_CENTER
        )
            .apply { if (hasShipmentDate) shipment.date = fixedDate }
            .apply {
                if (hasInCheckpoint) waybillSegmentStatusHistory =
                    mutableSetOf(WaybillSegmentStatusHistory(status = SegmentStatus.IN))
            }
        joinInOrder(listOf(currentSegment)).apply { platformClientId = platformClient.id }
        if (isDropoff) {
            currentSegment.partnerType = PartnerType.DELIVERY
        }
        return currentSegment
    }
}
