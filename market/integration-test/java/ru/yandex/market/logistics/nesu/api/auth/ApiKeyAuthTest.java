package ru.yandex.market.logistics.nesu.api.auth;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.logistics.apikeys.ApiKeysClient;
import ru.yandex.market.logistics.apikeys.model.KeyInfo;
import ru.yandex.market.logistics.apikeys.model.KeyUser;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

class ApiKeyAuthTest extends AbstractContextualTest {

    private static final String SERVICE_TOKEN = "test-service-token";
    private static final String USER_TOKEN = "test-user-token";
    private static final long USER_ID = 123;

    @Autowired
    private ApiKeysClient apiKeysClient;

    @BeforeEach
    void setupAuth() {
        when(apiKeysClient.checkKey(eq(SERVICE_TOKEN), eq(USER_TOKEN), anyString())).thenReturn(KeyInfo.builder()
            .user(KeyUser.builder().uid(USER_ID).build())
            .build());
    }

    @Test
    @DisplayName("Запрос без авторизационного заголовка")
    void noAuthHeader() throws Exception {
        mockMvc.perform(echo())
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Запрос с недействительным токеном")
    void expiredToken() throws Exception {
        doThrow(new RuntimeException("test exception"))
            .when(apiKeysClient).checkKey(eq(SERVICE_TOKEN), eq(USER_TOKEN), anyString());

        mockMvc.perform(echo().headers(authHeaders()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Метод, недоступный для API ключей")
    void inaccessibleMethod() throws Exception {
        mockMvc.perform(delete("/api/orders/123").headers(authHeaders()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Успешный запрос")
    void correctAuth() throws Exception {
        mockMvc.perform(echo().headers(authHeaders()))
            .andExpect(status().isOk())
            .andExpect(content().string(String.valueOf(USER_ID)));
    }

    @Nonnull
    private MockHttpServletRequestBuilder echo() {
        return get("/api/echo");
    }

    @Nonnull
    private HttpHeaders authHeaders() {
        HttpHeaders result = new HttpHeaders();
        result.add("X-Yandex-API-Key", USER_TOKEN);
        return result;
    }

}
