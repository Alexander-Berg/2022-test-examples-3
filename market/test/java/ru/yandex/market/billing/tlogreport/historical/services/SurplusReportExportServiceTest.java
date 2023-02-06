package ru.yandex.market.billing.tlogreport.historical.services;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

class SurplusReportExportServiceTest extends FunctionalTest {

    private static final Instant TRANSACTION_INSTANT =
            LocalDateTime.of(2021, 8, 1, 9, 33, 15).toInstant(ZoneOffset.UTC);
    private static final LocalDate TRANSACTION_DATE = LocalDate.of(2021, 8, 1);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TRANSACTION_INSTANT, ZoneId.of("+03")
    );

    private static final List<TransactionReportLogItem> SURPLUS_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("ff_surplus_supply")
                    .setPreviousTransactionId(null)
                    .setAmount(40000L)
                    .setPayload2("{\"is_correction\":false,\"ff_request_id\":505,\"id\":\"100001\"," +
                            "\"shop_sku\":\"test sku\",\"tariff\":200.00,\"count\":2," +
                            "\"service_datetime\":\"2021-08-01T13:00:00\",\"type\":\"начисление\"}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("ff_surplus_supply")
                    .setPreviousTransactionId(null)
                    .setAmount(90000L)
                    .setPayload2("{\"is_correction\":false,\"ff_request_id\":606,\"id\":\"200002\"," +
                            "\"shop_sku\":\"test sku\",\"tariff\":300.00,\"count\":3," +
                            "\"service_datetime\":\"2021-08-01T14:00:00\",\"type\":\"начисление\"}")

    );


    private static final List<TransactionReportLogItem> SURPLUS_CORR_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("ff_surplus_supply")
                    .setPreviousTransactionId(null)
                    .setAmount(7000L)
                    .setPayload2("{\"is_correction\":true,\"ff_request_id\":505,\"id\":\"100001\"," +
                            "\"shop_sku\":\"test sku\",\"tariff\":200.00,\"count\":2," +
                            "\"service_datetime\":\"2021-08-01T13:00:00\",\"type\":\"корректировка\"}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("ff_surplus_supply")
                    .setPreviousTransactionId(null)
                    .setAmount(-8000L)
                    .setPayload2("{\"is_correction\":true,\"ff_request_id\":606,\"id\":\"200002\"," +
                            "\"shop_sku\":\"test sku\",\"tariff\":300.00,\"count\":3," +
                            "\"service_datetime\":\"2021-08-01T14:00:00\",\"type\":\"корректировка\"}")
    );

    @Autowired
    @Qualifier("oraJdbcTemplate")
    private NamedParameterJdbcTemplate oraJdbcTemplate;

    private SurplusReportDao surplusReportDao;

    @BeforeEach
    public void init() {
        surplusReportDao = new SurplusReportDao(oraJdbcTemplate, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Сбор транзакции для surplus_delivery_blld_amounts")
    @DbUnitDataSet(before = "csv/SurplusReportExportServiceTest.before.csv")
    public void surplusTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = surplusReportDao.getSurplusBilledAmountsForUMReportExport(
                LocalDate.of(2021, 8, 1),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(SURPLUS_TRANS, transactions);
    }

    @Test
    @DisplayName("Сбор транзакции для surplus_delivery_blld_corr")
    @DbUnitDataSet(before = "csv/SurplusReportExportServiceTest.before.csv")
    public void surplusCorrTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = surplusReportDao.getSurplusBilledAmountsCorrsForUMReportExport(
                LocalDate.of(2021, 8, 1),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(SURPLUS_CORR_TRANS, transactions);
    }
}
