package ru.yandex.autotests.direct.utils.rules;

import org.junit.Rule;
import org.junit.Test;

public class SimpleExpectedExceptionTest {

    @Rule
    public ExpectedExceptionRule thrown= ExpectedExceptionRule.none();

    @Test
    public void throwsNothing() {
        // no exception expected, none thrown: passes.
    }

    @Test
    public void throwsExceptionWithSpecificType() {
        thrown.expect(NullPointerException.class);
        throw new NullPointerException();
    }

    @Test
    public void throwsException() {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("happened");
        throw new NullPointerException("What happened?");
    }

}
