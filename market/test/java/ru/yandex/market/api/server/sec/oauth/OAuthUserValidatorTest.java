package ru.yandex.market.api.server.sec.oauth;

import org.junit.Test;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.server.sec.ValidationResult;
import ru.yandex.market.api.server.sec.oauth.annotation.AuthSecured;
import ru.yandex.market.api.util.functional.Functionals;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class OAuthUserValidatorTest extends UnitTestBase {

    private OAuthUserValidator validator = new OAuthUserValidator();

    @Test
    public void shouldNotValidateInvalidStatus() {
        OauthUser user = createUser(OauthUser.Status.INVALID);

        ValidationResult<OauthUser> result = validator.validate(user);

        assertFalse(result.isValid());
        assertThat(result, instanceOf(OauthUserStatusValidationResult.class));
    }

    @Test
    public void shouldNotValidateUserWithoutScopes() {
        OauthUser user = createUser(OauthUser.Status.VALID);

        ValidationResult<OauthUser> result = validator.validate(user);

        assertFalse(result.isValid());
        assertThat(result, instanceOf(OauthUserScopesValidationResult.class));
    }

    @Test
    public void shouldNotValidateNullValue() {
        ValidationResult<OauthUser> result = validator.validate(null);

        assertFalse(result.isValid());
    }

    @Test
    public void shouldValidateCorrectUser() {
        OauthUser user = createUser(OauthUser.Status.VALID, AuthSecured.DEFAULT_SCOPES);

        ValidationResult<OauthUser> result = validator.validate(user);

        assertTrue(result.isValid());
    }

    private OauthUser createUser(OauthUser.Status status) {
        return createUser(status, null);
    }

    private OauthUser createUser(OauthUser.Status status, String grant) {
        OauthUser oauthUser = new OauthUser(1);
        oauthUser.setStatus(status);
        oauthUser.setScopes(Functionals.getOrDefault(grant, s -> new String[]{s}, new String[0]));
        return oauthUser;
    }
}