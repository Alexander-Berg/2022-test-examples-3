package ru.yandex.market.gutgin.tms.parser;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class JoinedGroupsParserTest {

    private static final Set<Character> groupSeparator = Set.of(',');
    private static final Set<Character> internalSeparator = Set.of(' ');
    private static final Set<String> words = Set.of("aaa", "bbb");
    private static final JoinedGroupsParser parser = JoinedGroupsParser.newInstance(groupSeparator, internalSeparator, words);

    @Test
    public void whenValidStringThenOk() {
        var result = parser.parse("aaa 1,bbb 2");
        Assert.assertEquals(2, result.size());
        Assert.assertEquals(1, result.get(0).getWordsBeforeNum().size());
        Assert.assertEquals("aaa", result.get(0).getWordsBeforeNum().get(0));
        Assert.assertEquals(1, (short) result.get(0).getParsedNum());
    }

    @Test
    public void whenValidNumInTheMiddleThenOk() {
        var result = parser.parse("bbb 1 aaa");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, result.get(0).getWordsBeforeNum().size());
        Assert.assertEquals("bbb", result.get(0).getWordsBeforeNum().get(0));
        Assert.assertEquals(1, result.get(0).getWordsAfterNum().size());
        Assert.assertEquals("aaa", result.get(0).getWordsAfterNum().get(0));
        Assert.assertEquals(1, (short) result.get(0).getParsedNum());
    }

    @Test
    public void whenSkipWordsThenOk() {
        var result = parser.parse("aaa ccc 1");
        Assert.assertEquals(1, result.size());
        Assert.assertEquals(1, result.get(0).getWordsBeforeNum().size());
        Assert.assertEquals("aaa", result.get(0).getWordsBeforeNum().get(0));
        Assert.assertEquals(1, (short) result.get(0).getParsedNum());
    }
}
