package ru.yandex.market.checkout.pushapi.web;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.checkout.pushapi.application.AbstractWebTestBase;
import ru.yandex.market.checkout.pushapi.service.EnvironmentService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EnvironmentControllerTest extends AbstractWebTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnvironmentService environmentService;

    @Test
    public void environmentGetAllTest() throws Exception {
        mockMvc.perform(get("/environment")
                        .with(request -> {
                            request.setRemoteAddr("2a02:6b8:c0c:91a1:0:51e4:49fd:0");
                            return request;
                        }))
                .andExpect(status().isOk());
    }

    @Test
    public void environmentDeleteTest() throws Exception {
        environmentService.addValue("delmeName", "delmeValue");
        Assertions.assertNotNull(environmentService.getValue("delmeName"));
        mockMvc.perform(delete("/environment?name={delmeName}", "delmeName"))
                .andExpect(status().isOk());
        Assertions.assertNull(environmentService.getValue("delmeName"));
    }

    @Test
    public void environmentPutTest() throws Exception {
        mockMvc.perform(put("/environment?name={delmeName}&value={delmeValue}",
                        "delmeName", "delmeValue"))
                .andExpect(status().isOk());

        Assertions.assertEquals(environmentService.getValue("delmeName"), "delmeValue");

        mockMvc.perform(put("/environment?name={delmeName}&value={anotherDelmeValue}",
                        "delmeName", "anotherDelmeValue"))
                .andExpect(status().isOk());

        Assertions.assertEquals(environmentService.getValue("delmeName"), "anotherDelmeValue");
    }
}
