package ru.yandex.market.deliverycalculator.storage.model.yadelivery.modifier;


import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import ru.yandex.market.deliverycalculator.storage.model.modifier.ValueLimiter;
import ru.yandex.market.deliverycalculator.storage.model.modifier.ValueModificationRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueModificationRuleTest {

    /**
     * Тест для {@link ValueModificationRule#applyFor(Number)}.
     * Применение операции суммирования.
     */
    @Test
    void testApplyFor_operationSum() {
        ValueModificationRule<Double> modificationRule = new ValueModificationRule.Builder<Double>()
                .withOperation(ValueModificationRule.OperationEnum.ADD)
                .withParameter(150.00)
                .build();

        assertEquals(BigDecimal.valueOf(300.00), modificationRule.applyFor(150.00));
    }

    /**
     * Тест для {@link ValueModificationRule#applyFor(Number)}.
     * Применение операции суммирования. На результат определен лимитер. В данном случае, мы вышли за максимальную
     * границу ограничителя. Ограничитель на верхнее значение срабатывает.
     */
    @Test
    void testApplyFor_operationSum_limiterLimitsMax() {
        ValueModificationRule<Double> modificationRule = new ValueModificationRule.Builder<Double>()
                .withOperation(ValueModificationRule.OperationEnum.ADD)
                .withParameter(150.00)
                .withResultLimit(new ValueLimiter<>(null, 298.00))
                .build();

        assertEquals(BigDecimal.valueOf(298.00), modificationRule.applyFor(150.00));
    }

    /**
     * Тест для {@link ValueModificationRule#applyFor(Number)}.
     * Применение операции умножения.
     */
    @Test
    void testApplyFor_operationMultiply() {
        ValueModificationRule<Integer> modificationRule = new ValueModificationRule.Builder<Integer>()
                .withOperation(ValueModificationRule.OperationEnum.MULTIPLY)
                .withParameter(3.00)
                .build();

        assertEquals(BigDecimal.valueOf(30.0), modificationRule.applyFor(10));
    }

    /**
     * Тест для {@link ValueModificationRule#applyFor(Number)}.
     * Применение операции вычитания.
     */
    @Test
    void testApplyFor_operationSubstract() {
        ValueModificationRule<Double> modificationRule = new ValueModificationRule.Builder<Double>()
                .withOperation(ValueModificationRule.OperationEnum.SUBSTRACT)
                .withParameter(2.00)
                .build();

        assertEquals(BigDecimal.valueOf(298.00), modificationRule.applyFor(300.00));
    }

    /**
     * Тест для {@link ValueModificationRule#applyFor(Number)}.
     * Применение операции вычитания.На результат определен лимитер. В данном случае, мы вышли за минимальную
     * границу ограничителя. Ограничитель на нижнее значение срабатывает.
     */
    @Test
    void testApplyFor_operationSubstract_limiterLimitsMin() {
        ValueModificationRule<Double> modificationRule = new ValueModificationRule.Builder<Double>()
                .withOperation(ValueModificationRule.OperationEnum.SUBSTRACT)
                .withParameter(2.00)
                .withResultLimit(new ValueLimiter<>(298.50, 299.00))
                .build();

        assertEquals(BigDecimal.valueOf(298.50), modificationRule.applyFor(300.00));
    }

    /**
     * Тест для {@link ValueModificationRule#applyFor(Number)}.
     * Применение операции деления.
     */
    @Test
    void testApplyFor_operationDivide() {
        ValueModificationRule<Double> modificationRule = new ValueModificationRule.Builder<Double>()
                .withOperation(ValueModificationRule.OperationEnum.DIVIDE)
                .withParameter(3.00)
                .build();

        assertEquals(100.00, modificationRule.applyFor(300.00).doubleValue(), 0.0);
    }

    /**
     * Тест для {@link ValueModificationRule#applyFor(Number)}.
     * Применение операции деления.
     */
    @Test
    void testApplyFor_operationFixed() {
        ValueModificationRule<Double> modificationRule = new ValueModificationRule.Builder<Double>()
                .withOperation(ValueModificationRule.OperationEnum.FIX_VALUE)
                .withParameter(200.00)
                .build();

        assertEquals(BigDecimal.valueOf(200.00), modificationRule.applyFor(300.00));
    }

    /**
     * Тест для {@link ValueModificationRule#applyFor(Number)}.
     * Применение операции "неизвестное значение".
     */
    @Test
    void testApplyFor_operationUnknownValue() {
        ValueModificationRule<Double> modificationRule = new ValueModificationRule.Builder<Double>()
                .withOperation(ValueModificationRule.OperationEnum.UNKNOWN_VALUE)
                .withParameter(200.00)
                .build();

        assertNull(modificationRule.applyFor(300.00));
    }

    /**
     * Тест для {@link ValueModificationRule#applyFor(Number)}.
     * Применение операции на нулловое значение.
     */
    @Test
    void testApplyFor_operationApplyForNullValue() {
        ValueModificationRule<Double> modificationRule = new ValueModificationRule.Builder<Double>()
                .withOperation(ValueModificationRule.OperationEnum.UNKNOWN_VALUE)
                .withParameter(200.00)
                .build();

        assertThrows(RuntimeException.class, () -> modificationRule.applyFor(null));
    }
}
