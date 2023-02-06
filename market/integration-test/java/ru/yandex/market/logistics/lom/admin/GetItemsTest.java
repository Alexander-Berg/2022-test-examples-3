package ru.yandex.market.logistics.lom.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.admin.filter.ItemSearchFilterDto;
import ru.yandex.market.logistics.lom.entity.enums.VatType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("Получение товаров заказов")
class GetItemsTest extends AbstractContextualTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArgument")
    @DisplayName("Поиск товаров")
    @DatabaseSetup("/controller/admin/item/before/items.xml")
    void search(
        @SuppressWarnings("unused") String displayName,
        ItemSearchFilterDto filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get("/admin/orders/items").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> searchArgument() {
        return Stream.of(
            Arguments.of(
                "По идентификатору заказа",
                new ItemSearchFilterDto().setOrderId(1L),
                "controller/admin/item/response/id_1_and_2.json"
            ),
            Arguments.of(
                "По подстроке наименования или артикула",
                new ItemSearchFilterDto().setOrderId(3L).setName("article-"),
                "controller/admin/item/response/id_4_and_6.json"

            ),
            Arguments.of(
                "По типу налогообложения",
                new ItemSearchFilterDto().setOrderId(3L).setVatType(VatType.NO_VAT),
                "controller/admin/item/response/id_4_and_5.json"
            )
        );
    }

    @Test
    @DisplayName("Валидация фильтра")
    void validateFilter() throws Exception {
        mockMvc.perform(
                get("/admin/orders/items")
                    .param("vatType", "invalid-vat-type")
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonContent("controller/admin/item/response/validate_filter.json"));
    }
}
