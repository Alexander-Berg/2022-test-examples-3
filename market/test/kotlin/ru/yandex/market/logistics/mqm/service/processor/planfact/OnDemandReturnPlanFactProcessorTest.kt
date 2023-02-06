package ru.yandex.market.logistics.mqm.service.processor.planfact

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.logistics.mqm.entity.PlanFact
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.mqm.utils.writeWaybillSegmentCheckpoint
import java.time.Instant

@ExtendWith(MockitoExtension::class)
internal class OnDemandReturnPlanFactProcessorTest {

    private val settingService = TestableSettingsService()
    private val processor = OnDemandReturnPlanFactProcessor(settingService)

    @Test
    @DisplayName("Проверка применимости процессора")
    fun isEligible() {
        val segment = createSegment()
        processor.isEligible(segment) shouldBe true
    }

    @ParameterizedTest
    @EnumSource(
        value = SegmentType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["COURIER"]
    )
    @DisplayName("Процессор не применим, если тип сегмента не COURIER")
    fun isNonEligibleIfSegmentTypeIsNotCourier(segmentType: SegmentType) {
        val segment = createSegment(segmentType = segmentType)
        processor.isEligible(segment) shouldBe false
    }

    @ParameterizedTest
    @EnumSource(
        value = PartnerType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["DELIVERY"]
    )
    @DisplayName("Процессор не применим, если тип партенера не DELIVERY")
    fun isNonEligibleIfPartnerTypeIsNotDelivery(partnerType: PartnerType) {
        val segment = createSegment(partnerType = partnerType)
        processor.isEligible(segment) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применим, если нет 35 чекпоинта")
    fun isNonEligibleIf35CheckpointIsMissing() {
        val checkpoint45WithTime = SegmentStatus.TRANSIT_PICKUP to Instant.parse("2021-10-29T10:00:00.00Z")
        val segment = createSegment(checkpoints = listOf(checkpoint45WithTime))
        processor.isEligible(segment) shouldBe false
    }

    @Test
    @DisplayName("Процессор не применим, если не onDemand")
    fun isNonEligibleIfIsNotOnDemand() {
        val segment = createSegment(isOnDemand = false)
        processor.isEligible(segment) shouldBe false
    }

    @Test
    @DisplayName("Расчет ожидаемого времени приемки заказа на дропофф СЦ от дропшипа")
    fun calculateDateTime() {
        val segment = createSegment()
        processor.calculateExpectedDatetime(segment) shouldBe Instant.parse("2021-10-29T11:00:00.00Z")
    }

    @Test
    @DisplayName("Считается, что факта нет, если 45 до 35")
    fun getFactTimeReturnsNullIf45Before35() {
        val waybillSegment = createSegment()
        val planFact = PlanFact().apply { entity = waybillSegment }
        processor.getFactTimeOrNull(planFact) shouldBe null
    }

    @Test
    @DisplayName("Считается что факт настал, если есть 45 чекпоинт после 35")
    fun getFactTimeReturns45If45After35() {
        val checkpoints = listOf(
            SegmentStatus.TRANSIT_PICKUP to Instant.parse("2021-10-29T09:00:00.00Z"),
            SegmentStatus.TRANSIT_COURIER_RECEIVED to Instant.parse("2021-10-29T10:00:00.00Z"),
            SegmentStatus.TRANSIT_PICKUP to Instant.parse("2021-10-29T11:00:00.00Z"),
        )
        val waybillSegment = createSegment(checkpoints = checkpoints)
        val planFact = PlanFact().apply { entity = waybillSegment }
        processor.getFactTimeOrNull(planFact) shouldBe Instant.parse("2021-10-29T11:00:00.00Z")
    }

    private fun createSegment(
        isOnDemand: Boolean = true,
        segmentType: SegmentType = SegmentType.COURIER,
        partnerType: PartnerType = PartnerType.DELIVERY,
        checkpoints: List<Pair<SegmentStatus, Instant>> = listOf(
            SegmentStatus.TRANSIT_PICKUP to Instant.parse("2021-10-29T09:00:00.00Z"),
            SegmentStatus.TRANSIT_COURIER_RECEIVED to Instant.parse("2021-10-29T10:00:00.00Z")
        )
    ): WaybillSegment {
        val currentSegment = WaybillSegment(
            externalId = "123",
            partnerId = 1L,
            partnerType = partnerType,
            segmentType = segmentType,
        )
        if (isOnDemand) currentSegment.waybillSegmentTags!!.add(WaybillSegmentTag.ON_DEMAND)
        joinInOrder(listOf(currentSegment))
        checkpoints.forEach { (status, time) -> writeWaybillSegmentCheckpoint(currentSegment, status, time) }
        return currentSegment
    }
}
