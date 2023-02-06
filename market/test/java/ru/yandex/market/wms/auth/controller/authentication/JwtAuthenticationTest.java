package ru.yandex.market.wms.auth.controller.authentication;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.common.model.enums.AuthenticationParam;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class JwtAuthenticationTest extends AuthIntegrationTest {

    private static final String USERNAME = "test";
    private static final String PASSWORD = "password";
    private static final String INCORRECT_PASSWORD = "incorrect_password";

    @Disabled
    @Test
    public void getTokenWithCorrectPassword() throws Exception {
        mockMvc.perform(addAuthorizationHeaders(post("/get-token"), PASSWORD))
                .andExpect(status().isOk())
                .andExpect(header().exists(AuthenticationParam.TOKEN.getCode()));
    }

    @Test
    public void getTokenWithIncorrectPassword() throws Exception {
        mockMvc.perform(addAuthorizationHeaders(post("/get-token"), INCORRECT_PASSWORD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getTokenWithEmptyHeaders() throws Exception {
        mockMvc.perform(post("/get-token"))
                .andExpect(status().isBadRequest());
    }

    private MockHttpServletRequestBuilder addAuthorizationHeaders(MockHttpServletRequestBuilder builder,
                                                                  String password) {
        return builder.header(AuthenticationParam.USERNAME.getCode(), USERNAME)
                .header(AuthenticationParam.PASSWORD.getCode(), password);
    }
}
