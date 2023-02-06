package ru.yandex.common.services.auth.blackbox;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class BlackboxServiceTest {

    private static final String DB_FIELDS_LOGIN = "accounts.login.uid";
    private static final String METHOD = "userinfo";
    private static final UserInfo USER_INFO = createUser(1L, "first");
    private static final OAuthInfo O_AUTH_INFO = createOAuthInfo();

    private final BlackboxService blackboxService = new BlackboxService();

    private HttpClient client;

    @BeforeEach
    void mockHttpClient() {
        blackboxService.setBlackboxUrl("localhost");
        client = Mockito.mock(HttpClient.class);
        blackboxService.setHttpClient(client);
    }

    private static Stream<Arguments> parameterizedTestData() {
        return Stream.of(
                Arguments.of(
                        "Параметры передаются как String",
                        (Function<BlackboxService, UserInfo>) service -> service.requestBlackbox(
                                METHOD,
                                "emails=getall&get_public_name=yes&regname=yes&uid=1&login=" + toEncode("ma@yandex.ru"),
                                DB_FIELDS_LOGIN,
                                new UserInfoResponseParser()
                        ),
                        (Consumer<URI>) uri -> assertThat(uri).hasParameter("method", "userinfo")
                                .hasParameter("dbfields", "accounts.login.uid")
                                .hasParameter("emails", "getdefault")
                                .hasParameter("emails", "getall")
                                .hasParameter("userip", "127.0.0.1")
                                .hasParameter("uid", "1")
                                .hasParameter("login", "ma@yandex.ru")
                                .hasParameter("regname", "yes")
                                .hasParameter("get_public_name", "yes")

                ),
                Arguments.of(
                        "Параметры передаются как Map",
                        (Function<BlackboxService, UserInfo>) service -> service.requestBlackbox(
                                METHOD,
                                ImmutableMap.of(
                                        "get_public_name", Collections.singletonList("yes"),
                                        "regname", Arrays.asList("yes", "no"),
                                        "uid", Collections.singletonList("1"),
                                        "emails", Collections.singletonList("getall"),
                                        "login", Collections.singletonList("ma@yandex.ru")
                                ),
                                DB_FIELDS_LOGIN,
                                new UserInfoResponseParser()
                        ),
                        (Consumer<URI>) uri -> assertThat(uri).hasParameter("method", "userinfo")
                                .hasParameter("dbfields", "accounts.login.uid")
                                .hasParameter("userip", "127.0.0.1")
                                .hasNoParameter("emails", "getdefault")
                                .hasParameter("emails", "getall")
                                .hasParameter("uid", "1")
                                .hasParameter("regname", "yes,no")
                                .hasParameter("login", "ma@yandex.ru")
                                .hasParameter("get_public_name", "yes")
                ),
                Arguments.of(
                        "Поиск информации о пользователе по уникальному идентификатору",
                        (Function<BlackboxService, UserInfo>) service -> service.userinfoByUid(150L),
                        (Consumer<URI>) uri -> assertThat(uri).hasParameter("method", "userinfo")
                                .hasParameter("dbfields", "accounts.login.uid")
                                .hasParameter("userip", "127.0.0.1")
                                .hasParameter("emails", "getdefault")
                                .hasNoParameter("emails", "getall")
                                .hasParameter("uid", "150")
                ),
                Arguments.of(
                        "Поиск информации о пользователе по логину",
                        (Function<BlackboxService, UserInfo>) service -> service.userinfoByLogin("ma@yandex.ru"),
                        (Consumer<URI>) uri -> assertThat(uri).hasParameter("method", "userinfo")
                                .hasParameter("dbfields", "accounts.login.uid")
                                .hasParameter("userip", "127.0.0.1")
                                .hasParameter("emails", "getdefault")
                                .hasNoParameter("emails", "getall")
                                .hasParameter("login", "ma@yandex.ru")
                )
        );
    }

    @SuppressWarnings("unused")
    @DisplayName("Запросы к blackbox c параметрами")
    @ParameterizedTest(name = "{0}")
    @MethodSource("parameterizedTestData")
    void requestBlackbox_parametrized_successful(String testName,
                                                 Function<BlackboxService, UserInfo> requestService,
                                                 Consumer<URI> assertUri) throws IOException {
        Mockito.when(client.execute(any(HttpUriRequest.class), any(ResponseHandler.class)))
                .thenReturn(USER_INFO);

        UserInfo userInfo = requestService.apply(blackboxService);
        assertThat(userInfo).isEqualTo(USER_INFO);

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        Mockito.verify(client).execute(captor.capture(), any(ResponseHandler.class));

        URI uri = captor.getValue().getURI();
        assertUri.accept(uri);
    }

    @DisplayName("Поиск информации о пользователе по токену")
    @Test
    void oauth_token_successful() throws IOException {
        Mockito.when(client.execute(any(HttpUriRequest.class), any(ResponseHandler.class)))
                .thenReturn(O_AUTH_INFO);

        OAuthInfo oAuthInfo = blackboxService.oauth("41%942-912fdR3_34f");
        assertThat(oAuthInfo).isEqualTo(O_AUTH_INFO);

        ArgumentCaptor<HttpUriRequest> captor = ArgumentCaptor.forClass(HttpUriRequest.class);
        Mockito.verify(client).execute(captor.capture(), any(ResponseHandler.class));

        URI uri = captor.getValue().getURI();
        assertThat(uri).hasParameter("method", "oauth")
                .hasParameter("dbfields", "accounts.login.uid")
                .hasParameter("userip", "127.0.0.1")
                .hasParameter("emails", "getdefault")
                .hasNoParameter("emails", "getall")
                .hasParameter("oauth_token", "41%942-912fdR3_34f");
    }

    @SuppressWarnings("SameParameterValue")
    private static UserInfo createUser(long uid, String login) {
        UserInfoResponseParser parser = new UserInfoResponseParser();
        parser.onUserId(uid);
        parser.onLogin(login);
        return parser.getParsedModel();
    }

    private static OAuthInfo createOAuthInfo() {
        OAuthInfo authInfo = new OAuthInfo();
        authInfo.clientId = "130";
        authInfo.login = "ma";
        authInfo.uid = 130L;
        return authInfo;
    }

    @SuppressWarnings("SameParameterValue")
    private static String toEncode(String param) {
        try {
            return URLEncoder.encode(param, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Wrong encode.");
        }
    }
}
