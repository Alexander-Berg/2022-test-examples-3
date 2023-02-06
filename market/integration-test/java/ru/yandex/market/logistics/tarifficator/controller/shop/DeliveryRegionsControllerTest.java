package ru.yandex.market.logistics.tarifficator.controller.shop;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
public class DeliveryRegionsControllerTest extends AbstractContextualTest {
    @Test
    @DisplayName("Тест получения дерева регионов")
    @DatabaseSetup("/controller/shop/delivery-regions/db/getDeliveryRegions.before.xml")
    void testGetDeliveryRegions() throws Exception {
        performDelliveryRegionsSearch(774L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/delivery-regions/json/getDeliveryRegions.json", false));
    }

    @Test
    @DisplayName("Тест получения дерева регионов для магазина, у которого регионаьные группы не найдены")
    @DatabaseSetup("/controller/shop/delivery-regions/db/getDeliveryRegions.before.xml")
    void testGeteliveryRegionsNoSuchShopRGs() throws Exception {
        performDelliveryRegionsSearch(777L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/delivery-regions/json/getDeliveryRegionsNoSuchShopRGs.json"));
    }

    @Nonnull
    private ResultActions performDelliveryRegionsSearch(long shopId) throws Exception {
        return mockMvc.perform(
            get("/v2/shops/" + shopId + "/delivery-regions")
                .contentType(MediaType.APPLICATION_JSON)
        );
    }
}
