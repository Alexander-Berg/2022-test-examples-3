package ru.yandex.market.jmf.attributes.test.phone;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.jmf.attributes.phone.PhoneDescriptor;
import ru.yandex.market.jmf.attributes.phone.PhoneType;
import ru.yandex.market.jmf.metadata.conf.metaclass.AttributeTypeConf;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;
import ru.yandex.market.jmf.utils.SerializationUtils;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractPhoneDescriptorTest {

    static ObjectMapper objectMapper = SerializationUtils.defaultObjectMapper();
    PhoneDescriptor descriptor;
    Attribute attribute;
    PhoneType type;

    @BeforeAll
    public void setUp() {
        descriptor = new PhoneDescriptor(objectMapper);
        attribute = new Attribute();
        type = new PhoneType(AttributeTypeConf.builder().withCode(PhoneType.CODE).build());
    }
}
