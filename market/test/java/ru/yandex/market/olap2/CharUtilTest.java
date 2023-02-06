package ru.yandex.market.olap2;

import java.util.Arrays;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import ru.yandex.market.olap2.util.CharUtil;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class CharUtilTest {
    @Test
    public void testReplace() {
        assertThat(CharUtil.replace("aabcd", new char[] {'a', 'd'}, '_'), is("__bc_"));
        assertThat(CharUtil.replace("aabcd", new char[] {}, '_'), is("aabcd"));
        assertThat(CharUtil.replace("aabcd", new char[] {'z'}, '_'), is("aabcd"));
    }

    @Test
    public void testQuoteJoin() {
        assertThat(CharUtil.quoteJoin('\'', ',', "a", "b", "c"),
            is("'a','b','c'"));
        assertThat(CharUtil.quoteJoin('\'', ',', "a"),
            is("'a'"));
        assertThat(CharUtil.quoteJoin('\'', ',', ""),
            is("''"));
    }

    @Test
    public void testQuote() {
        assertThat(CharUtil.quote("str"), is("\"str\""));
        assertThat(CharUtil.quote(""), is("\"\""));
        assertThat(CharUtil.quote(ImmutableSet.of("s1", "s2")),
            is(Arrays.asList("\"s1\"", "\"s2\"")));

    }

}
