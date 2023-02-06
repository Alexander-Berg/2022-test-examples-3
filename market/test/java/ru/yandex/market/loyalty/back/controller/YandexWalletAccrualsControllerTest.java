package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.YandexWalletAccrualDetails;
import ru.yandex.market.loyalty.api.model.YandexWalletAccrualRequest;
import ru.yandex.market.loyalty.api.model.YandexWalletAccrualRequestList;
import ru.yandex.market.loyalty.api.model.YandexWalletAccrualResponse;
import ru.yandex.market.loyalty.api.model.YandexWalletAccrualResponseList;
import ru.yandex.market.loyalty.api.model.YandexWalletAccrualStatus;
import ru.yandex.market.loyalty.api.model.YandexWalletRevertAccrualRequest;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDao;
import ru.yandex.market.loyalty.core.dao.YandexWalletTransactionDetailsDao;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletRefundTransactionStatus;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionPriority;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus.CONFIRMED;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

/**
 * @author <a href="mailto:khamitov-rail@yandex-team.ru">Rail Khamitov</a>
 * @date 09.03.2021
 */
@TestFor(YandexWalletAccrualsController.class)
public class YandexWalletAccrualsControllerTest extends MarketLoyaltyBackMockedDbTestBase {

    @Autowired
    private YandexWalletTransactionDao yandexWalletTransactionDao;
    @Autowired
    private YandexWalletTransactionDetailsDao yandexWalletTransactionDetailsDao;
    @Autowired
    private PromoManager promoManager;

    @Test
    public void shouldSuccessGenerateYandexWalletTransaction() {

        YandexWalletAccrualRequest request = new YandexWalletAccrualRequest(
                "test",
                "reference-id",
                "productId", 100L,
                BigDecimal.valueOf(100L),
                Map.of("cashback_service", "market", "has_plus", "true"),
                null
        );
        YandexWalletAccrualResponseList result = marketLoyaltyClient.accrual(new YandexWalletAccrualRequestList(
                Collections.singletonList(request)));
        assertEquals(1, result.getResult().size());
        assertEquals(YandexWalletAccrualStatus.SUCCESS, result.getResult().get(0).getStatus());
        List<YandexWalletTransaction> yandexWalletTransactions = yandexWalletTransactionDao.queryAll();
        assertEquals(1, yandexWalletTransactions.size());
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldThrowErrorIfAccrualNotExists() {
        marketLoyaltyClient.emergencyRevertAccrual(new YandexWalletRevertAccrualRequest(
                0L,
                DEFAULT_UID,
                "productId",
                "test"
        ));
    }

    @Test
    public void shouldRefundAccrual() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        YandexWalletTransaction t = yandexWalletTransactionDao.save(createSomeYandexWalletTransaction(DEFAULT_UID,
                promo.getId(), "CASHBACK_EMIT", CONFIRMED));

        marketLoyaltyClient.emergencyRevertAccrual(new YandexWalletRevertAccrualRequest(t.getId(),
                DEFAULT_UID, "CASHBACK_EMIT", null));

        assertThat(
                yandexWalletTransactionDao.findById(t.getId()).get(),
                hasProperty("refundStatus", equalTo(YandexWalletRefundTransactionStatus.IN_QUEUE))
        );
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldNotRefundAccrualAfterTwoHours() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        YandexWalletTransaction t = yandexWalletTransactionDao.save(createSomeYandexWalletTransaction(DEFAULT_UID,
                promo.getId(), "CASHBACK_EMIT", CONFIRMED));

        clock.spendTime(Duration.ofHours(2).plus(1, ChronoUnit.SECONDS));

        marketLoyaltyClient.emergencyRevertAccrual(new YandexWalletRevertAccrualRequest(t.getId(),
                DEFAULT_UID, "CASHBACK_EMIT", null));
    }

    @Test(expected = MarketLoyaltyException.class)
    public void shouldNotRefundAccrualForIncorrectProductId() {
        Promo promo = promoManager.createCashbackPromo(PromoUtils.Cashback.defaultPercent(BigDecimal.TEN));

        YandexWalletTransaction t = yandexWalletTransactionDao.save(createSomeYandexWalletTransaction(DEFAULT_UID,
                promo.getId(), "CASHBACK_EMIT", CONFIRMED));

        marketLoyaltyClient.emergencyRevertAccrual(new YandexWalletRevertAccrualRequest(t.getId(),
                DEFAULT_UID, "ANOTHER_CASHBACK_EMIT", null));
    }

    @Test
    public void shouldCreateSingleTransactionWithDetails() {
        YandexWalletAccrualResponse yandexWalletAccrualResponse = marketLoyaltyClient.accrualSingle(
                new YandexWalletAccrualRequest(
                        "campaign name",
                        "ref_id",
                        "product_id",
                        123L,
                        BigDecimal.valueOf(500),
                        Map.of("product_id", "sorry_comp"),
                        new YandexWalletAccrualDetails(
                                "telerman",
                                123L,
                                UUID.randomUUID().toString(),
                                "Test",
                                null,
                                false
                        )
                )
        );
        assertThat(yandexWalletAccrualResponse, hasProperty("status", equalTo(YandexWalletAccrualStatus.SUCCESS)));
        assertThat(yandexWalletTransactionDao.findAll(), hasSize(1));
        assertThat(yandexWalletTransactionDetailsDao.findAll(), hasSize(1));
    }


    @NotNull
    private YandexWalletTransaction createSomeYandexWalletTransaction(long uid, long promoId,
                                                                      String productId,
                                                                      YandexWalletTransactionStatus status) {
        return new YandexWalletTransaction(
                null,
                null,
                uid,
                BigDecimal.ONE,
                null,
                status,
                null,
                0,
                Timestamp.from(clock.instant()),
                "xxxyyyzzz",
                null,
                null,
                null,
                promoId,
                productId,
                YandexWalletTransactionPriority.HIGH,
                null,
                0,
                null,
                null,
                YandexWalletRefundTransactionStatus.NOT_QUEUED,
                null,
                null,
                "6ty78jdhyfi889kdjh7nm",
                null,
                null,
                null,
                null
        );
    }

}
