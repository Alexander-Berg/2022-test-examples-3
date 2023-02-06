package ru.yandex.market.logistics.calendaring.service.filter

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import ru.yandex.market.logistics.calendaring.base.SoftAssertionSupport
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType
import ru.yandex.market.logistics.calendaring.model.common.Failure
import ru.yandex.market.logistics.calendaring.model.common.Success
import ru.yandex.market.logistics.calendaring.model.domain.RequestSize
import ru.yandex.market.logistics.calendaring.model.domain.Slot
import ru.yandex.market.logistics.calendaring.model.domain.SlotDate
import ru.yandex.market.logistics.calendaring.model.dto.limits.GetLimitsDTO
import ru.yandex.market.logistics.calendaring.service.booking.filter.FilterContext
import ru.yandex.market.logistics.calendaring.service.booking.filter.SlotFilterByQuota
import ru.yandex.market.logistics.calendaring.service.limit.LimitDateTimeService
import ru.yandex.market.logistics.calendaring.service.limit.LimitService
import ru.yandex.market.logistics.calendaring.service.system.SystemPropertyService
import ru.yandex.market.logistics.calendaring.service.system.keys.SystemPropertyIntegerKey
import ru.yandex.market.logistics.calendaring.util.DateTimeInterval
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime


class SlotFilterByQuotaTest() : SoftAssertionSupport() {

    private val limitService: LimitService = mock(LimitService::class.java)
    private val limitDateTimeService: LimitDateTimeService = mock(LimitDateTimeService::class.java)
    private val systemPropertyService: SystemPropertyService = mock(SystemPropertyService::class.java)
    private val slotFilterByQuota: SlotFilterByQuota =
        SlotFilterByQuota(limitService, systemPropertyService)


    @BeforeEach
    fun mockSlotFilterByQuota() {
        `when`(systemPropertyService.getProperty(eq(SystemPropertyIntegerKey.GET_AVAILABLE_QUOTA_CHUNK_SIZE)))
            .thenReturn(30)
    }

    @Test
    fun whenHaveLimitHaveSlots() {

        val supplierType = SupplierType.THIRD_PARTY
        val warehouseId = 1L
        val bookingType = BookingType.SUPPLY

        `when`(
            limitService.getAvailableRequestSizeForDates(any())
        ).thenReturn(mapOf(LocalDate.of(2020, 5, 1) to RequestSize(10, 1)))

        val start = ZonedDateTime.of(2020, 5, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        val context = FilterContext(
            warehouseId,
            null,
            null,
            bookingType,
            supplierType,
            RequestSize(10, 1),
            listOf(
                SlotDate(
                    start.toLocalDate(),
                    listOf(
                        Slot(1L, DateTimeInterval(start, start.plusHours(1))),
                        Slot(1L, DateTimeInterval(start.plusHours(2), start.plusHours(3)))
                    ),
                    listOf(LocalDate.of(2020, 5, 1))
                )
            ),
            null,
            emptyList(),
            null
        )

        val filterResult = slotFilterByQuota.filter(context)
        softly.assertThat(filterResult).isExactlyInstanceOf(Success::class.java)

        if (filterResult is Success) {
            softly.assertThat(filterResult.value.slotCandidates).isNotEmpty
        }

        val captor = argumentCaptor<GetLimitsDTO>()
        verify(limitService, times(1)).getAvailableRequestSizeForDates(captor.capture())
        softly.assertThat(captor.lastValue.supplierType).isEqualTo(supplierType)
        softly.assertThat(captor.lastValue.warehouseId).isEqualTo(warehouseId)
        softly.assertThat(captor.lastValue.bookingType).isEqualTo(bookingType)
    }

    @Test
    fun whenNoLimitNoSlots() {

        val supplierType = SupplierType.THIRD_PARTY
        val warehouseId = 1L
        val bookingType = BookingType.SUPPLY

        `when`(
            limitService.getAvailableRequestSizeForDates(any())
        ).thenReturn(mapOf(LocalDate.of(2020, 5, 1) to RequestSize(5, 1)))

        val start = ZonedDateTime.of(2020, 5, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        val context = FilterContext(
            warehouseId,
            null,
            null,
            bookingType,
            supplierType,
            RequestSize(10, 1),
            listOf(
                SlotDate(
                    start.toLocalDate(),
                    listOf(
                        Slot(1L, DateTimeInterval(start, start.plusHours(1))),
                        Slot(1L, DateTimeInterval(start.plusHours(2), start.plusHours(3)))
                    ),
                    listOf(LocalDate.of(2020, 5, 1))
                )
            ),
            null,
            emptyList(),
            null
        )

        val filterResult = slotFilterByQuota.filter(context)
        softly.assertThat(filterResult).isExactlyInstanceOf(Failure::class.java)

        val captor = argumentCaptor<GetLimitsDTO>()
        verify(limitService, times(1)).getAvailableRequestSizeForDates(captor.capture())
        softly.assertThat(captor.lastValue.supplierType).isEqualTo(supplierType)
        softly.assertThat(captor.lastValue.warehouseId).isEqualTo(warehouseId)
        softly.assertThat(captor.lastValue.bookingType).isEqualTo(bookingType)
    }
}
