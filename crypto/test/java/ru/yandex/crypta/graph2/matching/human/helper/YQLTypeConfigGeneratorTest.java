package ru.yandex.crypta.graph2.matching.human.helper;

import org.junit.Test;

import static org.junit.Assert.assertTrue;


public class YQLTypeConfigGeneratorTest {
    @Test
    public void getYQlProtoFieldForSplitInfo() {
        String value = YQLTypeConfigGenerator.getYQlProtoFieldForSplitInfo();
        assertTrue(value.length() > 0);
    }
}
