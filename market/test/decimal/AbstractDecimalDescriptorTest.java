package ru.yandex.market.jmf.attributes.test.decimal;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import ru.yandex.market.jmf.attributes.conf.metaclass.DecimalTypeConf;
import ru.yandex.market.jmf.attributes.decimal.DecimalDescriptor;
import ru.yandex.market.jmf.attributes.decimal.DecimalType;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractDecimalDescriptorTest {

    DecimalDescriptor descriptor;
    Attribute attribute;
    DecimalType type;

    @BeforeAll
    public void setUp() {
        descriptor = new DecimalDescriptor();
        attribute = new Attribute();
        type = new DecimalType(DecimalTypeConf.builder().build());
    }
}
