package ru.yandex.chemodan.app.djfs.core.lenta;

import org.junit.Test;

import ru.yandex.chemodan.app.djfs.core.test.DjfsSingleUserTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.web.servlet.HttpServletRequestX;
import ru.yandex.misc.web.servlet.mock.MockHttpServletRequest;

/**
 * @author yappo
 */
public class AuthorizationClientTokenTest extends DjfsSingleUserTestBase {

    private String validToken = "valid-token";
    private DjfsUid validUid = DjfsUid.cons(123);

    @Test
    public void successfulAuthHeaderCheckWithoutUid() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "ClientToken token=" + "valid-token");
        AuthorizationClientToken clientToken = AuthorizationClientToken.cons(new HttpServletRequestX(req));
        Assert.equals(clientToken.getToken(), validToken);
    }

    @Test
    public void successfulAuthHeaderCheckWithUid1() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "ClientToken token=" + "valid-token" + ";uid=123");
        AuthorizationClientToken clientToken = AuthorizationClientToken.cons(new HttpServletRequestX(req));
        Assert.equals(clientToken.getToken(), validToken);
        Assert.equals(clientToken.getUid().get().asLong(), validUid.asLong());
    }

    @Test
    public void successfulAuthHeaderCheckWithUid2() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "ClientToken uid=123" + ";token=" + "valid-token");
        AuthorizationClientToken clientToken = AuthorizationClientToken.cons(new HttpServletRequestX(req));
        Assert.equals(clientToken.getToken(), validToken);
        Assert.equals(clientToken.getUid().get().asLong(), validUid.asLong());
    }

    @Test
    public void successfulAuthHeaderCheckWithoutUidSemicolonAtTheEnd() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "ClientToken token=" + "valid-token" + ";");
        AuthorizationClientToken clientToken = AuthorizationClientToken.cons(new HttpServletRequestX(req));
        Assert.equals(clientToken.getToken(), validToken);
    }

    @Test
    public void successfulAuthHeaderCheckWithUidSemicolonAtTheEnd1() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "ClientToken token=" + "valid-token" + ";uid=123" + ";");
        AuthorizationClientToken clientToken = AuthorizationClientToken.cons(new HttpServletRequestX(req));
        Assert.equals(clientToken.getToken(), validToken);
        Assert.equals(clientToken.getUid().get().asLong(), validUid.asLong());
    }

    @Test
    public void successfulAuthHeaderCheckWithUidSemicolonAtTheEnd2() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "ClientToken uid=123" + ";token=" + "valid-token" + ";");
        AuthorizationClientToken clientToken = AuthorizationClientToken.cons(new HttpServletRequestX(req));
        Assert.equals(clientToken.getToken(), validToken);
        Assert.equals(clientToken.getUid().get().asLong(), validUid.asLong());
    }

    @Test(expected = MissingAuthorizationHeader.class)
    public void failedAuthNoHeader() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        AuthorizationClientToken.cons(new HttpServletRequestX(req));
    }

    @Test
    public void failedAuthWrongToken() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "ClientToken token=" + "invalid-token");
        AuthorizationClientToken clientToken = AuthorizationClientToken.cons(new HttpServletRequestX(req));
        Assert.notEquals(clientToken.getToken(), validToken);
    }

    @Test(expected = InvalidAuthorizationClientToken.class)
    public void failedAuthInvalidUid() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "ClientToken token=" + "valid-token" + ";uid=some_uid");
        AuthorizationClientToken.cons(new HttpServletRequestX(req));
    }

    @Test(expected = InvalidAuthorizationClientTokenHeader.class)
    public void invalidClientTokenString() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "ClientToken123 token=" + "valid-token");
        AuthorizationClientToken.cons(new HttpServletRequestX(req));
    }
}
