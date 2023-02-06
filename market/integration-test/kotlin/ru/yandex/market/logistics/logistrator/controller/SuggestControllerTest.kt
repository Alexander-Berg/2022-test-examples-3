package ru.yandex.market.logistics.logistrator.controller

import com.google.common.collect.ImmutableSet
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import net.javacrumbs.jsonunit.spring.JsonUnitResultMatchers.json
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatcher
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.shaded.com.google.common.collect.ImmutableList
import ru.yandex.market.logistics.logistrator.AbstractContextualTest
import ru.yandex.market.logistics.management.entity.page.PageRequest
import ru.yandex.market.logistics.management.entity.page.PageResult
import ru.yandex.market.logistics.management.entity.request.geoBase.GeoBaseFilter
import ru.yandex.market.logistics.management.entity.request.legalInfo.LegalInfoFilter
import ru.yandex.market.logistics.management.entity.request.logistic.segment.LogisticSegmentFilter
import ru.yandex.market.logistics.management.entity.request.logistic.service.LogisticServiceFilter
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter
import ru.yandex.market.logistics.management.entity.response.LocationResponse
import ru.yandex.market.logistics.management.entity.response.core.Address
import ru.yandex.market.logistics.management.entity.response.legalInfo.LegalInfoResponse
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentDto
import ru.yandex.market.logistics.management.entity.response.logistic.segment.LogisticSegmentServiceDto
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamTypeResponse
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse
import ru.yandex.market.logistics.management.entity.response.partner.PartnerSubtypeResponse
import ru.yandex.market.logistics.management.entity.response.partner.PlatformClientResponse
import ru.yandex.market.logistics.management.entity.response.schedule.ScheduleDayResponse
import ru.yandex.market.logistics.management.entity.response.tariff.CargoTypeDto
import ru.yandex.market.logistics.management.entity.type.ActivityStatus
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType
import ru.yandex.market.logistics.management.entity.type.PartnerStatus
import ru.yandex.market.logistics.management.entity.type.PartnerType
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName
import ru.yandex.market.logistics.management.entity.type.ServiceType
import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

@DisplayName("Предложения для поиска")
internal class SuggestControllerTest : AbstractContextualTest() {

    @Test
    @DisplayName("Получить информацию о партнерах")
    fun suggestPartners() {
        val search = "fulfillment"
        val type = PartnerType.FULFILLMENT
        val subtype = PartnerSubtypeResponse.newBuilder()
            .id(1L)
            .name("Some subtype")
            .build()
        val filter = createSearchPartnerFilter(search, type, setOf(subtype.id))

        val partner = PartnerResponse.newBuilder()
            .id(1L)
            .marketId(829721L)
            .partnerType(type)
            .subtype(subtype)
            .name("FulfillmentService1")
            .readableName("Fulfillment service 1")
            .codeName("fulfillment_service_1")
            .abbreviation("ФФ СЦ №1 по РФ")
            .status(PartnerStatus.ACTIVE)
            .locationId(255)
            .trackingType("tt1")
            .billingClientId(123L)
            .rating(1)
            .domain("first.ff.example.com")
            .logoUrl(null)
            .params(com.google.common.collect.ImmutableList.of())
            .intakeSchedule(com.google.common.collect.ImmutableList.of())
            .build()

        whenever(lmsClient.searchPartners(eq(filter))).thenReturn(ImmutableList.of(partner))

        mockMvc.perform(get("/suggests/partners")
            .queryParam("search", search)
            .queryParam("types", type.name)
            .queryParam("subTypes", subtype.id.toString())
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/partner_suggestion.json")))
    }

    @Test
    @DisplayName("Постранично получить информацию о партнерах")
    fun suggestPartnersPaged() {
        val search = "fulfillment"
        val type = PartnerType.FULFILLMENT
        val subtype = PartnerSubtypeResponse.newBuilder()
            .id(1L)
            .name("Some subtype")
            .build()
        val filter = createSearchPartnerFilter(search, type, setOf(subtype.id))
        val pageNumber = 0
        val pageSize = 1

        val partner = PartnerResponse.newBuilder()
            .id(1L)
            .marketId(829721L)
            .partnerType(type)
            .subtype(subtype)
            .name("FulfillmentService1")
            .readableName("Fulfillment service 1")
            .codeName("fulfillment_service_1")
            .abbreviation("ФФ СЦ №1 по РФ")
            .status(PartnerStatus.ACTIVE)
            .locationId(255)
            .trackingType("tt1")
            .billingClientId(123L)
            .rating(1)
            .domain("first.ff.example.com")
            .logoUrl(null)
            .params(com.google.common.collect.ImmutableList.of())
            .intakeSchedule(com.google.common.collect.ImmutableList.of())
            .build()

        whenever(lmsClient.searchPartners(eq(filter), eq(PageRequest(pageNumber, pageSize))))
            .thenReturn(createSingeElementPageResult(partner))

        mockMvc.perform(get("/suggests/partners-paged")
            .queryParam("search", search)
            .queryParam("types", type.name)
            .queryParam("subTypes", subtype.id.toString())
            .queryParam("page", pageNumber.toString())
            .queryParam("size", pageSize.toString())
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/partner_suggestion.json")))
    }

    @Test
    @DisplayName("Получить информацию о логистических сегментах")
    fun suggestSegments() {
        val search = "10002"
        val partnerId = 2L
        val filter = createLogisticSegmentFilter(search, partnerId)

        val segment = LogisticSegmentDto()
            .setId(10002L)
            .setName("Movement segment")
            .setType(LogisticSegmentType.MOVEMENT)
            .setLocationId(1002)
            .setPartnerId(partnerId)
            .setServices(ImmutableList.of())
            .setNextSegmentIds(ImmutableList.of(10003L))
            .setPreviousSegmentIds(ImmutableList.of(10001L))

        whenever(lmsClient.searchLogisticSegments(eq(filter))).thenReturn(ImmutableList.of(segment))

        mockMvc.perform(get("/suggests/segments")
            .queryParam("search", search)
            .queryParam("partnerId", partnerId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/segment_suggestion.json")))
    }

    @Test
    @DisplayName("Постранично получить информацию о логистических сегментах")
    fun suggestSegmentsPaged() {
        val search = "10002"
        val partnerId = 2L
        val filter = createLogisticSegmentFilter(search, partnerId)
        val pageNumber = 0
        val pageSize = 1

        val segment = LogisticSegmentDto()
            .setId(10002L)
            .setName("Movement segment")
            .setType(LogisticSegmentType.MOVEMENT)
            .setLocationId(1002)
            .setPartnerId(partnerId)
            .setServices(ImmutableList.of())
            .setNextSegmentIds(ImmutableList.of(10003L))
            .setPreviousSegmentIds(ImmutableList.of(10001L))

        whenever(lmsClient.searchLogisticSegments(eq(filter), eq(PageRequest(pageNumber, pageSize))))
            .thenReturn(createSingeElementPageResult(segment))

        mockMvc.perform(get("/suggests/segments-paged")
            .queryParam("search", search)
            .queryParam("partnerId", partnerId.toString())
            .queryParam("page", pageNumber.toString())
            .queryParam("size", pageSize.toString())
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/segment_suggestion.json")))
    }

    @Test
    @DisplayName("Получить информацию о сервисах логистических сегментов")
    fun suggestServices() {
        val search = "10002"
        val segmentId = 2L
        val filter = createLogisticServiceFilter(search, segmentId)

        val service = LogisticSegmentServiceDto.builder()
            .setId(1L)
            .setStatus(ActivityStatus.ACTIVE)
            .setCode(ServiceCodeName.PROCESSING)
            .setType(ServiceType.INTERNAL)
            .setDuration(Duration.ofHours(10L))
            .setPrice(10)
            .setSchedule(ImmutableList.of(
                ScheduleDayResponse(6L, 1, LocalTime.of(10, 0, 0), LocalTime.of(19, 0, 0), true),
                ScheduleDayResponse(7L, 4, LocalTime.of(10, 0, 0), LocalTime.of(19, 0, 0), true),
                ScheduleDayResponse(8L, 3, LocalTime.of(10, 0, 0), LocalTime.of(19, 0, 0), true),
                ScheduleDayResponse(9L, 5, LocalTime.of(10, 0, 0), LocalTime.of(19, 0, 0), true),
                ScheduleDayResponse(10L, 2, LocalTime.of(10, 0, 0), LocalTime.of(19, 0, 0), true),
            ))
            .setDaysOff(ImmutableList.of(
                LocalDate.of(2021, 6, 1),
                LocalDate.of(2021, 6, 2),
            ))
            .setFrozen(false)
            .build()

        whenever(lmsClient.searchLogisticService(eq(filter))).thenReturn(ImmutableList.of(service))

        mockMvc.perform(get("/suggests/services")
            .queryParam("search", search)
            .queryParam("segmentId", segmentId.toString())
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/service_suggestion.json")))
    }

    @Test
    @DisplayName("Постранично получить информацию о сервисах логистических сегментов")
    fun suggestServicesPaged() {
        val search = "10002"
        val segmentId = 2L
        val filter = createLogisticServiceFilter(search, segmentId)
        val pageNumber = 0
        val pageSize = 1

        val service = LogisticSegmentServiceDto.builder()
            .setId(1L)
            .setStatus(ActivityStatus.ACTIVE)
            .setCode(ServiceCodeName.PROCESSING)
            .setType(ServiceType.INTERNAL)
            .setDuration(Duration.ofHours(10L))
            .setPrice(10)
            .setSchedule(ImmutableList.of(
                ScheduleDayResponse(6L, 1, LocalTime.of(10, 0, 0), LocalTime.of(19, 0, 0), true),
                ScheduleDayResponse(7L, 4, LocalTime.of(10, 0, 0), LocalTime.of(19, 0, 0), true),
                ScheduleDayResponse(8L, 3, LocalTime.of(10, 0, 0), LocalTime.of(19, 0, 0), true),
                ScheduleDayResponse(9L, 5, LocalTime.of(10, 0, 0), LocalTime.of(19, 0, 0), true),
                ScheduleDayResponse(10L, 2, LocalTime.of(10, 0, 0), LocalTime.of(19, 0, 0), true),
            ))
            .setDaysOff(ImmutableList.of(
                LocalDate.of(2021, 6, 1),
                LocalDate.of(2021, 6, 2),
            ))
            .setFrozen(false)
            .build()

        whenever(lmsClient.searchLogisticService(eq(filter), eq(PageRequest(pageNumber, pageSize))))
            .thenReturn(createSingeElementPageResult(service))

        mockMvc.perform(get("/suggests/services-paged")
            .queryParam("search", search)
            .queryParam("segmentId", segmentId.toString())
            .queryParam("page", pageNumber.toString())
            .queryParam("size", pageSize.toString())
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/service_suggestion.json")))
    }

    @Test
    @DisplayName("Получить юридическую информацию")
    fun suggestLegalInfos() {
        val search = "ТЕСТ"
        val filterMatcher = ArgumentMatcher<LegalInfoFilter> { argument -> argument?.searchQuery.equals(search) }

        val address = Address.newBuilder()
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
        val legalInfo = LegalInfoResponse(
            1L,
            null,
            "ООО ТЕСТ",
            1000000000000,
            "https://test.ru",
            "ООО",
            "7777777777",
            "+7(800)700-00-00",
            address,
            address,
            "roga@kopyta.ru",
            "332211",
            "112233",
            "account"
        )

        whenever(lmsClient.searchLegalInfo(argThat(filterMatcher))).thenReturn(ImmutableList.of(legalInfo))

        mockMvc.perform(get("/suggests/legal-infos")
            .queryParam("search", search)
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/legal_info_suggestion.json")))
    }

    @Test
    @DisplayName("Постранично получить юридическую информацию")
    fun suggestLegalInfosPaged() {
        val search = "ТЕСТ"
        val filterMatcher = ArgumentMatcher<LegalInfoFilter> { argument -> argument?.searchQuery.equals(search) }
        val pageNumber = 0
        val pageSize = 1

        val address = Address.newBuilder()
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
        val legalInfo = LegalInfoResponse(
            1L,
            null,
            "ООО ТЕСТ",
            1000000000000,
            "https://test.ru",
            "ООО",
            "7777777777",
            "+7(800)700-00-00",
            address,
            address,
            "roga@kopyta.ru",
            "332211",
            "112233",
            "account"
        )

        whenever(lmsClient.searchLegalInfo(argThat(filterMatcher), eq(PageRequest(pageNumber, pageSize))))
            .thenReturn(createSingeElementPageResult(legalInfo))

        mockMvc.perform(get("/suggests/legal-infos-paged")
            .queryParam("search", search)
            .queryParam("page", pageNumber.toString())
            .queryParam("size", pageSize.toString())
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/legal_info_suggestion.json")))
    }

    @Test
    @DisplayName("Получить информацию о локациях")
    fun suggestLocations() {
        val search = "Санкт"
        val filterMatcher = ArgumentMatcher<GeoBaseFilter> { argument -> argument?.searchQuery.equals(search) }

        val location = LocationResponse.builder()
            .id(1)
            .name("Санкт-Петербург")
            .build()

        whenever(lmsClient.searchLocations(argThat(filterMatcher))).thenReturn(ImmutableList.of(location))

        mockMvc.perform(get("/suggests/locations")
            .queryParam("search", search)
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/location_suggestion.json")))
    }

    @Test
    @DisplayName("Постранично получить информацию о локациях")
    fun suggestLocationsPaged() {
        val search = "Санкт"
        val filterMatcher = ArgumentMatcher<GeoBaseFilter> { argument -> argument?.searchQuery.equals(search) }
        val pageNumber = 0
        val pageSize = 1

        val location = LocationResponse.builder()
            .id(1)
            .name("Санкт-Петербург")
            .build()

        whenever(lmsClient.searchLocations(argThat(filterMatcher), eq(PageRequest(pageNumber, pageSize))))
            .thenReturn(createSingeElementPageResult(location))

        mockMvc.perform(get("/suggests/locations-paged")
            .queryParam("search", search)
            .queryParam("page", pageNumber.toString())
            .queryParam("size", pageSize.toString())
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/location_suggestion.json")))
    }

    @Test
    @DisplayName("Получить список всех возможных типов партнера")
    fun suggestPartnerTypes() {
        whenever(lmsClient.getPartnerTypeOptions()).thenReturn(ImmutableList.of(
            PartnerType.FULFILLMENT,
            PartnerType.DELIVERY
        ))

        mockMvc.perform(get("/suggests/partner-types"))
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/partner_type_suggestion.json")))
    }

    @Test
    @DisplayName("Получить список всех возможных подтипов партнера")
    fun suggestPartnerSubtypes() {
        val partnerType = PartnerType.FULFILLMENT

        whenever(lmsClient.getPartnerSubtypeOptions(eq(partnerType))).thenReturn(ImmutableList.of(
            PartnerSubtypeResponse.newBuilder()
                .id(1L)
                .name("Market courier")
                .build(),
            PartnerSubtypeResponse.newBuilder()
                .id(2L)
                .name("Market PVZ")
                .build()
        ))

        mockMvc.perform(get("/suggests/partner-subtypes")
            .queryParam("partnerType", partnerType.name)
        )
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/partner_subtype_suggestion.json")))
    }

    @Test
    @DisplayName("Получить список всех cargo types")
    fun suggestCargoTypes() {
        whenever(lmsClient.getAllCargoTypes()).thenReturn(ImmutableList.of(
            CargoTypeDto(1L, 123, "Cargo type A"),
            CargoTypeDto(2L, 456, "Cargo type B"),
            CargoTypeDto(3L, 789, "Cargo type C"),
        ))

        mockMvc.perform(get("/suggests/cargo-types"))
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/cargo_type_suggestion.json")))
    }

    @Test
    @DisplayName("Получить список всех клиентов платформы")
    fun suggestPlatformClients() {
        whenever(lmsClient.getPlatformClientOptions()).thenReturn(ImmutableList.of(
            PlatformClientResponse.builder()
                .id(1L)
                .name("Beru")
                .build(),
            PlatformClientResponse.builder()
                .id(2L)
                .name("Bringly")
                .build()
        ))

        mockMvc.perform(get("/suggests/platform-clients"))
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/platform_client_suggestion.json")))
    }

    @Test
    @DisplayName("Получить все возможные параметры партнёра")
    fun suggestPartnerParameters() {
        whenever(lmsClient.getPartnerExternalParamTypeOptions()).thenReturn(ImmutableList.of(
            PartnerExternalParamTypeResponse.builder()
                .id(1L)
                .key("LOGO")
                .description("Logo")
                .build(),
            PartnerExternalParamTypeResponse.builder()
                .id(2L)
                .key("IS_COMMON")
                .description("Is common")
                .build(),
            PartnerExternalParamTypeResponse.builder()
                .id(3L)
                .key("DAYS_FOR_RETURN_ORDER")
                .description("Days for return order")
                .build(),
            PartnerExternalParamTypeResponse.builder()
                .id(4L)
                .key("LAST_MILE_RECIPIENT_DEADLINE")
                .description("Last mile recipient deadline")
                .build()
        ))

        mockMvc.perform(get("/suggests/partner-parameters"))
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/partner_parameter_suggestion.json")))
    }

    @Test
    @DisplayName("Получить списки типов сервисов по умолчанию и обязательных типов сервисов для каждого типа сегмента")
    fun suggestSegmentServiceMap() {
        mockMvc.perform(get("/suggests/segment-service-map"))
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent("response/suggest/segment_service_map_suggestion.json")))
    }

    @Test
    @DisplayName("Получить все существующие типы возможных изменений заказа")
    fun suggestPossibleOrderChangeTypes() {
        mockMvc.perform(get("/suggests/possible-order-change-types"))
            .andExpect(status().isOk)
            .andExpect(json().isEqualTo(extractFileContent(
                "response/suggest/possible_order_change_types_suggestion.json"
            )))
    }

    private fun createSearchPartnerFilter(search: String, type: PartnerType, subtypes: Set<Long>) =
        SearchPartnerFilter.builder()
            .setSearchQuery(search)
            .setTypes(ImmutableSet.of(type))
            .setPartnerSubTypeIds(subtypes)
            .build()

    private fun createLogisticSegmentFilter(search: String, partnerId: Long) =
        LogisticSegmentFilter.builder()
            .setSearchQuery(search)
            .setPartnerIds(ImmutableSet.of(partnerId))
            .build()

    private fun createLogisticServiceFilter(search: String, segmentId: Long) =
        LogisticServiceFilter.builder()
            .setSearchQuery(search)
            .setSegmentIds(ImmutableSet.of(segmentId))
            .build()

    private fun <T> createSingeElementPageResult(element: T): PageResult<T> {
        val pageResult = PageResult<T>()
        pageResult.page = 0
        pageResult.size = 1
        pageResult.totalPages = 1
        pageResult.totalElements = 1
        pageResult.data = ImmutableList.of(element)
        return pageResult
    }
}
