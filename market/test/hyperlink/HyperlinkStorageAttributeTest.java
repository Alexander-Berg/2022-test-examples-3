package ru.yandex.market.jmf.attributes.test.hyperlink;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;

@ContextConfiguration(classes = HyperlinkStorageAttributeTest.Configuration.class)
public class HyperlinkStorageAttributeTest extends AbstractHyperlinkAttributeTest {

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:hyperlink_storage_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
