package ru.yandex.market.logistics.tarifficator.admin.tpl;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup(
    value = "/controller/admin/tpl/couriertariffs/db/before/search_prepare.xml",
    connection = "dbUnitQualifiedDatabaseConnection"
)
class GetCourierTariffDetailsTest extends AbstractContextualTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Получить детальную информацию о курьерском тарифе")
    void getTariffDetailInfo() throws Exception {
        mockMvc.perform(get("/admin/tpl-courier-tariffs/1"))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/tpl/couriertariffs/response/id_1_details.json"));
    }

    @Test
    @DisplayName("Получить детальную информацию о тарифе, тариф не найден")
    void getTariffDetailNotFound() throws Exception {
        mockMvc.perform(get("/admin/tpl-courier-tariffs/5"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [COURIER_TARIFF] with ids [[5]]"));
    }
}
