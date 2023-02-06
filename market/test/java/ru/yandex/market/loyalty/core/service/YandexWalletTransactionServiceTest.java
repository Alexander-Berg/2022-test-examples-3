package ru.yandex.market.loyalty.core.service;

import java.math.BigDecimal;
import java.sql.Timestamp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.util.Strings;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.CashbackProps;
import ru.yandex.market.loyalty.core.model.promo.BudgetMode;
import ru.yandex.market.loyalty.core.model.promo.CashbackLevelType;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletRefundTransactionStatus;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransaction;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionPayload;
import ru.yandex.market.loyalty.core.model.wallet.YandexWalletTransactionStatus;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.util.AssertionErrors.assertFalse;
import static ru.yandex.market.loyalty.core.model.promo.cashback.CashbackSource.YANDEX_BANK;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;

//todo @TestFor(YandexWalletTransactionService.class)
public class YandexWalletTransactionServiceTest extends MarketLoyaltyCoreMockedDbTestBase {
    @Autowired
    private YandexWalletTransactionService ywtService;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    public void shouldNotSaveNullToPayloadWhenAllFilled() {
        var testLongValue = 1L;
        var testStringValue = "test";

        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.ONE, CashbackLevelType.MULTI_ORDER)
                        .setBudgetMode(BudgetMode.SYNC)
                        .setBudgetSourcePartner(testStringValue)
                        .setBudgetSourcePartnerCompensationRate((int) testLongValue)
                        .setBudgetSourcePartnerCompensationVat((int) testLongValue)
        );

        var payload = ywtService.createFixedPromoPayload(
                true,
                testLongValue,
                testStringValue,
                testStringValue,
                testStringValue,
                testStringValue,
                testStringValue,
                promo,
                BigDecimal.ONE
        );

        assertFalse("Payload contains null value", payload.contains("null"));
        assertFalse("Payload contains empty value", payload.contains("\"\""));
    }

    @Test
    public void shouldNotSaveNullToPayloadWhenAllFilledWithEmptyString() {
        var testLongValue = 1L;
        var testStringValue = Strings.EMPTY;

        var promo = promoManager.createCashbackPromo(
                PromoUtils.Cashback.defaultFixed(BigDecimal.ONE, CashbackLevelType.MULTI_ORDER)
                        .setBudgetMode(BudgetMode.SYNC)
                        .setBudgetSourcePartner(testStringValue)
                        .setBudgetSourcePartnerCompensationRate((int) testLongValue)
                        .setBudgetSourcePartnerCompensationVat((int) testLongValue)
        );

        var payload = ywtService.createFixedPromoPayload(
                true,
                testLongValue,
                testStringValue,
                testStringValue,
                testStringValue,
                testStringValue,
                testStringValue,
                promo,
                BigDecimal.ONE
        );

        assertFalse("Payload contains null value", payload.contains("null"));
        assertFalse("Payload contains empty value", payload.contains("\"\""));
    }

    @Test
    public void shouldNotSaveNullToPayloadWhenAllFilledWithNull() {
        Long testLongValue = null;
        String testStringValue = null;

        var payload = ywtService.createFixedPromoPayload(
                true,
                testLongValue,
                testStringValue,
                testStringValue,
                testStringValue,
                testStringValue,
                testStringValue
        );

        assertFalse("Payload contains null value", payload.contains("null"));
        assertFalse("Payload contains empty value", payload.contains("\"\""));
    }

    @Test
    public void testFakePromoPayload() throws JsonProcessingException {
        final CashbackProps props = CashbackProps.builder()
                .setCashbackSource(YANDEX_BANK)
                .build();
        var transaction = YandexWalletTransaction.builder()
                .setUid(DEFAULT_UID)
                .setAmount(BigDecimal.TEN)
                .setTryCount(3)
                .setRefundTryCount(3)
                .setStatus(YandexWalletTransactionStatus.FAKE_IN_QUEUE)
                .setRefundStatus(YandexWalletRefundTransactionStatus.NOT_QUEUED)
                .setCreationTime(Timestamp.from(clock.instant()))
                .setUniqueKey("uKey")
                .setProductId("pid")
                .setPayload(ywtService.createFakePromoPayload(props))
                .build();
        ywtService.save(transaction);

        transaction = ywtService.findAllByUid(DEFAULT_UID).get(0);
        YandexWalletTransactionPayload payload = transaction.getPayloadAsObject(objectMapper);
        assertThat(payload.getCashbackService(), equalTo(YANDEX_BANK.toString()));
        assertThat(payload.getBankRequestVersion(), equalTo(0L));

        ywtService.incrementPayloadBankRequestVersion(transaction);

        payload = ywtService.findAllByUid(DEFAULT_UID).get(0).getPayloadAsObject(objectMapper);
        assertThat(payload.getBankRequestVersion(), equalTo(1L));
    }
}
