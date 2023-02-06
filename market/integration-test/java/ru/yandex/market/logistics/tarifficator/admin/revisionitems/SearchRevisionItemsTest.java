package ru.yandex.market.logistics.tarifficator.admin.revisionitems;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.filter.RevisionItemSearchFilterDto;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Поиск элементов поколения через админку")
@DatabaseSetup("/controller/tariffs/db/search_prepare.xml")
@DatabaseSetup("/controller/admin/pricelistfiles/before/search_prepare.xml")
@DatabaseSetup("/controller/admin/revisions/before/search_prepare.xml")
@DatabaseSetup(value = "/controller/admin/revisionitems/before/search_prepare.xml", type = DatabaseOperation.REFRESH)
class SearchRevisionItemsTest extends AbstractContextualTest {

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск элементов поколений")
    void search(
        String displayName,
        RevisionItemSearchFilterDto filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/revision-items").params(TestUtils.toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Triple.of(
                "Пустой фильтр",
                new RevisionItemSearchFilterDto(),
                "controller/admin/revisionitems/response/all.json"
            ),
            Triple.of(
                "Фильтр по идентификатору",
                new RevisionItemSearchFilterDto().setRevisionItemId(1L),
                "controller/admin/revisionitems/response/id_1.json"
            ),
            Triple.of(
                "Фильтр по идентификатору тарифа",
                new RevisionItemSearchFilterDto().setTariffId(1L),
                "controller/admin/revisionitems/response/id_1_2_3_4_5.json"
            ),
            Triple.of(
                "Фильтр по идентификатору поколения",
                new RevisionItemSearchFilterDto().setRevisionId(1L),
                "controller/admin/revisionitems/response/id_1_2.json"
            ),
            Triple.of(
                "Фильтр по идентификатору файла прайс-листа",
                new RevisionItemSearchFilterDto().setPriceListFileId(1L),
                "controller/admin/revisionitems/response/id_1_3_4.json"
            ),
            Triple.of(
                "Фильтр по всем параметрам",
                new RevisionItemSearchFilterDto()
                    .setTariffId(2L)
                    .setRevisionId(3L)
                    .setRevisionItemId(6L)
                    .setPriceListFileId(3L),
                "controller/admin/revisionitems/response/id_6.json"
            ),
            Triple.of(
                "Ни один элемент поколения не подходит под фильтр",
                new RevisionItemSearchFilterDto().setTariffId(3L),
                "controller/admin/common/empty_response.json"
            )
        )
            .map(triple -> Arguments.of(triple.getLeft(), triple.getMiddle(), triple.getRight()));
    }
}
