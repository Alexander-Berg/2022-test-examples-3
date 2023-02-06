package ru.yandex.market.logistics.nesu.controller.location;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class GetRegionTest extends AbstractContextualTest {
    @DisplayName("Успешное получение регионов с подрегионами")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("successArguments")
    void success(
        @SuppressWarnings("unused") String caseName,
        int regionId,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/back-office/location/regions/" + regionId))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> successArguments() {
        return Stream.of(
            Arguments.of(
                "Все регионы Центрального ФО",
                3,
                "controller/location/response/subregions/central_regions.json"
            ),
            Arguments.of(
                "Все города московской области",
                1,
                "controller/location/response/subregions/moscow_area_cities.json"
            ),
            Arguments.of(
                "Подрегион - город с районами (Саранск)",
                121905,
                "controller/location/response/subregions/city_subregion_with_children.json"
            )
        );
    }

    @DisplayName("Неуспешное получение регионов с подрегионами")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("failArguments")
    void fail(
        @SuppressWarnings("unused") String caseName,
        int regionId,
        ResultMatcher status,
        String errorMessage
    ) throws Exception {
        mockMvc.perform(get("/back-office/location/regions/" + regionId))
            .andExpect(status)
            .andExpect(errorMessage(errorMessage));
    }

    @Nonnull
    private static Stream<Arguments> failArguments() {
        return Stream.of(
            Arguments.of(
                "Несуществующий регион",
                99999,
                status().isNotFound(),
                "Failed to find [REGION] with ids [99999]"
            ),
            Arguments.of(
                "Континент",
                10001,
                status().isBadRequest(),
                "Region with id 10001 is not allowed"
            ),
            Arguments.of(
                "Район города",
                217991,
                status().isBadRequest(),
                "Region with id 217991 is not allowed"
            )
        );
    }
}
