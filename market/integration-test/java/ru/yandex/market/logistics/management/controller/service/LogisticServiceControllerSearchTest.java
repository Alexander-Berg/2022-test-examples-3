package ru.yandex.market.logistics.management.controller.service;

import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.logistic.service.LogisticServiceFilter;
import ru.yandex.market.logistics.management.util.TestUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Поиск логистических сервисов через external API")
@DatabaseSetup("/data/service/combinator/db/before/service_codes.xml")
@DatabaseSetup("/data/controller/logisticService/before/prepare_data_without_segments.xml")
@DatabaseSetup("/data/controller/logisticService/before/segment_with_services.xml")
class LogisticServiceControllerSearchTest extends AbstractContextualTest {

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("Поиск логистических сервисов")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("searchSegmentServicesProvider")
    void searchSegmentServices(
        @SuppressWarnings("unused") String caseName,
        LogisticServiceFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(
            put("/externalApi/logistic-services/search")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(objectMapper.writeValueAsBytes(filter))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(responsePath, Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER));
    }

    private static Stream<Arguments> searchSegmentServicesProvider() {
        return Stream.of(
            Arguments.of(
                "Поиск по идентификатору сегмента",
                LogisticServiceFilter.builder().setSearchQuery("1").build(),
                "data/controller/logisticService/search/search_result.json"
            ),
            Arguments.of(
                "Поиск по идентификатору сервиса",
                LogisticServiceFilter.builder().setSegmentIds(Set.of(1L)).build(),
                "data/controller/logisticService/search/search_result.json"
            )
        );
    }

    @DisplayName("Постраничный поиск логистических сервисов")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("searchSegmentServicesPagedProvider")
    void searchSegmentServicesPaged(
        @SuppressWarnings("unused") String caseName,
        Pageable pageable,
        String responsePath
    ) throws Exception {
        LogisticServiceFilter filter = LogisticServiceFilter.builder().build();
        mockMvc.perform(
            put("/externalApi/logistic-services/search-paged")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .param("size", String.valueOf(pageable.getPageSize()))
                .param("page", String.valueOf(pageable.getPageNumber()))
                .content(objectMapper.writeValueAsBytes(filter))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(responsePath, Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER));
    }

    private static Stream<Arguments> searchSegmentServicesPagedProvider() {
        return Stream.of(
            Arguments.of(
                "Первая страница",
                PageRequest.of(0, 1),
                "data/controller/logisticService/search/search_first_page.json"
            ),
            Arguments.of(
                "Слишком большой размер страницы",
                PageRequest.of(0, 50),
                "data/controller/logisticService/search/search_large_page_size.json"
            ),
            Arguments.of(
                "Слишком большой номер страницы",
                PageRequest.of(10, 10),
                "data/controller/logisticService/search/search_large_page_number.json"
            )
        );
    }
}
