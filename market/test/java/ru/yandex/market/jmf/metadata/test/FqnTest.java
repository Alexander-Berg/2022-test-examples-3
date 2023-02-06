package ru.yandex.market.jmf.metadata.test;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.metadata.Fqn;

public class FqnTest {

    @Test
    public void equalsTest() {
        Fqn fqn = new Fqn("classCode", "typeCode");
        Fqn fqn2 = new Fqn("classCode", "otherCode");
        Fqn fqn3 = new Fqn("otherClassCode", "otherCode");
        Assertions.assertFalse(fqn.equals(null));
        Assertions.assertTrue(fqn.equals(fqn));
        Assertions.assertFalse(fqn.equals(fqn2));
        Assertions.assertFalse(fqn.equals(fqn3));
        Assertions.assertFalse(fqn.equals(new Object()));
    }

    @Test
    public void fqnOfClass() {
        Fqn fqn = new Fqn("classCode");
        Fqn fqn2 = new Fqn("classCode", "typeCode");
        Assertions.assertEquals(fqn, fqn.fqnOfClass());
        Assertions.assertEquals(fqn, fqn2.fqnOfClass());
    }

    @Test
    public void isClass() {
        Fqn fqn = new Fqn("classCode");
        Fqn fqn2 = new Fqn("classCode", "typeCode");
        Assertions.assertTrue(fqn.isClass());
        Assertions.assertFalse(fqn2.isClass());
    }

    @Test
    public void isClassOf() {
        Fqn fqn = new Fqn("classCode");
        Fqn fqn2 = new Fqn("classCode", "typeCode");
        Fqn fqn3 = new Fqn("otherClassCode", "typeCode");
        Assertions.assertTrue(fqn.isClassOf(fqn));
        Assertions.assertTrue(fqn.isClassOf(fqn2));
        Assertions.assertFalse(fqn2.isClassOf(fqn));
        Assertions.assertFalse(fqn.isClassOf(fqn3));
    }

    @Test
    public void isType() {
        Fqn fqn = new Fqn("classCode");
        Fqn fqn2 = new Fqn("classCode", "typeCode");
        Assertions.assertFalse(fqn.isType());
        Assertions.assertTrue(fqn2.isType());
    }

    @Test
    public void isTypeOf() {
        Fqn fqn = new Fqn("classCode");
        Fqn fqn2 = new Fqn("classCode", "typeCode");
        Fqn fqn3 = new Fqn("otherClassCode", "typeCode");
        Assertions.assertFalse(fqn.isTypeOf(fqn));
        Assertions.assertFalse(fqn.isTypeOf(fqn2));
        Assertions.assertTrue(fqn2.isTypeOf(fqn));
        Assertions.assertFalse(fqn.isTypeOf(fqn3));
        Assertions.assertFalse(fqn2.isTypeOf(fqn3));
    }

    @Test
    public void parseClass() {
        final Fqn fqn = Fqn.parse("classCode");
        Assertions.assertTrue(fqn.isClass());
        Assertions.assertFalse(fqn.isType());
        Assertions.assertEquals("classCode", fqn.getId());
        Assertions.assertNull(fqn.getType());
    }

    @Test
    public void parseNull() {
        Assertions.assertNull(Fqn.parse(null));
        Assertions.assertNull(Fqn.parse(""));
    }

    @Test
    public void parseType() {
        final Fqn fqn = Fqn.parse("classCode$typeCode");
        Assertions.assertFalse(fqn.isClass());
        Assertions.assertTrue(fqn.isType());
        Assertions.assertEquals("classCode", fqn.getId());
        Assertions.assertEquals("typeCode", fqn.getType());
    }

    @Test
    public void print() {
        Fqn fqn = new Fqn("classCode", "typeCode");
        Assertions.assertEquals("classCode$typeCode", Fqn.print(fqn));
        Assertions.assertEquals("", Fqn.print(null));
    }

}
