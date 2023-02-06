package ru.yandex.market.logistics.nesu.controller.location;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.GeoObject;
import ru.yandex.common.geocoder.model.response.Kind;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.nesu.utils.MatcherUtils.validationErrorMatcher;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.objectError;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.missingParameter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@ParametersAreNonnullByDefault
class SearchRegionsTest extends AbstractContextualTest {
    @Autowired
    private GeoClient geoClient;

    @Test
    @DisplayName("Geo client ничего не вернул")
    void geoClientEmptyResult() throws Exception {
        mockGeoClient(List.of());

        performSearch("test")
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));
    }

    @Test
    @DisplayName("Geo client вернул только Россию")
    void onlyRussia() throws Exception {
        mockGeoClient(List.of(buildGeoObject("225", Kind.COUNTRY)));

        performSearch("test")
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));
    }

    @DisplayName("Значение maxResults по умолчанию")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("defaultMaxResultsArguments")
    void defaultMaxResults(
        @SuppressWarnings("unused") String caseName,
        List<GeoObject> geoClientResult,
        String responsePath
    ) throws Exception {
        mockGeoClient(geoClientResult);

        performSearch("test")
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> defaultMaxResultsArguments() {
        return Stream.of(
            Arguments.of(
                "Фильтрация несуществующего региона",
                List.of(
                    buildGeoObject("213", Kind.LOCALITY),
                    buildGeoObject("9999", Kind.LOCALITY)
                ),
                "controller/location/response/regions-search/moscow.json"
            ),
            Arguments.of(
                "Фильтрация района города по Kind",
                List.of(
                    buildGeoObject("213", Kind.LOCALITY),
                    buildGeoObject("42", Kind.DISTRICT)
                ),
                "controller/location/response/regions-search/moscow.json"
            ),
            Arguments.of(
                "Фильтрация района города по RegionType",
                List.of(
                    buildGeoObject("213", Kind.LOCALITY),
                    buildGeoObject("217991", Kind.DISTRICT)
                ),
                "controller/location/response/regions-search/moscow.json"
            ),
            Arguments.of(
                "Фильтрация повторяющегося района",
                List.of(
                    buildGeoObject("213", Kind.LOCALITY),
                    buildGeoObject("213", Kind.LOCALITY)
                ),
                "controller/location/response/regions-search/moscow.json"
            ),
            Arguments.of(
                "Фильтрация города в городе",
                List.of(
                    buildGeoObject("213", Kind.LOCALITY),
                    buildGeoObject("216", Kind.LOCALITY)
                ),
                "controller/location/response/regions-search/moscow.json"
            ),
            Arguments.of(
                "Значение по умолчанию равно 7",
                List.of(
                    buildGeoObject("225", Kind.COUNTRY),
                    buildGeoObject("3", Kind.PROVINCE),
                    buildGeoObject("40", Kind.PROVINCE),
                    buildGeoObject("1", Kind.AREA),
                    buildGeoObject("213", Kind.LOCALITY),
                    buildGeoObject("10716", Kind.LOCALITY),
                    buildGeoObject("121905", Kind.PROVINCE),
                    buildGeoObject("11117", Kind.PROVINCE),
                    buildGeoObject("121905", Kind.PROVINCE)
                ),
                "controller/location/response/regions-search/regions.json"
            )
        );
    }

    @Test
    @DisplayName("Фильтрация по maxResults")
    void setMaxResults() throws Exception {
        mockGeoClient(List.of(
            buildGeoObject("213", Kind.LOCALITY),
            buildGeoObject("10716", Kind.LOCALITY)
        ));

        performSearch("test", "1")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/location/response/regions-search/moscow.json"));
    }

    @Test
    @DisplayName("Валидация отсутствия строки поиска")
    void validateNoTerm() throws Exception {
        mockMvc.perform(search())
            .andExpect(status().isBadRequest())
            .andExpect(missingParameter("term", "String"));
    }

    @Test
    @DisplayName("Валидация пустой строки поиска")
    void validateEmptyTerm() throws Exception {
        performSearch("      ")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(
                objectError("term", "must not be blank", "NotBlank")
            ));
    }

    @Test
    @DisplayName("Валидация maxResults")
    void validateMaxResults() throws Exception {
        performSearch("test", "0")
            .andExpect(status().isBadRequest())
            .andExpect(validationErrorMatcher(
                objectError("maxResults", "must be greater than 0", "Positive")
            ));
    }

    @Nonnull
    private static GeoObject buildGeoObject(String geoId, Kind kind) {
        return  SimpleGeoObject.newBuilder()
            .withToponymInfo(ToponymInfo.newBuilder()
                .withGeoid(geoId)
                .withKind(kind)
                .build()
            )
            .withAddressInfo(AddressInfo.newBuilder()
                .withCountryInfo(CountryInfo.newBuilder().build())
                .withAreaInfo(AreaInfo.newBuilder().build())
                .withLocalityInfo(LocalityInfo.newBuilder().build())
                .build()
            )
            .withBoundary(Boundary.newBuilder().build())
            .build();
    }

    @Nonnull
    private ResultActions performSearch(String term) throws Exception {
        return mockMvc.perform(search().param("term", term));
    }

    @Nonnull
    private ResultActions performSearch(String term, String maxResults) throws Exception {
        return mockMvc.perform(search()
            .param("term", term)
            .param("maxResults", maxResults)
        );
    }

    @Nonnull
    private MockHttpServletRequestBuilder search() {
        return get("/back-office/location/regions/search");
    }

    private void mockGeoClient(List<GeoObject> result) {
        when(geoClient.find("Russia,test"))
            .thenReturn(result);
    }
}
