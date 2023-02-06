package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import ru.yandex.market.logistics.mqm.entity.additionaldata.TrackReceivedPlanFactAdditionalData
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import java.time.Duration
import java.time.LocalTime
import ru.yandex.market.logistics.mqm.service.PartnerService

@DisplayName("Создание план-фактов для сегментов")
class WaybillSegmentCreatedPlanFactProcessorTest : AbstractCreatedPlanFactProcessorTest() {

    @Autowired
    private lateinit var createdPlanFactProcessor: CreatedPlanFactProcessor

    @Autowired
    private lateinit var partnerService: PartnerService

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = SegmentType::class,
        mode = EnumSource.Mode.EXCLUDE,
        names = ["NO_OPERATION"]
    )
    @DisplayName("Доступность сегментов для обновления")
    fun isEligible(segmentType: SegmentType?) {
        val waybillSegment = mockSegment(PartnerType.DELIVERY, segmentType, 0)
        orderWithSegments(mutableListOf(waybillSegment))

        softly.assertThat(createdPlanFactProcessor.isEligible(waybillSegment)).isEqualTo(true)
    }

    @Test
    fun calculateExpectedDatetime() {
        val expectedDeadline = FIXED_TIME.plusSeconds(Duration.ofMinutes(15).toSeconds())
        val waybillSegment = mockSegment(PartnerType.DELIVERY, SegmentType.COURIER, 0)
        orderWithSegments(mutableListOf(waybillSegment))
        val dateTime = createdPlanFactProcessor.calculateExpectedDatetime(waybillSegment)

        softly.assertThat(dateTime).isEqualTo(expectedDeadline)
    }

    @Test
    fun getPayloadReturnCutoff() {
        val testCutoff = LocalTime.of(16, 55)
        val testWaybillSegmentCurrent = mockWaybillSegment(112L, 0)
        val testWaybillSegmentNext = mockWaybillSegment(123L, 1)

        mockOrder(mutableListOf(testWaybillSegmentCurrent, testWaybillSegmentNext))

        val expectedData = TrackReceivedPlanFactAdditionalData().apply { cutoffTime = testCutoff }

        doReturn(testCutoff).whenever(partnerService).findCutoffTime(any())

        softly.assertThat(createdPlanFactProcessor.getPayload(testWaybillSegmentCurrent)).isEqualTo(expectedData)
    }

    @Test
    fun getPayloadReturnDefaultCutoffIfNoRelationBetweenPartners() {
        val testWaybillSegmentCurrent = mockWaybillSegment(112L, 0)
        val testWaybillSegmentNext = mockWaybillSegment(123L, 1)

        mockOrder(mutableListOf(testWaybillSegmentCurrent, testWaybillSegmentNext))

        val expectedData = TrackReceivedPlanFactAdditionalData().apply { cutoffTime = DEFAULT_CUTOFF_TIME }

        doReturn(DEFAULT_CUTOFF_TIME).whenever(partnerService).findCutoffTime(any())

        softly.assertThat(createdPlanFactProcessor.getPayload(testWaybillSegmentCurrent)).isEqualTo(expectedData)
    }

    @Test
    fun getPayloadReturnDefaultCutoffForLastSegment() {
        val testWaybillSegment = mockWaybillSegment(112L, 0)

        mockOrder(mutableListOf(testWaybillSegment))
        val expectedData = TrackReceivedPlanFactAdditionalData().apply { cutoffTime = DEFAULT_CUTOFF_TIME }

        doReturn(DEFAULT_CUTOFF_TIME).whenever(partnerService).findCutoffTime(any())

        softly.assertThat(createdPlanFactProcessor.getPayload(testWaybillSegment)).isEqualTo(expectedData)
    }

    @Test
    @DisplayName("Проверяем не первый, но подходящий сегмент")
    fun eligibleNotFirst() {
        val order = ffScMvDs(PartnerType.DROPSHIP)
        val waybill: List<WaybillSegment> = order.waybill
        val mv = waybill[2]
        waybill.last().externalId = "ext-id"

        softly.assertThat(createdPlanFactProcessor.isEligible(mv)).isEqualTo(true)
    }

    @Test
    @DisplayName("Проверяем первый  сегмент - заказ не создан на всех сегментах")
    fun eligibleFirstAllNotCreated() {
        softly.assertThat(createdPlanFactProcessor.isEligible(ffScMvDs(PartnerType.DROPSHIP).waybill[0]))
            .isEqualTo(false)
    }

    @Test
    @DisplayName("Проверяем первый  сегмент - заказ создан на всех сегментах кроме первого")
    fun eligibleFirstAllCreated() {
        val waybill: List<WaybillSegment> = ffScMvDs(PartnerType.DROPSHIP).waybill
        waybill[1].apply { externalId = "1" } // sc
        waybill[2].apply { externalId = "2" } // mv
        waybill[3].apply { externalId = "3" } // ds
        val ff = waybill[0]

        softly.assertThat(createdPlanFactProcessor.isEligible(ff)).isEqualTo(false)
    }

    @Test
    @DisplayName("Проверяем первый  сегмент - заказ создан на всех сегментах кроме первого, первый не дропшип")
    fun eligibleFirstAllCreatedFF() {
        val waybill: List<WaybillSegment> = ffScMvDs(PartnerType.FULFILLMENT).waybill
        waybill[1].apply { externalId = "1" } // sc
        waybill[2].apply { externalId = "2" } // mv
        waybill[3].apply { externalId = "3" } // ds
        val ff = waybill[0]

        softly.assertThat(createdPlanFactProcessor.isEligible(ff)).isEqualTo(true)
    }

    companion object {
        private val DEFAULT_CUTOFF_TIME = LocalTime.of(23, 59)
    }
}
