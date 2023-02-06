package ru.yandex.market.logistics.management.controller;

import java.util.Set;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import ru.yandex.common.util.region.RegionType;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.geoBase.GeoBaseFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DatabaseSetup("/data/controller/geoBase/prepare_data.xml")
public class GeoBaseControllerTest extends AbstractContextualTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("searchLocationsProvider")
    @DisplayName("Поиск локаций")
    void searchLocations(
        @SuppressWarnings("unused") String displayName,
        GeoBaseFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(
            put("/geobase/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(filter))
        )
            .andExpect(status().isOk())
            .andExpect(testJson(responsePath));
    }

    private static Stream<Arguments> searchLocationsProvider() {
        return Stream.of(
            Arguments.of(
                "Получить информацию о локации по названию (общая подстрока)",
                GeoBaseFilter.builder()
                    .setSearchQuery("ан")
                    .build(),
                "data/controller/geoBase/response/search_163_164.json"
            ),
            Arguments.of(
                "Получить информацию о локации по названию (уникальная подстрока)",
                GeoBaseFilter.builder()
                    .setSearchQuery("аст")
                    .build(),
                "data/controller/geoBase/response/search_163.json"
            ),
            Arguments.of(
                "Получить информацию о локации по идентификатору в поисковом запросе",
                GeoBaseFilter.builder()
                    .setSearchQuery("163")
                    .build(),
                "data/controller/geoBase/response/search_163.json"
            ),
            Arguments.of(
                "Получить информацию о локации по идентификаторам",
                GeoBaseFilter.builder()
                    .setId(Set.of(163L, 164L))
                    .build(),
                "data/controller/geoBase/response/search_163_164.json"
            ),
            Arguments.of(
                "Получить информацию о локации по типу",
                GeoBaseFilter.builder()
                    .setType(Set.of(RegionType.CITY))
                    .build(),
                "data/controller/geoBase/response/search_163_164.json"
            )
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("searchLocationsPagedProvider")
    @DisplayName("Постраничный поиск локаций")
    void searchLocationsPaged(
        @SuppressWarnings("unused") String displayName,
        Pageable pageable,
        String responsePath
    ) throws Exception {
        mockMvc.perform(
            put("/geobase/search-paged")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(GeoBaseFilter.builder().build()))
                .param("size", String.valueOf(pageable.getPageSize()))
                .param("page", String.valueOf(pageable.getPageNumber()))
        )
            .andExpect(status().isOk())
            .andExpect(testJson(responsePath));
    }

    private static Stream<Arguments> searchLocationsPagedProvider() {
        return Stream.of(
            Arguments.of(
                "Первая страница",
                PageRequest.of(0, 2),
                "data/controller/geoBase/response/search_first_page.json"
            ),
            Arguments.of(
                "Последняя страница",
                PageRequest.of(1, 2),
                "data/controller/geoBase/response/search_last_page.json"
            ),
            Arguments.of(
                "Слишком большой размер страницы",
                PageRequest.of(0, 50),
                "data/controller/geoBase/response/search_large_page_size.json"
            ),
            Arguments.of(
                "Слишком большой номер страницы",
                PageRequest.of(10, 10),
                "data/controller/geoBase/response/search_large_page_number.json"
            )
        );
    }
}
