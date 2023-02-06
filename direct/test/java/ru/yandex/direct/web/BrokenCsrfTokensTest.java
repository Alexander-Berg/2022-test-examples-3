package ru.yandex.direct.web;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.web.core.security.csrf.CsrfValidationFailureException;
import ru.yandex.direct.web.core.security.csrf.CsrfValidator;

@RunWith(Parameterized.class)
public class BrokenCsrfTokensTest {
    private final String token;

    public BrokenCsrfTokensTest(String token) {
        this.token = token;
    }

    @Test(expected = CsrfValidationFailureException.class)
    public void illegalTokenMustThrowException() throws Exception {
        CsrfValidator validator = new CsrfValidator("secret");
        validator.checkCsrfToken(token, 1L);
    }

    @Parameterized.Parameters()
    public static Collection<Object> parameters() {
        return Arrays.asList("", "1", "Y_E5OjgOfLg7zLS_t");
    }
}
