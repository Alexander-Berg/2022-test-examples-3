package ru.yandex.market.jmf.attributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.jmf.db.api.DbService;
import ru.yandex.market.jmf.db.api.HqlQuery;
import ru.yandex.market.jmf.entity.AttributeTypeService;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.EntityService;
import ru.yandex.market.jmf.entity.EntitySnapshotService;
import ru.yandex.market.jmf.entity.RuntimeFilterHandlerFactory;
import ru.yandex.market.jmf.entity.query.ComparisonFilter;
import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;
import ru.yandex.market.jmf.metadata.Fqn;
import ru.yandex.market.jmf.metadata.MetadataService;
import ru.yandex.market.jmf.metadata.metaclass.Attribute;

import static org.junit.jupiter.params.provider.Arguments.arguments;

@Transactional
@ExtendWith(SpringExtension.class)
public abstract class AbstractAttributeTest {

    protected final Fqn fqn;
    protected final String attributeCode;

    protected final Fqn linkAttrMetaclass = Fqn.parse("linkAttrMetaclass");
    protected final String linkAttr1Code = "linkAttr1";
    protected final String linkAttr2Code = "linkAttr2";

    @Inject
    protected EntityService entityService;
    @Inject
    protected DbService dbService;
    @Inject
    protected EntitySnapshotService entitySnapshotService;
    @Inject
    protected AttributeTypeService attributeTypeService;
    @Inject
    protected MetadataService metadataService;
    @Inject
    protected RuntimeFilterHandlerFactory runtimeFilterHandlerFactory;

    protected AbstractAttributeTest() {
        this(Fqn.parse("e1"), "attr");
    }

    protected AbstractAttributeTest(Fqn fqn, String attributeCode) {
        this.fqn = fqn;
        this.attributeCode = attributeCode;
    }

    private Long count(Object value) {
        Filter filter = Filters.eq(attributeCode, value);
        Query q = Query.of(fqn).withFilters(filter);

        return doInTx(() -> dbService.count(q));
    }

    protected Entity createPersistedEntity(Object object) {
        Entity entity = getEntity(object);
        persist(entity);
        return entity;
    }

    public static Stream<Arguments> countOfElements() {
        return Stream.of(
                arguments(1),
                arguments(5)
        );
    }

    protected Entity createPersistedEntityWithAttr(Fqn fqn, String attrCode, Object value) {
        Entity entity = entityService.newInstance(fqn);
        entityService.setAttribute(entity, attrCode, value);
        persist(entity);
        return entity;
    }

    protected Entity createPersistedLinkedEntity(Object attrValue) {
        Entity entity1 = createPersistedEntityWithAttr(fqn, attributeCode, attrValue);
        Entity entity2 = createPersistedEntityWithAttr(linkAttrMetaclass, linkAttr2Code, entity1);
        return createPersistedEntityWithAttr(linkAttrMetaclass, linkAttr1Code, entity2);
    }

    protected <T> T doInTx(Supplier<T> action) {
        dbService.clear();
        T result = action.get();
        dbService.flush();
        return result;
    }

    protected List<Entity> createPersistedEntities(List<Object> objects) {
        List<Entity> list = new ArrayList<>();
        for (var object : objects) {
            list.add(createPersistedEntity(object));
        }
        return list;
    }

    /**
     * Проверяем, что в базе находим entity по значению атрибута с использованием фильтра EQ
     */
    @Test
    public void filterEq() {
        Object value = randomAttributeValue();
        createPersistedEntity(value);
        createPersistedEntity(randomAttributeValue());

        List<Entity> result = filter(value);

        Assertions.assertTrue(0 < result.size());
        for (Entity e : result) {
            Object resultValue = e.getAttribute(attributeCode);
            Assertions.assertEquals(value, resultValue, "Из базы данных должны получить ранее сохраненное значение");
        }

        long count = count(value);
        Assertions.assertEquals(result.size(), count, "Запрос на подсчет кол-ва объектов должен давать такое-же " +
                "кол-во как" +
                " и при получение объектов списком");
    }

    /**
     * Проверяем, что в базе находим entity по значению атрибута с использованием фильтра IN
     */
    @ParameterizedTest
    @MethodSource("countOfElements")
    public void filterIn(int countOfElements) {
        var values = new ArrayList<>();
        for (var i = 0; i < countOfElements; i++) {
            var value = randomAttributeValue();
            if (value instanceof List) {
                return;
            }
            values.add(value);
        }

        createPersistedEntities(values);
        createPersistedEntity(randomAttributeValue());

        List<Entity> result = filter(Filters.in(attributeCode, values));

        Assertions.assertEquals(countOfElements, result.size(), "Из базы данных должны получить ожидаемое количество " +
                "элементов");
    }

    private List<Entity> filter(Object value) {
        return filter(Filters.eq(attributeCode, value));
    }

    public List<Entity> filter(Filter filter) {
        Query q = Query.of(fqn).withFilters(filter).withAttributes(attributeCode);
        return doInTx(() -> new ArrayList<>(dbService.list(q)));
    }

    /**
     * Проверяем, что при фильтрации по неравенству, получаем из БД все entity,
     * кроме entity с указанным значением атрибута.
     */
    @Test
    public void filter_ne() {
        Object value = randomAttributeValue();
        Object otherValue = getOtherRandomValue(value);
        createPersistedEntity(value);
        createPersistedEntity(otherValue);

        List<Entity> results = filter(Filters.ne(attributeCode, value));

        Assertions.assertFalse(results.isEmpty(), "Должно что-то найтись");
        Assertions.assertEquals(1, results.size(), "Должно найтись ровно одно значение");
        Entity result = results.get(0);
        Object resultValue = result.getAttribute(attributeCode);
        Assertions.assertNotEquals(
                value, resultValue, "Найденное значение не должно совпадать с тем, которое мы отфильтровали");
        Assertions.assertEquals(otherValue, resultValue, "Должны найти второе значение");
    }

    @Test
    public void eqFilterOnLinkedObject() {
        Object value = randomAttributeValue();
        Entity entity3 = createPersistedLinkedEntity(value);

        Filter filter = Filters.eq(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                value
        );
        List<Entity> results = doInTx(() -> dbService.list(Query.of(linkAttrMetaclass).withFilters(filter)));
        Assertions.assertEquals(1, results.size(), "Должно найтись ровно одно значение");
        Assertions.assertEquals(entity3, results.get(0));
    }

    /**
     * Проверяем, что при фильтрации по неравенству, получаем из БД все entity (в том числе null),
     * кроме entity с указанным значением атрибута.
     */
    @Test
    public void filter_ne_with_empty() {
        Object value = randomAttributeValue();
        Object otherValue = getOtherRandomValue(value);
        createPersistedEntity(value);
        createPersistedEntity(otherValue);
        createPersistedEntity(null);

        List<Entity> results = filter(Filters.ne(attributeCode, value));

        Assertions.assertEquals(2, results.size(), "Должно найтись ровно два значения");

        List values =
                results.stream().map(e -> e.getAttribute(attributeCode)).collect(Collectors.toList());

        Assertions.assertFalse(
                values.contains(value), "Найденное значение не должно совпадать с тем, которое мы отфильтровали");
        Assertions.assertTrue(values.contains(otherValue), "Должны найти второе значение");
        Assertions.assertTrue(values.contains(getNullValue()), "Должны найти null");
    }

    /**
     * Проверяем, что в базе находим entity по значению атрибута
     */
    @Test
    public void filter_null() {
        Entity entity = createPersistedEntity(null);
        createPersistedEntity(randomAttributeValue());

        List<Entity> result = filter((Object) null);

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
        Entity entity = createPersistedEntity(value);

        Entity result = get(entity);

        Object attributeValue = result.getAttribute(attributeCode);
        Assertions.assertEquals(value, attributeValue, "Из базы данных должны получить ранее сохраненное значение");
    }

    protected Entity get(Entity e) {
        return doInTx(() -> dbService.get(e.getGid()));
    }

    protected Entity getEntity(Object o) {
        return getEntity(o, attributeCode);
    }

    protected Entity getEntity(Object o, String attrCode) {
        Entity entity = entityService.newInstance(fqn);
        Attribute attribute = metadataService.getMetaclassOrError(fqn).getAttributeOrError(attrCode);
        Object value = attributeTypeService.wrap(attribute, o);
        entityService.setAttribute(entity, attribute, value);
        return entity;
    }

    protected Object getNullValue() {
        return null;
    }

    private Object getOtherRandomValue(Object value) {
        for (int i = 0; i < 10; i++) {
            Object other = randomAttributeValue();
            if (!Objects.equals(value, other)) {
                return other;
            }
        }
        throw new RuntimeException("Random generates equal values");
    }

    protected <T> T getValueOfEntity(Entity entity) {
        return entity.getAttribute(attributeCode);
    }

    @Test
    public void get_null() {
        Entity entity = createPersistedEntity(null);

        Entity result = get(entity);

        Object attributeValue = result.getAttribute(attributeCode);
        Assertions.assertEquals(getNullValue(),
                attributeValue, "Из базы данных должны получить ранее сохраненное значение");
    }

    @BeforeEach
    public void init() {
        HqlQuery query = dbService.createQuery("FROM " + fqn.toString());
        for (Entity o : query.<Entity>list()) {
            dbService.delete(o);
        }
        dbService.flush();
    }

    protected void persist(Entity entity) {
        doInTx(() -> {
            dbService.save(entity);
            return null;
        });
    }

    /**
     * Проверяем успешность сохранения entity с заполненным значением атрибута
     */
    @Test
    public void persist() {
        Object value = randomAttributeValue();
        Entity entity = createPersistedEntity(value);

        Assertions.assertNotNull(entity.getGid(), "После сохранения у entity должен сформироваться gid");
    }

    /**
     * Проверяем успешность сохранения entity с значением атрибута равным null
     */
    @Test
    public void persist_null() {
        Entity entity = createPersistedEntity(null);

        Assertions.assertNotNull(entity.getGid(), "После сохранения у entity должен сформироваться gid");
    }

    protected abstract Object randomAttributeValue();

    @Test
    public void snapshot() {
        Object value = randomAttributeValue();
        Entity entity = createPersistedEntity(value);

        JsonNode snapshot = entitySnapshotService.toSnapshot(entity);
        Entity afterSnapshot = entitySnapshotService.fromSnapshot(fqn, snapshot);
        Object result = afterSnapshot.getAttribute(attributeCode);

        Assertions.assertEquals(value, result, "Должны получить значение объекта до создания snapshot-а");
    }

    @Test
    public void snapshot_null() {
        Entity entity = createPersistedEntity(null);

        JsonNode snapshot = entitySnapshotService.toSnapshot(entity);
        Entity afterSnapshot = entitySnapshotService.fromSnapshot(fqn, snapshot);
        Object result = afterSnapshot.getAttribute(attributeCode);

        Assertions.assertEquals(getNullValue(), result, "Должны получить значение объекта до создания snapshot-а");
    }

    protected <T extends Comparable<T>> void testComparisonFiltering(ComparisonFilter<T> filter,
                                                                     Map<T, Boolean> valuesAndExpectations) {
        for (T value : valuesAndExpectations.keySet()) {
            createPersistedEntity(value);
        }

        Query q = Query.of(fqn).withFilters(filter);
        List<Entity> result = dbService.list(q);
        Set<T> selectedValues = result.stream()
                .map(this::<T>getValueOfEntity)
                .collect(Collectors.toSet());

        valuesAndExpectations.forEach((value, shouldBeSelected) -> {
            if (Iterables.any(selectedValues, v -> isEqual(v, value)) ^ shouldBeSelected) {
                Assertions.fail("Значение %s %sдолжно было попасть в выборку %s"
                        .formatted(value, shouldBeSelected ? "" : "не ", filter));
            }
        });
    }

    protected <T extends Comparable<T>> boolean isEqual(T a, T b) {
        return Objects.equals(a, b);
    }

    protected <T extends Comparable> void testComparisonFilteringOnLinkedObject(ComparisonFilter<T> filter,
                                                                                Map<T, Boolean> valuesAndExpectations) {
        for (T value : valuesAndExpectations.keySet()) {
            createPersistedLinkedEntity(value);
        }

        Query q = Query.of(linkAttrMetaclass).withFilters(filter);
        List<Entity> result = new ArrayList(dbService.list(q));
        Set<T> selectedValues = result.stream()
                .map(entity -> (Entity) entity.getAttribute(linkAttr1Code))
                .filter(Objects::nonNull)
                .map(entity -> (Entity) entity.getAttribute(linkAttr2Code))
                .filter(Objects::nonNull)
                .map(entity -> entity.<T>getAttribute(attributeCode))
                .collect(Collectors.toSet());

        valuesAndExpectations.forEach((value, shouldBeSelected) ->
                Assertions.assertEquals(Iterables.any(selectedValues, v -> isEqual(v, value)), shouldBeSelected));
    }

    protected boolean doRuntimeFilter(Object attributeValue, Filter filter) {
        Entity entity = createPersistedEntity(attributeValue);
        Predicate<? super Entity> predicate = runtimeFilterHandlerFactory.getPredicate(entity.getMetaclass(), filter);
        return predicate.test(entity);
    }

}
