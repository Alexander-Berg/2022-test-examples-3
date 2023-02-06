package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class VkIdTest {
    @Test
    public void testIsValid() {
        VkId test1 = new VkId("");
        VkId test2 = new VkId("1234");
        VkId test3 = new VkId("001234");
        VkId test4 = new VkId("73645sdas");

        assertEquals(test1.getValue(), "");
        assertEquals(test1.isValid(), false);
        assertEquals(test2.getValue(), "1234");
        assertEquals(test2.isValid(), true);
        assertEquals(test3.getValue(), "001234");
        assertEquals(test3.isValid(), true);
        assertEquals(test4.getValue(), "73645sdas");
        assertEquals(test4.isValid(), true);
    }

    @Test
    public void testgetType() {
        VkId test = new VkId("");
        assertEquals(test.getType(), EIdType.VK_ID);
    }
}
