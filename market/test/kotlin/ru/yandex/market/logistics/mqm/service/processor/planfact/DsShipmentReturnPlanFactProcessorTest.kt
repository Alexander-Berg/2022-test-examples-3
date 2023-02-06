package ru.yandex.market.logistics.mqm.service.processor.planfact

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import ru.yandex.market.common.util.DateTimeUtils
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamResponse
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType
import ru.yandex.market.logistics.mqm.entity.lom.WaybillSegment
import ru.yandex.market.logistics.mqm.entity.lom.embedded.DeliveryInterval
import ru.yandex.market.logistics.mqm.entity.lom.embedded.WaybillShipment
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerSubtype
import ru.yandex.market.logistics.mqm.entity.lom.enums.PartnerType
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentStatus
import ru.yandex.market.logistics.mqm.entity.lom.enums.SegmentType
import ru.yandex.market.logistics.mqm.entity.lom.enums.WaybillSegmentTag
import ru.yandex.market.logistics.mqm.service.lms.LmsPartnerService
import ru.yandex.market.logistics.mqm.service.logging.LogService
import ru.yandex.market.logistics.mqm.utils.TestableSettingsService
import ru.yandex.market.logistics.mqm.utils.createWaybillSegmentWithCheckpoint
import ru.yandex.market.logistics.mqm.utils.joinInOrder
import ru.yandex.market.logistics.util.client.exception.HttpTemplateException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class DsShipmentReturnPlanFactProcessorTest {

    @Mock
    private lateinit var lmsPartnerService: LmsPartnerService

    @Mock
    private lateinit var logService: LogService

    private val settingService = TestableSettingsService()
    private lateinit var processor: DsShipmentReturnPlanFactProcessor

    @BeforeEach
    fun setUp() {
        processor = DsShipmentReturnPlanFactProcessor(
            settingService = settingService,
            logService = logService,
            lmsPartnerService = lmsPartnerService,
            clock = Clock.fixed(Instant.parse("2020-02-14T11:15:25.00Z"), DateTimeUtils.MOSCOW_ZONE),
            lrmReturnService = mock()
        )
    }

    @Test
    @DisplayName("Расчет ожидаемого времени после получения 70 чекпоинта")
    fun calculateExpectedDatetime() {
        val expectedDeadline = FIXED_TIME.plus(DsShipmentReturnPlanFactProcessor.DEFAULT_TIMEOUT)
        processor.calculateExpectedDatetime(createSegment()) shouldBe expectedDeadline
    }

    @Test
    @DisplayName("Расчет ожидаемого времени после получения 70 чп для контрактной СД при наличии настройки в LMS")
    fun calculateExpectedDateTimeWithPartnerContractDeliverySubtypeAndLmsSetting() {
        val expectedDeadline = FIXED_TIME.plus(Duration.ofDays(18))
        whenever(
            lmsPartnerService.getPartnerExternalParam(
                1L,
                PartnerExternalParamType.DAYS_FOR_CONTRACT_DS_SHIPMENT_RETURN
            )
        )
            .thenReturn(PartnerExternalParamResponse.newBuilder().value("10").build())

        processor.calculateExpectedDatetime(
            createSegment(
                partnerSubtype = PartnerSubtype.PARTNER_CONTRACT_DELIVERY
            )
        ) shouldBe expectedDeadline
    }

    @Test
    @DisplayName("Расчет ожидаемого времени после получения 70 чп для контрактной СД при отсутствии настройки в LMS")
    fun calculateExpectedDateTimeWithPartnerContractDeliveryAndNoLmsSetting() {
        val expectedDeadline = FIXED_TIME.plus(Duration.ofDays(10))
        whenever(
            lmsPartnerService.getPartnerExternalParam(
                1L,
                PartnerExternalParamType.DAYS_FOR_CONTRACT_DS_SHIPMENT_RETURN
            )
        )
            .thenThrow(HttpTemplateException::class.java)

        processor.calculateExpectedDatetime(
            createSegment(
                partnerSubtype = PartnerSubtype.PARTNER_CONTRACT_DELIVERY
            )
        ) shouldBe expectedDeadline
    }

    @Test
    @DisplayName("Проверка применимости процессора при типе предыдущего сегмента FULFILLMENT")
    fun isEligibleWithFulfillmentSegmentType() {
        processor.isEligible(createSegment(previousSegmentType = SegmentType.FULFILLMENT)) shouldBe true
    }

    @Test
    @DisplayName("Проверка применимости процессора при типе предыдущего сегмента SORTING_CENTER")
    fun isEligibleWithSortingCenterSegmentType() {
        processor.isEligible(createSegment(previousSegmentType = SegmentType.SORTING_CENTER)) shouldBe true
    }

    @Test
    @DisplayName("Процессор неприменим, если partnerType неверный")
    fun isNotEligibleIfPartnerTypeIsWrong() {
        processor.isEligible(createSegment(partnerType = PartnerType.OWN_DELIVERY)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим, если waybill из express заказа")
    fun isNotEligibleIfWaybillFromExpressOrder() {
        processor.isEligible(createSegment(withTag = true)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим, если сегмент является дропоффом")
    fun isEligibleWhenPartnerIsNotDropoff() {
        processor.isEligible(createSegment(isDropoff = true)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим если нет 70 чекпоинта в истории")
    fun isNonEligibleWithoutCheckpointInHistory() {
        processor.isEligible(createSegment(segmentStatus = SegmentStatus.IN)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим, если нет предыдущего сегмента")
    fun isNotEligibleIfNoPreviousSegment() {
        processor.isEligible(createSegment(hasPreviousSegment = false)) shouldBe false
    }

    @Test
    @DisplayName("Процессор неприменим, если предыдущий сегмент имеет статус RETURN_ARRIVED")
    fun isNotEligibleIfPreviousSegmentHasReturnArrivedStatus() {
        processor.isEligible(
            createSegment(
                previousSegmentType = SegmentType.FULFILLMENT,
                previousSegmentStatus = SegmentStatus.RETURN_ARRIVED
            )
        ) shouldBe false
    }

    private fun createSegment(
        partnerType: PartnerType = PartnerType.DELIVERY,
        partnerSubtype: PartnerSubtype = PartnerSubtype.DROPOFF,
        segmentStatus: SegmentStatus = SegmentStatus.RETURN_ARRIVED,
        previousSegmentStatus: SegmentStatus = SegmentStatus.OUT,
        previousSegmentType: SegmentType = SegmentType.FULFILLMENT,
        withTag: Boolean = false,
        hasPreviousSegment: Boolean = true,
        isDropoff: Boolean = false
    ): WaybillSegment {
        val currentSegment = createWaybillSegmentWithCheckpoint(SegmentType.COURIER, segmentStatus)
        currentSegment.partnerType = partnerType
        currentSegment.partnerSubtype = partnerSubtype
        currentSegment.shipment = WaybillShipment(
            date = LocalDate.of(2020, 12, 20)
        )

        if (isDropoff) {
            currentSegment.partnerType = PartnerType.DELIVERY
            currentSegment.segmentType = SegmentType.SORTING_CENTER
        }

        if (withTag) currentSegment.apply {
            waybillSegmentTags = mutableSetOf(WaybillSegmentTag.CALL_COURIER)
        }

        if (hasPreviousSegment) {
            val previousSegment = createWaybillSegmentWithCheckpoint(previousSegmentType, previousSegmentStatus)
            joinInOrder(listOf(previousSegment, currentSegment))
                .apply {
                    deliveryInterval = DeliveryInterval(
                        deliveryDateMax = LocalDate.of(2020, 12, 28)
                    )
                }
        }

        return currentSegment
    }

    companion object {
        private val FIXED_TIME = Instant.parse("2021-01-02T09:00:00.00Z")
    }
}
