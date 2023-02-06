package ru.yandex.market.jmf.attributes.test.time;

import java.time.LocalTime;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TimeDescriptorTest extends AbstractTimeDescriptorTest {

    @Test
    public void toDtoValue_null() {
        Object result = descriptor.toDtoValue(attribute, type, null);
        Assertions.assertNull(result);
    }

    @Test
    public void toDtoValue_value() {
        Object result = descriptor.toDtoValue(attribute, type, LocalTime.of(7, 13));
        Assertions.assertEquals("07:13:00", result);
    }

    @Test
    public void toDtoValue_valueSeconds() {
        Object result = descriptor.toDtoValue(attribute, type, LocalTime.of(7, 13, 17));
        Assertions.assertEquals("07:13:17", result);
    }

    @Test
    public void toString_null() {
        Object result = descriptor.toString(attribute, type, null);
        Assertions.assertNull(result);
    }

    @Test
    public void toString_value() {
        Object result = descriptor.toString(attribute, type, LocalTime.of(7, 13));
        Assertions.assertEquals("07:13:00", result);
    }
}
