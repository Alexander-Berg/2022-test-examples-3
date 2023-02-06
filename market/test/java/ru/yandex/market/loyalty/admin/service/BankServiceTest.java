package ru.yandex.market.loyalty.admin.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientException;

import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.core.service.bank.BankCalculatorService;
import ru.yandex.market.loyalty.core.service.bank.cashback.calculator.model.CalculatorResponse;
import ru.yandex.market.loyalty.core.service.bank.cashback.calculator.model.CashbackRule;
import ru.yandex.market.loyalty.core.service.bank.cashback.calculator.model.CashbackRulePercent;
import ru.yandex.market.loyalty.core.utils.BankTestUtils;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_YANDEX_UID;

@TestFor({BankCalculatorService.class, BankCoreService.class})
public class BankServiceTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private BankCalculatorService bankCalculatorService;
    @Autowired
    private BankCoreService bankCoreService;
    @Autowired
    private BankTestUtils bankTestUtils;

    @Test
    public void shouldGetSuccessfulCalculatorResult() {
        var response = new CalculatorResponse(CashbackRule.builder()
                .setMaxAmount("100.00")
                .setPercent(new CashbackRulePercent("10.00"))
                .setRuleIds(List.of("1"))
                .build());

        bankTestUtils.mockCalculatorWithSuccessResponse(response);

        var result = bankCalculatorService.getCashbackCalculationForUser(DEFAULT_UID);
        assertTrue("Empty result", result.isPresent());
        assertThat(result.get(), allOf(
                hasProperty("maxAmount", equalTo(response.getCashbackRule().getMaxAmount())),
                hasProperty("percent",
                        hasProperty("amount", equalTo(response.getCashbackRule().getPercent().getAmount()))
                ),
                hasProperty("ruleIds", containsInAnyOrder(response.getCashbackRule().getRuleIds().toArray()))
        ));
    }

    @Test
    public void shouldGetEmptyCalculatorResultOnRequestFail() {
        bankTestUtils.mockCalculatorWithThrowable(new RuntimeException());

        var result = bankCalculatorService.getCashbackCalculationForUser(DEFAULT_UID);
        assertTrue("Not empty result", result.isEmpty());
    }

    @Test
    public void shouldGetEmptyCalculatorResultOnEmptyUid() {
        var response = new CalculatorResponse(CashbackRule.builder()
                .setMaxAmount("100.00")
                .setPercent(new CashbackRulePercent("10.00"))
                .setRuleIds(List.of("1"))
                .build());

        bankTestUtils.mockCalculatorWithSuccessResponse(response);

        var result = bankCalculatorService.getCashbackCalculationForUser(null, null, null, null);
        assertTrue("Not empty result", result.isEmpty());
    }

    @Test
    public void shouldNotThrowsOnSuccessfulCoreResult() {
        bankTestUtils.mockCoreWithSuccess();
        bankCoreService.saveDeliveredCashbackAmount(
                "test_rrn",
                "test_authCode",
                DEFAULT_YANDEX_UID,
                "test_paymentId",
                BigDecimal.ZERO,
                null,
                0L
        );
    }

    @Test
    public void shouldThrowsMarketLoyaltyExceptionOnRestException() {
        bankTestUtils.mockCoreWithThrowable(new RestClientException("test"));
        MarketLoyaltyException exception = null;
        try {
            bankCoreService.saveDeliveredCashbackAmount(null);
        } catch (MarketLoyaltyException e) {
            exception = e;
        }

        assertEquals(exception.getMarketLoyaltyErrorCode(), MarketLoyaltyErrorCode.YANDEX_BANK_EXCEPTION);
    }

    @Test
    public void shouldThrowsMarketLoyaltyExceptionOnRuntimeException() {
        bankTestUtils.mockCoreWithThrowable(new RuntimeException("test"));
        MarketLoyaltyException exception = null;
        try {
            bankCoreService.saveDeliveredCashbackAmount(null);
        } catch (MarketLoyaltyException e) {
            exception = e;
        }

        assertEquals(exception.getMarketLoyaltyErrorCode(), MarketLoyaltyErrorCode.OTHER_ERROR);
    }
}
