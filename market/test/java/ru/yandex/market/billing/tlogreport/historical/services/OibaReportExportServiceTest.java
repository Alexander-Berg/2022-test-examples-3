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

class OibaReportExportServiceTest extends FunctionalTest {

    private static final Instant TRANSACTION_INSTANT =
            LocalDateTime.of(2021, 8, 1, 9, 33, 15).toInstant(ZoneOffset.UTC);
    private static final LocalDate TRANSACTION_DATE = LocalDate.of(2021, 8, 1);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TRANSACTION_INSTANT, ZoneOffset.UTC
    );

    private static final List<TransactionReportLogItem> OIBA_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("fee")
                    .setPreviousTransactionId(null)
                    .setAmount(200L)
                    .setPayload2("{\"is_correction\":false,\"item_id\":1,\"order_id\":10001," +
                            "\"order_creation_datetime\":\"2021-08-01T07:33:51\"," +
                            "\"shop_sku\":\"тестовый sku\",\"offer_name\":\"тестовый оффер\"," +
                            "\"price\":2.0,\"client_price\":2.0,\"count\":1,\"batch_size\":1,\"weight\":1," +
                            "\"length\":1,\"width\":1,\"height\":1,\"dimensions_sum\":3," +
                            "\"service_name\":\"Размещение товаров на витрине\",\"tariff\":2.0," +
                            "\"tariff_dimension\":\"%\",\"min_tariff\":0.1,\"max_tariff\":100.0," +
                            "\"service_before_tariff\":2.00,\"service_datetime\":\"2021-08-01T09:33:15\"," +
                            "\"is_cash_only\":false,\"region_to\":42,\"warehouse_id\":1}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("delivery_to_customer")
                    .setPreviousTransactionId(null)
                    .setAmount(303L)
                    .setPayload2("{\"is_correction\":false,\"item_id\":2,\"order_id\":10002," +
                            "\"order_creation_datetime\":\"2021-08-01T07:33:51\"," +
                            "\"shop_sku\":\"тестовый sku 2\",\"offer_name\":\"тестовый оффер 2\",\"price\":1.01," +
                            "\"client_price\":1.01,\"count\":3,\"batch_size\":1,\"weight\":2,\"length\":2,\"width\":2," +
                            "\"height\":2,\"dimensions_sum\":6,\"service_name\":\"Доставка покупателю\"," +
                            "\"tariff\":1.01,\"tariff_dimension\":\"руб.\",\"min_tariff\":0.1,\"max_tariff\":100.0," +
                            "\"service_before_tariff\":3.03,\"service_datetime\":\"2021-08-01T09:33:15\"," +
                            "\"is_cash_only\":false,\"region_to\":42,\"warehouse_id\":1}")

    );


    private static final List<TransactionReportLogItem> OIBA_CORR_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("delivery_to_customer")
                    .setPreviousTransactionId(null)
                    .setAmount(222L)
                    .setPayload2("{\"is_correction\":true,\"item_id\":1,\"order_id\":10001," +
                            "\"order_creation_datetime\":\"2021-08-01T07:33:51\"," +
                            "\"shop_sku\":\"тестовый sku\",\"offer_name\":\"тестовый оффер\"," +
                            "\"price\":2.0,\"client_price\":2.0,\"count\":1,\"batch_size\":1,\"weight\":1," +
                            "\"length\":1,\"width\":1,\"height\":1,\"dimensions_sum\":3," +
                            "\"service_name\":\"Корректировка за доставку покупателю\",\"tariff\":2.22," +
                            "\"tariff_dimension\":\"руб.\",\"service_datetime\":\"2021-08-01T09:33:15\"," +
                            "\"is_cash_only\":false,\"region_to\":42,\"warehouse_id\":1}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("fee")
                    .setPreviousTransactionId(null)
                    .setAmount(333L)
                    .setPayload2("{\"is_correction\":true,\"item_id\":2,\"order_id\":10002," +
                            "\"order_creation_datetime\":\"2021-08-01T07:33:51\"," +
                            "\"shop_sku\":\"тестовый sku 2\",\"offer_name\":\"тестовый оффер 2\",\"price\":1.01," +
                            "\"client_price\":1.01,\"count\":3,\"batch_size\":1,\"weight\":2,\"length\":2,\"width\":2," +
                            "\"height\":2,\"dimensions_sum\":6,\"service_name\":\"Корректировка комиссии\"," +
                            "\"tariff\":3.33,\"tariff_dimension\":\"руб.\"," +
                            "\"service_datetime\":\"2021-08-01T09:33:15\"," +
                            "\"is_cash_only\":false,\"region_to\":42,\"warehouse_id\":1}")
    );

    @Autowired
    @Qualifier("oraJdbcTemplate")
    private NamedParameterJdbcTemplate oraJdbcTemplate;

    private OibaReportDao oibaReportDao;

    @BeforeEach
    public void init() {
        oibaReportDao = new OibaReportDao(oraJdbcTemplate, FIXED_CLOCK);
    }

    @Test
    @DisplayName("Сбор транзакции для order_item_billed_amounts")
    @DbUnitDataSet(before = "csv/OibaReportExportServiceTest.before.csv")
    public void oibaTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = oibaReportDao.getOrderBilledAmountsForUMReportExport(
                LocalDate.of(2021, 8, 1),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(OIBA_TRANS, transactions);
    }

    @Test
    @DisplayName("Сбор транзакции для order_item_billed_amounts_corr")
    @DbUnitDataSet(before = "csv/OibaReportExportServiceTest.before.csv")
    public void oibaCorrTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = oibaReportDao.getOrderBilledAmountsCorrsForUMReportExport(
                LocalDate.of(2021, 8, 1),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(OIBA_CORR_TRANS, transactions);
    }
}
