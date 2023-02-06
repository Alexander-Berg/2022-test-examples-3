package ru.yandex.market.logistics.nesu.admin;

import java.time.LocalDate;
import java.time.Month;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.admin.model.request.AdminShopFilter;
import ru.yandex.market.logistics.nesu.client.enums.ShopRole;
import ru.yandex.market.logistics.nesu.client.enums.ShopStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/repository/sender/before/prepare_for_search.xml")
@DisplayName("Поиск магазинов")
class ShopSearchTest extends AbstractContextualTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArguments")
    @DisplayName("С фильтром")
    void search(String caseName, AdminShopFilter filter, String jsonPath) throws Exception {
        mockMvc.perform(get("/admin/shops").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(jsonPath));
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.of(
            Arguments.of(
                "По поисковому запросу: идентификатор магазина",
                filter().setFullTextSearch("50001"),
                "controller/admin/shop-search/1.json"
            ),
            Arguments.of(
                "По поисковому запросу: идентификатор продукта в Балансе",
                filter().setFullTextSearch("daas_1"),
                "controller/admin/shop-search/1.json"
            ),
            Arguments.of(
                "По поисковому запросу: market_id",
                filter().setFullTextSearch("10002"),
                "controller/admin/shop-search/2.json"
            ),
            Arguments.of(
                "По поисковому запросу: business_id",
                filter().setFullTextSearch("30002"),
                "controller/admin/shop-search/2.json"
            ),
            Arguments.of(
                "По поисковому запросу: название",
                filter().setFullTextSearch("ОО "),
                "controller/admin/shop-search/23.json"
            ),
            Arguments.of(
                "По поисковому запросу: идентификатор клиента в Балансе",
                filter().setFullTextSearch("20003"),
                "controller/admin/shop-search/3.json"
            ),
            Arguments.of(
                "По роли магазина",
                filter().setShopRole(ShopRole.DAAS),
                "controller/admin/shop-search/3.json"
            ),
            Arguments.of(
                "По статусу",
                filter().setShopStatus(ShopStatus.OFF),
                "controller/admin/shop-search/3.json"
            ),
            Arguments.of(
                "По дате создания",
                filter().setShopCreated(LocalDate.of(2019, Month.SEPTEMBER, 11)),
                "controller/admin/shop-search/3.json"
            ),
            Arguments.of(
                "По названию магазина",
                filter().setShopName(Set.of("ИП Доставкин")),
                "controller/admin/shop-search/1.json"
            ),
            Arguments.of(
                "По id магазина",
                filter().setShopId(Set.of(50001L)),
                "controller/admin/shop-search/1.json"
            ),
            Arguments.of(
                "По marketId магазина",
                filter().setMarketId(Set.of(10001L)),
                "controller/admin/shop-search/1.json"
            ),
            Arguments.of(
                "По нескольким названиям магазина",
                filter().setShopName(Set.of("ООО Отключаемся", "ООО \"Мир электроники\"")),
                "controller/admin/shop-search/23.json"
            ),
            Arguments.of(
                "По нескольким id магазинов",
                filter().setShopId(Set.of(50002L, 50003L)),
                "controller/admin/shop-search/23.json"
            ),
            Arguments.of(
                "По нескольким marketId магазинов",
                filter().setMarketId(Set.of(10002L, 10003L)),
                "controller/admin/shop-search/23.json"
            ),
            Arguments.of(
                "По нескольким businessId магазинов",
                filter().setBusinessId(Set.of(30002L, 30003L)),
                "controller/admin/shop-search/23.json"
            )
        );
    }

    @Nonnull
    private static AdminShopFilter filter() {
        return new AdminShopFilter();
    }

    @Test
    @DisplayName("При ограниченном размере списка")
    void checkTotalCount() throws Exception {
        mockMvc.perform(get("/admin/shops").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("totalCount").value("3"));
    }
}
