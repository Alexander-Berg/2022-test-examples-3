package ru.yandex.market.api.server.sec.oauth.scheme;

import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.sec.AuthorizationToken;
import ru.yandex.market.api.server.sec.SessionIdToken;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.api.util.AuthorizatoinTokenTestUtil.assertEqualsSessionIds;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class SimpleSessionIdAuthorizationSchemeTest extends UnitTestBase {

    private SimpleSessionIdAuthorizationScheme scheme = new SimpleSessionIdAuthorizationScheme();

    @Test
    public void shouldNotExtractSessionIdWithoutHost() {
        AuthorizationToken token = scheme.extract("SessionId Id=some/session+id,with=a-lot|of.symbols and spaces");

        assertNull(token);
    }

    @Test
    public void shouldNotExtractSessionIdWithoutId() {
        AuthorizationToken token = scheme.extract("SessionId Host=ya.ru");

        assertNull(token);
    }

    @Test
    public void shouldNotExtractSessionIdWhenKeyValueIncorrect() {
        AuthorizationToken token = scheme.extract("SessionId =aa");

        assertNull(token);
    }

    @Test
    public void shouldNotExtractSessionIdWhenFirstKeyValueIsEmpty() {
        AuthorizationToken token = scheme.extract("SessionId ;a=b");

        assertNull(token);
    }

    @Test
    public void shouldExtractSessionIdHostBeforeId() {
        AuthorizationToken token = scheme.extract("SessionId Host=yandex.com;" +
            " Id=some/session+id,with=a-lot|of.symbols and spaces");

        assertTrue(token instanceof SessionIdToken);
        assertEqualsSessionIds((SessionIdToken) token, new SessionIdToken("yandex.com", "some/session+id,with=a-lot|of.symbols and spaces"));
    }

    @Test
    public void shouldExtractSessionIdIdBeforeHost() {
        AuthorizationToken token = scheme.extract("SessionId Id=some/session+id,with=a-lot|of.symbols and spaces;" +
            " Host=yandex.ru");

        assertTrue(token instanceof SessionIdToken);
        assertEqualsSessionIds((SessionIdToken) token, new SessionIdToken("yandex.ru", "some/session+id,with=a-lot|of.symbols and spaces"));
    }

    @Test
    public void shouldExtractSessionIdWithOtherKeyValue() {
        AuthorizationToken token = scheme.extract("SessionId Foo1=Bar1;" +
            " Id=some/session+id,with=a-lot|of.symbols and spaces; Foo2=Bar2; Host=ya.ru; Foo3=Bar3");

        assertTrue(token instanceof SessionIdToken);
        assertEqualsSessionIds((SessionIdToken) token, new SessionIdToken("ya.ru", "some/session+id,with=a-lot|of.symbols and spaces"));
    }

    @Test
    public void shouldExtractSessionIdTrimLeftAndRight() {
        AuthorizationToken token = scheme.extract("SessionId    Id=some/session+id,with=a-lot|of.symbols and spaces;" +
            " Host=yandex.ru    ");

        assertTrue(token instanceof SessionIdToken);
        assertEqualsSessionIds((SessionIdToken) token, new SessionIdToken("yandex.ru", "some/session+id,with=a-lot|of.symbols and spaces"));
    }



}
