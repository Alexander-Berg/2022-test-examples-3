package ru.yandex.market.logistics.tarifficator.admin.withdrawpricelistfile;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.filter.WithdrawPriceListFileSearchFilter;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск файлов заборного прайс-листа через админку")
@DatabaseSetup("/controller/admin/withdrawtariffs/db/before/search_prepare.xml")
@DatabaseSetup("/controller/admin/withdrawpricelistfiles/db/before/search_prepare.xml")
class SearchWithdrawPriceListFileTest extends AbstractContextualTest {
    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск файлов заборного прайс-листа")
    void search(
        String displayName,
        WithdrawPriceListFileSearchFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/withdraw-price-list-files").params(TestUtils.toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "Пустой фильтр",
                new WithdrawPriceListFileSearchFilter(),
                "controller/admin/withdrawpricelistfiles/response/all.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору",
                new WithdrawPriceListFileSearchFilter().setWithdrawPriceListFileId(1L),
                "controller/admin/withdrawpricelistfiles/response/id_1.json"
            ),
            Arguments.of(
                "Фильтр по идентификатору тарифа",
                new WithdrawPriceListFileSearchFilter().setWithdrawTariffId(1L),
                "controller/admin/withdrawpricelistfiles/response/id_1_2.json"
            ),
            Arguments.of(
                "Фильтр по всем параметрам",
                new WithdrawPriceListFileSearchFilter()
                    .setWithdrawPriceListFileId(3L)
                    .setWithdrawTariffId(2L),
                "controller/admin/withdrawpricelistfiles/response/id_3.json"
            )
        );
    }
}
