package ru.yandex.market.jmf.attributes.test.bool;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;

@ContextConfiguration(classes = BooleanColumnAttributeTest.Configuration.class)
public class BooleanColumnAttributeTest extends AbstractBooleanAttributeTest {

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:boolean_column_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
