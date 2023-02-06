package ru.yandex.market.partner.notification.environment;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;

/**
 * Тесты для {@link ru.yandex.market.partner.notification.environment.EnvironmentService}.
 */
class EnvironmentServiceTest extends AbstractFunctionalTest {

    @Autowired
    private EnvironmentService environmentService;

    @Test
    @DisplayName("Вставка 1 нового значения")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.addValueTest.after.csv"
    )
    void addValueTest() {
        environmentService.setValue("environment.test.new.name", "new.correct.value1");
    }

    @Test
    @DisplayName("Установка 1 нового значения (замена старого новым)")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.setValueTest.after.csv"
    )
    void setValueTest() {
        environmentService.setValue("environment.test.first.name", "new.correct.value1");
    }

    @Test
    @DisplayName("Удаление всех значений по ключу")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.removeAll.after.csv"
    )
    void testRemoveAll() {
        String key = "environment.test.remove.all.values";
        environmentService.deleteVariable(key);
    }

    @Test
    @DisplayName("Установка одного пустого значения")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.setEmpty.after.csv"
    )
    void testSetOneEmptyValue() {
        String key = "environment.test.first.name";
        environmentService.setValue(key, "");
    }

    @Test
    @DisplayName("Получение значения по ключу")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv"
    )
    void getByName() {
        Assertions.assertEquals("correct.value1",
                environmentService.getValue("environment.test.first.name"));
        Assertions.assertEquals("correct.value3",
                environmentService.getValue("environment.test.second.name"));
        Assertions.assertEquals("2019-09-11T01:01:01.0Z",
                environmentService.getValue("environment.test.date.name"));
        Assertions.assertEquals(true, environmentService.
                getBooleanValue("environment.test.boolean.name"));
        Assertions.assertEquals(null, environmentService.
                getValue("environment.test.non.existent.name"));
    }

    @Test
    @DisplayName("Получение значений по ключу")
    @DbUnitDataSet(before = "EnvironmentServiceTest.getAll.before.csv")
    void getAll() {
        List<String> values = environmentService.getValues("environment.test.first.name", List.of());
        org.assertj.core.api.Assertions.assertThat(values)
                .containsExactlyInAnyOrder("correct.value1", "correct.value2", "correct.value3");
    }

    @Test
    @DisplayName("Вставка нескольких новых значений")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.addValuesTest.after.csv"
    )
    void addValuesTest() {
        List<String> newValues = List.of("new.correct.value1", "new.correct.value2");
        environmentService.addValues("environment.test.first.name", newValues);
    }
}
