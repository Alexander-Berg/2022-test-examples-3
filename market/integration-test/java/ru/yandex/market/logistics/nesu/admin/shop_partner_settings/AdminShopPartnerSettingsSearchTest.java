package ru.yandex.market.logistics.nesu.admin.shop_partner_settings;

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
import ru.yandex.market.logistics.nesu.admin.model.enums.AdminShopPartnerType;
import ru.yandex.market.logistics.nesu.admin.model.request.AdminShopPartnerFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DatabaseSetup("/repository/shop-partner/before/prepare_for_search.xml")
@DisplayName("Поиск настроек партнеров магазинов")
class AdminShopPartnerSettingsSearchTest extends AbstractContextualTest {

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchArguments")
    @DisplayName("С фильтром")
    void search(String caseName, AdminShopPartnerFilter filter, String jsonPath) throws Exception {
        mockMvc.perform(get("/admin/shops/partners").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(jsonPath));
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.of(
            Arguments.of(
                "По id магазина",
                filter().setShopId(50001L),
                "controller/admin/shop-partner-search/12.json"
            ),
            Arguments.of(
                "По id партнера",
                filter().setPartnerId(102L),
                "controller/admin/shop-partner-search/23.json"
            ),
            Arguments.of(
                "По типу партнера",
                filter().setPartnerType(AdminShopPartnerType.DROPSHIP),
                "controller/admin/shop-partner-search/13.json"
            )
        );
    }

    @Nonnull
    private static AdminShopPartnerFilter filter() {
        return new AdminShopPartnerFilter();
    }

    @Test
    @DisplayName("При ограниченном размере списка")
    void checkTotalCount() throws Exception {
        mockMvc.perform(get("/admin/shops/partners").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("totalCount").value("3"));
    }
}
