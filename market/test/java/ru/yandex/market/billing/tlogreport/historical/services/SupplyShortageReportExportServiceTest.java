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

class SupplyShortageReportExportServiceTest extends FunctionalTest {

    private static final LocalDate TRANSACTION_DATE = LocalDate.of(2021, 8, 1);

    private static final List<TransactionReportLogItem> SSBA_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("fee")
                    .setPreviousTransactionId(null)
                    .setAmount(2500L)
                    .setPayload2("{\"is_correction\":false,\"order_creation_datetime\":\"2021-08-01T00:00:00\"," +
                            "\"shop_sku\":\"тестовый sku 2\",\"offer_name\":\"тестовый оффер 2\",\"price\":0.0," +
                            "\"client_price\":0.0,\"count\":2," +
                            "\"service_name\":\"Размещение товаров на витрине\",\"tariff\":25.0," +
                            "\"tariff_dimension\":\"руб.\",\"service_datetime\":\"2021-08-01T00:00:00\"}"),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("fee")
                    .setPreviousTransactionId(null)
                    .setAmount(1878L)
                    .setPayload2("{\"is_correction\":false,\"order_creation_datetime\":\"2021-08-01T00:00:00\"," +
                            "\"shop_sku\":\"тестовый sku 3\",\"offer_name\":\"тестовый оффер 3\",\"price\":0.0," +
                            "\"client_price\":0.0,\"count\":3," +
                            "\"service_name\":\"Размещение товаров на витрине\",\"tariff\":6.26," +
                            "\"tariff_dimension\":\"руб.\",\"service_datetime\":\"2021-08-01T00:00:00\"}")
    );


    @Autowired
    private SupplyShortageReportDao supplyShortageReportDao;

    @Test
    @DisplayName("Сбор транзакции для supply_shortage_billed_amounts")
    @DbUnitDataSet(before = "csv/SupplyShortageReportExportServiceTest.before.csv")
    public void ssbaTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions =
                supplyShortageReportDao.getSupplyShortageBilledAmountsForUMReportExport(
                        LocalDate.of(2021, 8, 1),
                        1L,
                        -96L,
                        2
                );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(SSBA_TRANS, transactions);
    }
}
