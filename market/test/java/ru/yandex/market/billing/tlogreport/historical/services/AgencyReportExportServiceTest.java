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

class AgencyReportExportServiceTest extends FunctionalTest {

    private static final LocalDate TRANSACTION_DATE = LocalDate.of(2021, 8, 1);
    private static final LocalDate TRANSACTION_DATE_OLD = LocalDate.of(2021, 4, 1);

    private static final List<TransactionReportLogItem> AGENCY_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("agency_commission")
                    .setPreviousTransactionId(null)
                    .setAmount(666L)
                    .setPayload2("{\"order_id\":10001,\"client_price\":666.0,\"tariff_part\":2.0," +
                            "\"service_datetime\":\"2021-08-01T13:00:00\",\"type\":\"Начисление\"}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("agency_commission")
                    .setPreviousTransactionId(null)
                    .setAmount(777L)
                    .setPayload2("{\"order_id\":10002,\"client_price\":777.0,\"tariff_part\":3.0," +
                            "\"service_datetime\":\"2021-08-01T14:00:00\",\"type\":\"Начисление\"}")

    );

    private static final List<TransactionReportLogItem> AGENCY_DELIVERY_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("agency_commission")
                    .setPreviousTransactionId(null)
                    .setAmount(666L)
                    .setPayload2("{\"order_id\":10001,\"client_price\":666.0,\"tariff_part\":2.0," +
                            "\"service_datetime\":\"2021-08-01T14:00:00\",\"type\":\"Начисление\"}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("agency_commission")
                    .setPreviousTransactionId(null)
                    .setAmount(777L)
                    .setPayload2("{\"order_id\":10002,\"client_price\":777.0,\"tariff_part\":3.0," +
                            "\"service_datetime\":\"2021-08-01T15:00:00\",\"type\":\"Начисление\"}")
    );


    private static final List<TransactionReportLogItem> AGENCY_CORR_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("agency_commission")
                    .setPreviousTransactionId(null)
                    .setAmount(-155L)
                    .setPayload2("{\"order_id\":10001,\"client_price\":0.0,\"tariff_part\":0.0," +
                            "\"service_datetime\":\"2021-08-01T14:00:00\",\"type\":\"Корректировка\"}"
                    ),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("agency_commission")
                    .setPreviousTransactionId(null)
                    .setAmount(-255L)
                    .setPayload2("{\"order_id\":10002,\"client_price\":0.0,\"tariff_part\":0.0," +
                            "\"service_datetime\":\"2021-08-01T15:00:00\",\"type\":\"Корректировка\"}")
    );

    private static final List<TransactionReportLogItem> AGENCY_OLD_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE_OLD)
                    .setTransactionTime(TRANSACTION_DATE_OLD)
                    .setSupplierId(1L)
                    .setProduct("agency_commission")
                    .setPreviousTransactionId(null)
                    .setAmount(500L)
                    .setPayload2("{\"order_id\":10002,\"client_price\":50.2,\"tariff_part\":0.01," +
                            "\"service_datetime\":\"2021-04-01T20:15:34\",\"type\":\"Начисление\"}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE_OLD)
                    .setTransactionTime(TRANSACTION_DATE_OLD)
                    .setSupplierId(2L)
                    .setProduct("agency_commission")
                    .setPreviousTransactionId(null)
                    .setAmount(900L)
                    .setPayload2("{\"order_id\":10003,\"client_price\":90.3,\"tariff_part\":0.01," +
                            "\"service_datetime\":\"2021-04-01T22:15:34\",\"type\":\"Начисление\"}")
    );

    @Autowired
    private AgencyReportDao agencyReportDao;

    @Test
    @DisplayName("Сбор транзакции для agency_comm_billed_amounts")
    @DbUnitDataSet(before = "csv/AgencyReportExportServiceTest.before.csv")
    public void agencyTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = agencyReportDao.getAgencyBilledAmountsForUMReportExport(
                LocalDate.of(2021, 8, 1),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(AGENCY_TRANS, transactions);
    }

    @Test
    @DisplayName("Сбор транзакции для agency_comm_delivery_blld_mnt")
    @DbUnitDataSet(before = "csv/AgencyReportExportServiceTest.before.csv")
    public void agencyDeliveryTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = agencyReportDao.getAgencyDeliveryBilledAmountsForUMReportExport(
                LocalDate.of(2021, 8, 1),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(AGENCY_DELIVERY_TRANS, transactions);
    }

    @Test
    @DisplayName("Сбор транзакции для agency_comm_billed_corr и agency_comm_delivery_blld_corr")
    @DbUnitDataSet(before = "csv/AgencyReportExportServiceTest.before.csv")
    public void agencyCorrTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = agencyReportDao.getAgencyBilledAmountsCorrsForUMReportExport(
                LocalDate.of(2021, 8, 1),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(AGENCY_CORR_TRANS, transactions);
    }

    @Test
    @DisplayName("Сбор транзакции для bank_order_item")
    @DbUnitDataSet(before = "csv/AgencyOldReportExportServiceTest.before.csv")
    public void agencyOldTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = agencyReportDao.getAgencyBilledAmountsOldForUMReportExport(
                LocalDate.of(2021, 4, 1),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(AGENCY_OLD_TRANS, transactions);
    }
}
