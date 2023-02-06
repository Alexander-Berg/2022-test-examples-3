package ru.yandex.market.mbi.bpmn.function;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.pvm.runtime.ExecutionImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.bpmn.FunctionalTest;

/**
 * Тесты для {@link StatisticsCollector}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class StatisticsCollectorTest extends FunctionalTest {

    @Autowired
    private StatisticsCollector statisticsCollector;

    @Test
    @DisplayName("Получить статистику, когда ее нет. Вернется null")
    void getStats_notExists_returnNull() {
        VariableScope scope = new ExecutionImpl();
        Map<String, Object> actualStats = statisticsCollector.getStats(scope);
        Assertions.assertNull(actualStats);
    }

    @Test
    @DisplayName("Получить статистику, когда в ней лежит не Map. Вернется null")
    void getStats_notMap_returnNull() {
        VariableScope entity = new ExecutionImpl();
        entity.setVariable(StatisticsCollector.STATISTICS_PARAM, "abc");
        Map<String, Object> actualStats = statisticsCollector.getStats(entity);
        Assertions.assertNull(actualStats);
    }

    @Test
    @DisplayName("Получить статистику, когда в ней лежит Map. Вернется значение из переменное")
    void getStats_correctData_returnSuccess() {
        VariableScope entity = new ExecutionImpl();
        Map<String, Object> data = Map.of(
                "key1", "val123",
                "key2", Set.of("11", "22"),
                "key3", Map.of("innerKey", "innerValue")
        );
        entity.setVariable(StatisticsCollector.STATISTICS_PARAM, data);
        Map<String, Object> actualStats = statisticsCollector.getStats(entity);
        Assertions.assertEquals(data, actualStats);
    }

    @Test
    @DisplayName("Положить одно значение в корень. Успешное сохранение")
    void put_firstValue_success() {
        VariableScope scope = new ExecutionImpl();
        statisticsCollector.put(scope, "key1", "val1");

        Map<String, Object> actualStats = statisticsCollector.getStats(scope);

        Map<String, Object> expected = Map.of(
                "key1", "val1"
        );
        Assertions.assertEquals(expected, actualStats);
    }

    @Test
    @DisplayName("Положить два значение в корень. Оба сохраняются")
    void put_twoValues_success() {
        VariableScope scope = new ExecutionImpl();
        statisticsCollector.put(scope, "key1", "val1");
        statisticsCollector.put(scope, "key2", "val2");

        Map<String, Object> actualStats = statisticsCollector.getStats(scope);

        Map<String, Object> expected = Map.of(
                "key1", "val1",
                "key2", "val2"
        );
        Assertions.assertEquals(expected, actualStats);
    }

    @Test
    @DisplayName("Перезаписать значение, которое уже есть в статистике")
    void replaceValue_exists_success() {
        VariableScope scope = new ExecutionImpl();
        Map<String, Object> data = new HashMap<>() {{
            put("key1", "val1");
            put("key2", "val2");
        }};
        scope.setVariable(StatisticsCollector.STATISTICS_PARAM, data);

        statisticsCollector.put(scope, "key1", "new_value");

        Map<String, Object> actualStats = statisticsCollector.getStats(scope);

        Map<String, Object> expected = Map.of(
                "key1", "new_value",
                "key2", "val2"
        );
        Assertions.assertEquals(expected, actualStats);
    }

    @Test
    @DisplayName("Сохранить вложенное значение")
    void addInnerValue_empty_success() {
        VariableScope scope = new ExecutionImpl();

        statisticsCollector.put(scope, "key1/key2/key3", "value");

        Map<String, Object> actualStats = statisticsCollector.getStats(scope);

        Map<String, Object> expected = Map.of(
                "key1", Map.of("key2", Map.of("key3", "value"))
        );
        Assertions.assertEquals(expected, actualStats);
    }

    @Test
    @DisplayName("Уже есть вложенное значение. Хотим добавить еще одно значение в лист")
    void addInnerValue_exists_success() {
        VariableScope scope = new ExecutionImpl();
        Map<String, Object> data = new HashMap<>() {{
            put("key1", new HashMap<>() {{
                put("key2", new HashMap<>() {{
                    put("key3", "value");
                }});
            }});
        }};
        scope.setVariable(StatisticsCollector.STATISTICS_PARAM, data);

        statisticsCollector.put(scope, "key1/key2/key4", "value2");

        Map<String, Object> actualStats = statisticsCollector.getStats(scope);

        Map<String, Object> expected = Map.of(
                "key1", Map.of("key2", Map.of("key3", "value", "key4", "value2"))
        );
        Assertions.assertEquals(expected, actualStats);
    }

    @Test
    @DisplayName("Уже есть вложенное значение. Хотим заменить значение в листе")
    void replaceInnerValue_exists_success() {
        VariableScope scope = new ExecutionImpl();
        Map<String, Object> data = new HashMap<>() {{
            put("key1", new HashMap<>() {{
                put("key2", new HashMap<>() {{
                    put("key3", "value");
                }});
            }});
        }};
        scope.setVariable(StatisticsCollector.STATISTICS_PARAM, data);

        statisticsCollector.put(scope, "key1/key2/key3", "new_value");

        Map<String, Object> actualStats = statisticsCollector.getStats(scope);

        Map<String, Object> expected = Map.of(
                "key1", Map.of("key2", Map.of("key3", "new_value"))
        );
        Assertions.assertEquals(expected, actualStats);
    }

    @Test
    @DisplayName("Есть вложенное значение. Хотим добавить дочерние, которых еще нет")
    void addInnerValue_notExists_success() {
        VariableScope scope = new ExecutionImpl();
        Map<String, Object> data = new HashMap<>() {{
            put("key1", new HashMap<>() {{
                put("key2", "val2");
            }});
        }};
        scope.setVariable(StatisticsCollector.STATISTICS_PARAM, data);

        statisticsCollector.put(scope, "key1/key2/key3", "new_value");

        Map<String, Object> actualStats = statisticsCollector.getStats(scope);

        Map<String, Object> expected = Map.of(
                "key1", Map.of("key2", Map.of("key3", "new_value"))
        );
        Assertions.assertEquals(expected, actualStats);
    }

    @Test
    @DisplayName("Сохраняем мапу в статистику. Листовая нода")
    void addMap_leaf_success() {
        VariableScope scope = new ExecutionImpl();

        Map<String, Object> data = new HashMap<>() {{
            put("innerKey1", "val1");
            put("innerKey2", "val2");
        }};

        statisticsCollector.put(scope, "key1/key2", data);

        Map<String, Object> actualStats = statisticsCollector.getStats(scope);

        Map<String, Object> expected = Map.of(
                "key1", Map.of("key2", Map.of("innerKey1", "val1", "innerKey2", "val2"))
        );
        Assertions.assertEquals(expected, actualStats);
    }

    @Test
    @DisplayName("Сохраняем мапу в статистику. Корень")
    void addMap_root_success() {
        VariableScope scope = new ExecutionImpl();

        Map<String, Object> data = new HashMap<>() {{
            put("innerKey1", "val1");
            put("innerKey2", "val2");
        }};

        statisticsCollector.put(scope, "key1", data);

        Map<String, Object> actualStats = statisticsCollector.getStats(scope);

        Map<String, Object> expected = Map.of(
                "key1", Map.of("innerKey1", "val1", "innerKey2", "val2")
        );
        Assertions.assertEquals(expected, actualStats);
    }
}
