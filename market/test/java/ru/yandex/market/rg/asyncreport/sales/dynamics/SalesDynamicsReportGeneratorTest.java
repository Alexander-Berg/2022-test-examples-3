package ru.yandex.market.rg.asyncreport.sales.dynamics;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils;
import ru.yandex.market.core.replenishment.supplier.PilotSupplierYtDao;
import ru.yandex.market.core.yt.YtHttpFactory;
import ru.yandex.market.core.yt.YtTablesMockUtils;
import ru.yandex.market.rg.asyncreport.ReportFunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Функциональные тесты на {@link SalesDynamicsReportGenerator}
 */
@DbUnitDataSet(before = "SalesDynamicsReportGeneratorTest.before.csv")
class SalesDynamicsReportGeneratorTest extends ReportFunctionalTest {
    public static final int PARTNER_ID = 1;

    @Autowired
    private SalesDynamicsReportGenerator generator;
    @Autowired
    private YtHttpFactory ytFactory;
    @Autowired
    @Qualifier("salesDynamicsYt")
    private Yt yt;
    @Autowired
    private PilotSupplierYtDao pilotSupplierYtDao;

    private static void assertHeader(Sheet sheet) {
        List<List<Object>> header = Arrays.asList(
                Arrays.asList("Магазин: ", "ООО Ромашка"), Arrays.asList("Дата:  ", "2019-06-19"));
        ExcelTestUtils.assertCellValues(header, sheet, 1, 0);
        List<List<Object>> tableHeader = Collections.singletonList(
                Arrays.asList("Заказано с 23.05 по 29.05, шт.", "Заказано с 30.05 по 05.06, шт.",
                        "Заказано с 06.06 по 12.06, шт.", "Заказано с 13.06 по 19.06, шт."));
        ExcelTestUtils.assertCellValues(tableHeader, sheet, 4, 17);
        ExcelTestUtils.assertCellValues(tableHeader, sheet, 4, 31);
        ExcelTestUtils.assertCellValues(tableHeader, sheet, 4, 45);
    }

    private static void assertSheet(Sheet sheet) {
        List<List<Object>> rows = Arrays.asList(
                Arrays.asList("shop_sku_1", "123456789", "title_1", "vendor_name_1", "dep_1", "category_name_1",
                        "cat1_1",
                        "cat2_1",
                        1000L,
                        // Все склады2
                        1001L, 1002L, 1003L, 1004L, 1005L, null,
                        // Ростов-на-Дону
                        1032L, 1033L, 1034L, 1035L, 1036L, 1037L, 1038L, 1039L, 1040L, 1041L, 1042L, 1043L, 1044L, null,
                        // Томилино
                        1019L, 1020L, 1021L, 1022L, 1023L, 1024L, 1025L, 1026L, 1027L, 1028L, 1029L, 1030L, 1031L, null,
                        //Софьино
                        101L, 102L, 102L, 103L, 104L, 105L, 106L, 107L, 108L, 109L, 110L, 111L, 112L, null,
                        1045L, 1046L, 1047L, "schedule_1",
                        // весогабариты
                        1048L, 1049L, 1050L, 1051L, 1051.1
                ),
                Arrays.asList("shop_sku_2", "213456789", "title_2", "vendor_name_2", "dep_2", "category_name_2",
                        "cat1_2",
                        "cat2_2",
                        2000L, 2001L, 2002L, 2003L, 2004L, 2005L, null,
                        // Ростов-на-Дону
                        2032L, 2033L, 2034L, 2035L, 2036L, 2037L, 2038L, 2039L, 2040L, 2041L, 2042L, 2043L, 2044L, null,
                        // Томилино
                        2019L, 2020L, 2021L, 2022L, 2023L, 2024L, 2025L, 2026L, 2027L, 2028L, 2029L, 2030L, 2031L, null,
                        //Софьино
                        201L, 202L, 202L, 203L, 204L, 205L, 206L, 207L, 208L, 209L, 210L, 211L, 212L, null,
                        2045L, 2046L, 2047L, "schedule_1",
                        // весогабариты
                        2048L, 2049L, 2050L, 2051L, 2051.1
                ),
                Arrays.asList("shop_sku_3", "321456789", "title_3", "vendor_name_3", "dep_3", "category_name_3",
                        "cat1_3",
                        "cat2_3",
                        3000L, 3001L, 3002L, 3003L, 3004L, 3005L, null,
                        // Ростов-на-Дону
                        3032L, 3033L, 3034L, 3035L, 3036L, 3037L, 3038L, 3039L, 3040L, 3041L, 3042L, 3043L, 3044L, null,
                        // Томилино
                        3019L, 3020L, 3021L, 3022L, 3023L, 3024L, 3025L, 3026L, 3027L, 3028L, 3029L, 3030L, 3031L, null,
                        //Софьино
                        301L, 302L, 302L, 303L, 304L, 305L, 306L, 307L, 308L, 309L, 310L, 311L, 312L, null,
                        3045L, 3046L, 3047L, "schedule_1",
                        // весогабариты
                        3048L, 3049L, 3050L, 3051L, 3051.1
                )
        );
        ExcelTestUtils.assertCellValues(rows, sheet, 6, 0);
    }

    private static void assertHeaderForPilotSupplier(Sheet sheet) {
        List<List<Object>> header = Arrays.asList(
                Arrays.asList("Магазин: ", "ООО Ромашка"), Arrays.asList("Дата:  ", "2019-06-19"));
        ExcelTestUtils.assertCellValues(header, sheet, 1, 0);
        List<List<Object>> tableHeader = Collections.singletonList(
                Arrays.asList("Заказано с 23.05 по 29.05, шт.", "Заказано с 30.05 по 05.06, шт.",
                        "Заказано с 06.06 по 12.06, шт.", "Заказано с 13.06 по 19.06, шт."));
        ExcelTestUtils.assertCellValues(tableHeader, sheet, 4, 17);
        ExcelTestUtils.assertCellValues(tableHeader, sheet, 4, 29);
        ExcelTestUtils.assertCellValues(tableHeader, sheet, 4, 41);
    }

    private static void assertSheetForPilotSupplier(Sheet sheet) {
        List<List<Object>> rows = Arrays.asList(
                Arrays.asList("shop_sku_1", "123456789", "title_1", "vendor_name_1", "dep_1", "category_name_1",
                        "cat1_1",
                        "cat2_1",
                        1000L,
                        // Все склады2
                        1001L, 1002L, 1003L, 1004L, 1005L, null,
                        // Ростов-на-Дону
                        1032L, 1033L, 1034L, 1035L, 1036L, 1037L, 1038L, 1039L, 1040L, 1041L, 1042L, null,
                        // Томилино
                        1019L, 1020L, 1021L, 1022L, 1023L, 1024L, 1025L, 1026L, 1027L, 1028L, 1029L, null,
                        //Софьино
                        101L, 102L, 102L, 103L, 104L, 105L, 106L, 107L, 108L, 109L, 110L, null,
                        1045L, 1046L, 1047L, "schedule_1",
                        // весогабариты
                        1048L, 1049L, 1050L, 1051L, 1051.1
                ),
                Arrays.asList("shop_sku_2", "213456789", "title_2", "vendor_name_2", "dep_2", "category_name_2",
                        "cat1_2",
                        "cat2_2",
                        2000L, 2001L, 2002L, 2003L, 2004L, 2005L, null,
                        // Ростов-на-Дону
                        2032L, 2033L, 2034L, 2035L, 2036L, 2037L, 2038L, 2039L, 2040L, 2041L, 2042L, null,
                        // Томилино
                        2019L, 2020L, 2021L, 2022L, 2023L, 2024L, 2025L, 2026L, 2027L, 2028L, 2029L, null,
                        //Софьино
                        201L, 202L, 202L, 203L, 204L, 205L, 206L, 207L, 208L, 209L, 210L, null,
                        2045L, 2046L, 2047L, "schedule_1",
                        // весогабариты
                        2048L, 2049L, 2050L, 2051L, 2051.1
                ),
                Arrays.asList("shop_sku_3", "321456789", "title_3", "vendor_name_3", "dep_3", "category_name_3",
                        "cat1_3",
                        "cat2_3",
                        3000L, 3001L, 3002L, 3003L, 3004L, 3005L, null,
                        // Ростов-на-Дону
                        3032L, 3033L, 3034L, 3035L, 3036L, 3037L, 3038L, 3039L, 3040L, 3041L, 3042L, null,
                        // Томилино
                        3019L, 3020L, 3021L, 3022L, 3023L, 3024L, 3025L, 3026L, 3027L, 3028L, 3029L, null,
                        //Софьино
                        301L, 302L, 302L, 303L, 304L, 305L, 306L, 307L, 308L, 309L, 310L, null,
                        3045L, 3046L, 3047L, "schedule_1",
                        // весогабариты
                        3048L, 3049L, 3050L, 3051L, 3051.1
                )
        );
        ExcelTestUtils.assertCellValues(rows, sheet, 6, 0);
    }

    @Test
    @DisplayName("Отчет по динамике продаж")
    void testSalesDynamicsReport() throws IOException {
        when(pilotSupplierYtDao.getPilotSupplierIds()).thenReturn(List.of());

        YtTablesMockUtils.mockYt(yt, getClass(), "SalesReportControllerTest.yt.data.json", "salesDynamics");
        when(ytFactory.getYt(any())).thenReturn(yt);

        checkExcelReport("2019-06-19", PARTNER_ID, generator, sheet -> {
            assertHeader(sheet);
            assertSheet(sheet);
        });
    }

    @Test
    @DisplayName("Отчет по динамике продаж без колонки availability для даты, где эта колонка учитывается")
    void testSalesDynamicsReportWithoutAvailability_emptyReport() throws IOException {
        YtTablesMockUtils.mockYt(yt, getClass(), "SalesReportControllerTest.yt.data-without-availability.json",
                "salesDynamics");
        when(ytFactory.getYt(any())).thenReturn(yt);

        checkEmptyReport("2020-09-10", PARTNER_ID, generator);
    }

    @Test
    @DisplayName("Отчет по динамике продаж без колонки availability для даты, где эта колонка не учитывается")
    void testSalesDynamicsReportWithoutAvailability() throws IOException {
        when(pilotSupplierYtDao.getPilotSupplierIds()).thenReturn(List.of());

        YtTablesMockUtils.mockYt(yt, getClass(), "SalesReportControllerTest.yt.data-without-availability.json",
                "salesDynamics");
        when(ytFactory.getYt(any())).thenReturn(yt);

        checkExcelReport("2021-09-12", PARTNER_ID, generator, SalesDynamicsReportGeneratorTest::assertSheet);
    }

    //Пока отключаем функциональность, возможно совсем прийдется удалить
    // вместе с SalesDynamicsConfig.showAllColumns
    @Test
    @DisplayName("Отчет по динамике продаж для поставщиков в пилоте")
    @Disabled
    void testSalesDynamicsReport_forPilotSuppliers() throws IOException {
        when(pilotSupplierYtDao.getPilotSupplierIds()).thenReturn(List.of(1L));
        YtTablesMockUtils.mockYt(yt, getClass(), "SalesReportControllerTest.yt.data.json", "salesDynamics");
        when(ytFactory.getYt(any())).thenReturn(yt);

        checkExcelReport("2019-06-19", PARTNER_ID, generator, sheet -> {
            assertHeaderForPilotSupplier(sheet);
            assertSheetForPilotSupplier(sheet);
        });
    }

    @Test
    @DisplayName("Тест запроса нет данных")
    void testSalesDynamicsReportNotFound() {
        YtTablesMockUtils.mockEmptyResult(yt);
        when(ytFactory.getYt(any())).thenReturn(yt);

        checkEmptyReport("2020-09-10", PARTNER_ID, generator);
    }

    @Test
    @DisplayName("Тест запроса нет таблицы в yt")
    void testSalesDynamicsReportNotFoundTable() {
        YtTablesMockUtils.mockYtTableNotFound(yt);
        when(ytFactory.getYt(any())).thenReturn(yt);

        checkEmptyReport("2020-09-10", PARTNER_ID, generator);
    }
}
