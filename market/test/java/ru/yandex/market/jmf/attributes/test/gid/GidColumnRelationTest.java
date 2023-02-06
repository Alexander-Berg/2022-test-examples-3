package ru.yandex.market.jmf.attributes.test.gid;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.attributes.AbstractRelationTest;
import ru.yandex.market.jmf.entity.Entity;

@ContextConfiguration(classes = GidColumnRelationTest.Configuration.class)
public class GidColumnRelationTest extends AbstractRelationTest {
    @Override
    protected Object extractValue(Object value) {
        return null == value ? null : ((Entity) value).getGid();
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:gid_column_attribute_metadata.xml");
        }
    }
}
