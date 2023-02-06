package ru.yandex.market.mbo.skubd2.util;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Shamil Ablyazov, <a href="mailto:a-shar@yandex-team.ru"/>.
 */
public class ValueRangeTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();


    @Test
    public void extractLegalRange() {
        Assert.assertArrayEquals(new String[] {"39", "40", "41", "42", "43"}, ValueRange.extractList("39-43"));
        Assert.assertArrayEquals(new String[] {"0", "5", "10"}, ValueRange.extractList("0-5-10"));
        Assert.assertArrayEquals(new String[] {"XS", "S", "M", "L"}, ValueRange.extractList("XS/S/M/L"));
    }

    @Test
    public void extractWithExceptionHighLessThenLow() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("High value(5) less than low(10) value");
        ValueRange.tryExtractLongRange(new String[]{"10", "5"});
    }

    @Test
    public void extractWithExceptionVeryBigRange() {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Very big range: 101");
        ValueRange.tryExtractLongRange(new String[] {"0", "100"});
    }
}
