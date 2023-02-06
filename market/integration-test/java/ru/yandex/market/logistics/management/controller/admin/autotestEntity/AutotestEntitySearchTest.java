package ru.yandex.market.logistics.management.controller.admin.autotestEntity;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.management.AbstractContextualAspectValidationTest;
import ru.yandex.market.logistics.management.domain.dto.filter.admin.AdminAutotestEntityFilter;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;
import ru.yandex.market.logistics.test.integration.utils.QueryParamUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.jsonContent;

@DatabaseSetup("/data/controller/admin/autotestEntity/before/setup.xml")
public class AutotestEntitySearchTest extends AbstractContextualAspectValidationTest {

    @Test
    @DisplayName("Получить автотестовые сущности будучи неавторизованным")
    void getAutotestEntityIsUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/lms/autotest-entity")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Получить автотестовые сущности не имея прав")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void getAutotestEntityIsForbidden() throws Exception {
        mockMvc.perform(get("/admin/lms/autotest-entity")).andExpect(status().isForbidden());
    }

    @DisplayName("Поиск автотестовых сущностей")
    @MethodSource("filterArguments")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {LMSPlugin.AUTHORITY_ROLE_AUTOTEST_ENTITY})
    @ParameterizedTest(name = "[{index}] {0}")
    void filter(String displayName, AdminAutotestEntityFilter filter, String responsePath) throws Exception {
        mockMvc.perform(get("/admin/lms/autotest-entity").params(QueryParamUtils.toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> filterArguments() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminAutotestEntityFilter(),
                "data/controller/admin/autotestEntity/response/all.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору",
                new AdminAutotestEntityFilter().setId(1L),
                "data/controller/admin/autotestEntity/response/id_1.json"
            ),
            Arguments.of(
                "Фильтр по пути",
                new AdminAutotestEntityFilter().setPath("lms/partner/2"),
                "data/controller/admin/autotestEntity/response/id_2.json"
            ),
            Arguments.of(
                "Фильтр по всем параметрам",
                new AdminAutotestEntityFilter().setId(1L).setPath("lms/partner/1"),
                "data/controller/admin/autotestEntity/response/id_1.json"
            )
        );
    }

}
