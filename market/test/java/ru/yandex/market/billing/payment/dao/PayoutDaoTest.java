package ru.yandex.market.billing.payment.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.core.OperatingUnit;
import ru.yandex.market.billing.payment.model.Payout;
import ru.yandex.market.billing.payment.model.YtPayoutDto;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.payment.EntityType;
import ru.yandex.market.core.payment.PaymentOrderCurrency;
import ru.yandex.market.core.payment.PayoutProductType;
import ru.yandex.market.core.payment.PaysysTypeCc;
import ru.yandex.market.core.payment.TransactionType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasAmount;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasCheckouterId;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasCreatedAt;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasCurrency;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasEntityId;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasEntityType;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasOrderId;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasOrgId;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasPartnerId;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasPayoutGroupId;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasPayoutId;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasPaysysPartnerId;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasPaysysType;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasProductType;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasTransactionType;
import static ru.yandex.market.billing.payment.matcher.YtPayoutDtoMatcher.hasTrantime;

/**
 * Тесты для {@link PayoutDao}
 */
class PayoutDaoTest extends FunctionalTest {

    private static final LocalDate TEST_DATE = LocalDate.of(2021, 8, 1);
    private static final LocalDate TEST_DRAFT_DATE = LocalDate.of(2021, 7, 21);

    @Autowired
    private PayoutDao payoutDao;

    @DisplayName("Формирование черновиков команд на выплату")
    @Test
    @DbUnitDataSet(
            before = "PayoutDaoTest.testGetPaymentOrderDraftInfo.before.csv",
            after = "PayoutDaoTest.testGetPaymentOrderDraftInfo.after.csv"
    )
    void testGetCountOfChangePaymentOrderDraft() {
        Long count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE,
                10
        );
        assertEquals(9, count);

        count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE,
                3
        );
        assertEquals(0, count);

        count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE.plusDays(2),
                3
        );
        assertEquals(2, count);
    }

    @DisplayName("Формирование черновиков команд на выплату с chunk_size")
    @Test
    @DbUnitDataSet(
            before = "PayoutDaoTest.testGetPaymentOrderDraftInfo.before.csv",
            after = "PayoutDaoTest.testGetPaymentOrderDraftInfo.after.csv"
    )
    void testGetCountOfChangePaymentOrderDraftChunk() {
        Long count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE,
                5
        );
        assertEquals(5, count);

        count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE,
                4
        );
        assertEquals(4, count);

        count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE,
                3
        );
        assertEquals(0, count);

        count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE.plusDays(2),
                3
        );
        assertEquals(2, count);
    }

    @DisplayName("Формирование черновиков команд на выплату с обновлением существующих черновиков")
    @Test
    @DbUnitDataSet(
            before = "PayoutDaoTest.testGetPaymentOrderDraftInfoWithUpdate.before.csv",
            after = "PayoutDaoTest.testGetPaymentOrderDraftInfoWithUpdate.after.csv"
    )
    void testGetCountOfChangePaymentOrderDraftWithUpdate() {
        Long count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE,
                10
        );
        assertEquals(9, count);

        count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE.plusDays(2),
                10
        );
        assertEquals(2, count);

        count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE.plusDays(2),
                3
        );
        assertEquals(0, count);
    }

    @DisplayName("Формирование черновиков команд на выплату с существующими PROCESSED черновиками")
    @Test
    @DbUnitDataSet(
            before = "PayoutDaoTest.testGetPaymentOrderDraftInfoProcessed.before.csv",
            after = "PayoutDaoTest.testGetPaymentOrderDraftInfoProcessed.after.csv"
    )
    void testGetCountOfChangePaymentOrderDraftMinAmount() {
        Long count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE,
                10
        );
        assertEquals(9, count);

        count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE.plusDays(2),
                10
        );
        assertEquals(2, count);

        count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE.plusDays(2),
                3
        );
        assertEquals(0, count);
    }

    @DisplayName("Формирование черновиков команд на выплату с несколькими кривыми контрактами")
    @Test
    @DbUnitDataSet(
            before = "PayoutDaoTest.testGetCountOfChangePaymentOrderDraftContract.before.csv",
            after = "PayoutDaoTest.testGetCountOfChangePaymentOrderDraftContract.after.csv"
    )
    void testGetCountOfChangePaymentOrderDraftContract() {
        Long count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE,
                10
        );
        assertEquals(6, count);

        count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE.plusDays(2),
                10
        );
        assertEquals(1, count);

        count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE.plusDays(2),
                3
        );
        assertEquals(0, count);
    }

    @DisplayName("Формирование черновиков команд на выплату с несколькими кривыми контрактами по client_id")
    @Test
    @DbUnitDataSet(
            before = "PayoutDaoTest.testGetCountOfChangePaymentOrderDraftContractClient.before.csv",
            after = "PayoutDaoTest.testGetCountOfChangePaymentOrderDraftContractClient.after.csv"
    )
    void testGetCountOfChangePaymentOrderDraftContractClient() {
        Long count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE,
                10
        );
        assertEquals(0, count);

        count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE.plusDays(2),
                10
        );
        assertEquals(0, count);
    }

    @DisplayName("Вставить новые записи в таблицу payout")
    @Test
    @DbUnitDataSet(
            after = "PayoutDaoTest.testInsertNonExistingPayouts.after.csv"
    )
    void testInsertNonExistingPayouts() {
        LocalDateTime ldt = LocalDateTime.of(2021, 11, 11, 10, 0, 0);
        List<Payout> payouts = List.of(
                Payout.builder()
                        .setPayoutId(-1L)
                        .setEntityId(1L)
                        .setEntityType(EntityType.ITEM)
                        .setCheckouterId(20L)
                        .setTransactionType(TransactionType.PAYMENT)
                        .setPayoutProductType(PayoutProductType.SUBSIDY)
                        .setPaysysType(PaysysTypeCc.ACC_SBERBANK)
                        .setOrderId(300L)
                        .setPartnerId(4000L)
                        .setTrantime(ldt.atZone(ZoneId.systemDefault()).toInstant())
                        .setAmount(50000L)
                        .setPayoutGroupId(null)
                        .setPaysysPartnerId(600000L)
                        .setCurrency(PaymentOrderCurrency.RUB)
                        .setOrgId(OperatingUnit.YANDEX_MARKET)
                        .setAccrualId(1L)
                        .build(),
                Payout.builder()
                        .setPayoutId(-1L)
                        .setEntityId(5L)
                        .setEntityType(EntityType.ITEM)
                        .setCheckouterId(25L)
                        .setTransactionType(TransactionType.REFUND)
                        .setPayoutProductType(PayoutProductType.PARTNER_PAYMENT)
                        .setPaysysType(PaysysTypeCc.ACC_TINKOFF_CREDIT)
                        .setOrderId(305L)
                        .setPartnerId(4005L)
                        .setTrantime(ldt.atZone(ZoneId.systemDefault()).toInstant())
                        .setAmount(50005L)
                        .setPayoutGroupId(null)
                        .setPaysysPartnerId(600005L)
                        .setCurrency(PaymentOrderCurrency.ILS)
                        .setOrgId(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .build()
        );
        payoutDao.insertPayoutsIfNotExists(payouts);
    }

    @DisplayName("Попытка вставить записи в таблицу payout, которые уже существуют")
    @Test
    @DbUnitDataSet(
            before = "PayoutDaoTest.failInsertExistingPayouts.before.csv",
            after = "PayoutDaoTest.failInsertExistingPayouts.after.csv"
    )
    void failInsertExistingPayouts() {
        LocalDateTime ldt = LocalDateTime.of(2021, 11, 11, 10, 0, 0);
        List<Payout> payouts = List.of(
                Payout.builder()
                        .setPayoutId(-1L)
                        .setEntityId(1L)
                        .setEntityType(EntityType.ITEM)
                        .setCheckouterId(20L)
                        .setTransactionType(TransactionType.PAYMENT)
                        .setPayoutProductType(PayoutProductType.SUBSIDY)
                        .setPaysysType(PaysysTypeCc.ACC_SBERBANK)
                        .setOrderId(0L)
                        .setPartnerId(0L)
                        .setTrantime(ldt.atZone(ZoneId.systemDefault()).toInstant())
                        .setAmount(0L)
                        .setPayoutGroupId(null)
                        .setPaysysPartnerId(0L)
                        .setCurrency(PaymentOrderCurrency.ILS)
                        .setOrgId(OperatingUnit.YANDEX_MARKET_ISRAEL)
                        .setAccrualId(1L)
                        .build()
        );
        payoutDao.insertPayoutsIfNotExists(payouts);
    }

    @DisplayName("Получить обработанные выплаты из Постгреса")
    @Test
    @DbUnitDataSet(
            before = "PayoutDaoTest.testGetProcessedPayouts.before.csv"
    )
    void getProcessedPayouts() {
        List<YtPayoutDto> processedPayouts = payoutDao.getProcessedPayouts(TEST_DATE);
        assertEquals(1, processedPayouts.size());

        assertThat(processedPayouts,
                Matchers.contains(
                        allOf(
                                hasPayoutId(11L),
                                hasAmount(3000L),
                                hasCheckouterId(13L),
                                hasEntityId(2L),
                                hasEntityType(EntityType.ITEM),
                                hasTransactionType(TransactionType.PAYMENT),
                                hasProductType(PayoutProductType.PARTNER_PAYMENT),
                                hasPaysysType(PaysysTypeCc.ACC_SBERBANK),
                                hasCurrency(PaymentOrderCurrency.RUB),
                                hasOrderId(5L),
                                hasPartnerId(6L),
                                hasTrantime(LocalDateTime.of(2021, 8, 1, 3, 0)
                                        .atZone(ZoneId.systemDefault()).toInstant()),
                                hasCreatedAt(LocalDateTime.of(2021, 8, 1, 0, 0)
                                        .atZone(ZoneId.systemDefault()).toInstant()),
                                hasPayoutGroupId(2L),
                                hasPaysysPartnerId(1L),
                                hasOrgId(OperatingUnit.YANDEX_MARKET)
                        )));
    }

    @DisplayName("Получить количество необработанных выплат из Постгреса")
    @Test
    @DbUnitDataSet(
            before = "PayoutDaoTest.testGetUnprocessedPayoutCountByDate.before.csv"
    )
    void getUnprocessedPayoutCountByDate() {
        long count = payoutDao.getUnprocessedPayoutCountByDate(TEST_DATE);
        assertEquals(1, count);
    }

    @DisplayName("Не нужно собирать в драфты выплаты по 1p. Временный тест, надо выпилить")
    @Test
    @DbUnitDataSet(
            before = "PayoutDaoTest.testGetPaymentOrderDraftInfo1p.before.csv",
            after = "PayoutDaoTest.testGetPaymentOrderDraftInfo1p.after.csv"
    )
    void testGetCountOfChangePaymentOrderDraft1p() {
        Long count = payoutDao.updateAndGetCountOfChangePaymentOrderDraft(
                TEST_DRAFT_DATE,
                10
        );
        assertEquals(0, count);
    }
}
