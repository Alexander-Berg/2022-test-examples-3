package ru.yandex.market.api.server.sec.oauth.scheme;

import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.sec.AuthorizationToken;
import ru.yandex.market.api.server.sec.OAuthToken;
import ru.yandex.market.api.util.AuthorizatoinTokenTestUtil;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.api.util.AuthorizatoinTokenTestUtil.assertEqualsOAuthTokens;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class SimpleOauthAuthorizationSchemeTest extends UnitTestBase {

    private SimpleOauthAuthorizationScheme scheme = new SimpleOauthAuthorizationScheme();

    @Test
    public void shouldExtractToken() {
        AuthorizationToken token = scheme.extract("OAuth some-test-token");

        assertTrue(token instanceof OAuthToken);
        assertEqualsOAuthTokens((OAuthToken) token, new OAuthToken("some-test-token"));
    }

    @Test
    public void shouldReturnNullForNoSchemePrefix() {
        AuthorizationToken token = scheme.extract("token-without-prefix");

        assertThat(token, nullValue());
    }
}
