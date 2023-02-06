package ru.yandex.market.logistics.management.controller.admin;

import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_PARTNER;

@DatabaseSetup("/data/controller/admin/partner/prepare_data.xml")
class LmsPartnerSearchControllerTest extends AbstractContextualTest {
    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideSearchQueries")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER})
    void findBySearchQuery(String testName, String searchQuery, String responseJson) throws Exception {
        var requestBuilder = get("/admin/lms/partner");
        if (searchQuery != null) {
            requestBuilder.param("searchQuery", searchQuery);
        }

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                responseJson,
                true
            ));
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("provideNames")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER})
    void findByName(String testName, String name, String responseJson) throws Exception {
        var requestBuilder = get("/admin/lms/partner");
        if (name != null) {
            requestBuilder.param("name", name);
        }

        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                responseJson,
                true
            ));
    }

    @Test
    @DisplayName("Поиск по идентификатору юридической информации")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {AUTHORITY_ROLE_PARTNER})
    void findByLegalInfo() throws Exception {
        mockMvc.perform(get("/admin/lms/partner")
            .param("legalInfo", "1"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.testJson(
                "data/controller/admin/partner/find_by_legal_info/find_by_legal_info.json",
                true
            ));
    }

    private static Stream<Arguments> provideSearchQueries() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                "1",
                "data/controller/admin/partner/find_by_search_query/find_by_search_query_id.json"
            ),
            Arguments.of(
                "Поиск по name",
                "Delivery service 1",
                "data/controller/admin/partner/find_by_search_query/find_by_search_query_name.json"
            ),
            Arguments.of(
                "Поиск по name 2",
                "Delivery",
                "data/controller/admin/partner/find_by_search_query/find_by_search_query_name_2.json"
            ),
            Arguments.of(
                "Поиск по readableName",
                "Fulfillment service Two",
                "data/controller/admin/partner/find_by_search_query/find_by_search_query_readable_name.json"
            ),
            Arguments.of(
                "Поиск в верхнем регистре кириллицы",
                "СД",
                "data/controller/admin/partner/find_by_search_query/find_by_search_query_ru.json"
            ),
            Arguments.of(
                "Поиск в нижнем регистре кириллицы",
                "сд",
                "data/controller/admin/partner/find_by_search_query/find_by_search_query_ru.json"
            ),
            Arguments.of(
                "Поиск в верхнем регистре латиницы",
                "FULFILLMENT",
                "data/controller/admin/partner/find_by_search_query/find_by_search_query_en.json"
            ),
            Arguments.of(
                "Поиск в нижнем регистре латиницы",
                "fulfillment",
                "data/controller/admin/partner/find_by_search_query/find_by_search_query_en.json"
            ),
            Arguments.of(
                "Поиск с пустым searchQuery",
                "",
                "data/controller/admin/partner/find_by_search_query/find_by_search_query_empty.json"
            ),
            Arguments.of(
                "Поиск без searchQuery",
                null,
                "data/controller/admin/partner/find_by_search_query/find_by_search_query_empty.json"
            )
        );
    }

    private static Stream<Arguments> provideNames() {
        return Stream.of(
            Arguments.of(
                "Поиск в верхнем регистре кириллицы",
                "СД",
                "data/controller/admin/partner/find_by_name/find_by_name_ru.json"
            ),
            Arguments.of(
                "Поиск в нижнем регистре кириллицы",
                "сд",
                "data/controller/admin/partner/find_by_name/find_by_name_ru.json"
            ),
            Arguments.of(
                "Поиск в верхнем регистре латиницы",
                "FULFILLMENT",
                "data/controller/admin/partner/find_by_name/find_by_name_en.json"
            ),
            Arguments.of(
                "Поиск в нижнем регистре латиницы",
                "fulfillment",
                "data/controller/admin/partner/find_by_name/find_by_name_en.json"
            ),
            Arguments.of(
                "Поиск с пустым name",
                "",
                "data/controller/admin/partner/find_by_name/find_by_name_empty.json"
            ),
            Arguments.of(
                "Поиск без name",
                null,
                "data/controller/admin/partner/find_by_name/find_by_name_empty.json"
            )
        );
    }
}
