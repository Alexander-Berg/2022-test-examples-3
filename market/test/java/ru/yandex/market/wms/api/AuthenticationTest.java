package ru.yandex.market.wms.api;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.market.wms.common.model.enums.AuthenticationParam;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.shared.libs.utils.StringUtil;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@TestPropertySource(properties = "check.authentication=custom")
public class AuthenticationTest extends IntegrationTest {

    private static final String USER_VALUE = "TEST";
    private static final String TOKEN_VALUE = "TOKEN";
    private static final String DB_PATH = "/nsqlconfig/db.xml";
    private static final String PASSWORD_VALUE = "PASS";
    private MockWebServer server;

    @BeforeEach
    protected void initInforServer() throws IOException {
        server = new MockWebServer();
        server.start(8383);
        server.enqueue(
                new MockResponse()
                        .setStatus("HTTP/1.1 " + HttpStatus.OK.toString())
                        .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .setBody(StringUtil.resourceAsString("/authentication/authResponse.json"))
        );
    }

    @AfterEach
    protected void shutdownInforServer() throws IOException {
        server.shutdown();
    }

    private void checkServerHeaders(String username, String token, String password) throws InterruptedException {
        RecordedRequest recordedRequest = server.takeRequest(1, TimeUnit.SECONDS);
        if (recordedRequest != null) {
            String path = recordedRequest.getRequestUrl().uri().getPath();
            String actualUsername = recordedRequest.getHeader(AuthenticationParam.USERNAME.getCode());
            String actualToken = recordedRequest.getHeader(AuthenticationParam.TOKEN.getCode());
            String actualPassword = recordedRequest.getHeader(AuthenticationParam.PASSWORD.getCode());
            Assertions.assertEquals("/auth", path);
            Assertions.assertEquals(username, actualUsername);
            Assertions.assertEquals(token, actualToken);
            Assertions.assertEquals(password, actualPassword);
        }
    }

    /**
     * Кейс 1:
     * Username и токен указываются в заголовках запроса.
     * Куки пустые.
     * @throws Exception
     */
    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    public void correctHeaderAuthentication() throws Exception {
        mockMvc.perform(addAuthorizationHeaders(get("/nsqlconfig/values"), USER_VALUE, TOKEN_VALUE)
                .param("configkeys", "KEY5")
                .contentType(MediaType.APPLICATION_JSON));
        checkServerHeaders(USER_VALUE, TOKEN_VALUE, null);
    }

    /**
     * Кейс 2:
     * Username и токен указываются в куках.
     * Заголовки пустые.
     * @throws Exception
     */
    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    public void correctCookiesAuthentication() throws Exception {
        mockMvc.perform(addAuthorizationCookies(get("/nsqlconfig/values"), USER_VALUE, TOKEN_VALUE)
                .param("configkeys", "KEY5")
                .contentType(MediaType.APPLICATION_JSON));
        checkServerHeaders(USER_VALUE, TOKEN_VALUE, null);
    }

    /**
     * Кейс 3:
     * Верные username и токен указываются в заголовках, устаревшие - в куках.
     * Приоритет имеют те, которые находятся в заголовках, поэтому аутентификация будет успешной.
     * @throws Exception
     */
    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    public void correctHeadersIncorrectCookiesAuthentication() throws Exception {
        mockMvc.perform(
                addAuthorizationHeaders(
                        addAuthorizationCookies(
                                get("/nsqlconfig/values"), "wronguser", "wrongtoken"),
                        USER_VALUE, TOKEN_VALUE)
                .param("configkeys", "KEY5"));
        checkServerHeaders(USER_VALUE, TOKEN_VALUE, null);
    }

    /**
     * Кейс 4:
     * Username находится в заголовках, а токен - в куки.
     * Если в заголовках нет связки имени и пароля/токена, они ищутся в куках, поэтому в запросе будет только токен.
     * @throws Exception
     */
    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    public void userInHeadersTokenInCookiesAuthentication() throws Exception {
        mockMvc.perform(get("/nsqlconfig/values")
                        .header(AuthenticationParam.USERNAME.getCode(), USER_VALUE)
                        .cookie(new Cookie(AuthenticationParam.TOKEN.getCode(), TOKEN_VALUE))
                        .param("configkeys", "KEY5")
                        .contentType(MediaType.APPLICATION_JSON));
        checkServerHeaders(null, TOKEN_VALUE, null);
    }

    /**
     * Кейс 5:
     * Токен находится в заголовках, а username - в куки.
     * Поскольку в заголовках нету связки имя + пароль/токен, они ищутся в куки, в запросе останется только username
     * @throws Exception
     */
    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    public void tokenInHeadersUserInCookiesAuthentication() throws Exception {
        mockMvc.perform(get("/nsqlconfig/values")
                .header(AuthenticationParam.TOKEN.getCode(), TOKEN_VALUE)
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), USER_VALUE))
                .param("configkeys", "KEY5")
                .contentType(MediaType.APPLICATION_JSON));
        checkServerHeaders(USER_VALUE, null, null);
    }

    /**
     * Кейс 6:
     * В заголовках username и пароль.
     * @throws Exception
     */
    @Test
    @Disabled
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    public void usernamePasswordHeadersAuthentication() throws Exception {
        mockMvc.perform(get("/nsqlconfig/values")
                .header(AuthenticationParam.USERNAME.getCode(), USER_VALUE)
                .header(AuthenticationParam.PASSWORD.getCode(), PASSWORD_VALUE)
                .param("configkeys", "KEY5")
                .contentType(MediaType.APPLICATION_JSON));
        checkServerHeaders(USER_VALUE, null, PASSWORD_VALUE);
    }

    /**
     * Кейс 7:
     * В куках username и пароль.
     * @throws Exception
     */
    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    public void usernamePasswordCookiesAuthentication() throws Exception {
        mockMvc.perform(get("/nsqlconfig/values")
                .cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), USER_VALUE),
                        new Cookie(AuthenticationParam.PASSWORD.getCode(), PASSWORD_VALUE))
                .param("configkeys", "KEY5")
                .contentType(MediaType.APPLICATION_JSON));
        checkServerHeaders(USER_VALUE, null, PASSWORD_VALUE);
    }

    /**
     * Кейс 8:
     * Username, токен и пароль не указаны.
     * Ожидаем 401 ответ.
     * @throws Exception
     */
    @Test
    @DatabaseSetup(DB_PATH)
    @ExpectedDatabase(value = DB_PATH, assertionMode = NON_STRICT_UNORDERED)
    public void checkNoAuthentication() throws Exception {
        mockMvc.perform(get("/nsqlconfig/values")
                .param("configkeys", "KEY5")
                .contentType(MediaType.APPLICATION_JSON));
        checkServerHeaders(null, null, null);
    }

    private MockHttpServletRequestBuilder addAuthorizationCookies(
            MockHttpServletRequestBuilder builder,
            String username,
            String token) {
        return builder.cookie(new Cookie(AuthenticationParam.USERNAME.getCode(), username),
                new Cookie(AuthenticationParam.TOKEN.getCode(), token));
    }

    private MockHttpServletRequestBuilder addAuthorizationHeaders(
            MockHttpServletRequestBuilder builder,
            String username,
            String token) {
        return builder.header(AuthenticationParam.USERNAME.getCode(), username)
                .header(AuthenticationParam.TOKEN.getCode(), token);
    }
}
