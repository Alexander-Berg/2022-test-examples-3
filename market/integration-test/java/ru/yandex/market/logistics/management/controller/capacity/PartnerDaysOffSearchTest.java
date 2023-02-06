package ru.yandex.market.logistics.management.controller.capacity;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.capacity.PartnerDaysOffFilter;
import ru.yandex.market.logistics.management.entity.request.capacity.PartnerDaysOffFilterPartner;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.service.geobase.GeoBaseRegionsService;
import ru.yandex.market.logistics.management.util.CleanDatabase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.jsonContent;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;

@CleanDatabase
public class PartnerDaysOffSearchTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GeoBaseRegionsService geoBaseRegionsService;

    @DisplayName("Валидация фильтра")
    @ParameterizedTest(name = "[{index}] {0} {1}")
    @MethodSource("filterValidationSource")
    void filterValidation(
        String field,
        String error,
        PartnerDaysOffFilter filter
    ) throws Exception {
        search(filter)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors[0].field").value(field))
            .andExpect(jsonPath("errors[0].code").value(error));
    }

    private static Stream<Arguments> filterValidationSource() {
        return Stream.of(
            Triple.of(
                "platformClientId",
                "NotNull",
                defaultFilter().platformClientId(null).build()
            ),
            Triple.of(
                "locationFrom",
                "NotNull",
                defaultFilter().locationFrom(null).build()
            ),
            Triple.of(
                "locationsTo",
                "NotEmpty",
                defaultFilter().locationsTo(null).build()
            ),
            Triple.of(
                "locationsTo",
                "NotEmpty",
                defaultFilter().locationsTo(Set.of()).build()
            ),
            Triple.of(
                "dateFrom",
                "NotNull",
                defaultFilter().dateFrom(null).build()
            ),
            Triple.of(
                "dateTo",
                "NotNull",
                defaultFilter().dateTo(null).build()
            ),
            Triple.of(
                "partners",
                "NotEmpty",
                defaultFilter().partners(null).build()
            ),
            Triple.of(
                "partners",
                "NotEmpty",
                defaultFilter().partners(List.of()).build()
            ),
            Triple.of(
                "partners[0]",
                "NotNull",
                defaultFilter().partners(Collections.singletonList(null)).build()
            ),
            Triple.of(
                "partners[0].partnerId",
                "NotNull",
                defaultFilter().partners(List.of(defaultFilterPartner().partnerId(null).build())).build()
            ),
            Triple.of(
                "partners[0].deliveryType",
                "NotNull",
                defaultFilter().partners(List.of(defaultFilterPartner().deliveryType(null).build())).build()
            )
        ).map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }

    @Test
    @DisplayName("Поиск")
    @DatabaseSetup("/data/controller/capacity/days_off_search.xml")
    void search() throws Exception {
        geoBaseRegionsService.syncRegions();

        search(pathToJson("data/controller/capacity/days_off_search_filter.json"))
            .andExpect(jsonContent("data/controller/capacity/days_off_search_result.json"));
    }

    @Nonnull
    private static PartnerDaysOffFilter.Builder defaultFilter() {
        return PartnerDaysOffFilter.builder()
            .platformClientId(3L)
            .locationFrom(213)
            .locationsTo(Set.of(2))
            .dateFrom(LocalDate.of(2020, 1, 1))
            .dateTo(LocalDate.of(2020, 2, 2))
            .partners(List.of(defaultFilterPartner().build()));
    }

    @Nonnull
    private static PartnerDaysOffFilterPartner.Builder defaultFilterPartner() {
        return PartnerDaysOffFilterPartner.builder()
            .partnerId(1L)
            .deliveryType(DeliveryType.COURIER);
    }

    @Nonnull
    private ResultActions search(PartnerDaysOffFilter filter) throws Exception {
        return search(objectMapper.writeValueAsString(filter));
    }

    @Nonnull
    private ResultActions search(String content) throws Exception {
        return mockMvc.perform(
            put("/externalApi/partner-capacities/days-off/search")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(content)
        );
    }

}
