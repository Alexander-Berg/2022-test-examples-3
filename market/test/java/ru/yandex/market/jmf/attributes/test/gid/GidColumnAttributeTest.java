package ru.yandex.market.jmf.attributes.test.gid;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;

@ContextConfiguration(classes = GidColumnAttributeTest.Configuration.class)
public class GidColumnAttributeTest extends AbstractGidAttributeTest {

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:gid_column_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
