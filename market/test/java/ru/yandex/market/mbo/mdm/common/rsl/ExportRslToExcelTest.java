package ru.yandex.market.mbo.mdm.common.rsl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.CategoryRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.MskuRsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.Rsl;
import ru.yandex.market.mbo.mdm.common.masterdata.model.rsl.SskuRsl;

/**
 * @author dmserebr
 * @date 27/11/2019
 */
@SuppressWarnings("checkstyle:magicnumber")
public class ExportRslToExcelTest extends RslExcelTestBase {
    private String defaultDate = RslExcelSheetConfig.DATE_TIME_FORMATTER.format(Rsl.DEFAULT_START_DATE);

    @Test
    public void testNoData() {
        ExcelFile excelFile = excelExportService.createExcelFile();

        assertExcelFile(excelFile, Collections.emptyList());
    }

    @Test
    public void testMultipleCategoryRsls() {
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(12345L).setInRslDays(3).setOutRslPercents(5));
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(123456L).setInRslDays(4).setInRslPercents(12)
            .setOutRslPercents(5));
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(123458L).setInRslDays(5).setOutRslDays(44)
            .setInRslPercents(12).setOutRslPercents(54));

        ExcelFile excelFile = excelExportService.createExcelFile();

        assertExcelFile(excelFile, List.of(
            List.of("12345", "", "", "", "", "", "3", "", "", "5", defaultDate, "", ""),
            List.of("123456", "", "", "", "", "", "4", "", "12", "5", defaultDate, "", ""),
            List.of("123458", "", "", "", "", "", "5", "44", "12", "54", defaultDate, "", "")
        ));
    }

    @Test
    public void testGetCategoryNames() {
        Mockito.when(categoryCachingService.getCategoryName(Mockito.eq(12345L))).thenReturn("cat12345");
        Mockito.when(categoryCachingService.getCategoryName(Mockito.eq(123456L))).thenReturn("cat123456");

        categoryRslRepository.insert(new CategoryRsl().setCategoryId(12345L).setInRslDays(3).setOutRslPercents(5));
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(123456L).setInRslDays(4).setInRslPercents(12)
            .setOutRslPercents(5));

        ExcelFile excelFile = excelExportService.createExcelFile();

        assertExcelFile(excelFile, List.of(
            List.of("12345", "cat12345", "", "", "", "", "3", "", "", "5", defaultDate, "", ""),
            List.of("123456", "cat123456", "", "", "", "", "4", "", "12", "5", defaultDate, "", "")
        ));
    }

    @Test
    public void testOnlyMskuRsls() {
        prepareMapping(12345, 3L, 20, "ssku1");
        prepareMapping(12345, 4L, 20, "ssku2");

        mskuRslRepository.insert(new MskuRsl().setMskuId(3L).setInRslPercents(40).setOutRslPercents(30));
        mskuRslRepository.insert(new MskuRsl().setMskuId(5L).setInRslPercents(40).setOutRslPercents(60));

        ExcelFile excelFile = excelExportService.createExcelFile();

        assertExcelFile(excelFile, List.of(
            List.of("12345", "", "3", "", "", "", "", "", "40", "30", defaultDate, "", "Не задан ОСГ для категории"),
            List.of("0", "", "5", "", "", "", "", "", "40", "60", defaultDate, "", "Не найдена категория для MSKU")
        ));
    }

    @Test
    public void testMskuRslsWithCategoryRsl() {
        prepareMapping(12345, 3L, 20, "ssku1");
        prepareMapping(12345, 4L, 20, "ssku2");
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(12345L).setInRslPercents(5).setOutRslPercents(5));
        mskuRslRepository.insert(new MskuRsl().setMskuId(3L).setInRslPercents(40).setOutRslPercents(30));
        mskuRslRepository.insert(new MskuRsl().setMskuId(4L).setInRslPercents(40).setOutRslPercents(50));
        mskuRslRepository.insert(new MskuRsl().setMskuId(5L).setInRslPercents(40).setOutRslPercents(60));

        ExcelFile excelFile = excelExportService.createExcelFile();

        assertExcelFile(excelFile, List.of(
            List.of("12345", "", "", "", "", "", "", "", "5", "5", defaultDate, "", ""),
            List.of("12345", "", "3", "", "", "", "", "", "40", "30", defaultDate, "", ""),
            List.of("12345", "", "4", "", "", "", "", "", "40", "50", defaultDate, "", ""),
            List.of("0", "", "5", "", "", "", "", "", "40", "60", defaultDate, "", "Не найдена категория для MSKU")
        ));
    }

    @Test
    public void testSskuRslsWithoutMappings() {
        sskuRslRepository.insert(new SskuRsl().setSupplierId(20).setShopSku("ssku").setInRslDays(20).setOutRslDays(30));

        ExcelFile excelFile = excelExportService.createExcelFile();

        assertExcelFile(excelFile, List.of(
            List.of("0", "", "", "", "20", "ssku", "20", "30", "", "", defaultDate, "",
                "Нет подтвержденного маппинга для SSKU")
        ));
    }

    @Test
    public void testSskuRslsWithMappings() {
        prepareMapping(12345, 3L, 20, "ssku");
        sskuRslRepository.insert(new SskuRsl().setSupplierId(20).setShopSku("ssku").setInRslDays(20).setOutRslDays(30));

        ExcelFile excelFile = excelExportService.createExcelFile();

        assertExcelFile(excelFile, List.of(
            List.of("12345", "", "3", "", "20", "ssku", "20", "30", "", "", defaultDate, "",
                "Не задан ОСГ для категории")
        ));
    }

    @Test
    public void testSskuAndMskuRslsWithMappings() {
        prepareMapping(12345, 3L, 20, "ssku");
        mskuRslRepository.insert(new MskuRsl().setMskuId(3L).setInRslPercents(40).setOutRslPercents(30));
        sskuRslRepository.insert(new SskuRsl().setSupplierId(20).setShopSku("ssku").setInRslDays(20).setOutRslDays(30));

        ExcelFile excelFile = excelExportService.createExcelFile();

        assertExcelFile(excelFile, List.of(
            List.of("12345", "", "3", "", "", "", "", "", "40", "30", defaultDate, "", "Не задан ОСГ для категории"),
            List.of("12345", "", "3", "", "20", "ssku", "20", "30", "", "", defaultDate, "",
                "Не задан ОСГ для категории")
        ));
    }

    @Test
    public void testSskuRslsWithMskuRslsAndCategoryRslWithMappings() {
        prepareMapping(12345, 3L, 20, "ssku1");
        prepareMapping(12345, 4L, 20, "ssku2");
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(12345L).setInRslPercents(5).setOutRslPercents(5));
        mskuRslRepository.insert(new MskuRsl().setMskuId(3L).setInRslPercents(40).setOutRslPercents(30));
        mskuRslRepository.insert(new MskuRsl().setMskuId(4L).setInRslPercents(40).setOutRslPercents(50));
        sskuRslRepository.insert(new SskuRsl().setSupplierId(20).setShopSku("ssku1")
            .setInRslDays(25).setOutRslDays(35));
        sskuRslRepository.insert(new SskuRsl().setSupplierId(20).setShopSku("ssku2")
            .setInRslDays(20).setOutRslDays(30));
        sskuRslRepository.insert(new SskuRsl().setSupplierId(20).setShopSku("ssku3")
            .setInRslDays(20).setOutRslDays(30));

        ExcelFile excelFile = excelExportService.createExcelFile();

        assertExcelFile(excelFile, List.of(
            List.of("12345", "", "", "", "", "", "", "", "5", "5", defaultDate, "", ""),
            List.of("12345", "", "3", "", "", "", "", "", "40", "30", defaultDate, "", ""),
            List.of("12345", "", "3", "", "20", "ssku1", "25", "35", "", "", defaultDate, "", ""),
            List.of("12345", "", "4", "", "", "", "", "", "40", "50", defaultDate, "", ""),
            List.of("12345", "", "4", "", "20", "ssku2", "20", "30", "", "", defaultDate, "", ""),
            List.of("0", "", "", "", "20", "ssku3", "20", "30", "", "", defaultDate, "",
                "Нет подтвержденного маппинга для SSKU")
        ));
    }

    @Test
    public void testSskuRslsWithMskuRslsAndCategoryRslWithoutMappings() {
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(12345L).setInRslPercents(5).setOutRslPercents(5));
        mskuRslRepository.insert(new MskuRsl().setMskuId(3L).setInRslPercents(40).setOutRslPercents(30));
        mskuRslRepository.insert(new MskuRsl().setMskuId(4L).setInRslPercents(40).setOutRslPercents(50));
        sskuRslRepository.insert(new SskuRsl().setSupplierId(20).setShopSku("ssku")
            .setInRslDays(25).setOutRslDays(35));
        sskuRslRepository.insert(new SskuRsl().setSupplierId(20).setShopSku("ssku2")
            .setInRslDays(20).setOutRslDays(30));

        ExcelFile excelFile = excelExportService.createExcelFile();

        assertExcelFile(excelFile, List.of(
            List.of("12345", "", "", "", "", "", "", "", "5", "5", defaultDate, "", ""),
            List.of("0", "", "3", "", "", "", "", "", "40", "30", defaultDate, "", "Не найдена категория для MSKU"),
            List.of("0", "", "4", "", "", "", "", "", "40", "50", defaultDate, "", "Не найдена категория для MSKU"),
            List.of("0", "", "", "", "20", "ssku", "25", "35", "", "", defaultDate, "",
                "Нет подтвержденного маппинга для SSKU"),
            List.of("0", "", "", "", "20", "ssku2", "20", "30", "", "", defaultDate, "",
                "Нет подтвержденного маппинга для SSKU")
        ));
    }

    @Test
    public void testMultipleCategories() {
        prepareMapping(12345, 3L, 20, "ssku1");
        prepareMapping(12345, 4L, 20, "ssku2");
        prepareMapping(5678, 6L, 20, "ssku3");
        prepareMapping(5678, 6L, 25, "SSKU");
        prepareMapping(5678, 6L, 30, "QWERTY");
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(12345L).setInRslPercents(5).setOutRslPercents(5));
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(23456L).setInRslDays(50).setOutRslDays(50));
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(5678L).setInRslPercents(5).setOutRslPercents(5));
        mskuRslRepository.insert(new MskuRsl().setMskuId(3L).setInRslPercents(40).setOutRslPercents(30));
        mskuRslRepository.insert(new MskuRsl().setMskuId(4L).setInRslPercents(40).setOutRslPercents(50));
        mskuRslRepository.insert(new MskuRsl().setMskuId(6L).setInRslPercents(40).setOutRslPercents(50));
        sskuRslRepository.insert(new SskuRsl().setSupplierId(20).setShopSku("ssku1")
            .setInRslDays(25).setOutRslDays(35));
        sskuRslRepository.insert(new SskuRsl().setSupplierId(25).setShopSku("SSKU")
            .setInRslDays(60).setOutRslDays(60));
        sskuRslRepository.insert(new SskuRsl().setSupplierId(30).setShopSku("QWERTY")
            .setInRslDays(40).setOutRslDays(50));
        sskuRslRepository.insert(new SskuRsl().setSupplierId(20).setShopSku("ssku3")
            .setInRslDays(40).setOutRslDays(40));

        ExcelFile excelFile = excelExportService.createExcelFile();

        // SSKU without mapping is not exported
        assertExcelFile(excelFile, List.of(
            List.of("12345", "", "", "", "", "", "", "", "5", "5", defaultDate, "", ""),
            List.of("12345", "", "3", "", "", "", "", "", "40", "30", defaultDate, "", ""),
            List.of("12345", "", "3", "", "20", "ssku1", "25", "35", "", "", defaultDate, "", ""),
            List.of("12345", "", "4", "", "", "", "", "", "40", "50", defaultDate, "", ""),
            List.of("23456", "", "", "", "", "", "50", "50", "", "", defaultDate, "", ""),
            List.of("5678", "", "", "", "", "", "", "", "5", "5", defaultDate, "", ""),
            List.of("5678", "", "6", "", "", "", "", "", "40", "50", defaultDate, "", ""),
            List.of("5678", "", "6", "", "25", "SSKU", "60", "60", "", "", defaultDate, "", ""),
            List.of("5678", "", "6", "", "30", "QWERTY", "40", "50", "", "", defaultDate, "", ""),
            List.of("5678", "", "6", "", "20", "ssku3", "40", "40", "", "", defaultDate, "", "")
        ));
    }

    @Test
    public void testTwoDifferentDatesForCategory() {
        prepareMapping(12345, 3L, 20, "ssku1");
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(12345L).setInRslPercents(5).setOutRslPercents(5)
            .setActivatedAt(LocalDate.EPOCH));
        categoryRslRepository.insert(new CategoryRsl().setCategoryId(12345L).setInRslPercents(10).setOutRslPercents(10)
            .setActivatedAt(LocalDate.EPOCH.plusDays(1)));
        sskuRslRepository.insert(new SskuRsl().setSupplierId(20).setShopSku("ssku1")
            .setInRslDays(25).setOutRslDays(35));

        ExcelFile excelFile = excelExportService.createExcelFile();

        assertExcelFile(excelFile, List.of(
            List.of("12345", "", "", "", "", "", "", "", "5", "5",
                RslExcelSheetConfig.DATE_TIME_FORMATTER.format(LocalDate.EPOCH), "", ""),
            List.of("12345", "", "", "", "", "", "", "", "10", "10",
                RslExcelSheetConfig.DATE_TIME_FORMATTER.format(LocalDate.EPOCH.plusDays(1)), "", ""),
            List.of("12345", "", "3", "", "20", "ssku1", "25", "35", "", "", defaultDate, "", "")
        ));
    }
}
