package ru.yandex.market.logistics.tarifficator.admin.pickuppointdeliveryrule;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@ParametersAreNonnullByDefault
@DisplayName("Получение детальных данных о правилах доставки ПВЗ магазинов")
@DatabaseSetup("/controller/admin/pickuppointdeliveryrules/search.before.xml")
class GetPickupPointDeliveryRuleDetailTest extends AbstractContextualTest {

    @Test
    @DisplayName("Существующие данные")
    void existing() throws Exception {
        mockMvc.perform(get("/admin/pickup-point-delivery-rules/1"))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/pickuppointdeliveryrules/detail.json"));
    }

    @Test
    @DisplayName("Несуществующие данные")
    void nonExisting() throws Exception {
        mockMvc.perform(get("/admin/pickup-point-delivery-rules/100"))
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/admin/pickuppointdeliveryrules/not_found.json"));
    }

}
