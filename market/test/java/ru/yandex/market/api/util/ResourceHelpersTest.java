package ru.yandex.market.api.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author dimkarp93
 */
public class ResourceHelpersTest {
    @Test
    public void absolute() {
        assertEquals("ABSOLUTE\n", parse("/util-absolute.txt"));
    }

    @Test
    public void relative() {
        assertEquals("RELATIVE\n", parse("util-relative.txt"));
    }

    private String parse(String path) {
        return ApiStrings.valueOf(ResourceHelpers.getResource(path));
    }

}
