package ru.yandex.market.logistics.tarifficator.admin.tpl.couriertariffzonesortingcenter;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.filter.tpl.AdminCourierTariffZoneSortingCenterFileFilter;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup(
    value = "/controller/tpl/tariffs/tariff-zone-sorting-center/db/before/search_setup.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
class SearchFilesTest extends AbstractContextualTest {

    @DisplayName("Поиск файлов со связками тарифных зон с сортировочными центрами")
    @MethodSource("searchArguments")
    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    void search(
        String displayName,
        AdminCourierTariffZoneSortingCenterFileFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(
            get("/admin/tpl-courier-tariffs/zone-sorting-center/files")
                .params(TestUtils.toParams(filter))
        )
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new AdminCourierTariffZoneSortingCenterFileFilter(),
                "controller/tpl/tariffs/tariff-zone-sorting-center/response/all.json"
            ),
            Arguments.of(
                "Поиск по идентификатору",
                new AdminCourierTariffZoneSortingCenterFileFilter()
                    .setCourierTariffZoneSortingCenterFileId(1L),
                "controller/tpl/tariffs/tariff-zone-sorting-center/response/id_1.json"
            )
        );
    }
}
