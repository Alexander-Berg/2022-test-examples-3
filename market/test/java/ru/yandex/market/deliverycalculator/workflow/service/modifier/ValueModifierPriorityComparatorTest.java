package ru.yandex.market.deliverycalculator.workflow.service.modifier;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.deliverycalculator.storage.model.modifier.DeliveryModifiersMeta.ValueModifierMeta;
import ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule.OperationEnum.ADD;
import static ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule.OperationEnum.FIX_VALUE;
import static ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule.OperationEnum.MULTIPLY;
import static ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule.OperationEnum.SUBSTRACT;
import static ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule.OperationEnum.UNKNOWN_VALUE;

/**
 * Тест для {@link ValueModifierPriorityComparator}.
 */
class ValueModifierPriorityComparatorTest {

    private final ValueModifierPriorityComparator<Double> tested = new ValueModifierPriorityComparator<>(150.00);

    /**
     * Тест для {@link ValueModifierPriorityComparator#compare(ValueModifierMeta, ValueModifierMeta)}.
     *
     * @param modifier1      - первый сравниваемый модификатор
     * @param modifier2      - второй сравниваемый модификатор
     * @param expectedResult - ожидаемый результат сравнения.
     */
    @ParameterizedTest
    @MethodSource("argumentsForComparison")
    void testComparison(ValueModifierMeta<Double> modifier1,
                        ValueModifierMeta<Double> modifier2,
                        int expectedResult) {
        assertEquals(expectedResult, tested.compare(modifier1, modifier2));
    }

    static Stream<Arguments> argumentsForComparison() {
        return Stream.of(
                // Первый модификатор приоритетнее второго
                Arguments.of(new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(3)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(ADD)
                                        .withParameter(100.00)
                                        .build())
                                .build(),
                        new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(4)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(MULTIPLY)
                                        .withParameter(2.0)
                                        .build())
                                .build(),
                        -1),
                // Второй модификатор приоритетнее первого
                Arguments.of(new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(4)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(ADD)
                                        .withParameter(100.00)
                                        .build())
                                .build(),
                        new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(3)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(MULTIPLY)
                                        .withParameter(2.0)
                                        .build())
                                .build(),
                        1),
                // Второй модификатор приводит к большему изменению значения. Приоритет одинаковый.
                Arguments.of(new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(4)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(ADD)
                                        .withParameter(100.00)
                                        .build())
                                .build(),
                        new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(4)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(MULTIPLY)
                                        .withParameter(2.0)
                                        .build())
                                .build(),
                        1),
                // Второй модификатор приводит к большему изменению значения. Приоритет одинаковый.
                Arguments.of(new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(4)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(ADD)
                                        .withParameter(100.00)
                                        .build())
                                .build(),
                        new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(4)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(FIX_VALUE)
                                        .withParameter(400.00)
                                        .build())
                                .build(),
                        1),
                // Второй модификатор приводит к неизвестному значени. Приоритет одинаковы
                Arguments.of(new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(4)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(ADD)
                                        .withParameter(100.00)
                                        .build())
                                .build(),
                        new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(4)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(UNKNOWN_VALUE)
                                        .build())
                                .build(),
                        -1),
                // Второй модификатор уменьшает значение. Приоритет одинаковый
                Arguments.of(new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(4)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(ADD)
                                        .withParameter(100.00)
                                        .build())
                                .build(),
                        new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(4)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(SUBSTRACT)
                                        .withParameter(100.0)
                                        .build())
                                .build(),
                        -1),
                // Приоритет одинаковый. Оба модификатора приводят к одному и тому же модифицированному значению.
                // Но приоритет считается большим у операции умножения.
                Arguments.of(new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(4)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(ADD)
                                        .withParameter(150.00)
                                        .build())
                                .build(),
                        new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(4)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(MULTIPLY)
                                        .withParameter(2.0)
                                        .build())
                                .build(),
                        1),
                // Приоритет одинаковый. Оба модификатора приводят к одному и тому же модифицированному значению.
                // У операций тоже оинаковый приоритет.
                Arguments.of(new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(4)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(ADD)
                                        .withParameter(150.00)
                                        .build())
                                .build(),
                        new ValueModifierMeta.Builder<Double>()
                                .withId(1L)
                                .withPriority(4)
                                .withModificationRule(new ValueModificationRule.Builder<Double>()
                                        .withOperation(ADD)
                                        .withParameter(150.00)
                                        .build())
                                .build(),
                        0)
        );
    }
}
