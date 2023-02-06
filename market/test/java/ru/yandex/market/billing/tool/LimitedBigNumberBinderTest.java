package ru.yandex.market.billing.tool;

import java.math.BigDecimal;

import com.google.common.base.Strings;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

/**
 * Unit-тесты для {@link LimitedBigNumberBinder}.
 *
 * @author ivmelnik
 * @since 27.10.17
 */
@RunWith(JUnit4.class)
public class LimitedBigNumberBinderTest {

    private static final int BINDER_LENGTH = 25;

    private static final String TOO_LONG_STRING = Strings.repeat("a", BINDER_LENGTH + 1);
    private static final String TOO_LONG_NUMBER_STRING = Strings.repeat("1", BINDER_LENGTH + 1);

    private static final String ZERO_NUMBER_STRING = "0";
    private static final String NEGATIVE_NUMBER_STRING = "-123";
    private static final String EXPONENT_FIELD_NUMBER_STRING = "123E10";
    private static final String LEADING_ZERO_NUMBER_STRING = "0000123";
    private static final String NOT_NUMBER_STRING = "12345D12345";

    private static final String OK_NUMBER_STRING = "123";
    private static final BigDecimal OK_NUMBER = new BigDecimal(123);

    private static final String OK_MAXIMUM_NUMBER_STRING = Strings.repeat("1", BINDER_LENGTH);
    private static final BigDecimal OK_MAXIMUM_NUMBER = new BigDecimal(OK_MAXIMUM_NUMBER_STRING);

    private LimitedBigNumberBinder binder = new LimitedBigNumberBinder();

    {
        binder.setLength(BINDER_LENGTH);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooLong() throws Exception {
        binder.castFromString(TOO_LONG_STRING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tooLongNumber() throws Exception {
        binder.castFromString(TOO_LONG_NUMBER_STRING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroNumber() throws Exception {
        binder.castFromString(ZERO_NUMBER_STRING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeNumber() throws Exception {
        binder.castFromString(NEGATIVE_NUMBER_STRING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void exponentNumber() throws Exception {
        binder.castFromString(EXPONENT_FIELD_NUMBER_STRING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void leadingZeroNumber() throws Exception {
        binder.castFromString(LEADING_ZERO_NUMBER_STRING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void notNumber() throws Exception {
        binder.castFromString(NOT_NUMBER_STRING);
    }

    @Test
    public void ok() throws Exception {
        BigDecimal bigDecimal = binder.castFromString(OK_NUMBER_STRING);
        assertEquals(OK_NUMBER, bigDecimal);
    }

    @Test
    public void okMaximum() throws Exception {
        BigDecimal bigDecimal = binder.castFromString(OK_MAXIMUM_NUMBER_STRING);
        assertEquals(OK_MAXIMUM_NUMBER, bigDecimal);

    }

}
