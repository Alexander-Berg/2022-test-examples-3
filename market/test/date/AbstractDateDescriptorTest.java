package ru.yandex.market.jmf.attributes.test.date;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.jmf.attributes.date.DateDescriptor;
import ru.yandex.market.jmf.attributes.date.DateType;
import ru.yandex.market.jmf.metadata.conf.metaclass.AttributeTypeConf;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDateDescriptorTest {

    DateDescriptor descriptor;
    Attribute attribute;
    DateType type;

    @BeforeAll
    public void setUp() {
        descriptor = new DateDescriptor();
        attribute = new Attribute();
        type = new DateType(AttributeTypeConf.builder().withCode(DateType.CODE).build());
    }
}
