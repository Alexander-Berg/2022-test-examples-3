package ru.yandex.market.jmf.attributes.test.integer;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;

@ContextConfiguration(classes = IntegerStorageAttributeTest.Configuration.class)
public class IntegerStorageAttributeTest extends AbstractAttributeTest {

    @Override
    protected Object randomAttributeValue() {
        return Randoms.longValue();
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:integer_storage_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
