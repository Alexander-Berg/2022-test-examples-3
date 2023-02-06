package ru.yandex.market.wms.autostart.controller.admin;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import ru.yandex.market.wms.autostart.utils.TestcontainersConfiguration;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DebugControllerTest extends TestcontainersConfiguration {

    @Test
    @DatabaseSetup("/fixtures/autostart/debug/base_setup.xml")
    @DatabaseSetup("/fixtures/autostart/debug/before.xml")
    @ExpectedDatabase(value = "/fixtures/autostart/debug/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void test() throws Exception {
        mockMvc.perform(post("/debug/startOrders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"orderKeys\" : [\"ORD1001\"], \"buildingId\" : null}"))
                .andExpect(status().isOk());
    }
}
