package ru.yandex.market.logistics.management.controller.partner;

import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.partner.PartnerExternalParamRequest;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.util.TestUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("/data/controller/partner/search/prepare_data.xml")
class PartnerControllerSearchTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;

    private static Stream<Arguments> searchPartnersProvider() {
        return Stream.of(
            Arguments.of(
                "Поиск по набору идентификаторов",
                SearchPartnerFilter.builder()
                    .setIds(Set.of(1L, 3L))
                    .build(),
                "data/controller/partner/search/partner_1_3.json"
            ),
            Arguments.of(
                "Поиск по набору идентификаторов клиентов платформы",
                SearchPartnerFilter.builder()
                    .setPlatformClientIds(Set.of((3901L)))
                    .build(),
                "data/controller/partner/search/partner_with_platform_clientId_id_3901_response.json"
            ),
            Arguments.of(
                "Поиск по набору идентификаторов подтипов партнера",
                SearchPartnerFilter.builder()
                    .setPartnerSubTypeIds(Set.of((2L)))
                    .build(),
                "data/controller/partner/search/partner_2.json"
            ),
            Arguments.of(
                "Поиск по набору идентификаторов market-id",
                SearchPartnerFilter.builder()
                    .setMarketIds(Set.of(829722L))
                    .build(),
                "data/controller/partner/search/partner_2.json"
            ),
            Arguments.of(
                "Поиск по набору статусов",
                SearchPartnerFilter.builder()
                    .setStatuses(Set.of(PartnerStatus.ACTIVE))
                    .build(),
                "data/controller/partner/search/active_partners_response.json"
            ),
            Arguments.of(
                "Поиск по набору типов клиентов",
                SearchPartnerFilter.builder()
                    .setTypes(Set.of(PartnerType.FULFILLMENT))
                    .build(),
                "data/controller/partner/search/fulfillment_partners_response.json"
            ),
            Arguments.of(
                "Поиск по параметру (пересечение)",
                SearchPartnerFilter.builder()
                    .setExternalParamsIntersection(Set.of(
                        new PartnerExternalParamRequest(PartnerExternalParamType.DESCRIPTION, "Описание"),
                        new PartnerExternalParamRequest(PartnerExternalParamType.IS_COMMON, "true")
                    ))
                    .build(),
                "data/controller/partner/search/partner_3.json"
            ),
            Arguments.of(
                "Поиск по параметру (объединение)",
                SearchPartnerFilter.builder()
                    .setExternalParamsUnion(Set.of(
                        new PartnerExternalParamRequest(PartnerExternalParamType.DESCRIPTION, "Описание"),
                        new PartnerExternalParamRequest(PartnerExternalParamType.IS_COMMON, "true")
                    ))
                    .build(),
                "data/controller/partner/search/partner_1_3.json"
            ),
            Arguments.of(
                "Поиск по набору статусов связок",
                SearchPartnerFilter.builder()
                    .setPlatformClientIds(Set.of(3901L))
                    .setPlatformClientStatuses(Set.of(PartnerStatus.TESTING))
                    .build(),
                "data/controller/partner/search/testing_partners_response.json"
            ),
            Arguments.of(
                "Поиск по набору идентификаторов бизнеса",
                SearchPartnerFilter.builder()
                    .setBusinessIds(Set.of(2222L))
                    .build(),
                "data/controller/partner/search/partner_8.json"
            ),
            Arguments.of(
                "Поиск по набору идентификаторов поставщиков",
                SearchPartnerFilter.builder()
                    .setRealSupplierIds(Set.of("14"))
                    .build(),
                "data/controller/partner/search/partner_9.json"
            ),
            Arguments.of(
                "Поиск по идентификатору retail партнера",
                SearchPartnerFilter.builder()
                    .setIds(Set.of(1000L))
                    .build(),
                "data/controller/partner/search/retail_partner.json"
            ),
            Arguments.of(
                "Поиск по типу retail партнера",
                SearchPartnerFilter.builder()
                    .setTypes(Set.of(PartnerType.RETAIL))
                    .build(),
                "data/controller/partner/search/retail_partner.json"
            ),
            Arguments.of(
                "Поиск по запросу (имя партнера)",
                SearchPartnerFilter.builder()
                    .setSearchQuery("fulfillment service")
                    .build(),
                "data/controller/partner/search/partner_1_3.json"
            ),
            Arguments.of(
                "Поиск по запросу (имя партнера и тип)",
                SearchPartnerFilter.builder()
                    .setSearchQuery("service 1")
                    .setTypes(Set.of(PartnerType.DELIVERY))
                    .build(),
                "data/controller/partner/search/partner_2.json"
            ),
            Arguments.of(
                "Поиск по запросу (идентификатор)",
                SearchPartnerFilter.builder()
                    .setSearchQuery("9")
                    .build(),
                "data/controller/partner/search/partner_9.json"
            )
        );
    }

    @DisplayName("Поиск партнеров")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("searchPartnersProvider")
    void searchPartners(
        @SuppressWarnings("unused") String caseName,
        SearchPartnerFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(
            put("/externalApi/partners/search")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsBytes(filter))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(responsePath));
    }

    private static Stream<Arguments> searchPartnersWithInvalidFilterProvider() {
        return Stream.of(
            Arguments.of(
                SearchPartnerFilter.builder()
                    .setExternalParamsIntersection(Set.of(new PartnerExternalParamRequest(null, "1")))
                    .build()
            ),
            Arguments.of(
                SearchPartnerFilter.builder()
                    .setExternalParamsIntersection(
                        Set.of(new PartnerExternalParamRequest(PartnerExternalParamType.IS_COMMON, null)))
                    .build()
            )
        );
    }

    @ParameterizedTest
    @MethodSource("searchPartnersWithInvalidFilterProvider")
    void searchPartnersWithInvalidFilter(SearchPartnerFilter filter) throws Exception {
        mockMvc.perform(
            put("/externalApi/partners/search")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsBytes(filter))
        )
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ошибка в фильтре партнера")
    void getPartnersWithWrongFilter() throws Exception {
        MultiValueMap<String, String> filter = new LinkedMultiValueMap<>();
        filter.add("statuses", "fail");

        mockMvc.perform(
            get("/externalApi/partners")
                .params(filter)
        )
            .andExpect(status().is4xxClientError());
    }
}
