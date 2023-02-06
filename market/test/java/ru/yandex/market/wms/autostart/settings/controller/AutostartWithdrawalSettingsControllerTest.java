package ru.yandex.market.wms.autostart.settings.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetups({
        @DatabaseSetup(value = "/fixtures/autostart/withdrawals/withdrawal-settings-empty.xml", connection =
                "wmwhseConnection")
})
class AutostartWithdrawalSettingsControllerTest extends AutostartIntegrationTest {

    @Test
    public void get_update_get() throws Exception {
        String url = "/settings/withdrawal";

        String empty = json("{maxItemsPerOrder: 10, maxItemsPerPutwall: 500, ordersIntoPutWall: 30}");
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().json(empty, STRICT));

        String update = json("{maxItemsPerOrder: 20, maxItemsPerPutwall: 501, ordersIntoPutWall: 35}");
        mockMvc.perform(
                put(url)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(update))
                .andExpect(status().isOk());

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().json(update, STRICT));
    }
}
