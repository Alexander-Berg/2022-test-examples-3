package ru.yandex.market.crm.core.util;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author vtarasoff
 * @since 28.12.2021
 */
public class ExperimentsUtilTest {
    @Test
    void returnEmptyMapIfNull() {
        Assertions.assertEquals(Map.of(), ExperimentsUtil.parse(null));
    }

    @Test
    void returnEmptyMapIfEmpty() {
        Assertions.assertEquals(Map.of(), ExperimentsUtil.parse(""));
    }

    @Test
    void returnEmptyMapIfBlank() {
        Assertions.assertEquals(Map.of(), ExperimentsUtil.parse(" "));
    }

    @Test
    void returnCorrectMap() {
        var expected = new HashMap<String, String>();
        expected.put("abc", "123");
        expected.put("def", "");
        expected.put("jhk", "");
        expected.put("lmn", "456=789");

        Assertions.assertEquals(
                expected,
                ExperimentsUtil.parse(" abc =123 ;; ;def; jhk = ; lmn=456=789; ")
        );
    }
}
