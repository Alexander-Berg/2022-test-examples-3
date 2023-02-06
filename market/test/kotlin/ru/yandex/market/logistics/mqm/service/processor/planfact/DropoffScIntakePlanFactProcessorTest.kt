package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import ru.yandex.market.logistics.mqm.AbstractTest
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.PlatformClient
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint
import java.time.Instant

class DropoffScIntakePlanFactProcessorTest : AbstractTest() {

    private val settingService = TestableSettingsService()
    private val processor = DropoffScIntakePlanFactProcessor(settingService)

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.INCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка применимости процессора")
    fun isEligible(platformClient: PlatformClient) {
        val current = createSegments(platformClient = platformClient)
        processor.isEligible(current) shouldBe true
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = PlatformClient::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["BERU", "YANDEX_GO"]
    )
    @DisplayName("Проверка неприменимости процессора для platformClient")
    fun isNonEligiblePlatformClient(platformClient: PlatformClient) {
        val current = createSegments(platformClient = platformClient)
        processor.isEligible(current) shouldBe false
    }


    @Test
    @DisplayName("Процессор не применим если предыдущий сегмент не DROPOFF")
    fun isNonEligibleIfCurrentIsNotDropoff() {
        val current = createSegments(isPreviousDropoff = false)
        processor.isEligible(current) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["SORTING_CENTER"]
    )
    @DisplayName("Процессор не применим если тип текущего сегмента не SORTING_CENTER")
    fun isNonEligibleIfPreviousIsNotDropship(curPartnerType: PartnerType) {
        val current = createSegments(curPartnerType = curPartnerType)
        processor.isEligible(current) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применим если нет 130 чекпоинта")
    fun isNonEligibleIf130CheckpointIsMissing() {
        val current = createSegments(isPreviousSegmentHas130 = false)
        processor.isEligible(current) shouldBe false
    }

    @Test
    @DisplayName("Расчет ожидаемого времени приемки заказа на дропофф СЦ от дропшипа")
    fun calculateDateTime() {
        val current = createSegments()
        processor.calculateExpectedDatetime(current) shouldBe EXPECTED_TIME
    }

    private fun createSegments(
        isPreviousDropoff: Boolean = true,
        curPartnerType: PartnerType = PartnerType.SORTING_CENTER,
        isPreviousSegmentHas130: Boolean = true,
        platformClient: PlatformClient = PlatformClient.BERU
    ): WaybillSegment {
        val previousSegment = WaybillSegment(
            partnerId = 1L
        )
        val currentSegment = WaybillSegment(
            externalId = "123",
            partnerId = 2L,
            partnerType = curPartnerType
        )
        if (isPreviousSegmentHas130) {
            writeWaybillSegmentCheckpoint(previousSegment, SegmentStatus.OUT, FIXED_TIME)
        }
        joinInOrder(listOf(previousSegment, currentSegment)).apply { platformClientId = platformClient.id }
        if (isPreviousDropoff) {
            previousSegment.partnerType = PartnerType.DELIVERY
            previousSegment.segmentType = SegmentType.SORTING_CENTER
        }
        return currentSegment
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2021-01-01T15:00:00.00Z")
        private val EXPECTED_TIME = Instant.parse("2021-01-02T09:00:00.00Z")
    }
}
