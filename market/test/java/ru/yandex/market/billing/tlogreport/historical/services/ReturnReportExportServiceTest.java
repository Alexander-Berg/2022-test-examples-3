package ru.yandex.market.billing.tlogreport.historical.services;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.tlogreport.historical.model.TransactionReportLogItem;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class ReturnReportExportServiceTest extends FunctionalTest {

    private static final Instant TRANSACTION_INSTANT =
            LocalDateTime.of(2021, 8, 1, 9, 33, 15).toInstant(ZoneOffset.UTC);
    private static final LocalDate TRANSACTION_DATE = LocalDate.of(2021, 8, 1);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TRANSACTION_INSTANT, ZoneOffset.UTC
    );

    private static final List<TransactionReportLogItem> RETRURN_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("returned_orders_storage")
                    .setPreviousTransactionId(null)
                    .setAmount(1500L)
                    .setPayload2("{\"resupply_type\":\"Невыкуп\",\"service_datetime\":\"2021-08-01T00:00:00\"," +
                            "\"order_id\":10001,\"unredeemed_order_tariff\":15.00,\"type\":\"Начисление\"}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("returned_orders_storage")
                    .setPreviousTransactionId(null)
                    .setAmount(2500L)
                    .setPayload2("{\"resupply_type\":\"Невыкуп\",\"service_datetime\":\"2021-08-01T00:00:00\"," +
                            "\"order_id\":10002,\"unredeemed_order_tariff\":25.00,\"type\":\"Начисление\"}")

    );


    private static final List<TransactionReportLogItem> RETURN_CORR_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("returned_orders_storage")
                    .setPreviousTransactionId(null)
                    .setAmount(-1300)
                    .setPayload2("{\"resupply_type\":\"Невыкуп\",\"service_datetime\":\"2021-08-01T00:00:00\"," +
                            "\"order_id\":10001,\"type\":\"Корректировка\"}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("returned_orders_storage")
                    .setPreviousTransactionId(null)
                    .setAmount(-2300)
                    .setPayload2("{\"resupply_type\":\"Невыкуп\",\"service_datetime\":\"2021-08-01T00:00:00\"," +
                            "\"order_id\":10002,\"type\":\"Корректировка\"}")
    );

    @Autowired
    @Qualifier("oraJdbcTemplate")
    private NamedParameterJdbcTemplate oraJdbcTemplate;

    private ReturnReportDao returnReportDao;

    @BeforeEach
    public void init() {
        returnReportDao = new ReturnReportDao(oraJdbcTemplate, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Сбор транзакции для order_return_storage_billed")
    @DbUnitDataSet(before = "csv/ReturnReportExportServiceTest.before.csv")
    public void returnTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = returnReportDao.getReturnBilledAmountsForUMReportExport(
                LocalDate.of(2021, 8, 1),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(RETRURN_TRANS, transactions);
    }

    @Test
    @DisplayName("Сбор транзакции для order_return_storage_corr")
    @DbUnitDataSet(before = "csv/ReturnReportExportServiceTest.before.csv")
    public void returnCorrTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = returnReportDao.getReturnBilledAmountsCorrsForUMReportExport(
                LocalDate.of(2021, 8, 1),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(RETURN_CORR_TRANS, transactions);
    }
}
