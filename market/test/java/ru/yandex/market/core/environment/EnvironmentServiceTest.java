package ru.yandex.market.core.environment;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.mbi.environment.EnvironmentService;

/**
 * Тесты для {@link DBEnvironmentService}.
 *
 * @author belmatter
 */
class EnvironmentServiceTest extends FunctionalTest {

    @Autowired
    @Qualifier("dbEnvironmentService")
    private EnvironmentService environmentService;

    @Test
    @DisplayName("Получение значений по ключу")
    @DbUnitDataSet(before = "EnvironmentServiceTest.before.csv")
    void getValuesTest() {
        List<String> values = environmentService.getValues("environment.test.first.name", List.of());
        Assertions.assertThat(values)
                .containsExactlyInAnyOrder("correct.value1", "correct.value2");
    }

    @Test
    @DisplayName("Вставка 1 нового значения")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.addValueTest.after.csv"
    )
    void addValueTest() {
        environmentService.addValue("environment.test.first.name", "new.correct.value1");
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

    @Test
    @DisplayName("Установка 1 нового значения (замена старых новым)")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.setValueTest.after.csv"
    )
    void setValueTest() {
        environmentService.setValue("environment.test.first.name", "new.correct.value1");
    }

    @Test
    @DisplayName("Установка нескольких новых значений (замена старых новыми)")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.setValuesTest.after.csv"
    )
    void setValuesTest() {
        List<String> newValues = List.of("new.correct.value1", "new.correct.value2");
        environmentService.setValues("environment.test.first.name", newValues);
    }

    @Test
    @DisplayName("Удаление строк по паре ключ+значение")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.removeValuesTest.after.csv"
    )
    void removeValuesTest() {
        environmentService.removeValue("environment.test.first.name", "correct.value2");
    }

    @Test
    @DisplayName("Получение Instant по ключу без дефолтного значения")
    @DbUnitDataSet(before = "EnvironmentServiceTest.before.csv")
    void getInstantTest() {
        Instant dateTime = environmentService.getInstant("environment.test.date.name");
        Assertions.assertThat(dateTime)
                .isEqualTo(Instant.parse("2019-09-11T01:01:01Z"));
    }

    @Test
    @DisplayName("Получение Instant по ключу с дефолтным значением")
    @DbUnitDataSet(before = "EnvironmentServiceTest.before.csv")
    void getInstantWithDefaultTest() {
        Instant defaultValue = Instant.now();
        Instant dateTime = environmentService.getInstant("environment.test.first.name", defaultValue);
        Assertions.assertThat(dateTime)
                .isEqualTo(defaultValue);
        dateTime = environmentService.getInstant("environment.test.notfound", defaultValue);
        Assertions.assertThat(dateTime)
                .isEqualTo(defaultValue);
    }

    @Test
    @DisplayName("Сохранение Instant")
    @DbUnitDataSet(after = "EnvironmentServiceTest.after.csv")
    void saveInstantTest() {
        Instant newValue = Instant.parse("2020-01-01T01:01:01Z");
        environmentService.setInstant("environment.test.date.name", newValue);
        Instant dateTime = environmentService.getInstant("environment.test.date.name");
        Assertions.assertThat(dateTime)
                .isEqualTo(newValue);
    }

    @Test
    @DisplayName("Удаление всех значений по ключу")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.removeAll.after.csv"
    )
    void testRemoveAll() {
        String key = "environment.test.remove.all.values";
        environmentService.removeAllValues(key);
    }

    @Test
    @DisplayName("Установка пустых значений")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.setEmpty.after.csv"
    )
    void testSetEmptyValues() {
        String key = "environment.test.first.name";
        environmentService.setValues(key, List.of("", ""));
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
    @DisplayName("Установка null-значений")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.setNull.after.csv"
    )
    void testSetNullValues() {
        String key = "environment.test.remove.all.values";
        ArrayList<String> values = new ArrayList<>(2);
        values.add(null);
        values.add(null);
        environmentService.setValues(key, values);
    }

    @Test
    @DisplayName("Установка одного null-значения")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.setNull.after.csv"
    )
    void testSetOneNullValue() {
        String key = "environment.test.remove.all.values";
        environmentService.setValue(key, null);
    }

    @Test
    @DisplayName("Двойная установка пустых значений")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.setEmpty.after.csv"
    )
    void testDoubleSetEmptyValues() {
        String key = "environment.test.first.name";
        environmentService.setValues(key, List.of("", ""));
        environmentService.setValues(key, List.of("", ""));
        Optional<String> result = environmentService.getValue(key);
        Assertions.assertThat(result.isEmpty())
                .isEqualTo(true);
    }

    @Test
    @DisplayName("Двойная установка null-значений")
    @DbUnitDataSet(
            before = "EnvironmentServiceTest.before.csv",
            after = "EnvironmentServiceTest.setNull.after.csv"
    )
    void testDoubleSetNullValues() {
        String key = "environment.test.remove.all.values";
        ArrayList<String> values = new ArrayList<>(2);
        values.add(null);
        values.add(null);
        environmentService.setValues(key, values);
        environmentService.setValues(key, values);
        Optional<String> result = environmentService.getValue(key);
        Assertions.assertThat(result.isEmpty())
                .isEqualTo(true);
    }
}
