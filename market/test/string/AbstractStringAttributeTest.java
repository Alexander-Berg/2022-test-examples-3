package ru.yandex.market.jmf.attributes.test.string;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.crm.util.Randoms;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;

public abstract class AbstractStringAttributeTest extends AbstractAttributeTest {

    @Test
    public void startsWithFilterOnLinkedObject() {
        String value = Randoms.string();
        Entity entity1 = createPersistedEntityWithAttr(fqn, attributeCode, value + Randoms.string());
        Entity entity2 = createPersistedEntityWithAttr(linkAttrMetaclass, linkAttr2Code, entity1);
        Entity entity3 = createPersistedEntityWithAttr(linkAttrMetaclass, linkAttr1Code, entity2);

        Filter filter = Filters.startsWith(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                value
        );
        List<Entity> results = doInTx(() -> dbService.list(Query.of(linkAttrMetaclass).withFilters(filter)));
        Assertions.assertEquals(1, results.size(), "Должно найтись ровно одно значение");
        Assertions.assertEquals(entity3, results.get(0));
    }

    @Test
    public void containsFilterOnLinkedObject() {
        String value = Randoms.string();
        Entity entity1 = createPersistedEntityWithAttr(fqn, attributeCode,
                Randoms.string() + value + Randoms.string());
        Entity entity2 = createPersistedEntityWithAttr(linkAttrMetaclass, linkAttr2Code, entity1);
        Entity entity3 = createPersistedEntityWithAttr(linkAttrMetaclass, linkAttr1Code, entity2);

        Filter filter = Filters.contains(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                value
        );
        List<Entity> results = doInTx(() -> dbService.list(Query.of(linkAttrMetaclass).withFilters(filter)));
        Assertions.assertEquals(1, results.size(), "Должно найтись ровно одно значение");
        Assertions.assertEquals(entity3, results.get(0));
    }

    @Override
    protected String randomAttributeValue() {
        return Randoms.string();
    }
}
