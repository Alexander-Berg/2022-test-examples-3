package ru.yandex.market.logistics.nesu.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты ручки /back-office/shop АПИ ShopController")
@DatabaseSetup("/controller/shop/prepare.xml")
class ShopControllerTest extends AbstractContextualTest {

    @Test
    @DisplayName("Получить информацию о магазине")
    void getShopSuccess() throws Exception {
        getShop(1L)
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/get_shop.json"));
    }

    @Test
    @DisplayName("Магазин не найден")
    void getShopNotFound() throws Exception {
        getShop(10L)
            .andExpect(status().isNotFound())
            .andExpect(jsonContent("controller/shop/not_found.json"));
    }

    @NotNull
    private ResultActions getShop(long shopId) throws Exception {
        return mockMvc.perform(
            get("/back-office/shop")
                .param("userId", "1")
                .param("shopId", String.valueOf(shopId)));
    }
}
