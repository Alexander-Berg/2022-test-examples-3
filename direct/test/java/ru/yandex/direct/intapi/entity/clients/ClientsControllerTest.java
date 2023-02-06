package ru.yandex.direct.intapi.entity.clients;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.intapi.configuration.IntApiTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntApiTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientsControllerTest {

    @Autowired
    private Steps steps;

    @Autowired
    private ClientsController controller;

    private MockMvc mockMvc;

    private Long clientId;
    private String clientName;
    private String clientRole;
    private Long clientCountryRegionId;

    @Before
    public void before() throws Exception {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId().asLong();
        clientName = clientInfo.getClient().getName();
        clientRole = clientInfo.getClient().getRole().toString();
        clientCountryRegionId = clientInfo.getClient().getCountryRegionId();

        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    public void checkClientsGet_httpGet() throws Exception {
        mockMvc
                .perform(get("/clients/get").param("client_id", Long.toString(clientId)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content()
                        .json(String.format(
                                "{\"clients\":[{\"client_id\":%d,\"name\":\"%s\",\"role\":\"%s\"," +
                                        "\"country_region_id\":%d}]}",
                                clientId, clientName, clientRole, clientCountryRegionId)));
    }

    @Test
    public void checkClientsGet_httpPost() throws Exception {
        mockMvc
                .perform(post("/clients/get")
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                        .content(String.format("[%d]", clientId)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(content()
                        .json(String.format(
                                "{\"clients\":[{\"client_id\":%d,\"name\":\"%s\",\"role\":\"%s\"," +
                                        "\"country_region_id\":%d}]}",
                                clientId, clientName, clientRole, clientCountryRegionId)));
    }

}
