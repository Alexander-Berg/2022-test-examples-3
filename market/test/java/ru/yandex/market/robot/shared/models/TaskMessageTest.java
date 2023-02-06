package ru.yandex.market.robot.shared.models;

import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Alexandr Karnyukhin, <a href="mailto:shurk@yandex-team.ru"/>
 */
public class TaskMessageTest extends Assert {
    @Test
    public void testTrimSymbols() throws Exception {
        checkTrimSymbols("", "", "");
        checkTrimSymbols("some string", "some string", "");

        checkTrimSymbols("", "", "ABC");
        checkTrimSymbols("", "AABBCC", "ABC");
        checkTrimSymbols("abc", "AABabcBCC", "ABC");

        checkTrimSymbols("ome stri", "some string", "gns");
    }

    private void checkTrimSymbols(String expected, String srcString, String chars) {
        Assert.assertEquals(expected, TaskMessage.trimSymbols(srcString, new CharOpenHashSet(chars.toCharArray())));
    }
}
