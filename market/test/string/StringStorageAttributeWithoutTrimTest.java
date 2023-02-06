package ru.yandex.market.jmf.attributes.test.string;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;

@ContextConfiguration(classes = StringStorageAttributeWithoutTrimTest.Configuration.class)
public class StringStorageAttributeWithoutTrimTest extends AbstractStringAttributeTest {

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:string_without_trim_storage_attribute_metadata.xml",
                    "classpath:linkAttrMetaclass_metadata.xml");
        }
    }

    @Override
    protected String randomAttributeValue() {
        return " " + super.randomAttributeValue() + " ";
    }
}
