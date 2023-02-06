package ru.yandex.market.jmf.attributes.test.string;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;

@ContextConfiguration(classes = StringColumnAttributeWithoutTrimTest.Configuration.class)
public class StringColumnAttributeWithoutTrimTest extends AbstractStringAttributeTest {

    @Test
    public void likeFilter() {
        String value = randomAttributeValue();
        persist(getEntity(value));
        persist(getEntity(randomAttributeValue()));

        Filter filter = Filters.startsWith(attributeCode, value);
        Query q = Query.of(fqn).withFilters(filter);

        List<Entity> result = doInTx(() -> new ArrayList<>(dbService.list(q)));

        Assertions.assertEquals(1, result.size());
        for (Entity e : result) {
            String resultValue = getValueOfEntity(e);
            Assertions.assertEquals(value, resultValue, "Из базы данных должны получить ранее сохраненное значение");
        }
    }

    @Override
    protected String randomAttributeValue() {
        return " " + super.randomAttributeValue() + " ";
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:string_without_trim_column_attribute_metadata.xml",
                    "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
