package ru.yandex.market.logistics.nesu.controller.internal;

import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.missingParameter;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class InternalShopControllerTest extends AbstractContextualTest {

    @Test
    @DisplayName("Успешное получение магазинов с сендерами")
    @DatabaseSetup("/controller/shop/shop_with_senders.xml")
    void getShopWithSendersTest() throws Exception {
        getShopWithSenders(Set.of(201L))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/shop_with_senders_response.json"));
    }

    @Test
    @DisplayName("Успешное получение нескольких магазинов с сендерами")
    @DatabaseSetup("/controller/shop/shop_with_senders.xml")
    void getTwoShopWithSendersTest() throws Exception {
        getShopWithSenders(Set.of(201L, 202L))
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/shop/shop_with_two_senders_response.json"));
    }

    @Test
    @DisplayName("Неактивные магазины отсутствуют в результате")
    @DatabaseSetup("/controller/shop/shop_with_senders.xml")
    void getInactiveShopWithSendersTest() throws Exception {
        getShopWithSenders(Set.of(203L))
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));
    }

    @Test
    @DisplayName("Получение пустого списка магазинов")
    void getEmptyShopWithSendersTest() throws Exception {
        getShopWithSenders(Set.of(1L))
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));
    }

    @Test
    @DisplayName("Получение списка по не найденному market-id")
    @DatabaseSetup("/controller/shop/shop_with_senders.xml")
    void notFoundMarketId() throws Exception {
        getShopWithSenders(Set.of(42L))
            .andExpect(status().isOk())
            .andExpect(content().json(EMPTY_ARRAY));
    }

    @Test
    @DisplayName("Получение по null идентификатору")
    void nullMarketId() throws Exception {
        getShopWithSenders(Sets.newHashSet(1L, null))
            .andExpect(status().isBadRequest())
            .andExpect(content().json("{\"message\":\"For input string: \\\"null\\\"\",\"type\":\"UNKNOWN\"}"));
    }

    @Test
    @DisplayName("Получение по пустому списку параметров")
    void emptyParameters() throws Exception {
        getShopWithSenders(Set.of())
            .andExpect(status().isBadRequest())
            .andExpect(missingParameter("marketIds", "Set"));
    }

    @Nonnull
    private ResultActions getShopWithSenders(Set<Long> marketIds) throws Exception {
        String params = String.join("&", marketIds.stream()
            .map(id -> "marketIds=" + id)
            .collect(Collectors.toSet()));
        return mockMvc.perform(get("/internal/shops/with-senders?" + params));
    }
}
