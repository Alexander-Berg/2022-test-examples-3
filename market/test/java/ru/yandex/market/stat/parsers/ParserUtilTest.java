package ru.yandex.market.stat.parsers;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.stat.parsers.ParserUtil.SIGN;
import static ru.yandex.market.stat.parsers.ParserUtil.getListValues;

/**
 * @author aostrikov
 */
public class ParserUtilTest {
    @Test
    public void verifySignRegex() {
        assertTrue(Pattern.compile(SIGN).matcher("1").matches());
        assertTrue(Pattern.compile(SIGN).matcher("-1").matches());
        assertFalse(Pattern.compile(SIGN).matcher("0").matches());
        assertFalse(Pattern.compile(SIGN).matcher("-21").matches());
    }

    @Test
    public void shouldGetValuesFromList() {
        assertThat(getListValues("[1,2,3,4]"), is("1,2,3,4"));
        assertThat(getListValues("[]"), is(""));
        assertThat(getListValues("[  ]"), is(""));
        assertThat(getListValues("[1,2,3,  4]"), is("1,2,3,  4"));
        assertThat(getListValues("[1,2,3,4]   "), is("1,2,3,4"));
        assertThat(getListValues(null), is(nullValue()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldVerifyListFormat() {
        getListValues("[1,2,3,4 ");
    }
}
