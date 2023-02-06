package ru.yandex.market.loyalty.core.utils;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.core.config.qualifier.BankCashbackCalculatorApi;
import ru.yandex.market.loyalty.core.config.qualifier.BankCashbackCoreApi;
import ru.yandex.market.loyalty.core.service.bank.cashback.calculator.model.CalculatorResponse;
import ru.yandex.market.loyalty.core.service.bank.cashback.calculator.model.CashbackRule;
import ru.yandex.market.loyalty.core.service.bank.cashback.calculator.model.CashbackRulePercent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Component
public class BankTestUtils {
    public static final BigDecimal MAX_AMOUNT_DEFAULT = BigDecimal.valueOf(1000);
    public static final BigDecimal PERCENT_DEFAULT = BigDecimal.valueOf(10);

    public static final CalculatorResponse DEFAULT_RESPONSE = new CalculatorResponse(CashbackRule.builder()
            .setMaxAmount(MAX_AMOUNT_DEFAULT.toString())
            .setPercent(new CashbackRulePercent(PERCENT_DEFAULT.toString()))
            .setRuleIds(List.of("1"))
            .build());

    @Autowired
    @BankCashbackCalculatorApi
    private RestTemplate calculatorRestTemplate;
    @Autowired
    @BankCashbackCoreApi
    private RestTemplate coreRestTemplate;

    public void mockCalculatorWithDefaultResponse() {
        mockCalculatorWithSuccessResponse(DEFAULT_RESPONSE);
    }

    public void mockCalculatorWithSuccessResponse(CalculatorResponse response) {
        when(calculatorRestTemplate.exchange(any(RequestEntity.class), any(Class.class)))
                .thenReturn(ResponseEntity.ok(response));
    }

    public void mockCalculatorWithThrowable(Throwable throwable) {
        when(calculatorRestTemplate.exchange(any(RequestEntity.class), any(Class.class)))
                .thenThrow(throwable);
    }

    public void mockCalculatorWithNotFoundResponse() {
        when(calculatorRestTemplate.exchange(any(RequestEntity.class), any(Class.class)))
                .thenReturn(ResponseEntity.notFound().build());
    }

    public void mockCoreWithSuccess() {
        when(coreRestTemplate.exchange(any(RequestEntity.class), any(Class.class)))
                .thenReturn(ResponseEntity.ok().build());
    }

    public void mockCoreWithThrowable(Throwable throwable) {
        when(coreRestTemplate.exchange(any(), any(Class.class)))
                .thenThrow(throwable);
    }
}
