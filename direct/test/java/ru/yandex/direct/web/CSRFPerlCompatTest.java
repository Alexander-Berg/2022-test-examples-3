package ru.yandex.direct.web;


import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.web.core.security.csrf.CsrfValidator;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class CSRFPerlCompatTest {
    public static final long TOKEN_LIFETIME = 1000L;
    private final PerlCsrfTokens.TokenMetadata tokenMetadata;

    public CSRFPerlCompatTest(PerlCsrfTokens.TokenMetadata tokenMetadata) {
        this.tokenMetadata = tokenMetadata;
    }

    @Test
    public void perlCompatibilityTest() throws Exception {
        CsrfValidator validator =
                new CsrfValidator(PerlCsrfTokens.SECRET_KEY, TOKEN_LIFETIME, tokenMetadata::getIssuingTime);
        String actualToken = validator.createCsrfToken(tokenMetadata.getUid());
        assertThat(actualToken, is(tokenMetadata.getToken()));
    }

    @Parameterized.Parameters
    public static Collection<PerlCsrfTokens.TokenMetadata> parameters() {
        return PerlCsrfTokens.TOKENS;
    }
}
