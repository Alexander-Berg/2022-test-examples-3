package ru.yandex.market.billing.tlogreport.historical.services;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.tlogreport.historical.model.TransactionReportLogItem;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class SortingReportExportServiceTest extends FunctionalTest {

    private static final LocalDate TRANSACTION_DATE = LocalDate.of(2021, 8, 1);

    private static final List<TransactionReportLogItem> SORTING_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("sorting")
                    .setPreviousTransactionId(null)
                    .setAmount(1600L)
                    .setPayload2("{\"service_datetime\":\"2021-08-01T00:00:00\",\"fact_service_datetime\":" +
                            "\"2021-08-01T00:00:00\",\"order_count\":\"2\",\"location\":\"Сортировочный центр\"," +
                            "\"tariff\":15.00,\"summary\":16.00,\"min_tariff_summary\":1.00,\"type\":\"Начисление\"}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("sorting")
                    .setPreviousTransactionId(null)
                    .setAmount(500L)
                    .setPayload2("{\"service_datetime\":\"2021-08-01T00:00:00\",\"fact_service_datetime\":" +
                            "\"2021-08-01T00:00:00\",\"order_count\":\"2\",\"location\":\"Ваш склад\"," +
                            "\"tariff\":3.00,\"summary\":5.00,\"min_tariff_summary\":2.00,\"type\":\"Начисление\"}")

    );


    private static final List<TransactionReportLogItem> SORTING_CORR_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("sorting")
                    .setPreviousTransactionId(null)
                    .setAmount(-200L)
                    .setPayload2("{\"service_datetime\":\"2021-08-01T00:00:00\",\"fact_service_datetime\":" +
                            "\"2021-08-01T00:00:00\",\"location\":\"Сортировочный центр\",\"type\":\"Корректировка\"}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("sorting")
                    .setPreviousTransactionId(null)
                    .setAmount(300L)
                    .setPayload2("{\"service_datetime\":\"2021-08-01T00:00:00\",\"fact_service_datetime\":" +
                            "\"2021-08-01T00:00:00\",\"location\":\"Ваш склад\",\"type\":\"Корректировка\"}")
    );

    @Autowired
    private SortingReportDao sortingReportDao;

    @Test
    @DisplayName("Сбор транзакции для sorting_daily_billed_amounts")
    @DbUnitDataSet(before = "csv/SortingReportExportServiceTest.before.csv")
    public void sortingTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = sortingReportDao.getSortingBilledAmountsForUMReportExport(
                LocalDate.of(2021, 8, 1),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(SORTING_TRANS, transactions);
    }

    @Test
    @DisplayName("Сбор транзакции для sorting_daily_billing_corr")
    @DbUnitDataSet(before = "csv/SortingReportExportServiceTest.before.csv")
    public void sortingCorrTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = sortingReportDao.getSortingBilledAmountsCorrsForUMReportExport(
                LocalDate.of(2021, 8, 1),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(SORTING_CORR_TRANS, transactions);
    }
}
