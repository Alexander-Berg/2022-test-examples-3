package ru.yandex.market.jmf.attributes.test.array;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.crm.util.CrmCollections;
import ru.yandex.market.jmf.attributes.AbstractAttributeTest;
import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.query.Filter;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class AbstractArrayAttributeTest<T> extends AbstractAttributeTest {

    private final String arrayContainsHql;

    protected AbstractArrayAttributeTest(String arrayContainsHql) {
        this.arrayContainsHql = arrayContainsHql;
    }

    private static Stream<Arguments> containsAnyFilterCases() {
        return Stream.of(
                Arguments.of(List.of(1, 2), List.of(0, 1)),
                Arguments.of(List.of(3), List.of()),
                Arguments.of(List.of(0, 3), List.of(0)),
                Arguments.of(List.of(2), List.of(1))
        );
    }

    @Test
    public void arrayContainsHqlFunction() {
        Collection<T> values = randomAttributeValue();
        Entity entity = getEntity(values);
        persist(entity);
        persist(getEntity(randomAttributeValue()));

        List entities = dbService.createQuery(arrayContainsHql)
                .setParameter("v", Iterables.getFirst(values, null))
                .list();

        assertEquals(1, entities.size());
        assertEquals(entity, entities.get(0));

    }

    @Test
    public void inFilter() {
        Collection<T> values1 = randomAttributeValue();
        Collection<T> values2 = randomAttributeValue();
        Collection<T> values3 = randomAttributeValue();
        Collection<T> values4 = randomAttributeValue();

        Entity entity = getEntity(CrmCollections.union(Set.copyOf(values1), Set.copyOf(values2)));
        persist(entity);
        persist(getEntity(CrmCollections.union(Set.copyOf(values1), Set.copyOf(values2), Set.copyOf(values4))));

        Filter filter = Filters.in(attributeCode,
                CrmCollections.union(Set.copyOf(values1), Set.copyOf(values2), Set.copyOf(values3)));
        Query query = Query.of(fqn).withFilters(filter);
        List<Entity> entities = doInTx(() -> new ArrayList(dbService.list(query)));

        assertEquals(1, entities.size());
        assertEquals(entity, entities.get(0));
    }

    @Test
    public void likeFilter() {
        Object values = randomAttributeValue();
        persist(getEntity(values));
        persist(getEntity(randomAttributeValue()));

        Filter filter = Filters.eq(attributeCode, values);
        Query query = Query.of(fqn).withFilters(filter);
        List<Entity> entities = doInTx(() -> new ArrayList(dbService.list(query)));

        assertEquals(1, entities.size());
        for (var e : entities) {
            Object result = e.getAttribute(attributeCode);
            assertEquals(values, result, "Из базы данных должны получить ранее сохраненное значение");
        }
    }

    @Test
    public void containsAllFilter() {
        Collection<T> values1 = randomAttributeValue();
        Collection<T> values2 = randomAttributeValue();
        Collection<T> values3 = randomAttributeValue();
        Collection<T> values4 = randomAttributeValue();

        Entity entity = getEntity(CrmCollections.union(Set.copyOf(values1), Set.copyOf(values2), Set.copyOf(values3)));
        persist(entity);
        persist(getEntity(CrmCollections.union(Set.copyOf(values1), Set.copyOf(values2), Set.copyOf(values4))));

        Filter filter = Filters.containsAll(attributeCode,
                CrmCollections.union(Set.copyOf(values1), Set.copyOf(values3)));
        Query query = Query.of(fqn).withFilters(filter);
        List<Entity> entities = doInTx(() -> new ArrayList(dbService.list(query)));

        assertEquals(1, entities.size());
        assertEquals(entity, entities.get(0));
    }

    @ParameterizedTest
    @MethodSource(value = "containsAnyFilterCases")
    public void containsAnyFilter(List<Integer> filterIndexes, List<Integer> expectedEntityIndexes) {
        var values = List.of(
                randomAttributeValue(),
                randomAttributeValue(),
                randomAttributeValue(),
                randomAttributeValue()
        );

        var entities = List.of(
                getEntity(CrmCollections.union(Set.copyOf(values.get(0)), Set.copyOf(values.get(1)))),
                getEntity(CrmCollections.union(Set.copyOf(values.get(1)), Set.copyOf(values.get(2))))
        );

        entities.forEach(this::persist);

        var filterValues =
                CrmCollections.union(filterIndexes.stream().map(values::get).map(Set::copyOf).toArray(Set[]::new));
        var filter = Filters.containsAny(attributeCode, filterValues);
        var query = Query.of(fqn).withFilters(filter);
        List<Entity> actualEntities = doInTx(() -> dbService.list(query));
        Set<Entity> expectedEntities = expectedEntityIndexes.stream().map(entities::get).collect(Collectors.toSet());

        assertThat(actualEntities, hasSize(expectedEntityIndexes.size()));
        for (var actualEntity : actualEntities) {
            assertTrue(expectedEntities.contains(actualEntity));
        }
    }

    @Test
    public void containsAnyFilterOnLinkedObject() {
        var value1 = randomAttributeValue();
        var value2 = randomAttributeValue();
        Entity expectedEntity = createPersistedLinkedEntity(CrmCollections.concat(value1, value2));

        Filter filter = Filters.containsAny(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                value2
        );
        List<Entity> results = doInTx(() -> dbService.list(Query.of(linkAttrMetaclass).withFilters(filter)));
        Assertions.assertEquals(1, results.size(), "Должно найтись ровно одно значение");
        Assertions.assertEquals(expectedEntity, results.get(0));
    }

    @Test
    public void containsAllFilterOnLinkedObject() {
        var value1 = randomAttributeValue();
        var value2 = randomAttributeValue();
        Entity expectedEntity = createPersistedLinkedEntity(CrmCollections.concat(value1, value2));
        Entity unexpectedEntity = createPersistedLinkedEntity(value1);

        Filter filter = Filters.containsAll(
                String.format("%1$s@%2$s.%1$s@%3$s.%4$s@%5$s", linkAttrMetaclass, linkAttr1Code, linkAttr2Code, fqn,
                        attributeCode),
                CrmCollections.concat(value1, value2)
        );
        List<Entity> results = doInTx(() -> dbService.list(Query.of(linkAttrMetaclass).withFilters(filter)));
        Assertions.assertEquals(1, results.size(), "Должно найтись ровно одно значение");
        Assertions.assertEquals(expectedEntity, results.get(0));
    }

    @Test
    public void containsAll_runtime() {
        var value1 = randomAttributeValue();
        var value2 = randomAttributeValue();
        var concat = CrmCollections.concat(value1, value2);
        var concat2 = CrmCollections.concat(value1.stream().findFirst().stream().collect(Collectors.toList())
                , value2);

        Filter filter = Filters.containsAll(attributeCode, value1);

        boolean result = doRuntimeFilter(value1, filter);
        Assertions.assertTrue(result);

        result = doRuntimeFilter(concat, filter);
        Assertions.assertTrue(result);

        result = doRuntimeFilter(value2, filter);
        Assertions.assertFalse(result);

        result = doRuntimeFilter(concat2, filter);
        Assertions.assertFalse(result);
    }

    @Test
    public void containsAny_runtime() {
        var value1 = randomAttributeValue();
        var value2 = randomAttributeValue();
        var concat = CrmCollections.concat(value1, value2);
        var concat2 = CrmCollections.concat(value1.stream().findFirst().stream().collect(Collectors.toList())
                , value2);

        Filter filter = Filters.containsAny(attributeCode, value1);

        boolean result = doRuntimeFilter(value1, filter);
        Assertions.assertTrue(result);

        result = doRuntimeFilter(concat, filter);
        Assertions.assertTrue(result);

        result = doRuntimeFilter(value2, filter);
        Assertions.assertFalse(result);

        result = doRuntimeFilter(concat2, filter);
        Assertions.assertTrue(result);
    }

    @Override
    protected Object getNullValue() {
        return Collections.emptyList();
    }

    protected abstract Collection<T> randomAttributeValue();
}
