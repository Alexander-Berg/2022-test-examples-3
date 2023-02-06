package ru.yandex.market.logistics.mqm.service.processor.planfact

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.AbstractContextualTest
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegmentStatusHistory
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.createHistory
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import java.time.Duration
import java.time.Instant

class ExpressCourierSearchPlanFactProcessorTest: AbstractContextualTest() {

    @Autowired
    private lateinit var processor: ExpressCourierSearchPlanFactProcessor

    companion object{
        private val DEFAULT_TIME = Instant.parse("2021-01-01T00:00:00.00Z")
    }

    @Test
    fun isEligibleWith120() {
        val dropshipSegment = mockDropshipSegment()
        val deliverySegment = mockDeliverySegment()
        joinInOrder(
            listOf(
                dropshipSegment, deliverySegment
            )
        )

        softly.assertThat(processor.isEligible(deliverySegment)).isTrue
    }

    @Test
    fun isEligibleWith110() {
        val dropshipSegment = mockDropshipSegment(status = SegmentStatus.IN)
        val deliverySegment = mockDeliverySegment()
        joinInOrder(
            listOf(
                dropshipSegment, deliverySegment
            )
        )

        softly.assertThat(processor.isEligible(deliverySegment)).isTrue
    }

    @Test
    @DisplayName("Проверка, что процессор не применяется, если нет тега CALL_COURIER")
    fun isEligibleReturnFalseIfNoTag() {
        val dropshipSegment = mockDropshipSegment()
        val deliverySegment = mockDeliverySegment(WaybillSegmentTag.RETURN)
        joinInOrder(
            listOf(
                dropshipSegment, deliverySegment
            )
        )

        softly.assertThat(processor.isEligible(deliverySegment)).isFalse
    }

    @Test
    @DisplayName("Проверка, что процессор не применяется, если нет чекпоинта")
    fun isEligibleReturnFalseIfNoCheckpoint() {
        val dropshipSegment = mockDropshipSegment(status = SegmentStatus.OUT)
        val deliverySegment = mockDeliverySegment()
        joinInOrder(
            listOf(
                dropshipSegment, deliverySegment
            )
        )

        softly.assertThat(processor.isEligible(deliverySegment)).isFalse
    }

    @Test
    @DisplayName("Проверка расчета дедлайна на основе времени вызова курьера из lom, если 120 до call_courier")
    fun calculateExpectedDatetime120BeforeCallCourier() {
        val checkpoint120 = DEFAULT_TIME
        val dropshipSegment = WaybillSegment(
            partnerType = PartnerType.DROPSHIP,
            segmentType = SegmentType.FULFILLMENT,
            waybillSegmentIndex = 1,
        )
        dropshipSegment.waybillSegmentStatusHistory.addAll(
            listOf(
                createHistory(SegmentStatus.TRANSIT_PREPARED, checkpoint120),
                createHistory(SegmentStatus.IN, checkpoint120.plus(Duration.ofMinutes(1))),
            )
        )

        val deliverySegment = mockDeliverySegment()
        val testCallCourierTime = checkpoint120.plus(Duration.ofMinutes(1))
        deliverySegment.callCourierTime = testCallCourierTime
        joinInOrder(
            listOf(
                dropshipSegment, deliverySegment
            )
        )
        softly.assertThat(processor.calculateExpectedDatetime(deliverySegment))
            .isEqualTo(testCallCourierTime.plus(Duration.ofMinutes(20)))
    }

    @Test
    @DisplayName("Проверка расчета дедлайна на основе времени вызова курьера из lom, если 110 до call_courier")
    fun calculateExpectedDatetime110BeforeCallCourier() {
        val checkpoint110 = Instant.parse("2021-01-01T00:00:00.00Z")
        val dropshipSegment = WaybillSegment(
            partnerType = PartnerType.DROPSHIP,
            segmentType = SegmentType.FULFILLMENT,
            waybillSegmentIndex = 1,
        )
        dropshipSegment.waybillSegmentStatusHistory.addAll(
            listOf(
                createHistory(SegmentStatus.TRANSIT_PREPARED, checkpoint110.plus(Duration.ofMinutes(1))),
                createHistory(SegmentStatus.IN, checkpoint110),
            )
        )

        val deliverySegment = mockDeliverySegment(WaybillSegmentTag.CALL_COURIER)
        val testCallCourierTime = checkpoint110.plus(Duration.ofMinutes(1))
        deliverySegment.callCourierTime = testCallCourierTime
        joinInOrder(
            listOf(
                dropshipSegment, deliverySegment
            )
        )
        softly.assertThat(processor.calculateExpectedDatetime(deliverySegment))
            .isEqualTo(testCallCourierTime.plus(Duration.ofMinutes(20)))
    }

    @Test
    @DisplayName("Проверка расчета дедлайна на основе времени вызова курьера из lom, если 120 после call_courier")
    fun calculateExpectedDatetime120AfterCallCourier() {
        val checkpoint120 = DEFAULT_TIME
        val dropshipSegment = mockDropshipSegment(
            checkpoint120,
            SegmentStatus.TRANSIT_PREPARED
        )
        val deliverySegment = mockDeliverySegment()
        val testCallCourierTime = checkpoint120.minus(Duration.ofMinutes(1))
        deliverySegment.callCourierTime = testCallCourierTime
        joinInOrder(
            listOf(
                dropshipSegment, deliverySegment
            )
        )
        softly.assertThat(processor.calculateExpectedDatetime(deliverySegment))
            .isEqualTo(checkpoint120.plus(Duration.ofMinutes(20)))
    }

    @Test
    @DisplayName("Проверка расчета дедлайна")
    fun calculateExpectedDatetimeLegacy() {
        val processorLegacy = ExpressCourierSearchPlanFactProcessor(
            useLomCourierSearchDeadline = false,
            settingService = TestableSettingsService()
        )
        val dropshipSegment = mockDropshipSegment(
            date = Instant.parse("2021-01-01T00:00:00.00Z"),
            status = SegmentStatus.TRANSIT_PREPARED,
        )
        val deliverySegment = mockDeliverySegment(WaybillSegmentTag.CALL_COURIER)
        joinInOrder(
            listOf(
                dropshipSegment, deliverySegment
            )
        )

        softly.assertThat(processorLegacy.calculateExpectedDatetime(deliverySegment))
            .isEqualTo(Instant.parse("2021-01-01T00:20:00.00Z"))
    }

    private fun mockDropshipSegment(
        date: Instant = DEFAULT_TIME,
        status: SegmentStatus = SegmentStatus.TRANSIT_PREPARED,
        created: Instant? = null
    ): WaybillSegment {
        val segment = WaybillSegment(
            partnerType = PartnerType.DROPSHIP,
            segmentType = SegmentType.FULFILLMENT,
            waybillSegmentIndex = 1,
        )
        segment.waybillSegmentStatusHistory = mutableSetOf(
            WaybillSegmentStatusHistory(
                status = status,
                date = date,
                created = created
            )
        )

        return segment
    }

   private fun mockDeliverySegment(tag: WaybillSegmentTag = WaybillSegmentTag.CALL_COURIER) = WaybillSegment(
        partnerType = PartnerType.DELIVERY,
        segmentType = SegmentType.COURIER,
        waybillSegmentTags = mutableSetOf(tag),
        waybillSegmentIndex = 2,
    )
}
