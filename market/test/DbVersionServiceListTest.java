package ru.yandex.market.jmf.logic.def.test;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.jmf.entity.Entity;
import ru.yandex.market.jmf.entity.HasGid;
import ru.yandex.market.jmf.entity.query.Filters;
import ru.yandex.market.jmf.entity.query.Query;

public class DbVersionServiceListTest extends AbstractDbVersionServiceTest {
    private Query query;

    @BeforeEach
    void setUpList() {
        query = Query.of(DUMMY_FQN)
                .withFilters(Filters.in(HasGid.GID, Arrays.asList(entityGidsArray)));
    }

    @Test
    void testListDefault() {
        List<Entity> entities = dbService.list(query);
        Assertions.assertEquals(entityValuesArray.length, entities.size(),
                "Текущее количество сущностей не верно");

        assertEqualsEntities(entities, values -> values.lastEditValue, "Текущее значение сущности не верно");
    }

    @Test
    void testListBefore() {
        List<Entity> entities = dbVersionService.list(query, BEFORE_TIME);
        Assertions.assertEquals(0, entities.size(), "Сущности еще не были созданы");
    }

    @Test
    void testListAfter() {
        List<Entity> entities = dbVersionService.list(query, AFTER_TIME);
        assertEqualsEntities(entities, values -> values.lastEditValue, "Последнее значение не верно");
    }

    @Test
    void testListAtCreateTime() {
        testList(CREATE_TIME, values -> values.createValue, true);
    }

    @Test
    void testListAtFirstEditTime() {
        testList(FIRST_EDIT_TIME, values -> values.firstEditValue, false);
    }

    @Test
    void testListAtLastEditTime() {
        testList(LAST_EDIT_TIME, values -> values.lastEditValue, false);
    }

    void testList(OffsetDateTime atTime,
                  Function<EntityValues, String> getExpectedValueFunction,
                  Boolean expectedZeroEntities) {
        List<Entity> beforeTimeEntities = dbVersionService.list(query, atTime.minusHours(1));
        if (expectedZeroEntities) {
            Assertions.assertEquals(0, beforeTimeEntities.size(), "Сущности еще не были созданы");
        } else {
            assertNotEqualsEntities(beforeTimeEntities, getExpectedValueFunction,
                    "Значение перед редактированием не верно");
        }

        List<Entity> atTimeEntities = dbVersionService.list(query, atTime);
        assertEqualsEntities(atTimeEntities, getExpectedValueFunction, "Значение в момент редактирования не верно");

        List<Entity> afterTimeEntities = dbVersionService.list(query, atTime.plusHours(1));
        assertEqualsEntities(afterTimeEntities, getExpectedValueFunction, "Значение после редактирования не верно");
    }

    private void assertEqualsEntities(Collection<Entity> entities,
                                      Function<EntityValues, String> getExpectedValueFunction,
                                      String message) {
        for (int i = 0; i < entityValuesArray.length; i++) {
            String gid = entityGidsArray[i];
            EntityValues values = entityValuesArray[i];
            Entity entity = findByGid(gid, entities);
            Assertions.assertEquals(
                    getExpectedValueFunction.apply(values),
                    entity.getAttribute(VERSIONED_STRING_ATTRIBUTE),
                    message);
        }
    }

    private void assertNotEqualsEntities(Collection<Entity> entities,
                                         Function<EntityValues, String> getExpectedValueFunction,
                                         String message) {
        for (int i = 0; i < entityValuesArray.length; i++) {
            String gid = entityGidsArray[i];
            EntityValues values = entityValuesArray[i];
            Entity entity = findByGid(gid, entities);
            Assertions.assertNotEquals(
                    getExpectedValueFunction.apply(values),
                    entity.getAttribute(VERSIONED_STRING_ATTRIBUTE),
                    message);
        }
    }

    private Entity findByGid(String gid, Collection<Entity> entity) {
        return entity.stream().filter(e -> gid.equals(e.getGid())).findFirst().orElseThrow();
    }
}
