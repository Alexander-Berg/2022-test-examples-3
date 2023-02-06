package ru.yandex.market.jmf.attributes.test.object;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.attributes.AbstractRelationTest;

@ContextConfiguration(classes = ObjectStorageRelationTest.Configuration.class)
public class ObjectStorageRelationTest extends AbstractRelationTest {
    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:object_storage_attribute_metadata.xml");
        }
    }
}
