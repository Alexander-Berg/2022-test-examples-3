package ru.yandex.market.checkout.checkouter.controller;

import io.qameta.allure.junit4.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import ru.yandex.market.checkout.allure.Tags;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.auth.AuthInfo;
import ru.yandex.market.checkout.helpers.AuthHelper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.SKIP_MUID_GENERATION_LIMIT;

public class AuthControllerTest extends AbstractWebTestBase {

    private static String urlTemplate = "/auth";
    private static String ip = "127.0.0.1";
    private static String userAgent = "test";
    private static String cookie = "";

    @Autowired
    private AuthHelper authHelper;


    @Tag(Tags.AUTO)
    @DisplayName("POST /auth: Получение muid")
    @Test
    public void auth() throws Exception {
        mockMvc.perform(
                post(urlTemplate)
                        .content(String.format("{\"ip\": \"%s\", \"userAgent\": \"%s\"}", ip, userAgent))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("cookie", cookie)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.muid").isString())
                .andExpect(jsonPath("$.cookie").isString());
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /auth: Получение muid ограничено лимитом")
    @Test
    public void authMuidGenerationLimit() throws Exception {
        for (int attempt = 0; attempt < 10; attempt++) {
            mockMvc.perform(
                    post(urlTemplate)
                            .content(String.format("{\"ip\": \"%s\", \"userAgent\": \"%s\"}", ip, userAgent))
                            .contentType(MediaType.APPLICATION_JSON_UTF8))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.muid").isString())
                    .andExpect(jsonPath("$.cookie").isString());
        }
        for (int attempt = 0; attempt < 10; attempt++) {
            mockMvc.perform(
                    post(urlTemplate)
                            .content(String.format("{\"ip\": \"%s\", \"userAgent\": \"%s\"}", ip, userAgent))
                            .contentType(MediaType.APPLICATION_JSON_UTF8));
        }

        mockMvc.perform(
                post(urlTemplate)
                        .content(String.format("{\"ip\": \"%s\", \"userAgent\": \"%s\"}", ip, userAgent))
                        .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is(420));
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /auth: Получение muid с флагом skipMuidGenerationLimit")
    @Test
    public void authMuidGenerationWithoutLimit() throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            mockMvc.perform(
                    post(urlTemplate)
                            .content(String.format("{\"ip\": \"%s\", \"userAgent\": \"%s\"}", ip, userAgent))
                            .contentType(MediaType.APPLICATION_JSON_UTF8)
                            .param(SKIP_MUID_GENERATION_LIMIT, "true"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.muid").isString())
                    .andExpect(jsonPath("$.cookie").isString());
        }
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /auth: проверка ошибки при отсутствии ip")
    @Test
    public void authWithoutIp() throws Exception {
        mockMvc.perform(
                post(urlTemplate)
                        .content(String.format("{\"userAgent\": \"%s\"}", userAgent))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("cookie", cookie)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Missing IP"));

        mockMvc.perform(
                post(urlTemplate)
                        .content(String.format("{\"ip\": \"\", \"userAgent\": \"%s\"}", userAgent))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("cookie", cookie)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Missing IP"));
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /auth: проверка ошибки при отсутствии userAgent")
    @Test
    public void authWithoutUserAgent() throws Exception {
        mockMvc.perform(
                post(urlTemplate)
                        .content(String.format("{\"ip\": \"%s\"}", ip))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("cookie", cookie)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Missing User-Agent"));

        mockMvc.perform(
                post(urlTemplate)
                        .content(String.format("{\"ip\": \"%s\", \"userAgent\": \"\"}", ip))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("cookie", cookie)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("Missing User-Agent"));
    }

    @Tag(Tags.AUTO)
    @DisplayName("POST /auth: Получение muid и проверка cookie")
    @Test
    public void authWithCookie() throws Exception {
        AuthInfo authInfo = authHelper.getAuthInfo();
        String responseCookie = authInfo.getCookie();

        mockMvc.perform(
                post(urlTemplate)
                        .content(String.format("{\"ip\": \"%s\", \"userAgent\": \"%s\"}", ip, userAgent))
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .param("cookie", responseCookie)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.muid").isString())
                .andExpect(jsonPath("$.cookie").value(responseCookie));
    }
}
