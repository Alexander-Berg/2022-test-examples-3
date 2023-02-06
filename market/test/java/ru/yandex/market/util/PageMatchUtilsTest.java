package ru.yandex.market.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class PageMatchUtilsTest {
    private static final String TEST_STRING = "test/1/{2}/3/4";
    private static final String PAGEMATCH_ROW_TEMPLATE = "%s\t%s\t%s";

    @Test
    void patternToClickphiteIdTest() {
        String actual = PageMatchUtils.patternToClickphiteId(TEST_STRING);
        String expected = "test_1_*_3_4";
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void patternToClickphitePatternTest() {
        String actual = PageMatchUtils.patternToClickphitePattern(TEST_STRING);
        String expected = "/test/1/<2>/3/4";
        Assertions.assertEquals(expected, actual);
    }

    @Test
    void toPageMatcherLineTest() {
        String actual = PageMatchUtils.toPageMatcherLine("/" + TEST_STRING + "/", "test");
        String expected = String.format(PAGEMATCH_ROW_TEMPLATE, "test_1_*_3_4", "/test/1/<2>/3/4", "test");
        Assertions.assertEquals(expected, actual);
    }
}
