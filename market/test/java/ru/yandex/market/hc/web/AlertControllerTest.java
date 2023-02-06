package ru.yandex.market.hc.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.hc.annotation.WebControllerTest;
import ru.yandex.market.hc.service.MemcachedService;
import ru.yandex.market.hc.service.RateLimitingService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author: aproskriakov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebControllerTest(classes = AlertController.class)
public class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemcachedService memcachedService;

    @MockBean
    RateLimitingService rateLimitingService;

    @Test
    public void testAlertRequest() throws Exception {
        String hostName = "antifraud";
        String serviceName = "antifraud-cache.tst.vs.market.yandex.net:900/antifraud/loyalty/restrictions/many";
        String status = "OK";
        String req = "{\n" +
                "    \"checks\": [\n" +
                "        {\n" +
                "            \"host_name\": \"" + hostName + "\",\n" +
                "            \"service_name\": \"" + serviceName + "\",\n" +
                "            \"status\": \"" + status + "\",\n" +
                "            \"hash\": \"хэш состояния агрегата, для сверки\",\n" +
                "            \"flags\": [\"actual\"]\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        mockMvc.perform(post("/alert")
                .content(req)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testAlertRequestWithEmptyChecks() throws Exception {
        String req = "{\n" +
                "    \"checks\": []\n" +
                "}";
        mockMvc.perform(post("/alert")
                .content(req)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void testAlertRequestWithEmptyService() throws Exception {
        String hostName = "antifraud-cache.tst.vs.market.yandex.net:900";
        String serviceName = "";
        String status = "OK";
        String req = "{\n" +
                "    \"checks\": [\n" +
                "        {\n" +
                "            \"host_name\": \"" + hostName + "\",\n" +
                "            \"service_name\": \"" + serviceName + "\",\n" +
                "            \"status\": \"" + status + "\",\n" +
                "            \"hash\": \"хэш состояния агрегата, для сверки\",\n" +
                "            \"flags\": [\"actual\", \"invalid\", \"flapping\", \"downtime\", \"unreach\", " +
                "\"no_data\"]\n" +
                "        }\n" +
                "    ]\n" +
                "}";
        mockMvc.perform(post("/alert")
                .content(req)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is4xxClientError());
    }
}
