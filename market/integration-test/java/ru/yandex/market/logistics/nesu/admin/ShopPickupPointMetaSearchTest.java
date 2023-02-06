package ru.yandex.market.logistics.nesu.admin;

import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.request.AdminShopPickupPointMetaFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@ParametersAreNonnullByDefault
@DatabaseSetup("/repository/shop-pickup-point-meta/before.xml")
@DisplayName("Поиск данных о ПВЗ и тарифах")
class ShopPickupPointMetaSearchTest extends AbstractContextualTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArguments")
    @DisplayName("С фильтром")
    void search(String caseName, AdminShopPickupPointMetaFilter filter, String jsonPath) throws Exception {
        mockMvc.perform(get("/admin/shop-pickup-point-metas").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(jsonPath));
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.of(
            Arguments.of(
                "По пустому фильтру",
                filter(),
                "controller/admin/shop-pickup-point-meta/0.json"
            ),
            Arguments.of(
                "По id магазина",
                filter().setShopId(2L),
                "controller/admin/shop-pickup-point-meta/1.json"
            ),
            Arguments.of(
                "По id лог. точки в системе LMS",
                filter().setLogisticPointId(10L),
                "controller/admin/shop-pickup-point-meta/2.json"
            ),
            Arguments.of(
                "По id в Тариффикаторе",
                filter().setTarifficatorId(1L),
                "controller/admin/shop-pickup-point-meta/3.json"
            ),
            Arguments.of(
                "По фильтру со всеми полями",
                filter()
                    .setShopId(2L)
                    .setLogisticPointId(10L)
                    .setTarifficatorId(1L),
                "controller/admin/shop-pickup-point-meta/4.json"
            )
        );
    }

    @Nonnull
    private static AdminShopPickupPointMetaFilter filter() {
        return new AdminShopPickupPointMetaFilter();
    }

    @Test
    @DisplayName("При ограниченном размере списка")
    void checkTotalCount() throws Exception {
        mockMvc.perform(get("/admin/shop-pickup-point-metas").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("totalCount").value("2"));
    }
}
