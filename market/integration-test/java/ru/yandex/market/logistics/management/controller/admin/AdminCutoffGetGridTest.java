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

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Получение кат-оффов")
@DatabaseSetup("/data/controller/admin/cutoff/prepare_data.xml")
public class AdminCutoffGetGridTest extends AbstractContextualTest {
    @DisplayName("Успешно")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getGridArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_CUT_OFF})
    void getGridSuccessReadOnly(
        @SuppressWarnings("unused") String caseName,
        Map<String, String> queryParams,
        String responsePathReadOnlyMode
    ) throws Exception {
        mockMvc.perform(get("/admin/lms/cut-off").params(toParams(queryParams)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePathReadOnlyMode));
    }

    @Nonnull
    private static Stream<Arguments> getGridArguments() {
        return Stream.of(
            Arguments.of(
                "Без фильтрации",
                Map.of(),
                "data/controller/admin/cutoff/search_without_filter.json"
            ),
            Arguments.of(
                "По идентификатору связок партнеров",
                Map.of("partnerRelation", "1"),
                "data/controller/admin/cutoff/search_by_partner_relation.json"
            ),
            Arguments.of(
                "По региону",
                Map.of("locationId", "2"),
                "data/controller/admin/cutoff/search_by_location.json"
            )
        );
    }

    @DisplayName("Невалидный фильтр")
    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_CUT_OFF})
    void getGridFailure() throws Exception {
        mockMvc.perform(get("/admin/lms/cut-off").param("locationId", "не лонг"))
            .andExpect(status().isBadRequest());
    }

}
