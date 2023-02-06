package ru.yandex.market.checker.db;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checker.FunctionalTest;
import ru.yandex.market.checker.api.model.Entity;
import ru.yandex.market.checker.api.model.EntityRequestBody;
import ru.yandex.market.checker.matchers.EntityMatchers;
import ru.yandex.market.checker.model.SortType;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ParametersAreNonnullByDefault
class EntityDaoTest extends FunctionalTest {

    @Autowired
    private EntityDao entityDao;

    @Test
    @DbUnitDataSet(before = "getEntities.before.csv")
    @DisplayName("Получить первую страницу записей и проверить сортировку по id")
    void test_getFirstPage() {
        List<Entity> entitiesAsc = entityDao.getEntities(0, 50, SortType.ASC, "id");
        assertEquals(entitiesAsc.size(), 3);
        assertEquals(entitiesAsc.get(0).getId(), 1L);
        assertEquals(entitiesAsc.get(2).getId(), 3L);

        List<Entity> entitiesDesc = entityDao.getEntities(0, 50, SortType.DESC, "id");
        assertEquals(entitiesDesc.get(0).getId(), 3L);
        assertEquals(entitiesDesc.get(2).getId(), 1L);
    }

    @Test
    @DbUnitDataSet(
            after = "createEntity.after.csv"
    )
    @DisplayName("Создать сущность")
    void test_createEntity() {
        Entity entity = entityDao.createOrUpdateEntity(
                null,
                new EntityRequestBody().name("test").description("description")
        );
        assertThat(entity, allOf(
                EntityMatchers.hasId(1L),
                EntityMatchers.hasName("test"),
                EntityMatchers.hasDescription("description")
            )
        );
    }

    @Test
    @DbUnitDataSet(
            before = "updateEntity.before.csv",
            after = "createEntity.after.csv"
    )
    @DisplayName("Обновить сущность")
    void test_updateEntity() {
        Entity entity = entityDao.createOrUpdateEntity(
                1L,
                new EntityRequestBody().name("test").description("description")
        );
        assertThat(entity, allOf(
                EntityMatchers.hasId(1L),
                EntityMatchers.hasName("test"),
                EntityMatchers.hasDescription("description")
                )
        );
    }
}
