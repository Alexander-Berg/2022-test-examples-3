package ru.yandex.market.mbo.mdm.common.rsl.validation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.rsl.RslExcelRowData;
import ru.yandex.market.mbo.mdm.common.rsl.RslType;

public class RslDuplicatesValidatorTest {

    RslDuplicatesValidator rslDuplicatesValidator = new RslDuplicatesValidator();

    @Test
    public void testWhenCorrectShouldPass() {
        List<RslExcelRowData> rows = List.of(
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("0 дня").setEndOfThreshold("15 дней")
                .setRealId("kek").setType("FIRST_PARTY")
                .setCategoryId(1L)
                .setInRslDays(1).setOutRslDays(1)
                .setRowNumber(1),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("15 дней").setEndOfThreshold("1 месяц")
                .setRealId("kek").setType("FIRST_PARTY")
                .setCategoryId(2L)
                .setInRslDays(3).setOutRslDays(4)
                .setRowNumber(2),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("1 месяц").setEndOfThreshold("3 месяца")
                .setRealId("kek").setType("FIRST_PARTY")
                .setCategoryId(2L)
                .setInRslDays(1).setOutRslDays(1)
                .setRowNumber(3),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("3 месяца").setEndOfThreshold("1 год")
                .setRealId("kek").setType("FIRST_PARTY")
                .setCategoryId(3L)
                .setInRslDays(1).setOutRslDays(1)
                .setRowNumber(4)
        );
       List<RslValidationError> errors = rslDuplicatesValidator.validate(rows,
           Collections.singletonList(RslType.FIRST_PARTY));
        Assertions.assertThat(errors).isEmpty();

    }

    @Test
    public void testWhenIncorrectShouldFail() {
        List<RslExcelRowData> rows = List.of(
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("2 дня").setEndOfThreshold("4 месяца")
                .setRealId("kek").setType("FIRST_PARTY")
                .setCategoryId(1L)
                .setInRslDays(1).setOutRslDays(1)
                .setRowNumber(1),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("2 дня").setEndOfThreshold("4 месяца")
                .setRealId("kek").setType("FIRST_PARTY")
                .setCategoryId(1L)
                .setInRslDays(1).setOutRslDays(1)
                .setRowNumber(2),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("2 дня").setEndOfThreshold("4 месяца")
                .setRealId("kek").setType("FIRST_PARTY")
                .setCategoryId(2L)
                .setInRslDays(1).setOutRslDays(1)
                .setRowNumber(3),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("2 дня").setEndOfThreshold("4 месяца")
                .setRealId("kek").setType("FIRST_PARTY")
                .setCategoryId(3L)
                .setInRslDays(1).setOutRslDays(1)
                .setRowNumber(4),
            new RslExcelRowData().setCargoType750(true)
                .setBeginningOfThreshold("2 дня").setEndOfThreshold("4 месяца")
                .setRealId("kek").setType("FIRST_PARTY")
                .setCategoryId(3L)
                .setInRslDays(1).setOutRslDays(1)
                .setRowNumber(5)
        );

        List<RslValidationError> errors = rslDuplicatesValidator.validate(rows,
            Collections.singletonList(RslType.FIRST_PARTY));

        Assertions.assertThat(errors.stream().map(RslValidationError::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
            "Дублирующаяся запись ОСГ, строка: 4",
            "Дублирующаяся запись ОСГ, строка: 5",
            "Дублирующаяся запись ОСГ, строка: 1",
            "Дублирующаяся запись ОСГ, строка: 2");
    }
}
