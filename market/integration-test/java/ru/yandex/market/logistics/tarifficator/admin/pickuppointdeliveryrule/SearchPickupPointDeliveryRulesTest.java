package ru.yandex.market.logistics.tarifficator.admin.pickuppointdeliveryrule;


import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.filter.AdminPickupPointDeliveryRuleSearchFilter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@ParametersAreNonnullByDefault
@DisplayName("Поиск данных о правилах доставки ПВЗ магазинов")
@DatabaseSetup("/controller/admin/pickuppointdeliveryrules/search.before.xml")
class SearchPickupPointDeliveryRulesTest extends AbstractContextualTest {

    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource("searchArguments")
    @DisplayName("С фильтром")
    void search(String caseName, AdminPickupPointDeliveryRuleSearchFilter filter, String jsonPath) throws Exception {
        mockMvc.perform(get("/admin/pickup-point-delivery-rules").params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(jsonPath));
    }

    @Nonnull
    private static Stream<Arguments> searchArguments() {
        return Stream.of(
            Arguments.of(
                "По пустому фильтру",
                filter(),
                "controller/admin/pickuppointdeliveryrules/0.json"
            ),
            Arguments.of(
                "По id магазина",
                filter().setShopId(2L),
                "controller/admin/pickuppointdeliveryrules/1.json"
            ),
            Arguments.of(
                "По id лог. точки в системе LMS",
                filter().setLmsLogisticsPointId(1L),
                "controller/admin/pickuppointdeliveryrules/2.json"
            ),
            Arguments.of(
                "По фильтру со всеми полями",
                filter()
                    .setShopId(2L)
                    .setLmsLogisticsPointId(1L),
                "controller/admin/pickuppointdeliveryrules/3.json"
            )
        );
    }

    @Nonnull
    private static AdminPickupPointDeliveryRuleSearchFilter filter() {
        return new AdminPickupPointDeliveryRuleSearchFilter();
    }

    @Test
    @DisplayName("При ограниченном размере списка")
    void checkTotalCount() throws Exception {
        mockMvc.perform(get("/admin/pickup-point-delivery-rules").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("totalCount").value("2"));
    }

}
