package ru.yandex.market.tanker.client.request;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Vadim Lyalin
 */
public class TranslationParamTest {
    @Test
    public void testGetName() {
        assertEquals("project-id", TranslationParam.PROJECT_ID.getName());
    }
}
