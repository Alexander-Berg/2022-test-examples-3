package ru.yandex.crypta.graph;

import org.junit.Test;

import ru.yandex.crypta.lib.proto.identifiers.EIdType;

import static org.junit.Assert.assertEquals;

public class Sha256Test {
    @Test
    public void testIsValid() {
        Sha256 test1 = new Sha256("0123456789abcdefABCDEF98765432100123456789abcdefABCDEF9876543210");
        Sha256 test2 = new Sha256("0123456789abcdefABCDEF987654321000123456789abcdefABCDEF98765432100");
        Sha256 test3 = new Sha256("0123456789qbcdefABCDEF98765432100123456789qbcdefABCDEF9876543210");

        assertEquals(test1.getValue(), "0123456789abcdefABCDEF98765432100123456789abcdefABCDEF9876543210");
        assertEquals(test1.isValid(), true);
        assertEquals(test2.getValue(), "0123456789abcdefABCDEF987654321000123456789abcdefABCDEF98765432100");
        assertEquals(test2.isValid(), false);
        assertEquals(test3.getValue(), "0123456789qbcdefABCDEF98765432100123456789qbcdefABCDEF9876543210");
        assertEquals(test3.isValid(), false);
    }

    @Test
    public void testgetType() {
        Sha256 test = new Sha256("");
        assertEquals(test.getType(), EIdType.SHA256);
    }
}
