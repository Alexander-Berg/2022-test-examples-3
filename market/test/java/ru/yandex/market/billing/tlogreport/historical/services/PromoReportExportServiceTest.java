package ru.yandex.market.billing.tlogreport.historical.services;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.tlogreport.historical.model.TransactionReportLogItem;
import ru.yandex.market.common.test.db.DbUnitDataSet;

class PromoReportExportServiceTest extends FunctionalTest {

    private static final Instant TRANSACTION_INSTANT =
            LocalDateTime.of(2021, 8, 1, 9, 33, 15).toInstant(ZoneOffset.UTC);
    private static final LocalDate TRANSACTION_DATE = LocalDate.of(2021, 8, 1);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TRANSACTION_INSTANT, ZoneOffset.UTC
    );

    private static final List<TransactionReportLogItem> CABA_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(774L)
                    .setProduct("cpa_auction_promotion")
                    .setPreviousTransactionId(null)
                    .setAmount(20000L)
                    .setPayload2(
                            "{\"order_id\":10001,\"shop_sku\":\"test shop sku 22\",\"offer_name\":\"test offer 22\"," +
                            "\"category_name\":\"test category 1\",\"price\":5.67,\"count\":1," +
                            "\"service_name\":\"Расходы на рекламные кампании\",\"bet\":3.43," +
                            "\"service_datetime\":\"2021-08-01T00:00:00\",\"cpa_promotion_bonus_spent\":2.43}"
                    ),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(774L)
                    .setProduct("cpa_auction_promotion")
                    .setPreviousTransactionId(null)
                    .setAmount(30000L)
                    .setPayload2(
                            "{\"order_id\":10001,\"shop_sku\":\"test shop sku 44\",\"offer_name\":\"test offer 44\"," +
                            "\"category_name\":\"test category 2\",\"price\":567.00,\"count\":1," +
                            "\"service_name\":\"Расходы на рекламные кампании\",\"bet\":52.91," +
                            "\"service_datetime\":\"2021-08-01T00:00:00\"}")
    );

    private static final List<TransactionReportLogItem> OPERATION_TRANS = List.of(
            new TransactionReportLogItem()
                    .setTransactionId(-97L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(1L)
                    .setProduct("cpa_auction_promotion")
                    .setPreviousTransactionId(null)
                    .setAmount(-60000L)
                    .setPayload2("{\"service_name\":\"Корректировка суммы списания\",\"service_datetime\":\"2021-08-01T00:00:00\"}"
                    ),
            new TransactionReportLogItem()
                    .setTransactionId(-98L)
                    .setEventTime(TRANSACTION_DATE)
                    .setTransactionTime(TRANSACTION_DATE)
                    .setSupplierId(2L)
                    .setProduct("cpa_auction_promotion")
                    .setPreviousTransactionId(null)
                    .setAmount(150000L)
                    .setPayload2("{\"service_name\":\"Корректировка суммы списания\",\"service_datetime\":\"2021-08-01T00:00:00\"}")
    );


    @Autowired
    private PromoAuctionReportDao promoAuctionReportDao;

    @Test
    @DisplayName("Сбор транзакции для cpa_auction_promotion")
    @DbUnitDataSet(before = "csv/PromoReportExportServiceTest.before.csv")
    public void cabaTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = promoAuctionReportDao.getPromotionBilledAmountsForUMReportExport(
                LocalDate.now(FIXED_CLOCK),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(CABA_TRANS, transactions);
    }

    @Test
    @DisplayName("Сбор транзакции для operation")
    @DbUnitDataSet(before = "csv/PromoReportExportServiceTest.before.csv")
    public void promoOperationTransactionForReportCollectionTest() {
        List<TransactionReportLogItem> transactions = promoAuctionReportDao.getOperationBilledAmountsForUMReportExport(
                LocalDate.now(FIXED_CLOCK),
                1L,
                -96L,
                2
        );
        Assertions.assertEquals(2, transactions.size());
        Assertions.assertEquals(OPERATION_TRANS, transactions);
    }
}
