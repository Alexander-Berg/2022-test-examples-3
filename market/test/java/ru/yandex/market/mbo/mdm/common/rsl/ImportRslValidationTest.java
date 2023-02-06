package ru.yandex.market.mbo.mdm.common.rsl;

import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.Rsl;

/**
 * @author dmserebr
 * @date 18/12/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ImportRslValidationTest {
    @Test
    public void testValidRows() {
        List<RslExcelRowData> rows = List.of(
            RslExcelRowDataBuilder.start().categoryId(1L).inRslDays(1).outRslDays(1).rowNumber(1).build(),
            RslExcelRowDataBuilder.start().categoryId(2L).inRslDays(1).outRslDays(1).rowNumber(2).build(),
            RslExcelRowDataBuilder.start().categoryId(3L).inRslDays(1).outRslDays(1).rowNumber(4).build()
        );

        List<String> errors = RslValidator.validateRows(rows);

        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testDuplicateCategoryRows() {
        List<RslExcelRowData> rows = List.of(
            RslExcelRowDataBuilder.start().categoryId(2L).inRslDays(1).outRslDays(1).rowNumber(1).build(),
            RslExcelRowDataBuilder.start().categoryId(2L).inRslDays(1).outRslDays(1).rowNumber(2).build(),
            RslExcelRowDataBuilder.start().categoryId(2L).startDate(LocalDate.MAX).inRslDays(1).outRslDays(1)
                .rowNumber(3).build(),
            RslExcelRowDataBuilder.start().categoryId(3L).inRslDays(1).outRslDays(1).rowNumber(4).build()
        );

        List<String> errors = RslValidator.validateRows(rows);

        Assertions.assertThat(errors).containsExactly("На строках 1 и 2 дублируются объекты, для которых заданы ОСГ");
    }

    @Test
    public void testDuplicateMskuAndSskuRows() {
        List<RslExcelRowData> rows = List.of(
            RslExcelRowDataBuilder.start().categoryId(2L).mskuId(10L).inRslDays(1).outRslDays(1).rowNumber(1).build(),
            RslExcelRowDataBuilder.start().categoryId(2L).mskuId(10L).supplierId(1).shopSku("1")
                .inRslDays(1).outRslDays(1).rowNumber(2).build(),
            RslExcelRowDataBuilder.start().categoryId(2L).mskuId(10L).supplierId(1).shopSku("1")
                .inRslDays(1).outRslDays(1).rowNumber(3).build(),
            RslExcelRowDataBuilder.start().categoryId(2L).mskuId(10L).inRslDays(1).outRslDays(1).rowNumber(4).build(),
            RslExcelRowDataBuilder.start().categoryId(2L).mskuId(10L).supplierId(1).shopSku("1")
                .inRslDays(1).outRslDays(1).rowNumber(5).build()
        );

        List<String> errors = RslValidator.validateRows(rows);

        Assertions.assertThat(errors).containsExactly(
            "На строках 1 и 4 дублируются объекты, для которых заданы ОСГ",
            "На строках 2 и 3 дублируются объекты, для которых заданы ОСГ",
            "На строках 2 и 5 дублируются объекты, для которых заданы ОСГ",
            "На строках 3 и 5 дублируются объекты, для которых заданы ОСГ"
        );
    }

    @Test
    public void testRowsWithDifferentLevel() {
        List<RslExcelRowData> rows = List.of(
            RslExcelRowDataBuilder.start().categoryId(2L).inRslDays(1).outRslDays(1).rowNumber(1).build(),
            RslExcelRowDataBuilder.start().categoryId(2L).mskuId(10L).inRslDays(1).outRslDays(1).rowNumber(2).build(),
            RslExcelRowDataBuilder.start().categoryId(2L).mskuId(10L).supplierId(1).shopSku("1")
                .inRslDays(1).outRslDays(1).rowNumber(3).build()
        );

        List<String> errors = RslValidator.validateRows(rows);

        Assertions.assertThat(errors).isEmpty();
    }

    @Test
    public void testDifferentStartDates() {
        List<RslExcelRowData> rows = List.of(
            RslExcelRowDataBuilder.start().categoryId(2L).inRslDays(1).outRslDays(1).rowNumber(1).build(),
            RslExcelRowDataBuilder.start().categoryId(2L).startDate(LocalDate.MIN).inRslDays(1).outRslDays(1)
                .rowNumber(2).build(),
            RslExcelRowDataBuilder.start().categoryId(2L).startDate(LocalDate.MAX).inRslDays(1).outRslDays(1)
                .rowNumber(3).build(),
            RslExcelRowDataBuilder.start().categoryId(2L).startDate(Rsl.DEFAULT_START_DATE).inRslDays(1).outRslDays(1)
                .rowNumber(4).build()
        );

        List<String> errors = RslValidator.validateRows(rows);

        Assertions.assertThat(errors).containsExactly(
            "На строках 1 и 4 дублируются объекты, для которых заданы ОСГ"
        );
    }

    @Test
    public void supplierIdAndShopSkuShouldBeFilled() {
        List<RslExcelRowData> rows = List.of(
            RslExcelRowDataBuilder.start().categoryId(2L).mskuId(10L).shopSku("1")
                .inRslDays(1).outRslDays(1).rowNumber(1).build(),
            RslExcelRowDataBuilder.start().categoryId(2L).mskuId(10L).supplierId(1)
                .inRslDays(1).outRslDays(1).rowNumber(2).build(),
            RslExcelRowDataBuilder.start().categoryId(2L).mskuId(10L).supplierId(1).shopSku("1")
                .inRslDays(1).outRslDays(1).rowNumber(3).build()
        );

        List<String> errors = RslValidator.validateRows(rows);

        Assertions.assertThat(errors).containsExactly(
            "На строках 1 и 2 дублируются объекты, для которых заданы ОСГ",
            "На строке 1 должны быть заданы одновременно ID поставщика и SSKU",
            "На строке 2 должны быть заданы одновременно ID поставщика и SSKU"
        );
    }
}
