package ru.yandex.market.jmf.attributes.test.object;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.attributes.AbstractRelationTest;

@ContextConfiguration(classes = ObjectColumnRelationTest.Configuration.class)
public class ObjectColumnRelationTest extends AbstractRelationTest {
    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:object_column_attribute_metadata.xml");
        }
    }
}
