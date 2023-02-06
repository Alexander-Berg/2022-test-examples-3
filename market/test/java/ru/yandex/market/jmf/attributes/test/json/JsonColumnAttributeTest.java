package ru.yandex.market.jmf.attributes.test.json;

import java.util.Map;

import javax.annotation.Nonnull;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;

@ContextConfiguration(classes = JsonColumnAttributeTest.Configuration.class)
public class JsonColumnAttributeTest extends AbstractJsonAttributeTest {
    @Nonnull
    protected Object randomValue() {
        return Map.of(
                "a_int", Randoms.intValue(),
                "b_date", Randoms.dateTime().toString()
        );
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {

        public Configuration() {
            super("classpath:json_column_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }

    }
}
