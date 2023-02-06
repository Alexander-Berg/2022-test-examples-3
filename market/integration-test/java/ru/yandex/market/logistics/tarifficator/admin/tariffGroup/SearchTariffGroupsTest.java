package ru.yandex.market.logistics.tarifficator.admin.tariffGroup;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.admin.controller.AdminTariffGroupController;
import ru.yandex.market.logistics.tarifficator.model.filter.AdminTariffGroupSearchFilter;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск через админку групп тарифов")
@DatabaseSetup("/controller/admin/tariffGroup/before/prepare_groups.xml")
class SearchTariffGroupsTest extends AbstractContextualTest {

    @DisplayName("Поиск через админку групп тарифов")
    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource
    void testSearchTariffGroups(
        @SuppressWarnings("unused") String displayName,
        AdminTariffGroupSearchFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get(AdminTariffGroupController.PATH_ADMIN_TARIFF_GROUPS).params(TestUtils.toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @SuppressWarnings("unused")
    @Nonnull
    private static Stream<Arguments> testSearchTariffGroups() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminTariffGroupSearchFilter(),
                "controller/admin/tariffGroup/response/tariff_groups_39_23_18.json"
            ),
            Arguments.of(
                "Фильтр по ID",
                new AdminTariffGroupSearchFilter().setTariffGroupId(18L),
                "controller/admin/tariffGroup/response/tariff_groups_18.json"
            ),
            Arguments.of(
                "Фильтр по описанию",
                new AdminTariffGroupSearchFilter().setDescription("нечёт"),
                "controller/admin/tariffGroup/response/tariff_groups_39_23.json"
            ),
            Arguments.of(
                "Фильтр по всем параметрам",
                new AdminTariffGroupSearchFilter().setTariffGroupId(23L).setDescription("нечёт"),
                "controller/admin/tariffGroup/response/tariff_groups_23.json"
            )
        );
    }
}
