package ru.yandex.market.wms.picking.modules.controller.pickingOrders;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PickingEndpointControllerTest extends IntegrationTest {

    @Test
    @DatabaseSetup("/fixtures/pickingOrdersEndpoint/1/task_detail.xml")
    public void getEndpointHappyPath() throws Exception {
        mockMvc.perform(get("/endpoint/123"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("fixtures/pickingOrdersEndpoint/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/fixtures/pickingOrdersEndpoint/4/db.xml")
    public void getEndpointHappyPathWithFinalToLoc() throws Exception {
        mockMvc.perform(get("/endpoint/123"))
                .andExpect(status().is2xxSuccessful())
                .andExpect(content().json(getFileContent("fixtures/pickingOrdersEndpoint/4/response.json")))
                .andReturn();
    }

    @Test
    @DatabaseSetup("/fixtures/pickingOrdersEndpoint/2/task_detail.xml")
    public void getEndpointNotFoundException() throws Exception {
        mockMvc.perform(get("/endpoint/123"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }

    @Test
    @DatabaseSetup("/fixtures/pickingOrdersEndpoint/3/task_detail.xml")
    public void getEndpointFoundTooManyEndpointsException() throws Exception {
        mockMvc.perform(get("/endpoint/123"))
                .andExpect(status().is4xxClientError())
                .andReturn();
    }
}
