package ru.yandex.market.mbo.gwt.utils;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 28.09.2017
 */
@SuppressWarnings("checkstyle:magicnumber")
public class StringUtilTest {

    @Test
    public void formatDuration() {
        assertThat(StringUtil.formatDuration(0), is("00:00:00"));
        assertThat(StringUtil.formatDuration(42), is("00:00:42"));
        assertThat(StringUtil.formatDuration(60), is("00:01:00"));
        assertThat(StringUtil.formatDuration(158 * 60 * 60 + 31 * 60 + 59), is("158:31:59"));
        assertThat(StringUtil.formatDuration(-158 * 60 * 60 - 31 * 60 - 59), is("-158:31:59"));
    }

    @Test
    public void parseLongParses() {
        assertThat(StringUtil.tryParseLong("42"), is(42L));
    }

    @Test
    public void parseLongNotThrowException() {
        assertThat(StringUtil.tryParseLong("whoops"), is(nullValue()));
    }

    @Test
    public void parseLongNull() {
        assertThat(StringUtil.tryParseLong(null), is(nullValue()));
    }
}
