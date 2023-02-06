package ru.yandex.market.checkout.checkouter.order.getOrder;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.order.Color;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GetOrdersByExpiredTest extends GetOrdersTestBase {

    private final String urlTemplate = "/get-orders";

    @Epic(Epics.GET_ORDER)
    @Story(Stories.POST_GET_ORDERS)
    @DisplayName("POST /get-orders: работа ручки expired.")
    @Test
    public void expiredTest() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"WHITE\",\"BLUE\"],\"expired\": \"%s\"}", true))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*].id").doesNotExist());

        mockMvc.perform(
                MockMvcRequestBuilders.post(urlTemplate)
                        .content(String.format("{\"rgbs\":[\"WHITE\",\"BLUE\"],\"expired\": \"%s\"}", false))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param(CheckouterClientParams.RGB, Color.BLUE.name())
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[*].id", hasSize(3)));
    }
}
