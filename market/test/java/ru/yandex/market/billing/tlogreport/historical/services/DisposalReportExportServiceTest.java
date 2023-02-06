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

class DisposalReportExportServiceTest extends FunctionalTest {

    private static final Instant TRANSACTION_INSTANT =
            LocalDateTime.of(2021, 8, 1, 9, 33, 15).toInstant(ZoneOffset.UTC);
    private static final LocalDate TRANSACTION_DATE = LocalDate.of(2021, 8, 1);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TRANSACTION_INSTANT, ZoneOffset.UTC
    );

    private static final List<TransactionReportLogItem> DISPOSAL_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("self_requested_disposal")
                    .setPreviousTransactionId(null)
                    .setAmount(6000L)
                    .setPayload2("{\"shop_sku\":\"test sku 2\",\"offer_name\":\"test item name\",\"count\":2,\"weight\":0.03," +
                            "\"length\":18,\"width\":8,\"height\":1,\"dimensions_sum\":27,\"service_name\":" +
                            "\"Организация утилизации\",\"tariff\":30.00,\"request_datetime\":" +
                            "\"2021-08-01T07:00:00\",\"service_datetime\":\"2021-08-01T10:33:15\"}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("self_requested_disposal")
                    .setPreviousTransactionId(null)
                    .setAmount(12000L)
                    .setPayload2("{\"shop_sku\":\"test sku 1\",\"offer_name\":\"test sku 1\",\"count\":3,\"weight\":0.03," +
                            "\"length\":18,\"width\":8,\"height\":1,\"dimensions_sum\":27,\"service_name\":" +
                            "\"Организация утилизации\",\"tariff\":40.00,\"request_datetime\":" +
                            "\"2021-08-01T08:00:00\",\"service_datetime\":\"2021-08-01T11:33:15\"}")

    );

    @Autowired
    @Qualifier("oraJdbcTemplate")
    private NamedParameterJdbcTemplate oraJdbcTemplate;

    private DisposalReportDao disposalReportDao;

    @BeforeEach
    public void init() {
        disposalReportDao = new DisposalReportDao(oraJdbcTemplate, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Сбор транзакции для disposal_billed_amount")
    @DbUnitDataSet(before = "csv/DisposalReportExportServiceTest.before.csv")
    public void disposalTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = disposalReportDao.getDisposalBilledAmountsForUMReportExport(
                LocalDate.now(FIXED_CLOCK),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(DISPOSAL_TRANS, transactions);
    }
}
