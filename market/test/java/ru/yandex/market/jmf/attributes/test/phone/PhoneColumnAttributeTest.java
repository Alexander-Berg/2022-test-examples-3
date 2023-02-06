package ru.yandex.market.jmf.attributes.test.phone;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.domain.Phone;
import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;

@ContextConfiguration(classes = PhoneColumnAttributeTest.Configuration.class)
public class PhoneColumnAttributeTest extends AbstractAttributeTest {

    @Override
    protected Object randomAttributeValue() {
        return Phone.fromNormalized(Randoms.phoneNumber());
    }

    @Override
    protected Object getNullValue() {
        return Phone.empty();
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:phone_column_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
