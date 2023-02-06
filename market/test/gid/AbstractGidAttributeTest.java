package ru.yandex.market.jmf.attributes.test.gid;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;

public abstract class AbstractGidAttributeTest extends AbstractAttributeTest {

    @Test
    public void classOneOfFilterOnLinkedObject() {
        Object value1 = randomAttributeValue("e1");
        Entity entity1 = createPersistedEntityWithAttr(fqn, attributeCode, value1);
        Entity entity2 = createPersistedEntityWithAttr(linkAttrMetaclass, linkAttr2Code, entity1);
        Entity entity3 = createPersistedEntityWithAttr(linkAttrMetaclass, linkAttr1Code, entity2);

        Object value2 = randomAttributeValue("e2");
        Entity entity4 = createPersistedEntityWithAttr(fqn, attributeCode, value2);
        Entity entity5 = createPersistedEntityWithAttr(linkAttrMetaclass, linkAttr2Code, entity4);
        Entity entity6 = createPersistedEntityWithAttr(linkAttrMetaclass, linkAttr1Code, entity5);

        Filter filter = Filters.classOneOf(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                List.of("e2")
        );
        List<Entity> results = doInTx(() -> dbService.list(Query.of(linkAttrMetaclass).withFilters(filter)));
        Assertions.assertEquals(1, results.size(), "Должно найтись ровно одно значение");
        Assertions.assertEquals(entity6, results.get(0));
    }

    @Override
    protected Object randomAttributeValue() {
        return randomAttributeValue("e2");
    }

    private Object randomAttributeValue(String fqn) {
        Entity entity = entityService.newInstance(Fqn.parse(fqn));
        persist(entity);
        return entity.getGid();
    }

    public void snapshot() {
        // Gid не поддерживает snapshoting
    }
}
