package ru.yandex.market.antifraud.yql.step;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class LfTimestampParserTest {
    @Test
    public void testParse() {
        assertThat(LfTimestampParser.lfTimestampToDay("2017-08-25T03:00:00"), is(20170825));
        assertThat(LfTimestampParser.lfTimestampToDay("2017-08-25T23:00:00"), is(20170825));
        assertThat(LfTimestampParser.lfTimestampToDay("2017-08-25T23:45:00"), is(20170825));
        assertThat(LfTimestampParser.lfTimestampToDay("2010-12-01T23:45:00"), is(20101201));
    }

    @Test
    public void testPrevDate() {
        assertThat(LfTimestampParser.prevDateArchive("2017-08-25"), is("2017-08-24"));
        assertThat(LfTimestampParser.prevDateArchive("2017-08-01"), is("2017-07-31"));
    }

    @Test
    public void testDatePart() {
        assertThat(LfTimestampParser.recentDatePart("2017-08-25T03:00:00"), is("2017-08-25"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDatePartFail() {
        LfTimestampParser.recentDatePart("2017-10-11");
    }
}