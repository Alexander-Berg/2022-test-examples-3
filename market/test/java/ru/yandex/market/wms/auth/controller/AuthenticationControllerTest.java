package ru.yandex.market.wms.auth.controller;

import java.io.IOException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.hamcrest.core.StringStartsWith;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LookupAttemptingCallback;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.common.model.enums.AuthenticationParam;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class AuthenticationControllerTest extends AuthIntegrationTest {

    private static final String USER_COOKIE = "TEST";
    private static final String PASSWORD_COOKIE = "PASSWORD";
    private static final String INCORRECT_PASSWORD_COOKIE = "INCORRECT_PASSWORD";
    private static final String TOKEN_COOKIE = "TOKEN";
    private static final String JWT_START = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJ0ZXN0I";

    private MockWebServer server;

    @Autowired
    @MockBean
    private LdapTemplate ldapTemplate;

    @BeforeEach
    protected void initInforServer() throws IOException {
        server = new MockWebServer();
        server.start(8383);
    }

    @AfterEach
    protected void shutdownInforServer() throws IOException {
        server.shutdown();
        Mockito.reset(ldapTemplate);
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/patch/user-data/before.xml", connection = "scprdd1Connection")
    public void loginWithCorrectPassword() throws Exception {
        doNothing().when(ldapTemplate).authenticate(any(), eq(PASSWORD_COOKIE));
        server.enqueue(new MockResponse().setStatus("HTTP/1.1 " + HttpStatus.OK).setBody(JWT_START));
        mockMvc.perform(addAuthorizationCookiesWithPassword(get("/login")))
                .andExpect(status().isOk())
                .andExpect(content().string(StringStartsWith.startsWith(JWT_START)));
    }

    @Test
    @DatabaseSetup(value = "/db/dao/user/patch/user-data/before.xml", connection = "scprdd1Connection")
    public void loginWithIncorrectPassword() throws Exception {
        doThrow(new RuntimeException("Incorrect password"))
                .when(ldapTemplate)
                .authenticate(any(), eq(INCORRECT_PASSWORD_COOKIE), any(LookupAttemptingCallback.class));
        server.enqueue(new MockResponse().setStatus("HTTP/1.1 " + HttpStatus.UNAUTHORIZED));
        mockMvc.perform(addAuthorizationCookiesWithIncorrrectPassword(get("/login")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void correctLogout() throws Exception {
        server.enqueue(new MockResponse().setStatus("HTTP/1.1 " + HttpStatus.OK));
        mockMvc.perform(put("/logout-user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void checkAdHealthCheckIsOk() throws Exception {
        mockMvc.perform(get("/hc/ad")).andExpect(status().isOk());
    }

    private MockHttpServletRequestBuilder addAuthorizationCookiesWithPassword(MockHttpServletRequestBuilder builder) {
        return builder.header(AuthenticationParam.USERNAME.getCode(), USER_COOKIE)
                .header(AuthenticationParam.PASSWORD.getCode(), PASSWORD_COOKIE);
    }
    private MockHttpServletRequestBuilder addAuthorizationCookiesWithIncorrrectPassword(
            MockHttpServletRequestBuilder builder
    ) {
        return builder.header(AuthenticationParam.USERNAME.getCode(), USER_COOKIE)
                .header(AuthenticationParam.PASSWORD.getCode(), INCORRECT_PASSWORD_COOKIE);
    }
}
