package ru.yandex.market.jmf.attributes.test.hyperlink;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.hyperlink.Hyperlink;

public class HyperlinkDescriptorTest extends AbstractHyperlinkDescriptorTest {

    @Test
    public void toDtoValue_null() {
        Object result = descriptor.toDtoValue(attribute, type, null);
        Assertions.assertNull(result);
    }

    @Test
    public void toDtoValue() {
        Hyperlink value = new Hyperlink(Randoms.string(), Randoms.string());
        Object result = descriptor.toDtoValue(attribute, type, value);
        Assertions.assertEquals(value, result);
    }

    @Test
    public void toString_null() {
        Object result = descriptor.toString(attribute, type, null);
        Assertions.assertNull(result);
    }


    @Test
    public void toString_value() {
        Hyperlink value = new Hyperlink("hrefA", "valueB");
        Object result = descriptor.toString(attribute, type, value);
        Assertions.assertEquals("((valueB hrefA))", result);
    }
}
