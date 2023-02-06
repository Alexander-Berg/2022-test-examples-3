package ru.yandex.market.mbo.mdm.common.rsl.validation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.Rsl;
import ru.yandex.market.mbo.mdm.common.rsl.RslExcelRowData;
import ru.yandex.market.mbo.mdm.common.rsl.RslType;

public class RslRowValidatorTest {

    RslRowValidator rslRowValidator = new RslRowValidator();

    @Test
    public void testWhenCorrectShouldPass() {
        List<RslExcelRowData> rows = List.of(
            new RslExcelRowData().setCategoryId(1L).setInRslDays(1).setOutRslDays(1).setRowNumber(1)
                .setStartDate(Rsl.DEFAULT_START_DATE),
            new RslExcelRowData().setCategoryId(2L).setInRslDays(1).setOutRslDays(1).setRowNumber(2)
                .setStartDate(Rsl.DEFAULT_START_DATE),
            new RslExcelRowData().setCategoryId(3L).setInRslDays(1).setOutRslDays(1).setRowNumber(4)
                .setStartDate(Rsl.DEFAULT_START_DATE),
            new RslExcelRowData().setCategoryId(3L).setInRslPercents(1).setOutRslPercents(1).setRowNumber(5)
                .setStartDate(Rsl.DEFAULT_START_DATE),
            new RslExcelRowData().setCategoryId(3L).setInRslPercents(1).setOutRslPercents(1).setRowNumber(6)
                .setStartDate(Rsl.DEFAULT_START_DATE)
        );

        List<RslValidationError> errors = rslRowValidator.validate(rows,
            Collections.singletonList(RslType.FIRST_PARTY));
        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testWhenIncorrectShouldFail() {

        List<RslExcelRowData> rows = List.of(
            new RslExcelRowData().setCategoryId(1L).setInRslDays(1).setOutRslDays(1).setRowNumber(1),
            new RslExcelRowData().setCategoryId(2L).setInRslDays(1).setOutRslDays(1)
                .setStartDate(Rsl.DEFAULT_START_DATE),
            new RslExcelRowData().setCategoryId(3L).setOutRslDays(1).setRowNumber(4)
                .setStartDate(Rsl.DEFAULT_START_DATE),
            new RslExcelRowData().setCategoryId(3L).setInRslPercents(1).setOutRslPercents(1).setRowNumber(5),
            new RslExcelRowData().setCategoryId(3L).setInRslPercents(1).setOutRslPercents(1).setRowNumber(6)
                .setStartDate(Rsl.DEFAULT_START_DATE)
        );

        List<RslValidationError> errors = rslRowValidator.validate(rows,
            Collections.singletonList(RslType.FIRST_PARTY));
        Assertions.assertThat(errors.stream().map(RslValidationError::toString).collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                "На строке 1 должна быть указана дата активации",
                "На строке 4 должны быть заданы и входящие, и исходящие ОСГ",
                "На строке 5 должна быть указана дата активации");

    }

}
