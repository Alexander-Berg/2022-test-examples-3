package ru.yandex.market.ir.decision;

import junit.framework.TestCase;

public class AbstractRuleTest extends TestCase {

    public void testExtractName() {
        assertEquals("синий", AbstractRule.extractName("синий:value"));
        assertEquals("синий", AbstractRule.extractName("синий"));
    }
}
