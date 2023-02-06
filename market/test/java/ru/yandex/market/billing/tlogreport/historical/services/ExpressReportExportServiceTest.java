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

class ExpressReportExportServiceTest extends FunctionalTest {

    private static final Instant TRANSACTION_INSTANT =
            LocalDateTime.of(2021, 8, 1, 9, 33, 15).toInstant(ZoneOffset.UTC);
    private static final LocalDate TRANSACTION_DATE = LocalDate.of(2021, 8, 1);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TRANSACTION_INSTANT, ZoneOffset.UTC
    );

    private static final List<TransactionReportLogItem> EXPRESS_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("express_delivery_to_customer")
                    .setPreviousTransactionId(null)
                    .setAmount(2000L)
                    .setPayload2("{\"order_id\":10001,\"service_name\":\"Возврат заказа (невыкупа)\",\"tariff\":20.00," +
                            "\"tariff_dimension\":\"РУБ\",\"service_datetime\":\"2021-08-01T09:33:15\"}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("express_delivery_to_customer")
                    .setPreviousTransactionId(null)
                    .setAmount(3000L)
                    .setPayload2("{\"order_id\":10002,\"service_name\":\"Экспресс-доставка покупателю\",\"tariff\":30.00," +
                            "\"tariff_dimension\":\"РУБ\",\"service_datetime\":\"2021-08-01T09:33:15\"}")

    );


    private static final List<TransactionReportLogItem> EXPRESS_CORR_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("express_delivery_to_customer")
                    .setPreviousTransactionId(null)
                    .setAmount(200L)
                    .setPayload2("{\"order_id\":10001,\"service_name\":\"Корректировка\"," +
                            "\"service_datetime\":\"2021-08-01T09:33:15\"}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("express_delivery_to_customer")
                    .setPreviousTransactionId(null)
                    .setAmount(300L)
                    .setPayload2("{\"order_id\":10002,\"service_name\":\"Корректировка\"," +
                            "\"service_datetime\":\"2021-08-01T09:33:15\"}")
    );

    @Autowired
    @Qualifier("oraJdbcTemplate")
    private NamedParameterJdbcTemplate oraJdbcTemplate;

    private ExpressReportDao expressReportDao;

    @BeforeEach
    public void init() {
        expressReportDao = new ExpressReportDao(oraJdbcTemplate, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Сбор транзакции для express_delivery_blld_amounts")
    @DbUnitDataSet(before = "csv/ExpressReportExportServiceTest.before.csv")
    public void expressTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = expressReportDao.getExpressBilledAmountsForUMReportExport(
                LocalDate.of(2021, 8, 1),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(EXPRESS_TRANS, transactions);
    }

    @Test
    @DisplayName("Сбор транзакции для express_delivery_blld_corr")
    @DbUnitDataSet(before = "csv/ExpressReportExportServiceTest.before.csv")
    public void expressCorrTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = expressReportDao.getExpressBilledAmountsCorrsForUMReportExport(
                LocalDate.of(2021, 8, 1),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(EXPRESS_CORR_TRANS, transactions);
    }
}
