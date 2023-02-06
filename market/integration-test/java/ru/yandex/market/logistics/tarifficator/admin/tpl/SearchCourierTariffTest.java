package ru.yandex.market.logistics.tarifficator.admin.tpl;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.enums.tpl.CourierTariffStatus;
import ru.yandex.market.logistics.tarifficator.model.filter.tpl.AdminCourierTariffSearchFilter;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup(
    value = "/controller/admin/tpl/couriertariffs/db/before/search_prepare.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
class SearchCourierTariffTest extends AbstractContextualTest {
    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск курьерских тарифов")
    void search(
        String displayName,
        AdminCourierTariffSearchFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/tpl-courier-tariffs").params(TestUtils.toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminCourierTariffSearchFilter(),
                "controller/admin/tpl/couriertariffs/response/all.json"
            ),
            Arguments.of(
                "Поиск по идентификатору",
                new AdminCourierTariffSearchFilter().setCourierTariffId(1L),
                "controller/admin/tpl/couriertariffs/response/id_1.json"
            ),
            Arguments.of(
                "Поиск по статусу",
                new AdminCourierTariffSearchFilter().setStatus(CourierTariffStatus.SUCCESS),
                "controller/admin/tpl/couriertariffs/response/id_2_3.json"
            ),
            Arguments.of(
                "Поиск по всем параметрам",
                new AdminCourierTariffSearchFilter().setCourierTariffId(2L).setStatus(CourierTariffStatus.SUCCESS),
                "controller/admin/tpl/couriertariffs/response/id_2.json"
            ),
            Arguments.of(
                "Поиск несуществующего тарифа",
                new AdminCourierTariffSearchFilter().setCourierTariffId(1L).setStatus(CourierTariffStatus.SUCCESS),
                "controller/admin/tpl/couriertariffs/response/empty.json"
            )
        );
    }
}
