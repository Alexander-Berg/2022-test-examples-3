package ru.yandex.market.api.server.sec.oauth;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;

/**
 * @author dimkarp93
 */
public class OAuthUserStatusValidationResultTest extends UnitTestBase {
    @Test
    public void shouldBeOkIfStatusValid() {
        doTest(true, OauthUser.Status.VALID);
    }

    @Test
    public void shouldBeOkIfStatusNeedRequest() {
        doTest(true, OauthUser.Status.NEED_RESET);
    }


    @Test
    public void shouldBeNotOkIfStatusDisabled() {
        doTest(false, OauthUser.Status.DISABLED);
    }

    @Test
    public void shouldBeNotOkIfStatusInvalid() {
        doTest(false, OauthUser.Status.INVALID);
    }


    @Test
    public void shouldBeNotOkIfStatusUnknown() {
        doTest(false, OauthUser.Status.UNKNOWN);
    }


    private static void doTest(boolean isValid, OauthUser.Status status) {
        OauthUser user = new OauthUser(1L);
        user.setStatus(status);

        OauthUserStatusValidationResult result = new OauthUserStatusValidationResult(user);

        Assert.assertEquals(isValid, result.isValid());
    }
}
