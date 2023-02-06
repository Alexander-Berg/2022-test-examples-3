package ru.yandex.market.logistics.logistrator.controller

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.refEq
import com.nhaarman.mockitokotlin2.whenever
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import ru.yandex.market.logistics.logistrator.AbstractContextualTest
import ru.yandex.market.logistics.management.entity.request.geoBase.GeoBaseFilter
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter
import ru.yandex.market.logistics.management.entity.request.schedule.CalendarsFilter
import ru.yandex.market.logistics.management.entity.response.LocationResponse
import ru.yandex.market.logistics.management.entity.response.core.Address
import ru.yandex.market.logistics.management.entity.response.core.Phone
import ru.yandex.market.logistics.management.entity.response.legalInfo.LegalInfoResponse
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentServiceDto
import ru.yandex.market.logistics.management.entity.response.partner.PartnerCapacityDto
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParam
import ru.yandex.market.logistics.management.entity.response.partner.PartnerForbiddenCargoTypesDto
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse
import ru.yandex.market.logistics.management.entity.response.partner.PlatformClientDto
import ru.yandex.market.logistics.management.entity.response.point.Contact
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointGateResponse
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse
import ru.yandex.market.logistics.management.entity.response.schedule.CalendarHolidaysResponse
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse
import ru.yandex.market.logistics.management.entity.type.ActivityStatus
import ru.yandex.market.logistics.management.entity.type.CapacityService
import ru.yandex.market.logistics.management.entity.type.CapacityType
import ru.yandex.market.logistics.management.entity.type.CountingType
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType
import ru.yandex.market.logistics.management.entity.type.PartnerStatus
import ru.yandex.market.logistics.management.entity.type.PartnerType
import ru.yandex.market.logistics.management.entity.type.PhoneType
import ru.yandex.market.logistics.management.entity.type.PointType
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.EnumSet
import java.util.Optional

internal class ScOrFfPartnerControllerGetTest : AbstractContextualTest() {

    @BeforeEach
    fun setUp() {
        whenever(clock.zone).thenReturn(ZoneId.of("UTC"))
        whenever(clock.instant()).thenReturn(LocalDateTime.of(2022, 1, 7, 12, 0, 0).toInstant(ZoneOffset.UTC))
    }

    @Test
    fun testGetScOrFfPartner() {
        val partner = mockPartnerLmsResponse()
        mockLogisticsPointLmsResponse(partner.id)
        mockLogisticSegmentsLmsResponse(partner.id)

        mockMvc.perform(get("/partners/sorting-centers/100"))
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/sc_or_ff_get/get_100.json")))
    }

    @Test
    fun testGetScOrFfPartner_partnerNotFound() {
        val partnerId = 404L
        whenever(lmsClient.getPartner(eq(partnerId))).thenReturn(Optional.empty())

        mockMvc.perform(get("/partners/sorting-centers/$partnerId"))
            .andExpect(status().isNotFound)
            .andExpect(errorMessage("SC or FF partner with with id $partnerId is not found"))
    }

    @Test
    fun testGetScOrFfPartner_logisticPointNotFound() {
        val partner = mockPartnerLmsResponse()
        whenever(lmsClient.searchLogisticSegments(refEq(LogisticSegmentFilter.builder()
            .setPartnerIds(setOf(partner.id))
            .build()
        )))
            .thenReturn(emptyList())

        mockMvc.perform(get("/partners/sorting-centers/${partner.id}"))
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent(
                "response/sc_or_ff_get/get_100_wo_logistic_point_and_segments.json"
            )))
    }

    private fun mockPartnerLmsResponse(): PartnerResponse {
        val partner = PartnerResponse.newBuilder()
            .id(100)
            .marketId(100100)
            .partnerType(PartnerType.FULFILLMENT)
            .subtype(PartnerSubtypeResponse.newBuilder().id(100500).build())
            .name("FulfillmentPartner")
            .readableName("Fulfillment Partner")
            .status(PartnerStatus.ACTIVE)
            .platformClients(listOf(
                PlatformClientDto.newBuilder()
                    .id(100)
                    .name("Platform client 1")
                    .status(PartnerStatus.ACTIVE)
                    .build(),
                PlatformClientDto
                    .newBuilder()
                    .id(200)
                    .name("Platform client 2")
                    .status(PartnerStatus.INACTIVE)
                    .build(),
                PlatformClientDto.newBuilder().id(300).name("Platform client 3").status(PartnerStatus.TESTING)
                    .build(),
            ))
            .locationId(200)
            .calendarId(300)
            .params(listOf(
                PartnerExternalParam(PartnerExternalParamType.IS_COMMON.name, "", "true"),
                PartnerExternalParam(PartnerExternalParamType.LOGO.name, "", "https://test.ru/logo.png"),
                PartnerExternalParam(PartnerExternalParamType.DAYS_FOR_RETURN_ORDER.name, "", "7")
            ))
            .build()
        val partnerLocation = LocationResponse.builder().id(partner.locationId).name("Зудово").build()
        val partnerLegalInfo = LegalInfoResponse(
            1,
            partner.id,
            "ООО ТЕСТ",
            1000000000000,
            "https://test.ru",
            "ООО",
            "7777777777",
            "+7(800)700-00-00",
            ADDRESS,
            ADDRESS,
            "roga@kopyta.ru",
            "332211",
            "112233",
            "account"
        )
        val partnerFutureHolidays = listOf(
            CalendarHolidaysResponse.builder().id(partner.calendarId).days(listOf(
                LocalDate.of(2022, 1, 7),
                LocalDate.of(2022, 2, 23)
            ))
                .build()
        )
        val partnerPastHolidays = listOf(
            CalendarHolidaysResponse.builder().id(partner.calendarId).days(listOf(
                LocalDate.of(2022, 1, 1),
                LocalDate.of(2022, 1, 2),
                LocalDate.of(2022, 1, 3)
            ))
                .build()
        )
        val partnerForbiddenCargoTypes = listOf(PartnerForbiddenCargoTypesDto(
            partner.id,
            partner.marketId,
            setOf(111, 141, 199)
        ))

        val capacityValues = listOf(
            PartnerCapacityDto.newBuilder()
                .partnerId(partner.id)
                .locationFrom(RUSSIA_LOCATION)
                .locationTo(NOT_RUSSIA_LOCATION)
                .type(CapacityType.REGULAR)
                .countingType(CountingType.ITEM)
                .capacityService(CapacityService.INBOUND)
                .value(5L)
                .day(null)
                .build(),
            PartnerCapacityDto.newBuilder()
                .partnerId(partner.id)
                .locationFrom(RUSSIA_LOCATION)
                .locationTo(RUSSIA_LOCATION)
                .type(CapacityType.REGULAR)
                .countingType(CountingType.ITEM)
                .capacityService(CapacityService.SHIPMENT)
                .value(100L)
                .day(null)
                .build(),
            PartnerCapacityDto.newBuilder()
                .partnerId(partner.id)
                .locationFrom(RUSSIA_LOCATION)
                .locationTo(RUSSIA_LOCATION)
                .type(CapacityType.REGULAR)
                .countingType(CountingType.ORDER)
                .capacityService(CapacityService.SHIPMENT)
                .value(200L)
                .day(null)
                .build(),
            PartnerCapacityDto.newBuilder()
                .partnerId(partner.id)
                .locationFrom(RUSSIA_LOCATION)
                .locationTo(RUSSIA_LOCATION)
                .type(CapacityType.RESERVE)
                .countingType(CountingType.ITEM)
                .capacityService(CapacityService.SHIPMENT)
                .value(5L)
                .day(LocalDate.parse("2022-07-07"))
                .build(),
        )

        whenever(lmsClient.getPartner(eq(partner.id)))
            .thenReturn(Optional.of(partner))
        whenever(lmsClient.searchLocations(refEq(GeoBaseFilter.builder()
            .setId(setOf(partnerLocation.id.toLong()))
            .build()
        )))
            .thenReturn(listOf(partnerLocation))
        whenever(lmsClient.getPartnerLegalInfo(eq(partner.id)))
            .thenReturn(Optional.of(partnerLegalInfo))
        whenever(lmsClient.getHolidays(refEq(CalendarsFilter.builder()
            .calendarIds(listOf(partner.calendarId))
            .dateFrom(LocalDate.of(2022, 1, 7))
            .dateTo(LocalDate.of(2023, 1, 7))
            .build()
        )))
            .thenReturn(partnerFutureHolidays)
        whenever(lmsClient.getHolidays(refEq(CalendarsFilter.builder()
            .calendarIds(listOf(partner.calendarId))
            .dateFrom(LocalDate.of(2021, 1, 7))
            .dateTo(LocalDate.of(2022, 1, 6))
            .build()
        )))
            .thenReturn(partnerPastHolidays)
        whenever(lmsClient.getPartnerForbiddenCargoTypes(eq(listOf(partner.id))))
            .thenReturn(partnerForbiddenCargoTypes)
        whenever(lmsClient.getPartnerCapacities(partner.id))
            .thenReturn(capacityValues)

        return partner
    }

    private fun mockLogisticsPointLmsResponse(partnerId: Long) {
        val logisticPoint = LogisticsPointResponse.newBuilder()
            .id(500)
            .name("Test logistic point")
            .externalId("500500")
            .contact(Contact("Иван", "Иванов", "Иванович"))
            .phones(setOf(
                Phone("79990000000", "123", "Основной телефон", PhoneType.PRIMARY),
                Phone("79991111111", "456", "Дополнительный телефон", PhoneType.ADDITIONAL)
            ))
            .address(ADDRESS)
            .schedule(setOf(
                ScheduleDayResponse(100, 1, LocalTime.of(9, 0), LocalTime.of(16, 0), true),
                ScheduleDayResponse(101, 2, LocalTime.of(10, 0), LocalTime.of(17, 0), true),
                ScheduleDayResponse(102, 3, LocalTime.of(11, 0), LocalTime.of(18, 0), true),
                ScheduleDayResponse(103, 4, LocalTime.of(12, 0), LocalTime.of(19, 0), true),
                ScheduleDayResponse(104, 5, LocalTime.of(13, 0), LocalTime.of(20, 0), true),
                ScheduleDayResponse(105, 6, LocalTime.of(14, 0), LocalTime.of(21, 0), true),
                ScheduleDayResponse(106, 7, LocalTime.of(15, 0), LocalTime.of(22, 0), true),
            ))
            .calendarId(400)
            .dayOffCalendarId(500)
            .build()
        val logisticPointCalendarHolidays = listOf(
            CalendarHolidaysResponse.builder().id(logisticPoint.calendarId).days(listOf(
                LocalDate.of(2022, 1, 7),
                LocalDate.of(2022, 2, 23)
            ))
                .build()
        )
        val logisticPointDayOffHolidays = listOf(
            CalendarHolidaysResponse.builder().id(logisticPoint.dayOffCalendarId).days(listOf(
                LocalDate.of(2022, 2, 1),
                LocalDate.of(2022, 2, 23)
            ))
                .build()
        )
        val logisticPointGates = listOf(
            LogisticsPointGateResponse.newBuilder()
                .id(1000)
                .gateNumber("G1")
                .enabled(true)
                .schedule(setOf(
                    ScheduleDayResponse(201, 1, LocalTime.of(9, 0), LocalTime.of(15, 0), true),
                    ScheduleDayResponse(202, 2, LocalTime.of(10, 0), LocalTime.of(16, 0), true),
                    ScheduleDayResponse(203, 3, LocalTime.of(11, 0), LocalTime.of(17, 0), true),
                    ScheduleDayResponse(204, 4, LocalTime.of(12, 0), LocalTime.of(18, 0), true),
                    ScheduleDayResponse(205, 5, LocalTime.of(13, 0), LocalTime.of(19, 0), true),
                    ScheduleDayResponse(206, 6, LocalTime.of(14, 0), LocalTime.of(20, 0), true),
                    ScheduleDayResponse(207, 7, LocalTime.of(15, 0), LocalTime.of(21, 0), true),
                ))
                .types(EnumSet.allOf(ru.yandex.market.logistics.management.entity.type.GateTypeResponse::class.java))
                .build(),
            LogisticsPointGateResponse.newBuilder()
                .id(1002)
                .gateNumber("G2")
                .enabled(false)
                .types(EnumSet.of(ru.yandex.market.logistics.management.entity.type.GateTypeResponse.INBOUND))
                .build()
        )

        whenever(lmsClient.getLogisticsPoints(refEq(LogisticsPointFilter.newBuilder()
            .partnerIds(setOf(partnerId))
            .type(PointType.WAREHOUSE)
            .build()
        )))
            .thenReturn(listOf(logisticPoint))
        whenever(lmsClient.getHolidays(refEq(CalendarsFilter.builder()
            .calendarIds(listOf(logisticPoint.calendarId))
            .dateFrom(LocalDate.of(2022, 1, 7))
            .dateTo(LocalDate.of(2023, 1, 7))
            .build()
        )))
            .thenReturn(logisticPointCalendarHolidays)
        whenever(lmsClient.getHolidays(refEq(CalendarsFilter.builder()
            .calendarIds(listOf(logisticPoint.dayOffCalendarId))
            .dateFrom(LocalDate.of(2022, 1, 7))
            .dateTo(LocalDate.of(2023, 1, 7))
            .build()
        )))
            .thenReturn(logisticPointDayOffHolidays)
        whenever(lmsClient.getLogisticsPointGates(eq(logisticPoint.id)))
            .thenReturn(logisticPointGates)
    }

    private fun mockLogisticSegmentsLmsResponse(partnerId: Long) {
        val logisticSegments = listOf(
            LogisticSegmentDto()
                .setId(1000)
                .setName("Активный склад")
                .setLocationId(300)
                .setType(LogisticSegmentType.WAREHOUSE)
                .setServices(listOf(
                    LogisticSegmentServiceDto.builder()
                        .setId(1001)
                        .setStatus(ActivityStatus.ACTIVE)
                        .setCode(ServiceCodeName.INBOUND)
                        .setDuration(Duration.ofHours(1))
                        .build(),
                    LogisticSegmentServiceDto.builder()
                        .setId(1002)
                        .setStatus(ActivityStatus.INACTIVE)
                        .setCode(ServiceCodeName.PROCESSING)
                        .setDuration(Duration.ofHours(2))
                        .build(),
                )),
            LogisticSegmentDto()
                .setId(2000)
                .setName("Неактивный склад")
                .setLocationId(400)
                .setType(LogisticSegmentType.WAREHOUSE)
                .setServices(listOf(
                    LogisticSegmentServiceDto.builder()
                        .setId(2001)
                        .setStatus(ActivityStatus.INACTIVE)
                        .setCode(ServiceCodeName.INBOUND)
                        .setDuration(Duration.ofHours(3))
                        .build(),
                    LogisticSegmentServiceDto.builder()
                        .setId(2002)
                        .setStatus(ActivityStatus.INACTIVE)
                        .setCode(ServiceCodeName.PROCESSING)
                        .setDuration(Duration.ofHours(4))
                        .build(),
                ))
        )
        val activeWarehouseLocation = LocationResponse.builder()
            .id(logisticSegments[0].locationId)
            .name("Киряково")
            .build()
        val inactiveWarehouseLocation = LocationResponse.builder()
            .id(logisticSegments[1].locationId)
            .name("Козловка")
            .build()

        whenever(lmsClient.searchLogisticSegments(refEq(LogisticSegmentFilter.builder()
            .setPartnerIds(setOf(partnerId))
            .build()
        )))
            .thenReturn(logisticSegments)
        whenever(lmsClient.searchLocations(refEq(GeoBaseFilter.builder()
            .setId(setOf(activeWarehouseLocation.id.toLong()))
            .build()
        )))
            .thenReturn(listOf(activeWarehouseLocation))
        whenever(lmsClient.searchLocations(refEq(GeoBaseFilter.builder()
            .setId(setOf(inactiveWarehouseLocation.id.toLong()))
            .build()
        )))
            .thenReturn(listOf(inactiveWarehouseLocation))
    }

    private companion object {
        val ADDRESS: Address = Address.newBuilder()
            .addressString("село Зудово, Болотнинский район, Новосибирская область, Россия, Солнечная улица, 9A, 2")
            .apartment("318")
            .building("A")
            .house("6")
            .housing("2")
            .latitude(55.822463.toBigDecimal())
            .longitude(84.258002.toBigDecimal())
            .locationId(133543)
            .postCode("633372")
            .settlement("Юридическое село")
            .country("Россия")
            .shortAddressString("село Зудово, Солнечная улица, 9A, 2")
            .subRegion("Солнечная")
            .exactLocationId(133543)
            .build()

        private const val RUSSIA_LOCATION = 225

        private const val NOT_RUSSIA_LOCATION = 22
    }
}
