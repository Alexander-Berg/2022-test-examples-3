package ru.yandex.market.wms.auth.controller.authentication;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.auth.config.AuthIntegrationTest;
import ru.yandex.market.wms.auth.service.JwtAuthenticationService;
import ru.yandex.market.wms.common.model.enums.AuthenticationParam;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties = "check.authentication=default")
public class CheckAuthenticationForControllerTest extends AuthIntegrationTest {

    private static final String USER_COOKIE = "TEST";
    private static final String TOKEN_COOKIE = "TOKEN";

    private MockWebServer server;

    @Autowired
    protected JwtAuthenticationService jwtAuthenticationService;

    @BeforeEach
    protected void initInforServer() throws IOException {
        server = new MockWebServer();
        server.start(9999);
    }

    @AfterEach
    protected void shutdownInforServer() throws IOException {
        server.shutdown();
    }

    @Test
    public void checkAuthSuccess() throws Exception {
        String token = jwtAuthenticationService.generateJwtToken(USER_COOKIE, Collections.emptySet(), TOKEN_COOKIE);
        Dispatcher dispatcher = new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                if (Objects.equals(request.getPath(), "/auth/check-token")) {
                    String newToken = request.getHeader(AuthenticationParam.TOKEN.getCode());
                    Assertions.assertNotNull(token, newToken);
                    return new MockResponse()
                            .setStatus("HTTP/1.1 " + HttpStatus.OK)
                            .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .setBody(Objects.requireNonNull(newToken));
                } else {
                    return new MockResponse().setResponseCode(404);
                }
            }
        };
        server.setDispatcher(dispatcher);

        mockMvc.perform(addJwtHeader(get("/check-auth"), token))
            .andExpect(status().isOk());
        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        if (recordedRequest != null) {
            String path = recordedRequest.getRequestUrl().uri().getPath();
            String actualToken = recordedRequest.getHeader(AuthenticationParam.TOKEN.getCode());
            Assertions.assertEquals("/auth/check-token", path);
            Assertions.assertEquals(token, actualToken);
        }
    }

    @Test
    public void checkAuthUnauthorized() throws Exception {
        server.enqueue(new MockResponse().setStatus("HTTP/1.1 " + HttpStatus.UNAUTHORIZED));
        mockMvc.perform(addInvalidJwtHeader(get("/check-auth")))
            .andExpect(status().isUnauthorized());
    }

    private MockHttpServletRequestBuilder addJwtHeader(MockHttpServletRequestBuilder builder, String token) {
        return builder.header(AuthenticationParam.TOKEN.getCode(), token);
    }

    private MockHttpServletRequestBuilder addInvalidJwtHeader(MockHttpServletRequestBuilder builder) {
        String token = jwtAuthenticationService.generateJwtToken(USER_COOKIE, Collections.emptySet(), TOKEN_COOKIE);
        return builder.header(AuthenticationParam.TOKEN.getCode(), token + "x");
    }
}
