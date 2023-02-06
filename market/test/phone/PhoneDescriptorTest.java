package ru.yandex.market.jmf.attributes.test.phone;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.domain.Phone;

public class PhoneDescriptorTest extends AbstractPhoneDescriptorTest {

    @Test
    public void toDtoValue_null() {
        Object result = descriptor.toDtoValue(attribute, type, null);
        Assertions.assertNull(result);
    }

    @Test
    public void toDtoValue_empty() {
        Object result = descriptor.toDtoValue(attribute, type, Phone.empty());
        Assertions.assertTrue(result instanceof Map);
        Assertions.assertEquals("", ((Map) result).get("main"));
        Assertions.assertNull(((Map) result).get("raw"));
        Assertions.assertNull(((Map) result).get("ext"));
    }

    @Test
    public void toDtoValue() {
        Object result = descriptor.toDtoValue(attribute, type, Phone.fromRaw("+71234567890 доб. 123"));
        Assertions.assertTrue(result instanceof Map);
        Assertions.assertEquals("+71234567890", ((Map) result).get("main"));
        Assertions.assertEquals("+71234567890 доб. 123", ((Map) result).get("raw"));
        Assertions.assertEquals("123", ((Map) result).get("ext"));
    }

    @Test
    public void toString_null() {
        Object result = descriptor.toString(attribute, type, null);
        Assertions.assertNull(result);
    }

    @Test
    public void toString_empty() {
        String result = descriptor.toString(attribute, type, Phone.empty());
        Assertions.assertEquals("", result);
    }

    @Test
    public void toString_value() {
        Object result = descriptor.toString(attribute, type, Phone.fromRaw("+71234567890 доб. 123"));
        Assertions.assertEquals("+71234567890#123", result);
    }
}
