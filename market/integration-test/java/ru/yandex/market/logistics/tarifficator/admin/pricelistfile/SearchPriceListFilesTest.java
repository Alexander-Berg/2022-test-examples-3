package ru.yandex.market.logistics.tarifficator.admin.pricelistfile;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.filter.PriceListFileSearchFilterDto;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск файлов прайс-листа через админку")
@DatabaseSetup("/controller/tariffs/db/search_prepare.xml")
@DatabaseSetup("/controller/admin/pricelistfiles/before/search_prepare.xml")
class SearchPriceListFilesTest extends AbstractContextualTest {

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск файлов прайс-листа")
    void search(
        String displayName,
        PriceListFileSearchFilterDto filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/price-list-files").params(TestUtils.toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Triple.of(
                "Пустой фильтр",
                new PriceListFileSearchFilterDto(),
                "controller/admin/pricelistfiles/response/all.json"
            ),
            Triple.of(
                "Фильтр по идентификатору",
                new PriceListFileSearchFilterDto().setPriceListFileId(1L),
                "controller/admin/pricelistfiles/response/id_1.json"
            ),
            Triple.of(
                "Фильтр по идентификатору тарифа",
                new PriceListFileSearchFilterDto().setTariffId(1L),
                "controller/admin/pricelistfiles/response/id_1_2.json"
            ),
            Triple.of(
                "Фильтр по всем параметрам",
                new PriceListFileSearchFilterDto().setPriceListFileId(3L).setTariffId(2L),
                "controller/admin/pricelistfiles/response/id_3.json"
            ),
            Triple.of(
                "Ни один файл не подходит под фильтр",
                new PriceListFileSearchFilterDto().setPriceListFileId(3L).setTariffId(1L),
                "controller/admin/common/empty_response.json"
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }
}
