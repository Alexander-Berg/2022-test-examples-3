package ru.yandex.market.jmf.attributes.test.date;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;

@ContextConfiguration(classes = DateStorageAttributeTest.Configuration.class)
public class DateStorageAttributeTest extends AbstractAttributeTest {

    @Override
    protected Object randomAttributeValue() {
        return Randoms.date();
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:date_storage_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
