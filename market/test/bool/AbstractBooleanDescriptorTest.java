package ru.yandex.market.jmf.attributes.test.bool;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.jmf.attributes.bool.BooleanDescriptor;
import ru.yandex.market.jmf.attributes.bool.BooleanType;
import ru.yandex.market.jmf.attributes.conf.metaclass.BooleanTypeConf;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractBooleanDescriptorTest {

    BooleanDescriptor descriptor;
    Attribute attribute;
    BooleanType type;

    @BeforeAll
    public void setUp() {
        descriptor = new BooleanDescriptor();
        attribute = new Attribute();
        type = new BooleanType(BooleanTypeConf.builder().build());
    }
}
