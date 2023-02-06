package ru.yandex.market.api.internal.blackbox.data;


import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.server.sec.AuthorizationToken;
import ru.yandex.market.api.server.sec.OAuthToken;
import ru.yandex.market.api.server.sec.SessionIdToken;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class OauthUserParserTest {

    private AuthorizationToken testToken = new OAuthToken("test-token");
    private AuthorizationToken sessionIdTestToken = new SessionIdToken("yandex.ru", "test-token");

    @Test
    public void shouldParse() throws Exception {
        OauthUser oauthUser = parseResource(this.testToken, "oauth-user-data.xml");

        assertEquals(42L, oauthUser.getUid());
        assertEquals(testToken, oauthUser.getToken());
        assertEquals("test-login", oauthUser.getLogin());
        assertEquals("test-name", oauthUser.getName());
        assertEquals("test-client-id", oauthUser.getClientId());
        assertEquals("test-id", oauthUser.getLoginId());
        assertEquals(new String[]{"grants:test1", "grants:test2"}, oauthUser.getScopes());
        assertEquals(OauthUser.Status.VALID, oauthUser.getStatus());
        assertFalse(oauthUser.getHasYandexPlus());
    }

    @Test
    public void shouldParseInvalid() throws Exception {
        OauthUser oauthUser = parseResource(this.testToken, "oauth-user-invalid-data.xml");

        assertEquals(0, oauthUser.getUid());
        assertEquals(testToken, oauthUser.getToken());
        assertEquals(OauthUser.Status.INVALID, oauthUser.getStatus());
    }

    @Test
    public void shouldParseSessionIdUser() throws Exception {
        OauthUser oauthUser = parseResource(this.sessionIdTestToken, "oauth-user-valid-session-id.xml");

        assertEquals(42L, oauthUser.getUid());
        assertEquals(sessionIdTestToken, oauthUser.getToken());
        assertEquals("test-login", oauthUser.getLogin());
        assertEquals("test-name", oauthUser.getName());
        assertNull(oauthUser.getClientId());
        assertEquals(new String[0], oauthUser.getScopes());
        assertEquals(OauthUser.Status.VALID, oauthUser.getStatus());
    }

    @Test
    public void shouldParseUserException() {
        OauthUser oauthUser = parseResource(this.sessionIdTestToken, "oauth-user-exception.xml");

        assertNull(oauthUser.getStatus());
        assertEquals("2", oauthUser.getErrorId());
        assertEquals("INVALID_PARAMS", oauthUser.getErrorCode());
        assertEquals("BlackBox error: Sign don't match", oauthUser.getErrorMessage());
    }

    @Test
    public void shouldParsePhone() {
        // вызов системы
        OauthUser oauthUser = parseResource(this.sessionIdTestToken, "oauth-user-with-phone.xml");

        // проверка утверждений
        Assert.assertNotNull(oauthUser.getPhones());
        Assert.assertEquals("В ответе сервиса 1 номер телефона", 1, oauthUser.getPhones().size());
        Assert.assertEquals("Должно совпадать со значением из ответа", "+79321034950", oauthUser.getPhones().get(0));
    }

    @Test
    public void shouldParseLastName() {
        // вызов системы
        OauthUser oauthUser = parseResource(this.sessionIdTestToken, "oauth-user-with-phone.xml");

        // проверка утверждений
        Assert.assertEquals("Должно совпадать со значением из ответа", "Shestakov", oauthUser.getLastName());
    }

    @Test
    public void shouldParseFirstName() {
        // вызов системы
        OauthUser oauthUser = parseResource(this.sessionIdTestToken, "oauth-user-with-phone.xml");

        // проверка утверждений
        Assert.assertEquals("Должно совпадать со значением из ответа", "Nikolay", oauthUser.getFirstName());
    }

    @Test
    public void shouldParseEmail() {
        // вызов системы
        OauthUser oauthUser = parseResource(this.sessionIdTestToken, "oauth-user-with-phone.xml");

        // проверка утверждений
        Assert.assertEquals("Должно совпадать со значением из ответа", "ns_0@spp.com", oauthUser.getEmail());
    }

    @Test
    public void shouldParseYandexPlusUnsubscribed() {
        // вызов системы
        OauthUser oauthUser = parseResource(this.sessionIdTestToken, "oauth-user-data-with-yandex-plus-unsubscribed.xml");

        // проверка отсутствия подписки на яндекс плюс
        Assert.assertFalse(oauthUser.getHasYandexPlus());
    }

    @Test
    public void shouldParseYandexPlusSubscribed() {
        // вызов системы
        OauthUser oauthUser = parseResource(this.sessionIdTestToken, "oauth-user-data-with-yandex-plus-subscribed.xml");

        // проверка наличия подписки на яндекс плюс
        Assert.assertTrue(oauthUser.getHasYandexPlus());
    }

    @Test
    public void shouldParseNeedReset() {
        OauthUser oauthUser = parseResource(
            this.sessionIdTestToken,
            "oauth-need-reset.xml"
        );

        Assert.assertEquals(OauthUser.Status.NEED_RESET, oauthUser.getStatus());
    }

    private OauthUser parseResource(AuthorizationToken token, String resourceName) {
        byte[] resource = ResourceHelpers.getResource(resourceName);
        return new OauthUserParser(token).parse(resource);
    }
}
