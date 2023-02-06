package ru.yandex.direct.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.web.core.security.csrf.CsrfValidator;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class CsrfValidatorTest {
    public static final long TOKEN_LIFETIME = 1000L;

    private final String secret;
    private final long uid;
    private final long now;
    private final long csrfIssuingTime;
    private final boolean isValidForNow;

    public CsrfValidatorTest(String secret, long uid, long csrfIssuingTime, long now, boolean isValidForNow) {
        this.secret = secret;
        this.uid = uid;
        this.csrfIssuingTime = csrfIssuingTime;
        this.now = now;
        this.isValidForNow = isValidForNow;
    }

    @Test
    public void csrfLifetimeTest() throws Exception {
        CsrfValidator csrfIssuer = new CsrfValidator(secret, TOKEN_LIFETIME, () -> csrfIssuingTime);
        String csrfValue = csrfIssuer.createCsrfToken(uid);
        CsrfValidator csrfValidator = new CsrfValidator(secret, TOKEN_LIFETIME, () -> now);
        assertThat(csrfValidator.checkCsrfToken(csrfValue, uid), is(isValidForNow));
    }


    @Parameterized.Parameters()
    public static Collection<Object[]> parameters() {
        long goodLifeTime = TOKEN_LIFETIME - 1;
        long badLifeTime = TOKEN_LIFETIME + 1;
        List<Object[]> result = new ArrayList<>(PerlCsrfTokens.TOKENS.size());
        for (PerlCsrfTokens.TokenMetadata token : PerlCsrfTokens.TOKENS) {
            result.add(new Object[]{PerlCsrfTokens.SECRET_KEY, token.getUid(), token.getIssuingTime(),
                    token.getIssuingTime() + goodLifeTime, true});
            result.add(new Object[]{PerlCsrfTokens.SECRET_KEY, token.getUid(), token.getIssuingTime(),
                    token.getIssuingTime() + badLifeTime, false});
        }
        return result;
    }
}
