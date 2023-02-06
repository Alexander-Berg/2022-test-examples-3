package ru.yandex.market.jmf.attributes.test.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.jmf.attributes.AbstractAttributeTestConfiguration;
import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.db.api.HqlQuery;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.AttributeFqn;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ObjectsColumnAttributeTest.Configuration.class)
public class ObjectsColumnAttributeTest {

    protected final Fqn fqn = Fqn.parse("e1");
    protected final Fqn linkAttrMetaclass = Fqn.parse("linkAttrMetaclass");
    protected final String attributeCode = "attr";
    protected final String linkAttr1Code = "linkAttr1";
    protected final String linkAttr2Code = "linkAttr2";

    @Inject
    protected EntityService entityService;
    @Inject
    protected MetadataService metadataService;
    @Inject
    protected PlatformTransactionManager txmanager;
    @Inject
    protected DbService dbService;

    protected TransactionTemplate txTemplate;

    protected Object randomAttributeValue() {
        return Collections.singleton(randomAttributeValueItem());
    }

    static Stream<Arguments> countOfElements() {
        return Stream.of(
                arguments(1, 0, 1),
                arguments(1, 3, 1),
                arguments(3, 0, 3),
                arguments(0, 2, 0),
                arguments(3, 2, 3),
                arguments(0, 0, 0)
        );
    }

    protected Entity randomAttributeValueItem() {
        Entity entity = entityService.newInstance(Fqn.parse("e2"));
        persist(entity);
        return entity;
    }

    @BeforeEach
    public void init() {
        txTemplate = new TransactionTemplate(txmanager);

        HqlQuery query = dbService.createQuery("FROM " + fqn.toString());
        for (Entity o : query.<Entity>list()) {
            dbService.delete(o);
        }
        dbService.flush();
    }

    /**
     * Проверяем, что в базе находим entity по значению атрибута
     */
    @Test
    public void filter() {
        Object value = randomAttributeValue();
        persist(getEntity(value));
        persist(getEntity(randomAttributeValue()));

        List<Entity> result = filter(value);

        for (Entity e : result) {
            Object resultValue = e.getAttribute(attributeCode);
            Assertions.assertEquals(value, resultValue, "Из базы данных должны получить ранее сохраненное значение");
        }
    }

    protected List<Object> randomAttributeValues(int count) {
        var list = new ArrayList<>();
        for (var i = 0; i < count; i++) {
            list.add(randomAttributeValueItem());
        }
        return list;
    }

    /**
     * Проверяем, что в базе находим entity фильтром In
     */
    @ParameterizedTest
    @MethodSource("countOfElements")
    public void filterIn(int matchingQuantity, int nonMatchingQuantity, int foundQuantity) {
        List<Object> values = randomAttributeValues(matchingQuantity);
        for (var value : values) {
            persist(getEntity(Collections.singleton(value)));
        }
        values.addAll(randomAttributeValues(nonMatchingQuantity));
        persist(getEntity(randomAttributeValue()));

        List<Entity> result = filter(values);

        Assertions.assertEquals(foundQuantity, result.size());
    }

    @Test
    public void filterEq() {
        Object value = randomAttributeValueItem();
        persist(getEntity(Collections.singleton(value)));
        persist(getEntity(randomAttributeValue()));

        List<Entity> result = filter(value);

        Assertions.assertEquals(1, result.size());
    }

    private List<Entity> filter(Object value) {
        AttributeFqn attributeFqn = metadataService.getMetaclassOrError(fqn).getAttribute(attributeCode).getFqn();
        Filter filter = value instanceof Collection ?
                Filters.in(attributeFqn.getCode(), (Collection) value)
                : Filters.eq(attributeFqn, value);

        return doInTx(() -> dbService.list(Query.of(fqn).withFilters(filter)));
    }

    protected void persist(Entity entity) {
        doInTx(() -> {
            dbService.save(entity);
            return null;
        });
    }

    private <T> T doInTx(Supplier<T> action) {
        dbService.clear();
        T result = action.get();
        dbService.flush();
        return result;
    }

    private Entity getEntity(Object o) {
        return getEntity(o, attributeCode);
    }

    private Entity getEntity(Object o, String attrCode) {
        Entity entity = entityService.newInstance(fqn);
        entityService.setAttribute(entity, attrCode, o);
        return entity;
    }

    private Entity createPersistedEntityWithAttr(Fqn fqn, String attrCode, Object value) {
        Entity entity = entityService.newInstance(fqn);
        entityService.setAttribute(entity, attrCode, value);
        persist(entity);
        return entity;
    }

    /**
     * Проверяем, что в базе находим entity по значению атрибута
     */
    @Test
    public void filter_null() {
        Entity entity = getEntity(null);
        persist(entity);
        persist(getEntity(randomAttributeValue()));

        List<Entity> result = filter(null);

        Assertions.assertEquals(1,
                result.size(), "должны получить один элемент т.к. только один элемент удовлетворяет условию");
        Entity resultEntity = result.get(0);
        Assertions.assertEquals(entity, resultEntity, "Из базы данных должны получить ранее сохраненное значение");
    }

    /**
     * Проверяем, что из базы получаем ранее сохраненное значение
     */
    @Test
    public void get() {
        Object value = randomAttributeValue();
        Entity entity = getEntity(value);

        persist(entity);

        Entity result = get(entity);

        Object attributeValue = result.getAttribute(attributeCode);
        Assertions.assertEquals(value, attributeValue, "Из базы данных должны получить ранее сохраненное значение");
    }

    private Entity get(Entity e) {
        return doInTx(() -> dbService.get(e.getGid()));
    }

    @Test
    public void get_null() {
        Entity entity = getEntity(null);

        persist(entity);

        Entity result = get(entity);

        Collection<Entity> attributeValue = result.getAttribute(attributeCode);
        Assertions.assertTrue(attributeValue.isEmpty(), "Из базы данных должны получить ранее сохраненное значение");
    }

    /**
     * Проверяем успешность сохранения entity с заполненным значением атрибута
     */
    @Test
    public void persist() {
        Object value = randomAttributeValue();
        Entity entity = getEntity(value);

        persist(entity);

        Assertions.assertNotNull(entity.getGid(), "После сохранения у entity должен сформироваться gid");
    }

    /**
     * Проверяем успешность сохранения entity с значением атрибута равным null
     */
    @Test
    public void persist_null() {
        Entity entity = getEntity(null);

        persist(entity);

        Assertions.assertNotNull(entity.getGid(), "После сохранения у entity должен сформироваться gid");
    }

    @Test
    public void eqFilterOnLinkedObject() {
        Object value = randomAttributeValue();
        Entity entity1 = createPersistedEntityWithAttr(fqn, attributeCode, value);
        Entity entity2 = createPersistedEntityWithAttr(linkAttrMetaclass, linkAttr2Code, entity1);
        Entity entity3 = createPersistedEntityWithAttr(linkAttrMetaclass, linkAttr1Code, entity2);

        Filter filter = Filters.in(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                (Collection) value
        );
        List<Entity> results = doInTx(() -> dbService.list(Query.of(linkAttrMetaclass).withFilters(filter)));
        Assertions.assertEquals(1, results.size(), "Должно найтись ровно одно значение");
        Assertions.assertEquals(entity3, results.get(0));
    }

    @Test
    public void containsAnyFilterOnLinkedObject() {
        Entity item1 = randomAttributeValueItem();
        Entity item2 = randomAttributeValueItem();
        Entity item3 = randomAttributeValueItem();

        Object value = Set.of(item1, item2);
        Entity expectedEntity = createPersistedLinkedEntity(value);

        Filter filter = Filters.containsAny(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                Set.of(item2, item3)
        );
        List<Entity> results = doInTx(() -> dbService.list(Query.of(linkAttrMetaclass).withFilters(filter)));
        Assertions.assertEquals(1, results.size(), "Должно найтись ровно одно значение");
        Assertions.assertEquals(expectedEntity, results.get(0));
    }

    @Test
    public void containsAllFilter() {
        Entity item1 = randomAttributeValueItem();
        Entity item2 = randomAttributeValueItem();
        Entity item3 = randomAttributeValueItem();

        Entity entity = getEntity(Set.of(item1, item2, item3));
        persist(entity);
        persist(getEntity(Set.of(item1)));
        persist(getEntity(Set.of(item1, item3)));

        Filter filter = Filters.containsAll(attributeCode, Set.of(item1, item2));
        Query query = Query.of(fqn).withFilters(filter);
        List<Entity> entities = doInTx(() -> new ArrayList(dbService.list(query)));

        assertEquals(1, entities.size());
        assertEquals(entity, entities.get(0));
    }

    @Test
    public void containsAllFilterOnLinkedObject() {
        Entity item1 = randomAttributeValueItem();
        Entity item2 = randomAttributeValueItem();
        Entity item3 = randomAttributeValueItem();

        Entity expectedEntity = createPersistedLinkedEntity(Set.of(item1, item2, item3));
        createPersistedLinkedEntity(Set.of(item1));
        createPersistedLinkedEntity(Set.of(item1, item3));

        Filter filter = Filters.containsAll(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                Set.of(item1, item2)
        );
        List<Entity> results = doInTx(() -> dbService.list(Query.of(linkAttrMetaclass).withFilters(filter)));
        Assertions.assertEquals(1, results.size(), "Должно найтись ровно одно значение");
        Assertions.assertEquals(expectedEntity, results.get(0));
    }

    private Entity createPersistedLinkedEntity(Object attrValue) {
        Entity entity1 = createPersistedEntityWithAttr(fqn, attributeCode, attrValue);
        Entity entity2 = createPersistedEntityWithAttr(linkAttrMetaclass, linkAttr2Code, entity1);
        return createPersistedEntityWithAttr(linkAttrMetaclass, linkAttr1Code, entity2);
    }

    @org.springframework.context.annotation.Configuration
    public static class Configuration extends AbstractAttributeTestConfiguration {
        public Configuration() {
            super("classpath:objects_column_attribute_metadata.xml", "classpath:linkAttrMetaclass_metadata.xml");
        }
    }
}
