package ru.yandex.market.core.fulfillment.report.excel;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentSubmethod;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.fulfillment.report.OrderStatisticsReportRow;
import ru.yandex.market.core.fulfillment.report.OrderStatisticsReportRowPrices;
import ru.yandex.market.core.fulfillment.report.SummaryReportData;
import ru.yandex.market.core.order.model.MbiOrderStatus;
import ru.yandex.market.core.tax.model.VatRate;

import static ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils.assertCellValue;
import static ru.yandex.market.core.fulfillment.report.excel.ExcelTestUtils.assertCellValues;

@DbUnitDataSet(before = "../OrderReportDaoTest.before.csv")
class StatisticsReportGeneratorTest extends FunctionalTest {

    private static final YearMonth NEW_REPORT_DATE = YearMonth.of(2021, Month.DECEMBER);

    @Autowired
    private StatisticsReportGenerator statisticsReportGenerator;

    // Ниже координаты ячеек в формате (номер строки, номер столбца)

    @Test
    @DisplayName("Проверка построения и рендера данных в xlsx")
    void testExcelGeneration() throws IOException {
        SummaryReportData summaryReportData = mockSummaryReportData();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        statisticsReportGenerator.generateReport(1L, NEW_REPORT_DATE, outputStream, summaryReportData);
        Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(outputStream.toByteArray()));
        Sheet summarySheet = workbook.getSheetAt(0);
        Sheet deliverySheet = workbook.getSheetAt(1);
        Sheet deliveredSheet = workbook.getSheetAt(2);
        Sheet unredeemedSheet = workbook.getSheetAt(3);
        Sheet returnedSheet = workbook.getSheetAt(4);

        // Раскомментировать строку для сохранения сгенерированного excel в файл.
//        ExcelTestUtils.write(workbook);

        // assert detailed sheets
        assertDetailedSheet(deliverySheet);
        assertDetailedSheet(deliveredSheet);
        assertDetailedSheet(unredeemedSheet);
        assertDetailedSheet(returnedSheet);

        // assert summary sheet
        assertCell(summarySheet, SheetOverall.ORG_NAME_CELL, "ООО Ромашишка");
        assertCell(summarySheet, SheetOverall.CONTRACT_CELL,
                "Сводный отчёт по данным статистики по Договору № 123/45");
        assertCell(summarySheet, SheetOverall.APPENDIX_CELL,
                "Приложения 1-4 формируются в личном кабинете Заказчика и являются неотъемлемой частью " +
                        "сводного отчёта");

        List<List<Object>> deliverySheetExpectedValues = Arrays.asList(
                Arrays.asList(1, "offerName1", "shopSku1", 10, "Передан в доставку", "16.12.2021",
                        "16.12.2021", null, "Яндекс.Сплит (предоплата)",
                        "Без НДС", 502, 2, 2, 3, 495, 5020, 70, 4950),
                Arrays.asList(2, "offerName2", "shopSku2", 10, "Доставлен", "18.12.2021",
                        "18.12.2021", "19.12.2021", "Яндекс.Сплит (предоплата)",
                        "Без НДС", 502, 2, 2, 3, 495, 5020, 70, 4950),
                Arrays.asList(3, "offerName3", "shopSku3", 10, "Отменен при доставке", "20.12.2021",
                        "20.12.2021", "23.12.2021", "Яндекс.Сплит (предоплата)",
                        "20%", 502, 2, 2, 3, 495, 5020, 70, 4950),
                Arrays.asList(4, "offerName4", "shopSku4", 10, "Доставлен", "21.12.2021",
                        "21.12.2021", "23.12.2021", "Яндекс.Сплит (предоплата)",
                        "10%", 502, 2, 2, 3, 495, 5020, 70, 4950)
        );

        List<List<Object>> deliveredSheetExpectedValues = Arrays.asList(
                Arrays.asList(2, "offerName2", "shopSku2", 10, 10, "Доставлен", "18.12.2021",
                        "18.12.2021", "19.12.2021", "Яндекс.Сплит (предоплата)",
                        "Без НДС", 502, 2, 2, 3, 495, 5020, 70, 4950),
                Arrays.asList(4, "offerName4", "shopSku4", 10, 10, "Доставлен", "21.12.2021",
                        "21.12.2021", "23.12.2021", "Яндекс.Сплит (предоплата)",
                        "10%", 502, 2, 2, 3, 495, 5020, 70, 4950)
        );

        List<List<Object>> unredeemedSheetExpectedValues = Arrays.asList(
                Arrays.asList(3, "offerName3", "shopSku3", 10, 10, "Отменен при доставке", "20.12.2021",
                        "20.12.2021", "23.12.2021", "25.12.2021", "Яндекс.Сплит (предоплата)",
                        "20%", 502, 2, 2, 3, 495, 5020, 70, 4950)
        );

        List<List<Object>> returnedSheetExpectedValues = Arrays.asList(
                Arrays.asList(4, "offerName4", "shopSku4", 10, 2, "Доставлен", "21.12.2021",
                        "21.12.2021", "23.12.2021", "27.12.2021", "Яндекс.Сплит (предоплата)",
                        "10%", 502, 2, 10, 15, 475, 1004, 54, 950)
        );

        List<List<Object>> deliveryExpectedValues = Arrays.asList(
                Arrays.asList(40, 20080, 19800),//всего
                Arrays.asList(null, null, null),//пустая строка
                Arrays.asList(10, 5020, 4950),
                Arrays.asList(0, 0, 0),
                Arrays.asList(10, 5020, 4950),
                Arrays.asList(0, 0, 0),
                Arrays.asList(20, 10040, 9900)
        );

        List<List<Object>> deliveredExpectedValues = Arrays.asList(
                Arrays.asList(20, 10040, 9900),//всего
                Arrays.asList(null, null, null),//пустая строка
                Arrays.asList(0, 0, 0),
                Arrays.asList(0, 0, 0),
                Arrays.asList(10, 5020, 4950),
                Arrays.asList(0, 0, 0),
                Arrays.asList(10, 5020, 4950)
        );

        List<List<Object>> unredeemedExpectedValues = Arrays.asList(
                Arrays.asList(10, 5020, 4950),//всего
                Arrays.asList(null, null, null),//пустая строка
                Arrays.asList(10, 5020, 4950),
                Arrays.asList(0, 0, 0),
                Arrays.asList(0, 0, 0),
                Arrays.asList(0, 0, 0),
                Arrays.asList(0, 0, 0)
        );

        List<List<Object>> returnedExpectedValues = Arrays.asList(
                Arrays.asList(2, 1004, 950),//всего
                Arrays.asList(null, null, null),//пустая строка
                Arrays.asList(0, 0, 0),
                Arrays.asList(0, 0, 0),
                Arrays.asList(2, 1004, 950),
                Arrays.asList(0, 0, 0),
                Arrays.asList(0, 0, 0)
        );

        assertCellValues(deliverySheetExpectedValues, deliverySheet, 13, 0);
        assertCellValues(deliveredSheetExpectedValues, deliveredSheet, 13, 0);
        assertCellValues(unredeemedSheetExpectedValues, unredeemedSheet, 13, 0);
        assertCellValues(returnedSheetExpectedValues, returnedSheet, 13, 0);

        assertCellValues(deliveryExpectedValues, summarySheet, 14, 2);
        assertCellValues(deliveredExpectedValues, summarySheet, 25, 2);
        assertCellValues(unredeemedExpectedValues, summarySheet, 36, 2);
        assertCellValues(returnedExpectedValues, summarySheet, 47, 2);
    }

    private void assertCell(Sheet sheet, Pair<Integer, Integer> cellCoords, String value) {
        assertCellValue(sheet.getRow(cellCoords.getLeft()).getCell(cellCoords.getRight()), value);
    }

    private void assertDetailedSheet(Sheet detailedSheet) {
        assertCell(detailedSheet, SheetDetailed.PERIOD_CELL, "за период с 01 по 31 декабря 2021 года");
        assertCell(detailedSheet, SheetDetailed.ORG_NAME_CELL, "ООО Ромашишка");
        assertCell(detailedSheet, SheetDetailed.CONTRACT_CELL,
                "Отчёт по данным статистики по Договору № 123/45");
    }

    /**
     * Ячейки листа с детальной информацией.
     */
    private static class SheetDetailed {
        private static final Pair<Integer, Integer> PERIOD_CELL = Pair.of(3, 8);
        private static final Pair<Integer, Integer> CONTRACT_CELL = Pair.of(2, 6);
        private static final Pair<Integer, Integer> ORG_NAME_CELL = Pair.of(7, 2);
    }

    /**
     * Ячейки листа с суммарной информацией.
     */
    private static class SheetOverall {
        private static final Pair<Integer, Integer> CONTRACT_CELL = Pair.of(2, 3);
        private static final Pair<Integer, Integer> ORG_NAME_CELL = Pair.of(7, 2);
        private static final Pair<Integer, Integer> APPENDIX_CELL = Pair.of(56, 1);
    }

    private SummaryReportData mockSummaryReportData() {
        var row1 = OrderStatisticsReportRow.builder()
                .setPartnerId(1L)
                .setOrderId(1L)
                .setCreationDate(LocalDateTime.of(2021, 12, 16, 13, 15, 0))
                .setShippingTime(LocalDateTime.of(2021, 12, 16, 18, 15, 0))
                .setPaymentType(PaymentType.PREPAID)
                .setPaymentMethod(PaymentMethod.YANDEX)
                .setOfferName("offerName1")
                .setShopSku("shopSku1")
                .setVatRate(VatRate.NO_VAT)
                .setMbiOrderStatus(MbiOrderStatus.DELIVERY)
                .setDeliveryPrices(new OrderStatisticsReportRowPrices(10, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setDeliveredPrices(new OrderStatisticsReportRowPrices(0, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setUnredeemedPrices(new OrderStatisticsReportRowPrices(0, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setReturnedPrices(new OrderStatisticsReportRowPrices(0, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setPaymentSubmethod(PaymentSubmethod.BNPL)
                .build();

        var row2 = OrderStatisticsReportRow.builder()
                .setPartnerId(1L)
                .setOrderId(2L)
                .setCreationDate(LocalDateTime.of(2021, 12, 18, 13, 20, 0))
                .setShippingTime(LocalDateTime.of(2021, 12, 18, 18, 15, 0))
                .setDeliveryTime(LocalDateTime.of(2021, 12, 19, 10, 15, 0))
                .setPaymentType(PaymentType.PREPAID)
                .setPaymentMethod(PaymentMethod.YANDEX)
                .setOfferName("offerName2")
                .setShopSku("shopSku2")
                .setVatRate(VatRate.NO_VAT)
                .setMbiOrderStatus(MbiOrderStatus.DELIVERED)
                .setDeliveryPrices(new OrderStatisticsReportRowPrices(10, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setDeliveredPrices(new OrderStatisticsReportRowPrices(10, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setUnredeemedPrices(new OrderStatisticsReportRowPrices(0, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setReturnedPrices(new OrderStatisticsReportRowPrices(0, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setPaymentSubmethod(PaymentSubmethod.BNPL)
                .build();
        //заказ отменен при доставке, но есть дата доставки -не должен попасть в доставленные
        var row3 = OrderStatisticsReportRow.builder()
                .setPartnerId(1L)
                .setOrderId(3L)
                .setCreationDate(LocalDateTime.of(2021, 12, 20, 13, 40, 0))
                .setShippingTime(LocalDateTime.of(2021, 12, 20, 18, 15, 0))
                .setDeliveryTime(LocalDateTime.of(2021, 12, 23, 18, 15, 0))
                .setResupplyTime(LocalDateTime.of(2021, 12, 25, 10, 15, 0))
                .setPaymentType(PaymentType.PREPAID)
                .setPaymentMethod(PaymentMethod.YANDEX)
                .setOfferName("offerName3")
                .setShopSku("shopSku3")
                .setVatRate(VatRate.VAT_20)
                .setMbiOrderStatus(MbiOrderStatus.CANCELLED_IN_DELIVERY)
                .setDeliveryPrices(new OrderStatisticsReportRowPrices(10, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setDeliveredPrices(new OrderStatisticsReportRowPrices(0, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setUnredeemedPrices(new OrderStatisticsReportRowPrices(10, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setReturnedPrices(new OrderStatisticsReportRowPrices(0, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setPaymentSubmethod(PaymentSubmethod.BNPL)
                .build();
        var row4 = OrderStatisticsReportRow.builder()
                .setPartnerId(1L)
                .setOrderId(4L)
                .setCreationDate(LocalDateTime.of(2021, 12, 21, 13, 40, 0))
                .setShippingTime(LocalDateTime.of(2021, 12, 21, 18, 15, 0))
                .setDeliveryTime(LocalDateTime.of(2021, 12, 23, 10, 15, 0))
                .setResupplyTime(LocalDateTime.of(2021, 12, 27, 10, 15, 0))
                .setPaymentType(PaymentType.PREPAID)
                .setPaymentMethod(PaymentMethod.YANDEX)
                .setOfferName("offerName4")
                .setShopSku("shopSku4")
                .setVatRate(VatRate.VAT_10)
                .setMbiOrderStatus(MbiOrderStatus.DELIVERED)
                .setDeliveryPrices(new OrderStatisticsReportRowPrices(10, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setDeliveredPrices(new OrderStatisticsReportRowPrices(10, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setUnredeemedPrices(new OrderStatisticsReportRowPrices(0, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setReturnedPrices(new OrderStatisticsReportRowPrices(2, BigDecimal.valueOf(500),
                        BigDecimal.valueOf(2), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(30)))
                .setPaymentSubmethod(PaymentSubmethod.BNPL)
                .build();

        SummaryReportData summaryReportData = new SummaryReportData();
        summaryReportData.addReportRow(row1, NEW_REPORT_DATE);
        summaryReportData.addReportRow(row2, NEW_REPORT_DATE);
        summaryReportData.addReportRow(row3, NEW_REPORT_DATE);
        summaryReportData.addReportRow(row4, NEW_REPORT_DATE);
        return summaryReportData;
    }
}
