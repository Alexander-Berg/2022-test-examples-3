package ru.yandex.market.jmf.attributes.test.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.jmf.attributes.email.EmailDescriptor;
import ru.yandex.market.jmf.attributes.email.EmailType;
import ru.yandex.market.jmf.metadata.conf.metaclass.AttributeTypeConf;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.utils.SerializationUtils;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EmailDescriptorTest {

    static ObjectMapper objectMapper = SerializationUtils.defaultObjectMapper();
    EmailDescriptor descriptor;
    Attribute attribute;
    EmailType type;

    @BeforeAll
    public void setUp() {
        descriptor = new EmailDescriptor(objectMapper);
        attribute = new Attribute();
        type = new EmailType(AttributeTypeConf.builder().withCode(EmailType.CODE).build());
    }

    @Test
    public void testWrapInvalidEmail() {
        var invalidValue = "invalid email";
        var actual = descriptor.wrap(attribute, type, invalidValue);
        Assertions.assertEquals(invalidValue, actual);
    }

    @Test
    public void testWrapBlankEmail() {
        var actual = descriptor.wrap(attribute, type, "   ");
        Assertions.assertNull(actual);
    }

    @Test
    public void testWrapNull() {
        var actual = descriptor.wrap(attribute, type, null);
        Assertions.assertNull(actual);
    }

    @Test
    public void testWrapEmailWithName() {
        var value = "test name <test@yandex.ru>";
        var actual = descriptor.wrap(attribute, type, value);
        Assertions.assertEquals(value, actual);
    }

    @Test
    public void testWrapEmailWithUpperCaseSymbols() {
        var actual = descriptor.wrap(attribute, type, "Test Name <   tEst@yAndeX.ru >");
        Assertions.assertEquals("Test Name <test@yandex.ru>", actual);
    }

    @Test
    public void testWrapInvalidEmailWithName() {
        var invalidValue = "test name <test@ yandex.ru>";
        var actual = descriptor.wrap(attribute, type, invalidValue);
        Assertions.assertEquals(invalidValue, actual);
    }
}
