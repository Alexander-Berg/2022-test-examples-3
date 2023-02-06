package ru.yandex.market.checkout.helpers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import ru.yandex.market.checkout.checkouter.auth.AuthInfo;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebTestHelper
public class AuthHelper {

    private static String urlTemplate = "/auth";
    private static String ip = "127.0.0.1";
    private static String userAgent = "test";
    private static String cookie = "";

    @Autowired
    private TestSerializationService testSerializationService;
    @Autowired
    private MockMvc mockMvc;

    public AuthInfo getAuthInfo() throws Exception {
        MvcResult result = mockMvc.perform(
                post(urlTemplate)
                        .content(String.format("{\"ip\": \"%s\", \"userAgent\": \"%s\"}", ip, userAgent))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("cookie", cookie)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.muid").isString())
                .andExpect(jsonPath("$.cookie").isString())
                .andReturn();
        return testSerializationService.deserializeCheckouterObject(result.getResponse().getContentAsString(),
                AuthInfo.class);
    }

    public void auth(String ip, String userAgent, int expectedStatus) throws Exception {
        mockMvc.perform(
                post(urlTemplate)
                        .content(String.format("{\"ip\": \"%s\", \"userAgent\": \"%s\"}", ip, userAgent))
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is(expectedStatus));
    }

    public void ordersByBindKey(HttpHeaders headers, int expectedStatus) throws Exception {
        mockMvc.perform(
                get("/orders/by-bind-key/{bindKey}", "123.asdasd")
                        .param(CheckouterClientParams.UID, "123")
                        .headers(headers)
        )
                .andExpect(status().is(expectedStatus));
    }

}
