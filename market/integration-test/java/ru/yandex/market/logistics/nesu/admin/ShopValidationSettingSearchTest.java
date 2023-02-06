package ru.yandex.market.logistics.nesu.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminShopRole;
import ru.yandex.market.logistics.nesu.admin.model.request.AdminShopValidationSettingFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/repository/validation/default_validation_settings.xml")
class ShopValidationSettingSearchTest extends AbstractContextualTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArguments")
    @DisplayName("Поиск настроек валидации")
    void search(
        @SuppressWarnings("unused") String displayName,
        AdminShopValidationSettingFilter filter,
        String jsonPath
    ) throws Exception {
        mockMvc.perform(get("/admin/validation/settings").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(jsonPath));
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.of(
            Arguments.of(
                "Поисковый запрос без параметров",
                filter(),
                "controller/admin/validation/full_search.json"
            ),
            Arguments.of(
                "Поисковый запрос с кроссдоком",
                filter().setShopRole(AdminShopRole.SUPPLIER),
                "controller/admin/validation/supplier_search.json"
            ),
            Arguments.of(
                "Поисковый запрос с дропшипом",
                filter().setShopRole(AdminShopRole.DROPSHIP),
                "controller/admin/validation/dropship_search.json"
            ),
            Arguments.of(
                "Поисковый запрос с ролью по которой нет настроек",
                filter().setShopRole(AdminShopRole.DAAS),
                "controller/admin/validation/empty_search.json"
            )
        );
    }

    @Nonnull
    private static AdminShopValidationSettingFilter filter() {
        return new AdminShopValidationSettingFilter();
    }
}
