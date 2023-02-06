package ru.yandex.market.logistics.management.controller.admin;

import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.ResultMatcher;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/data/controller/admin/services/prepare_day_offs.xml")
public class AdminLogisticServiceForcedDayOffControllerTest extends AbstractContextualAspectValidationTest {
    private static final String GRID_URL = "/admin/lms/logistic-services/forced-day-off";
    private static final String DETAIL_URL = GRID_URL + "/2";

    private static final Map<String, String> PARENT_COMPONENT_PROPERTIES = Map.of(
        "idFieldName", "id",
        "parentSlug", "logistic-services",
        "parentId", "1"
    );

    @Test
    @DisplayName("Найти дейоффы - Неавторизованный пользователь")
    void getDayOffGridUnauthorized() throws Exception {
        mockMvc.perform(get(GRID_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Найти дейоффы - Недостаточно прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void getDayOffGridForbidden() throws Exception {
        mockMvc.perform(get(GRID_URL).param("entityId", "1")).andExpect(status().isForbidden());
    }

    @DisplayName("Найти дейоффы - Успешно")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getGridArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_DAY_OFF})
    void getDayOffGridSuccess(
        @SuppressWarnings("unused") String name,
        Map<String, String> params,
        String responsePath
    ) throws Exception {
        mockMvc
            .perform(
                get(GRID_URL)
                    .param("entityId", "1")
                    .params(toParams(params))
            )
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> getGridArguments() {
        return Stream.of(
            Arguments.of(
                "Без фильтрации",
                Map.of(),
                "data/controller/admin/services/search_no_filter.json"
            ),
            Arguments.of(
                "По дате",
                Map.of("day", "2020-01-02"),
                "data/controller/admin/services/search_by_day.json"
            ),
            Arguments.of(
                "С размером страницы",
                Map.of("size", "2"),
                "data/controller/admin/services/search_with_size.json"
            ),
            Arguments.of(
                "С номером страницы",
                Map.of("size", "2", "page", "1"),
                "data/controller/admin/services/search_with_page.json"
            ),
            Arguments.of(
                "С сортировкой",
                Map.of("sort", "day,asc"),
                "data/controller/admin/services/search_with_sort.json"
            )
        );
    }

    @Test
    @DisplayName("Найти деталку по дейоффу - Неавторизованный пользователь")
    void getDayOffDetailUnauthorized() throws Exception {
        mockMvc.perform(get(DETAIL_URL)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Найти деталку по дейоффу - Недостаточно прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void getDayOffDetailForbidden() throws Exception {
        mockMvc.perform(get(DETAIL_URL).params(toParams(PARENT_COMPONENT_PROPERTIES)))
            .andExpect(status().isForbidden());
    }

    @DisplayName("Найти деталку по дейоффу - Успешно")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getDetailArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_LOGISTIC_SERVICE_DAY_OFF})
    void getDayOffDetailSuccess(
        @SuppressWarnings("unused") String name,
        int id,
        ResultMatcher status,
        ResultMatcher response
    ) throws Exception {
        mockMvc
            .perform(
                get(GRID_URL + "/" + id)
                    .params(toParams(PARENT_COMPONENT_PROPERTIES))
            )
            .andExpect(status)
            .andExpect(response);
    }

    @Nonnull
    private static Stream<Arguments> getDetailArguments() {
        return Stream.of(
            Arguments.of(
                "Не найдена",
                222,
                status().isNotFound(),
                content().string("")
            ),
            Arguments.of(
                "Найдена",
                2,
                status().isOk(),
                TestUtil.jsonContent("data/controller/admin/services/detail_found.json")
            )
        );
    }
}
