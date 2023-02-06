package ru.yandex.market.logistics.tarifficator.admin.withdrawtariff;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.util.TestUtils;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.tarifficator.util.ValidationUtil.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение деталей заборного тарифа через админку")
@DatabaseSetup("/controller/admin/withdrawtariffs/db/before/search_prepare.xml")
class GetWithdrawTariffDetailTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получить детальную информацию о тарифе")
    void getTariffDetailInfo() throws Exception {
        mockMvc.perform(get("/admin/withdraw-tariffs/1"))
            .andExpect(status().isOk())
            .andExpect(TestUtils.jsonContent("controller/admin/withdrawtariffs/response/id_1_details.json"));
    }

    @Test
    @DisplayName("Получить детальную информацию о тарифе, тариф не найден")
    void getTariffDetailNotFound() throws Exception {
        mockMvc.perform(get("/admin/withdraw-tariffs/5"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [WITHDRAW_TARIFF] with ids [[5]]"));
    }
}
