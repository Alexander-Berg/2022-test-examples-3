package ru.yandex.market.mbo.mdm.common.rsl.validation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.rsl.RslExcelRowData;
import ru.yandex.market.mbo.mdm.common.rsl.RslType;

public class RslThresholdsValidatorTest {

    RslThresholdsValidator rslThresholdsValidator = new RslThresholdsValidator();

    @Test
    public void testWhenCorrectWithSupplierKeyShouldPass() {

        List<RslExcelRowData> rows = List.of(
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("0 дней").setEndOfThreshold("15 дней")
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(1L)
                .setSupplierId(42)
                .setRowNumber(1),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("15 дней").setEndOfThreshold("1 месяц")
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(1L)
                .setSupplierId(42)
                .setRowNumber(2),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 месяц").setEndOfThreshold("3 месяца")
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(1L)
                .setSupplierId(42)
                .setRowNumber(3),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("3 месяца").setEndOfThreshold("1 год")
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(1L)
                .setSupplierId(42)
                .setRowNumber(4),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 год").setEndOfThreshold("не ограничен")
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(1L)
                .setSupplierId(42)
                .setRowNumber(5),

        new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("0 дней").setEndOfThreshold("15 дней")
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(2L)
                .setSupplierId(42)
                .setRowNumber(6),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("15 дней").setEndOfThreshold("1 месяц")
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(2L)
                .setSupplierId(42)
                .setRowNumber(7),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 месяц").setEndOfThreshold("3 месяца")
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(2L)
                .setSupplierId(42)
                .setRowNumber(8),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("3 месяца").setEndOfThreshold("1 год")
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(2L)
                .setSupplierId(42)
                .setRowNumber(9),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 год").setEndOfThreshold("не ограничен")
                .setType("FIRST_PARTY")
                .setRealId("kek")
                .setCategoryId(2L)
                .setSupplierId(42)
                .setRowNumber(10)
        );
        List<RslValidationError> errors = rslThresholdsValidator.validate(rows,
            Collections.singletonList(RslType.FIRST_PARTY));
        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testWhenCorrectWithGlobalTypeShouldPass() {
        List<RslExcelRowData> rows = List.of(
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("0 дней").setEndOfThreshold("15 дней")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(1),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("15 дней").setEndOfThreshold("1 месяц")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(2),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 месяц").setEndOfThreshold("3 месяца")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(3),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("3 месяца").setEndOfThreshold("1 год")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(4),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 год").setEndOfThreshold("не ограничен")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(5),

        new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("0 дней").setEndOfThreshold("15 дней")
                .setType("GLOBAL_THIRD_PARTY")
                .setRowNumber(6),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("15 дней").setEndOfThreshold("1 месяц")
                .setType("GLOBAL_THIRD_PARTY")
                .setRowNumber(7),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 месяц").setEndOfThreshold("3 месяца")
                .setType("GLOBAL_THIRD_PARTY")
                .setRowNumber(8),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("3 месяца").setEndOfThreshold("1 год")
                .setType("GLOBAL_THIRD_PARTY")
                .setRowNumber(9),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 год").setEndOfThreshold("не ограничен")
                .setType("GLOBAL_THIRD_PARTY")
                .setRowNumber(10)
        );
        List<RslValidationError> errors = rslThresholdsValidator.validate(rows,
            List.of(RslType.GLOBAL_FIRST_PARTY, RslType.GLOBAL_THIRD_PARTY));
        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testWhenIncorrectFirstAndLastLinesShouldFail() {
        List<RslExcelRowData> rows = List.of(
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("0 дней").setEndOfThreshold("10 дней")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(1),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("15 дней").setEndOfThreshold("1 месяц")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(2),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 месяц").setEndOfThreshold("3 месяца")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(3),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("3 месяца").setEndOfThreshold("1 год")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(4),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 год").setEndOfThreshold("не ограничен")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(5),

            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("0 дней").setEndOfThreshold("15 дней")
                .setType("GLOBAL_THIRD_PARTY")
                .setRowNumber(6),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("15 дней").setEndOfThreshold("7 дней")
                .setType("GLOBAL_THIRD_PARTY")
                .setRowNumber(7),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("7 дней").setEndOfThreshold("3 месяца")
                .setType("GLOBAL_THIRD_PARTY")
                .setRowNumber(8),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("3 месяца").setEndOfThreshold("1 год")
                .setType("GLOBAL_THIRD_PARTY")
                .setRowNumber(9),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 год").setEndOfThreshold("2 дня")
                .setType("GLOBAL_THIRD_PARTY")
                .setRowNumber(10)
        );
        List<RslValidationError> errors = rslThresholdsValidator.validate(rows,
            List.of(RslType.GLOBAL_FIRST_PARTY, RslType.GLOBAL_THIRD_PARTY));
        Assertions.assertThat(errors.stream().map(RslValidationError::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                "Правый порог в строке 1 отличается от левого в строке 2 ",
                "На строке 7 начало порога ОСГ больше окончания ",
                "На строке 10 начало порога ОСГ больше окончания "
            );
    }

    @Test
    public void testWhenIncorrectFirstLineShouldFailAccordingly() {
        List<RslExcelRowData> rows = List.of(
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("20 дней").setEndOfThreshold("15 дней")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(1),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("15 дней").setEndOfThreshold("1 месяц")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(2),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 месяц").setEndOfThreshold("3 месяца")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(3),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("3 месяца").setEndOfThreshold("1 год")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(4),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 год").setEndOfThreshold("не ограничен")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(5)
        );
        List<RslValidationError> errors = rslThresholdsValidator.validate(rows,
            List.of(RslType.GLOBAL_FIRST_PARTY, RslType.GLOBAL_THIRD_PARTY));
        Assertions.assertThat(errors.stream().map(RslValidationError::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("На строке 1 начало порога ОСГ больше окончания ");
    }

    @Test
    public void testWhenEmptyLineShouldFailEarly() {
        List<RslExcelRowData> rows = List.of(
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("20 дней").setEndOfThreshold("1 месяц")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(1),
            new RslExcelRowData().setCargoType750(true)
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(2),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 месяц").setEndOfThreshold("3 месяца")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(3),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("3 месяца").setEndOfThreshold("1 год")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(4),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 год").setEndOfThreshold("не ограничен")
                .setType("GLOBAL_FIRST_PARTY")
                .setRowNumber(5)
        );
        List<RslValidationError> errors = rslThresholdsValidator.validate(rows,
            List.of(RslType.GLOBAL_FIRST_PARTY, RslType.GLOBAL_THIRD_PARTY));
        Assertions.assertThat(errors.stream().map(RslValidationError::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("На строке 2 не указаны начало или конец порога использования ОСГ ");
    }
}
