package ru.yandex.market.logistics.nesu.api.auth;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.nesu.api.AbstractApiTest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class OAuthTest extends AbstractApiTest {

    @Test
    @DisplayName("Запрос без авторизационного заголовка")
    void noAuthHeader() throws Exception {
        mockMvc.perform(get("/api/echo"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Запрос с авторизационным заголовком неверного формата")
    void invalidAuthHeader() throws Exception {
        mockMvc.perform(get("/api/echo").header("Authorization", "Basic 123abc"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Запрос с недействительным токеном")
    void expiredToken() throws Exception {
        when(blackboxService.oauth(authHolder.getToken())).thenReturn(authHolder.createInfo(Map.of(
            "error", "expired_token"
        )));

        mockMvc.perform(get("/api/echo").headers(authHeaders()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Запрос с недостаточным набором прав")
    void missingScope() throws Exception {
        when(blackboxService.oauth(authHolder.getToken())).thenReturn(authHolder.createInfo(Map.of(
            "error", "OK",
            "scope", Set.of("")
        )));

        mockMvc.perform(get("/api/echo").headers(authHeaders()))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Успешный запрос")
    void correctAuth() throws Exception {
        long id = 123;

        when(blackboxService.oauth(authHolder.getToken())).thenReturn(authHolder.createInfo(Map.of(
            "error", "OK",
            "scope", Set.of("delivery:partner-api"),
            "uid", id
        )));

        mockMvc.perform(get("/api/echo").headers(authHeaders()))
            .andExpect(status().isOk())
            .andExpect(content().string(String.valueOf(id)));
    }

}
