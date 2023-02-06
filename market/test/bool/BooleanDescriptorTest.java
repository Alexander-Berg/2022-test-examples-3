package ru.yandex.market.jmf.attributes.test.bool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class BooleanDescriptorTest extends AbstractBooleanDescriptorTest {

    @Test
    public void toDto_null() {
        Object result = descriptor.toDtoValue(attribute, type, null);
        Assertions.assertNull(result);
    }

    @Test
    public void toDto_true() {
        Object result = descriptor.toDtoValue(attribute, type, Boolean.TRUE);
        Assertions.assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void toDto_false() {
        Object result = descriptor.toDtoValue(attribute, type, Boolean.FALSE);
        Assertions.assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void toString_null() {
        String result = descriptor.toString(attribute, type, null);
        Assertions.assertEquals(null, result);
    }

    @Test
    public void toString_true() {
        String result = descriptor.toString(attribute, type, Boolean.TRUE);
        Assertions.assertEquals("true", result);
    }

    @Test
    public void toString_false() {
        String result = descriptor.toString(attribute, type, Boolean.FALSE);
        Assertions.assertEquals("false", result);
    }
}
