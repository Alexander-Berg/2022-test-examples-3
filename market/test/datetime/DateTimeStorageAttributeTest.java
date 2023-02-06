package ru.yandex.market.jmf.attributes.test.datetime;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.entity.Entity;

@ContextConfiguration(classes = DateTimeStorageAttributeTest.Configuration.class)
public class DateTimeStorageAttributeTest extends AbstractAttributeTest {

    @Override
    protected Object randomAttributeValue() {
        return Randoms.dateTime();
    }

    @Test
    public void getWithEmptyMillis() {
        OffsetDateTime value = Randoms.dateTime();
        int millis = value.get(ChronoField.MILLI_OF_SECOND);
        value = value.minus(millis, ChronoUnit.MILLIS);
        Entity entity = createPersistedEntity(value);

        Entity result = get(entity);

        Object attributeValue = result.getAttribute(attributeCode);
        Assertions.assertEquals(value, attributeValue, "Из базы данных должны получить ранее сохраненное значение");
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:datetime_storage_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
