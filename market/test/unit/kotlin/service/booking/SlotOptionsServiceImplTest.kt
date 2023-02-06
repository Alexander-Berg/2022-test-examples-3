package ru.yandex.market.logistics.calendaring.service.booking

import com.nhaarman.mockitokotlin2.anyOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.stubbing.Answer
import ru.yandex.market.logistics.calendaring.booking.FilterSlotsByDistinctTimeStrategy
import ru.yandex.market.logistics.calendaring.client.dto.GetFreeSlotsRequest
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingType
import ru.yandex.market.logistics.calendaring.client.dto.enums.SupplierType
import ru.yandex.market.logistics.calendaring.meta.field.service.MetaFieldService
import ru.yandex.market.logistics.calendaring.model.common.FunResult
import ru.yandex.market.logistics.calendaring.model.common.Success
import ru.yandex.market.logistics.calendaring.model.domain.GateInfo
import ru.yandex.market.logistics.calendaring.model.domain.Slot
import ru.yandex.market.logistics.calendaring.model.dto.GatesScheduleDTO
import ru.yandex.market.logistics.calendaring.model.dto.WarehouseDTO
import ru.yandex.market.logistics.calendaring.repository.SystemPropertyJpaRepository
import ru.yandex.market.logistics.calendaring.service.CalendaringWarehousesService
import ru.yandex.market.logistics.calendaring.service.booking.filter.FilterContext
import ru.yandex.market.logistics.calendaring.service.booking.filter.SlotFilterProcessor
import ru.yandex.market.logistics.calendaring.service.datetime.DateTimeServiceImpl
import ru.yandex.market.logistics.calendaring.service.gates.GateService
import ru.yandex.market.logistics.calendaring.service.limit.LimitDateTimeService
import ru.yandex.market.logistics.calendaring.service.system.SystemPropertyService
import ru.yandex.market.logistics.calendaring.service.validation.PeriodValidationService
import ru.yandex.market.logistics.calendaring.util.DateTimeInterval
import ru.yandex.market.logistics.management.entity.type.GateTypeResponse
import ru.yandex.market.logistics.management.entity.type.PartnerType
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

internal class SlotOptionsServiceImplTest {

    @Test
    fun test() {
        Mockito.mock(MetaFieldService::class.java)

        val systemPropertyRepo = Mockito.mock(SystemPropertyJpaRepository::class.java)
        Mockito.`when`(systemPropertyRepo.findByName(anyOrNull()))
            .thenReturn(null)

        val calendaringWarehousesService = Mockito.mock(CalendaringWarehousesService::class.java)
        val mskOffset = ZoneId.ofOffset("GMT", ZoneOffset.ofHours(3))
        Mockito.`when`(calendaringWarehousesService.getTimeZoneByWarehouseId(anyOrNull()))
            .thenReturn(mskOffset)

        Mockito.`when`(calendaringWarehousesService.getWarehouse(anyOrNull()))
            .thenReturn(WarehouseDTO(1, "1", true, PartnerType.FULFILLMENT))

        val zoneId = ZoneId.systemDefault()
        val instant = LocalDateTime
            .from(DateTimeFormatter.ISO_LOCAL_DATE_TIME
                .parse("2018-01-01T00:00:00"))
            .atZone(zoneId)
            .toInstant()
        val clock = Clock.fixed(instant, zoneId)

        val gateService = Mockito.mock(GateService::class.java)
        Mockito.`when`(gateService.getGatesSchedule(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(Success(GatesScheduleDTO(1L, listOf(
                GateInfo(1L, "gateNumber1", true, EnumSet.of(GateTypeResponse.INBOUND), emptyList()),
                GateInfo(2L, "gateNumber2", true, EnumSet.of(GateTypeResponse.INBOUND), emptyList()),
            ), emptyList())))

        val freeIntervalsService = Mockito.mock(FreeIntervalsService::class.java)
        val unsplitted = listOf(Slot(1, DateTimeInterval.of(
            LocalDate.of(2021, 5, 17),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            mskOffset)))

        val splitted = listOf(Slot(1, DateTimeInterval.of(
            LocalDate.of(2021, 5, 1),
            LocalTime.of(10, 0),
            LocalTime.of(10, 30),
            mskOffset)),
            Slot(1, DateTimeInterval.of(
                LocalDate.of(2021, 5, 1),
                LocalTime.of(10, 30),
                LocalTime.of(11, 0),
                mskOffset)))

        Mockito.`when`(freeIntervalsService.getFreeIntervals(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(unsplitted)

        val splitterService = Mockito.mock(IntervalSplitterService::class.java)
        Mockito.`when`(splitterService.splitByDurationsWithStep(com.nhaarman.mockitokotlin2.eq(unsplitted), anyOrNull(), anyOrNull()))
            .thenReturn(splitted)

        val limitDateTimeService = Mockito.mock(LimitDateTimeService::class.java)
        Mockito.`when`(limitDateTimeService.getQuotaDatesForSlotDate(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(listOf(LocalDate.of(2021, 5, 1)))

        val slotFilterProcessor = Mockito.mock(SlotFilterProcessor::class.java)
        Mockito.`when`(slotFilterProcessor.process(anyOrNull()))
            .thenAnswer(Answer<FunResult<FilterContext, String>> {
                val obj = it.getArgument(0, FilterContext::class.java)!!
                Success(obj)
            })

        val slotOptionsService = SlotOptionsServiceImpl(
            gateService,
            Mockito.mock(BookingService::class.java),
            freeIntervalsService,
            splitterService,
            slotFilterProcessor,
            limitDateTimeService,
            calendaringWarehousesService,
            Mockito.mock(PeriodValidationService::class.java),
            DateTimeServiceImpl(clock),
            SystemPropertyService(systemPropertyRepo)
        )

        val getFreeSlotsRequest = GetFreeSlotsRequest(
            setOf(1L),
            null,
            BookingType.SUPPLY,
            60,
            null,
            LocalDateTime.of(2021, 5, 1, 10, 0, 0),
            LocalDateTime.of(2021, 5, 1, 11, 0, 0),
            SupplierType.THIRD_PARTY,
            "К101",
            10,
            1,
            LocalDate.of(2021, 1, 1),
            listOf(1L)
        )

        val slots = slotOptionsService.getSlots(
            getFreeSlotsRequest = getFreeSlotsRequest,
            filteringStrategy = FilterSlotsByDistinctTimeStrategy()
        )

        assertEquals(1, slots.size)
    }
}

