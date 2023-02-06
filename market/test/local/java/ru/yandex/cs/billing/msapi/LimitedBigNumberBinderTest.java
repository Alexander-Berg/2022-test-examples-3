package ru.yandex.cs.billing.msapi;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class LimitedBigNumberBinderTest {
    @Test
    public void testBigExponentNumberTest() {
        try {
            LimitedBigNumberBinder binder = new LimitedBigNumberBinder();
            binder.castFromString("688497706e1919297810");
        } catch (IllegalArgumentException ex) {
            assertThat("Wrong error message", ex.getMessage(),
                    Matchers.startsWith("Number must be plain (no exponent field). Argument = "));
        } catch (OutOfMemoryError er) {
            fail();
        }
    }
}
