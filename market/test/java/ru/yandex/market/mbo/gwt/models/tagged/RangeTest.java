package ru.yandex.market.mbo.gwt.models.tagged;

import org.junit.Test;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alexander Kramarev (https://staff.yandex-team.ru/pochemuto/)
 * @date 01.12.2015
 */
@SuppressWarnings("checkstyle:magicNumber")
public class RangeTest {

    @Test
    public void testCompareTo() throws Exception {
        Range r1 = new Range(4, 5);
        Range r2 = new Range(4, 5);

        assertThat("equals", r1.compareTo(r2), is(0));

        r1 = new Range(3, 5);
        r2 = new Range(4, 5);
        assertThat("leftmost first", r1.compareTo(r2), lessThan(0));
        assertThat("rightmost last", r2.compareTo(r1), greaterThan(0));

        r1 = new Range(4, 5);
        r2 = new Range(4, 6);
        assertThat("shorter first", r1.compareTo(r2), lessThan(0));
        assertThat("longer last", r2.compareTo(r1), greaterThan(0));
    }

    @Test(expected = NullPointerException.class)
    public void testCompareWithNull() throws Exception {
        Range r1 = new Range(10, 10);
        r1.compareTo(null);
    }
}
