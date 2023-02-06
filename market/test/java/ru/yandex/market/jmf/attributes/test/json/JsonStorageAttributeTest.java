package ru.yandex.market.jmf.attributes.test.json;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;

@ContextConfiguration(classes = JsonStorageAttributeTest.Configuration.class)
public class JsonStorageAttributeTest extends AbstractJsonAttributeTest {
    @Nonnull
    @Override
    protected Object randomValue() {
        return Map.of(
                "a_boolean", Randoms.booleanValue(),
                "b_string", Randoms.string(),
                "c_int", Randoms.intValue(),
                "d_date", Randoms.dateTime().toString(),
                "e_array", List.of(Randoms.string(), Randoms.string()),
                "f_map", Map.of(
                        "a_boolean", Randoms.booleanValue(),
                        "b_string", Randoms.string(),
                        "c_int", Randoms.intValue(),
                        "d_date", Randoms.dateTime().toString(),
                        "e_array", List.of(Randoms.string(), Randoms.string()))
        );
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:json_storage_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }

    }
}
