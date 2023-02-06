package ru.yandex.market.checker.controller;

import java.util.List;
import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.checker.FunctionalTest;
import ru.yandex.market.checker.TestUtils;
import ru.yandex.market.checker.api.model.Entity;
import ru.yandex.market.checker.api.model.EntityRequestBody;
import ru.yandex.market.checker.matchers.EntityMatchers;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.error.info.model.ErrorInfo;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checker.matchers.ErrorInfoMatcher.hasCode;
import static ru.yandex.market.checker.matchers.ErrorInfoMatcher.hasMessage;
import static ru.yandex.market.common.test.spring.FunctionalTestHelper.get;
import static ru.yandex.market.common.test.spring.FunctionalTestHelper.post;

/**
 * Тесты для {@link ru.yandex.market.checker.controller.EntityController}
 */
class EntityControllerTest extends FunctionalTest {

    @DbUnitDataSet(
            before = "entity/getEntities.before.csv",
            after = "entity/getEntities.before.csv"
    )
    @DisplayName("Сортируем по-умолчанию, фильтруем sql-injection")
    @Test
    void test_getAllEntities_shouldReturnAll(
    ) {
        ResponseEntity<String> response = get(baseUrl() + "/entities?page=0&pageSize=100&sortBy=id;drop public.entities&sortType=asc");
        assertOk(response);

        List<Entity> entities = TestUtils.parseListResults(response.getBody(), Entity.class);

        assertEquals(4L, entities.size());
    }

    @DbUnitDataSet(
            before = "entity/getEntities.before.csv",
            after = "entity/getEntities.before.csv"
    )
    @DisplayName("Получаем entity по id")
    @Test
    void test_getById_shouldReturnEntity() {
        ResponseEntity<String> response = get(baseUrl() + "/entities/1");
        assertOk(response);

        Entity entity = TestUtils.parseOneResult(response.getBody(), Entity.class);

        assertThat(entity, allOf(
                EntityMatchers.hasId(1L),
                EntityMatchers.hasName("shipment_1p"),
                EntityMatchers.hasDescription("some 1p shipment info")
        ));
    }

    @DbUnitDataSet(
    after = "entity/createEntity.after.csv"
    )
    @DisplayName("Создание сущности сверки")
    @Test
    void test_createEntity() {
        EntityRequestBody requestBody = new EntityRequestBody()
                .name("new entity")
                .description("some new entity");
        HttpEntity<String> requestEntity = new HttpEntity<>(convertToJson(requestBody), jsonHeaders());
        ResponseEntity<String> response = post(baseUrl() + "/entities", requestEntity);

        assertOk(response);

        Entity resultEntity = TestUtils.parseOneResult(response.getBody(), Entity.class);

        assertThat(resultEntity, allOf(
                EntityMatchers.hasId(1L),
                EntityMatchers.hasName("new entity"),
                EntityMatchers.hasDescription("some new entity")
        ));
    }

    @DbUnitDataSet(
            before = "entity/updateEntity.before.csv",
            after = "entity/updateEntity.after.csv"
    )
    @DisplayName("Обновляем сверку")
    @Test
    void test_updateEntity() {
        EntityRequestBody requestBody = new EntityRequestBody()
                .name("updated entity")
                .description("some updated entity");
        HttpEntity<String> requestEntity = new HttpEntity<>(convertToJson(requestBody), jsonHeaders());
        ResponseEntity<String> response = post(baseUrl() + "/entities/1", requestEntity);

        assertOk(response);

        Entity resultEntity = TestUtils.parseOneResult(response.getBody(), Entity.class);

        assertThat(resultEntity, allOf(
                EntityMatchers.hasId(1L),
                EntityMatchers.hasName("updated entity"),
                EntityMatchers.hasDescription("some updated entity")
        ));
    }

    @DbUnitDataSet(
            before = "entity/updateEntity.before.csv",
            after = "entity/updateEntity.before.csv"
    )
    @DisplayName("Обновляем сверку с ошибкой")
    @Test
    void test_updateEntity_shouldFail() {
        EntityRequestBody requestBody = new EntityRequestBody()
                .name("updated entity")
                .description("some updated entity");
        HttpEntity<String> requestEntity = new HttpEntity<>(convertToJson(requestBody), jsonHeaders());
        Supplier<ResponseEntity<String>> responseSupplier = () -> post(baseUrl() + "/entities/1234234", requestEntity);

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                responseSupplier::get);

        List<ErrorInfo> errors = TestUtils.parseListResults(exception.getResponseBodyAsString(), ErrorInfo.class);
        Assert.assertThat(errors, hasSize(1));
        Assert.assertThat(errors.get(0), Matchers.allOf(
                hasCode("ENTITY_NOT_FOUND"),
                hasMessage("Entity with id 1234234 is not found")
        ));
    }

    @DbUnitDataSet(
            before = "entity/updateEntity.before.csv",
            after = "entity/deleteEntity.after.csv"
    )
    @DisplayName("Успешное удаление существующей записи")
    @Test
    void test_deleteEntity() {
        ResponseEntity<String> response = post(baseUrl() + "/entities/1/delete");
        assertOk(response);

    }

    @DbUnitDataSet(
            before = "entity/updateEntity.before.csv",
            after = "entity/updateEntity.before.csv"
    )
    @DisplayName("Ошибка при удалении несуществующей сущности")
    @Test
    void test_deleteEntity_shouldFail() {
        Supplier<ResponseEntity<String>> responseSupplier = () -> post(baseUrl() + "/entities/13784634/delete");

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                responseSupplier::get);

        List<ErrorInfo> errors = TestUtils.parseListResults(exception.getResponseBodyAsString(), ErrorInfo.class);
        Assert.assertThat(errors, hasSize(1));
        Assert.assertThat(errors.get(0), Matchers.allOf(
                hasCode("ENTITY_NOT_FOUND"),
                hasMessage("Entity with id 13784634 is not found")
        ));
    }
}
