package ru.yandex.market.partner;

import java.util.HashSet;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils;
import ru.yandex.market.partner.test.context.FunctionalTest;

/**
 * @author Vadim Lyalin
 */
abstract public class PartnerReportGeneratorTest extends FunctionalTest {

    protected static void assertEquals(XSSFWorkbook expected, XSSFWorkbook result) {
        assertEquals(expected, result, new HashSet<>());
    }

    protected static void assertEquals(XSSFWorkbook expected, XSSFWorkbook result, HashSet<Integer> ignoredColumns) {
        ExcelTestUtils.assertEquals(expected, result, ignoredColumns);
    }
}
