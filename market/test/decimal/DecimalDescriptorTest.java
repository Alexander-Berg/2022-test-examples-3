package ru.yandex.market.jmf.attributes.test.decimal;

import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.util.Randoms;

public class DecimalDescriptorTest extends AbstractDecimalDescriptorTest {

    @Test
    public void toDto() {
        BigDecimal value = Randoms.bigDecimal();
        Object result = descriptor.toDtoValue(attribute, type, value);
        Assertions.assertEquals(value.toPlainString(), result);
    }

    @Test
    public void toDto_null() {
        Object result = descriptor.toDtoValue(attribute, type, null);
        Assertions.assertNull(result);
    }

    @Test
    public void toString_null() {
        Object result = descriptor.toString(attribute, type, null);
        Assertions.assertNull(result);
    }

    @Test
    public void toString_value() {
        BigDecimal value = Randoms.bigDecimal();
        Object result = descriptor.toString(attribute, type, value);
        Assertions.assertEquals(value.toString(), result);
    }
}
