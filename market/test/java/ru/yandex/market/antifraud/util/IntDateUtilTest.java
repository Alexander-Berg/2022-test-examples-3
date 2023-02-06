package ru.yandex.market.antifraud.util;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class IntDateUtilTest {
    @Test
    public void testHyphenation() {
        assertThat(
                IntDateUtil.hyphenated(20171031),
                is("2017-10-31"));

        assertThat(
                IntDateUtil.hyphenated(20170101),
                is("2017-01-01"));
    }

    @Test
    public void testHyphenatedToInt() {
        assertThat(IntDateUtil.hyphenated("2017-10-31"), is(20171031));
        assertThat(IntDateUtil.hyphenated("2017-01-08"), is(20170108));
    }

    @Test
    public void testNow() {
        String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        assertThat(IntDateUtil.today(), is(today));
    }

    @Test
    public void testToInt() throws ParseException {
        assertThat(IntDateUtil.toInt(
                new SimpleDateFormat("yyyy-MM-dd").parse("2017-01-20")),
                is(20170120));
    }

    @Test
    public void testFromInt() {
        Date d = IntDateUtil.fromInt(20170120);
        assertThat(new SimpleDateFormat("yyyy-MM-dd").format(d), is("2017-01-20"));
    }
}
