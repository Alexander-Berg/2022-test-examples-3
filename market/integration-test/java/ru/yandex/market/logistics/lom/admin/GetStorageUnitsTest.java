package ru.yandex.market.logistics.lom.admin;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Получение грузомест заказа")
@DatabaseSetup("/controller/admin/storageUnit/prepare.xml")
class GetStorageUnitsTest extends AbstractContextualTest {
    @Test
    @DisplayName("Получение грузомест заказа")
    void getUnits() throws Exception {
        requestUnits("1")
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/storageUnit/response.json"));
    }

    @Test
    @DisplayName("Получение грузомест несуществующего заказа")
    void getUnitsFail() throws Exception {
        requestUnits("2")
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with id [2]"));
    }

    @Nonnull
    private ResultActions requestUnits(String orderId) throws Exception {
        return mockMvc.perform(get("/admin/orders/storage-units").param("orderId", orderId));
    }
}
