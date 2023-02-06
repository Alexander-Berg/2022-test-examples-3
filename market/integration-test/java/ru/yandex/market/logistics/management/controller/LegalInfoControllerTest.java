package ru.yandex.market.logistics.management.controller;

import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.legalInfo.LegalInfoFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DatabaseSetup("/data/controller/legalInfo/prepare_data.xml")
class LegalInfoControllerTest extends AbstractContextualTest {

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("searchLegalInfoProvider")
    @DisplayName("Поиск юридической информации")
    void searchLegalInfo(
        @SuppressWarnings("unused") String displayName,
        LegalInfoFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(
            put("/externalApi/legal-info/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(filter))
        )
            .andExpect(status().isOk())
            .andExpect(testJson(responsePath));
    }

    private static Stream<Arguments> searchLegalInfoProvider() {
        return Stream.of(
            Arguments.of(
                "Получить юридическую информацию по наименованию организации (общая подстрока)",
                LegalInfoFilter.builder()
                    .setSearchQuery("ro")
                    .build(),
                    "data/controller/legalInfo/response/search_101_102.json"
            ),
            Arguments.of(
                "Получить юридическую информацию по наименованию организации (уникальная подстрока)",
                LegalInfoFilter.builder()
                    .setSearchQuery("roma")
                    .build(),
                    "data/controller/legalInfo/response/search_102.json"
            ),
            Arguments.of(
                "Получить юридическую информацию по идентификатору организации",
                LegalInfoFilter.builder()
                    .setSearchQuery("102")
                    .build(),
                    "data/controller/legalInfo/response/search_102.json"
            )
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("searchLegalInfoPagedProvider")
    @DisplayName("Постраничный поиск юридической информации")
    void searchLegalInfoPaged(
        @SuppressWarnings("unused") String displayName,
        Pageable pageable,
        String responsePath
    ) throws Exception {
        LegalInfoFilter filter = LegalInfoFilter.builder().build();
        mockMvc.perform(
            put("/externalApi/legal-info/search-paged")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(filter))
                .param("size", String.valueOf(pageable.getPageSize()))
                .param("page", String.valueOf(pageable.getPageNumber()))
        )
            .andExpect(status().isOk())
            .andExpect(testJson(responsePath));
    }

    private static Stream<Arguments> searchLegalInfoPagedProvider() {
        return Stream.of(
            Arguments.of(
                "Первая страница",
                PageRequest.of(0, 1),
                "data/controller/legalInfo/response/search_first_page.json"
            ),
            Arguments.of(
                "Последняя страница",
                PageRequest.of(1, 1),
                "data/controller/legalInfo/response/search_last_page.json"
            ),
            Arguments.of(
                "Слишком большой размер страницы",
                PageRequest.of(0, 50),
                "data/controller/legalInfo/response/search_large_page_size.json"
            ),
            Arguments.of(
                "Слишком большой номер страницы",
                PageRequest.of(10, 10),
                "data/controller/legalInfo/response/search_large_page_number.json"
            )
        );
    }
}
