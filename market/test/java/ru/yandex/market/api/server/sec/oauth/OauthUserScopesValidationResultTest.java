package ru.yandex.market.api.server.sec.oauth;

import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.server.sec.SessionIdToken;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class OauthUserScopesValidationResultTest extends UnitTestBase {

    @Test
    public void shouldAlwaysValidateSessionIdUser() {
        OauthUser oauthUser = Mockito.mock(OauthUser.class);
        when(oauthUser.hasScope(anyString())).thenReturn(false);
        when(oauthUser.getToken()).thenReturn(new SessionIdToken("yandex.ru", "test"));


        OauthUserScopesValidationResult result = OauthUserScopesValidationResult.getResult(oauthUser, "some-grants");
        assertTrue(result.isValid());
    }
}
