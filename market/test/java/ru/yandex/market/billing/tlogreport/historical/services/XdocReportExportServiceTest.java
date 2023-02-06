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

class XdocReportExportServiceTest extends FunctionalTest {

    private static final Instant TRANSACTION_INSTANT =
            LocalDateTime.of(2021, 8, 1, 9, 33, 15).toInstant(ZoneOffset.UTC);
    private static final LocalDate TRANSACTION_DATE = LocalDate.of(2021, 8, 1);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TRANSACTION_INSTANT, ZoneOffset.UTC
    );

    private static final List<TransactionReportLogItem> XDOC_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("ff_xdoc_supply")
                    .setPreviousTransactionId(null)
                    .setAmount(14000L)
                    .setPayload2("{\"ff_request_id\":2,\"id\":\"0000006585\",\"is_box\":\"палета\",\"tariff\":140.00," +
                            "\"box_count\":1,\"service_datetime\":\"2021-08-01T10:33:15\"}"
                    ),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("ff_xdoc_supply")
                    .setPreviousTransactionId(null)
                    .setAmount(50000L)
                    .setPayload2("{\"ff_request_id\":1,\"id\":\"0000005782\",\"is_box\":\"коробка\"," +
                            "\"tariff\":250.00,\"box_count\":2,\"service_datetime\":\"2021-08-01T11:33:15\"}")
    );


    @Autowired
    @Qualifier("oraJdbcTemplate")
    private NamedParameterJdbcTemplate oraJdbcTemplate;

    private XdocReportDao xdocReportDao;

    @BeforeEach
    public void init() {
        xdocReportDao = new XdocReportDao(oraJdbcTemplate, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Сбор транзакции для xdoc_supply_billed_amount")
    @DbUnitDataSet(before = "csv/XdocReportExportServiceTest.before.csv")
    public void xdocTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = xdocReportDao.getXdocBilledAmountsForUMReportExport(
                LocalDate.now(FIXED_CLOCK),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(XDOC_TRANS, transactions);
    }
}
