package ru.yandex.market.logistics.tarifficator.controller.shop;

import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тестирование изменения состояния маркетного ПВЗ для магазина")
@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@ParametersAreNonnullByDefault
public class MarketPickupControllerTest extends AbstractContextualTest {
    @Test
    @DisplayName("Включение маркетного ПВЗ")
    @DatabaseSetup("/controller/shop/market-pickup/regionGroups.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/market-pickup/enableMarketPickup.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testEnableMarketPickup() throws Exception {
        mockMvc.perform(
            post(String.format("/shops/%s/market-pickup?enabled=%s", 774, true)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/shop/market-pickup/json/market_pickup_response_enabled.json"
            ));
    }

    @Test
    @DisplayName("Отключение маркетного ПВЗ")
    @DatabaseSetup("/controller/shop/market-pickup/regionGroups.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/market-pickup/disableMarketPickup.after.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testDisableMarketPickup() throws Exception {
        mockMvc.perform(
                post(String.format("/shops/%s/market-pickup?enabled=%s", 775, false)))
            .andExpect(status().isOk())
            .andExpect(jsonContent(
                "controller/shop/market-pickup/json/market_pickup_response_disabled.json"
            ));
    }

    @Test
    @DisplayName("Включение маркетного ПВЗ, если нет собственной региональной группы возвращает 404")
    @DatabaseSetup("/controller/shop/market-pickup/regionGroups.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/market-pickup/regionGroups.before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testCannotChangeMarketPickup() throws Exception {
        mockMvc.perform(
                post(String.format("/shops/%s/market-pickup?enabled=%s", 776, false)))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage(String.format("Failed to find self region group for shop %s", 776)));
    }

    @Test
    @DisplayName("Включение маркетного ПВЗ, если нет региональных групп возвращает 404")
    @DatabaseSetup("/controller/shop/market-pickup/regionGroups.before.xml")
    @ExpectedDatabase(
        value = "/controller/shop/market-pickup/regionGroups.before.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testToggleMarketPickupNoRegionGroups() throws Exception {
        mockMvc.perform(
                post(String.format("/shops/%s/market-pickup?enabled=%s", 777, false)))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage(String.format("Failed to find self region group for shop %s", 777)));
    }

    @Test
    @DisplayName("Получение включенного состояния маркетного ПВЗ")
    @DatabaseSetup("/controller/shop/market-pickup/regionGroups.before.xml")
    void testGetMarketPickupEnabled() throws Exception {
        mockMvc.perform(
                get(String.format("/shops/%s/market-pickup", 775)))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/market-pickup/json/market_pickup_response_enabled.json"));
    }

    @Test
    @DisplayName("Получение отключенного состояния маркетного ПВЗ")
    @DatabaseSetup("/controller/shop/market-pickup/regionGroups.before.xml")
    void testGetMarketPickupDisabled() throws Exception {
        mockMvc.perform(
                get(String.format("/shops/%s/market-pickup", 774)))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/market-pickup/json/market_pickup_response_disabled.json"));
    }

    @Test
    @DisplayName("Получение статуса маркетного ПВЗ для магазина, у которого нет собственной региональной группы")
    @DatabaseSetup("/controller/shop/market-pickup/regionGroups.before.xml")
    void testGetMarketPickupWithoutSelfDelivery() throws Exception {
        mockMvc.perform(
                get(String.format("/shops/%s/market-pickup", 776)))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage(String.format("Failed to find self region group for shop %s", 776)));
    }

    @Test
    @DisplayName("Получение статуса маркетного ПВЗ для магазина, у которого нет региональных групп возвращает 404")
    @DatabaseSetup("/controller/shop/market-pickup/regionGroups.before.xml")
    void testGetMarketPickupNoRegionGroups() throws Exception {
        mockMvc.perform(
                get(String.format("/shops/%s/market-pickup", 777)))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage(String.format("Failed to find self region group for shop %s", 777)));
    }

    @Test
    @DisplayName("Получение статуса маркетного ПВЗ для магазина со значением null")
    @DatabaseSetup("/controller/shop/market-pickup/regionGroups.before.xml")
    void testGetMarketPickupNullPickupInfo() throws Exception {
        mockMvc.perform(
                get(String.format("/shops/%s/market-pickup", 778)))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/market-pickup/json/market_pickup_response_disabled.json"));
    }
}
