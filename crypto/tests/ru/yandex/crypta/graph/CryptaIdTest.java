package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class CryptaIdTest {
    @Test
    public void testIsValid() {
        CryptaId test1 = new CryptaId("");
        CryptaId test2 = new CryptaId("1234");
        CryptaId test3 = new CryptaId("001234");
        CryptaId test4 = new CryptaId("73645sdas");

        assertEquals(test1.getValue(), "");
        assertEquals(test1.isValid(), false);
        assertEquals(test2.getValue(), "1234");
        assertEquals(test2.isValid(), true);
        assertEquals(test3.getValue(), "001234");
        assertEquals(test3.isValid(), true);
        assertEquals(test4.getValue(), "73645sdas");
        assertEquals(test4.isValid(), false);
    }

    @Test
    public void testgetType() {
        CryptaId test = new CryptaId("");
        assertEquals(test.getType(), EIdType.CRYPTA_ID);
    }
}
