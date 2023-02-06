package ru.yandex.market.logistics.nesu.admin;

import java.util.Optional;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.service.marketid.MarketIdService;
import ru.yandex.market.logistics.nesu.utils.MarketIdFactory;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DatabaseSetup("/repository/sender/before/prepare_for_search.xml")
class ShopGetTest extends AbstractContextualTest {

    @Autowired
    private MarketIdService marketIdService;

    @BeforeEach
    void setup() {
        when(marketIdService.findAccountById(10001L)).thenReturn(Optional.of(MarketIdFactory.marketAccount()));
        when(marketIdService.findAccountById(10002L)).thenReturn(Optional.of(MarketIdFactory.marketAccount()));
    }

    @Test
    @DisplayName("Получить магазин")
    void getShop() throws Exception {
        getShop(50001L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shop-get/1.json"));
    }

    @Test
    @DisplayName("Получить магазин с заполненным businessId")
    void getShopWithBusinessID() throws Exception {
        getShop(50002L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/admin/shop-get/2.json"));
    }

    @Test
    @DisplayName("Магазин не найден")
    void getShopNotFound() throws Exception {
        getShop(10L)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [SHOP] with ids [10]"));
    }

    @Nonnull
    private ResultActions getShop(long shopId) throws Exception {
        return mockMvc.perform(get("/admin/shops/" + shopId));
    }
}
