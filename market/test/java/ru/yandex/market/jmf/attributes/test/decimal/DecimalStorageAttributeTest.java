package ru.yandex.market.jmf.attributes.test.decimal;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;

@ContextConfiguration(classes = DecimalStorageAttributeTest.Configuration.class)
public class DecimalStorageAttributeTest extends AbstractAttributeTest {

    @Override
    protected Object randomAttributeValue() {
        return Randoms.bigDecimal();
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:decimal_storage_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
