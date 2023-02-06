package ru.yandex.market.wms.autostart.autostartlogic.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class AosLogControllerTest extends AutostartIntegrationTest {

    @Test
    @DatabaseSetup("/controller/log/get-last-waves/db/before.xml")
    public void getLastWaves3outOf4() throws Exception {
        ResultActions result = mockMvc.perform(get("/log/waves")
                .param("limit", "3")
                .param("offset", "0")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/log/get-last-waves/http/response.json")));
    }
}
