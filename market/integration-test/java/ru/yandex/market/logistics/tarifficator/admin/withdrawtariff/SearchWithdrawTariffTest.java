package ru.yandex.market.logistics.tarifficator.admin.withdrawtariff;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.filter.WithdrawTariffSearchFilter;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск заборных тарифов через админку")
@DatabaseSetup("/controller/admin/withdrawtariffs/db/before/search_prepare.xml")
@ParametersAreNonnullByDefault
class SearchWithdrawTariffTest extends AbstractContextualTest {

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск заборных тарифов")
    void search(
        String displayName,
        WithdrawTariffSearchFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/withdraw-tariffs").params(TestUtils.toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new WithdrawTariffSearchFilter(),
                "controller/admin/withdrawtariffs/response/all.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору",
                new WithdrawTariffSearchFilter().setWithdrawTariffId(1L),
                "controller/admin/withdrawtariffs/response/id_1.json"
            ),
            Arguments.of(
                "Фильтр по активности",
                new WithdrawTariffSearchFilter().setEnabled(false),
                "controller/admin/withdrawtariffs/response/id_2_3.json"
            ),
            Arguments.of(
                "Фильтр по всем параметрам",
                new WithdrawTariffSearchFilter()
                    .setWithdrawTariffId(3L)
                    .setEnabled(false),
                "controller/admin/withdrawtariffs/response/id_3.json"
            )
        );
    }
}
