package ru.yandex.market.jmf.attributes.test.time;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.jmf.attributes.time.TimeDescriptor;
import ru.yandex.market.jmf.attributes.time.TimeType;
import ru.yandex.market.jmf.metadata.conf.metaclass.AttributeTypeConf;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractTimeDescriptorTest {

    TimeDescriptor descriptor;
    Attribute attribute;
    TimeType type;

    @BeforeAll
    public void setUp() {
        descriptor = new TimeDescriptor();
        attribute = new Attribute();
        type = new TimeType(AttributeTypeConf.builder().withCode(TimeType.CODE).build());
    }
}
