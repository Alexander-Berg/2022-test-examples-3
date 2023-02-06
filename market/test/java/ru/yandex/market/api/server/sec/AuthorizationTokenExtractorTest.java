package ru.yandex.market.api.server.sec;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.Maps;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.CommonClient;
import ru.yandex.market.api.server.sec.oauth.OAuthSecurityConfig;
import ru.yandex.market.api.server.sec.oauth.scheme.CookieSessionIdAuthorizationScheme;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.api.util.AuthorizatoinTokenTestUtil.assertEqualsOAuthTokens;
import static ru.yandex.market.api.util.AuthorizatoinTokenTestUtil.assertEqualsSessionIds;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithContext
public class AuthorizationTokenExtractorTest extends UnitTestBase {
    private AuthorizationTokenExtractor authorizationTokenExtractor;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        Map<String, Collection<AuthorizationType>> config = Maps.newHashMap();
        config.put(CommonClient.Type.EXTERNAL.name(), Collections.singleton(AuthorizationType.OAuth));
        config.put(CommonClient.Type.INTERNAL.name(), Arrays.asList(AuthorizationType.OAuth,
                AuthorizationType.SessionId));
        config.put(CommonClient.Type.MOBILE.name(), Arrays.asList(AuthorizationType.OAuth,
                AuthorizationType.SessionId, AuthorizationType.SberLog));
        config.put(CommonClient.Type.VENDOR.name(), Collections.singleton(AuthorizationType.OAuth));

        this.authorizationTokenExtractor = new AuthorizationTokenExtractor(
                new OAuthSecurityConfig(config)
        );
    }

    @Test
    public void shouldExtractOauthTokenFromAuthorizationHeader() {
        clientOfType(CommonClient.Type.EXTERNAL);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "OAuth some-oauth-token");

        checkOAuth(new OAuthToken("some-oauth-token"), extract(request));
    }

    @Test
    public void shouldExtractOauthTokenFromXUserAuthorizationHeader() {
        clientOfType(CommonClient.Type.EXTERNAL);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthorizationTokenExtractor.OAUTH_TOKEN_HEADER_2_NAME, "OAuth some-oauth-token");

        checkOAuth(new OAuthToken("some-oauth-token"), extract(request));
    }

    @Test
    public void shouldNotExtractSessionIdSchemeForExternal() {
        clientOfType(Client.Type.EXTERNAL);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthorizationTokenExtractor.OAUTH_TOKEN_HEADER_2_NAME, "SessionId Host=ya.ru; Id=session id" +
                " token");

        checkNotExist(extract(request));
    }

    @Test
    public void shouldExtractSessionIdSchemeForInternal() {
        clientOfType(Client.Type.INTERNAL);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthorizationTokenExtractor.OAUTH_TOKEN_HEADER_2_NAME, "SessionId Host=ya.ru; Id=session id" +
                " token");

        checkSessionId(new SessionIdToken("ya.ru", "session id token"), extract(request));
    }

    @Test
    public void shouldNotTryExtractEmptyTokenFromAuthorizationHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "OAuth ");

        checkNotExist(extract(request));
    }

    @Test
    public void shouldNotExtractSberLogTokenFromXUserAuthorizationHeaderForExternal() {
        clientOfType(CommonClient.Type.EXTERNAL);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthorizationTokenExtractor.OAUTH_TOKEN_HEADER_2_NAME, "SberLog 123");

        checkNotExist(extract(request));
    }


    @Test
    public void shouldNotExtractSberLogTokenFromXUserAuthorizationHeaderForVendor() {
        clientOfType(CommonClient.Type.VENDOR);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthorizationTokenExtractor.OAUTH_TOKEN_HEADER_2_NAME, "SberLog 123");

        checkNotExist(extract(request));
    }

    @Test
    public void shouldExtractSberLogTokenFromXUserAuthorizationHeaderForInternal() {
        clientOfType(CommonClient.Type.INTERNAL);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthorizationTokenExtractor.OAUTH_TOKEN_HEADER_2_NAME, "SberLog 123");

        checkNotExist(extract(request));
    }

    @Test
    public void shouldExtractSberLogTokenFromXUserAuthorizationHeader() {
        clientOfType(CommonClient.Type.MOBILE);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthorizationTokenExtractor.OAUTH_TOKEN_HEADER_2_NAME, "SberLog 123");

        checkSberLog(new SberLogToken("123"), extract(request));
    }

    @Test
    public void shouldPreferSberLogTokenThanOAuth() {
        clientOfType(CommonClient.Type.MOBILE);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(AuthorizationTokenExtractor.OAUTH_TOKEN_HEADER_2_NAME, "SberLog 123");
        request.addHeader(AuthorizationTokenExtractor.OAUTH_TOKEN_HEADER_2_NAME, "OAuth 45");

        checkSberLog(new SberLogToken("123"), extract(request));
    }

    @Test
    public void shouldExtractSessionFromCookieForInternal() {
        clientOfType(CommonClient.Type.INTERNAL);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(CookieSessionIdAuthorizationScheme.SESSION_ID_PREFIX, "session id token"),
                new Cookie("anotherData", "any")
        );
        request.addHeader(HttpHeaders.HOST, "ya.ru");

        checkSessionId(new SessionIdToken("ya.ru", "session id token"), extract(request));
    }

    @Test
    public void shouldExtractSessionFromCookieForExternal() {
        clientOfType(CommonClient.Type.EXTERNAL);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(CookieSessionIdAuthorizationScheme.SESSION_ID_PREFIX, "session id token"),
                new Cookie("anotherData", "any")
        );
        request.addHeader(HttpHeaders.HOST, "ya.ru");

        checkNotExist(extract(request));
    }

    @Test
    public void checkPriorityCookieTokenFromXUserAuthorization() {
        clientOfType(CommonClient.Type.INTERNAL);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(CookieSessionIdAuthorizationScheme.SESSION_ID_PREFIX, "session id token"),
                new Cookie("anotherData", "any")
        );
        request.addHeader(HttpHeaders.HOST, "ya.ru");

        request.addHeader(AuthorizationTokenExtractor.OAUTH_TOKEN_HEADER_2_NAME, "OAuth some-oauth-token");

        checkOAuth(new OAuthToken("some-oauth-token"), extract(request));
    }

    @Test
    public void checkPriorityCookieTokenFromAuthorization() {
        clientOfType(CommonClient.Type.EXTERNAL);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(CookieSessionIdAuthorizationScheme.SESSION_ID_PREFIX, "session id token"),
                new Cookie("anotherData", "any")
        );
        request.addHeader(HttpHeaders.HOST, "ya.ru");

        request.addHeader(HttpHeaders.AUTHORIZATION, "OAuth some-oauth-token");

        checkOAuth(new OAuthToken("some-oauth-token"), extract(request));
    }

    @Test
    public void shouldExtractSessionFromCookieForInternalWithOutHost() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(CookieSessionIdAuthorizationScheme.SESSION_ID_PREFIX, "session id token"),
                new Cookie("anotherData", "any")
        );

        checkNotExist(extract(request));
    }

    @Test
    public void shouldExtractSessionFromCookieForInternalEmptySessionId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie(CookieSessionIdAuthorizationScheme.SESSION_ID_PREFIX, ""),
                new Cookie("anotherData", "any")
        );
        request.addHeader(HttpHeaders.HOST, "ya.ru");

        checkNotExist(extract(request));
    }

    private void checkOAuth(OAuthToken expected, Optional<AuthorizationToken> oActual) {
        assertTrue(oActual.isPresent());
        AuthorizationToken actual = oActual.get();

        assertTrue(actual instanceof OAuthToken);

        assertEqualsOAuthTokens(expected, (OAuthToken) actual);
    }

    private void checkSessionId(SessionIdToken expected, Optional<AuthorizationToken> oActual) {
        assertTrue(oActual.isPresent());
        AuthorizationToken actual = oActual.get();

        assertTrue(actual instanceof SessionIdToken);

        assertEqualsSessionIds(expected, (SessionIdToken) actual);
    }

    public void checkSberLog(SberLogToken expected, Optional<AuthorizationToken> oActual) {
        assertTrue(oActual.isPresent());

        AuthorizationToken actual = oActual.get();

        assertTrue(actual instanceof SberLogToken);

        assertEquals(expected.getToken(), ((SberLogToken) actual).getToken());
    }

    private void checkNotExist(Optional<AuthorizationToken> token) {
        assertFalse(token.isPresent());
    }

    private static void clientOfType(Client.Type type) {
        ContextHolder.update(ctx -> {
            Client client = ctx.getClient();
            client.setType(type);
            client.setId(type.name());
        });
    }

    private Optional<AuthorizationToken> extract(HttpServletRequest request) {
        return authorizationTokenExtractor.chooseToken(authorizationTokenExtractor.extractTokens(request));
    }
}
