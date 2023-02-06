package ru.yandex.direct.core.entity.currency.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.currency.model.CurrencyRate;
import ru.yandex.direct.core.entity.currency.repository.CurrencyRateRepository;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.currency.service.MoneyAssert.assertThat;

public class CurrencyRateServiceTest {

    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    @SuppressWarnings("FieldCanBeLocal")
    private CurrencyRateRepository currencyRateRepository;
    private CurrencyRateService serviceUnderTest;

    @Before
    public void setup() {
        currencyRateRepository = mock(CurrencyRateRepository.class);
        when(currencyRateRepository.getCurrencyRate(eq(CurrencyCode.USD), any()))
                .thenAnswer(invocation -> {
                    LocalDate date = (LocalDate) invocation.getArguments()[1];
                    return new CurrencyRate()
                            .withCurrencyCode(CurrencyCode.USD)
                            .withDate(date)
                            .withRate(BigDecimal.valueOf(60));
                });

        serviceUnderTest = new CurrencyRateService(currencyRateRepository);
    }

    @Test
    public void convertMoney_fromRubToYnd_success() {
        Money moneyRub = Money.valueOf(BigDecimal.valueOf(60), CurrencyCode.RUB);
        Money actualMoney = serviceUnderTest.convertMoney(moneyRub, CurrencyCode.YND_FIXED);

        assertThat(actualMoney)
                .hasAmount(TWO)
                .hasCurrency(CurrencyCode.YND_FIXED);
    }

    @Test
    public void convertMoney_fromRubToUsd_success() {
        Money moneyRub = Money.valueOf(BigDecimal.valueOf(60), CurrencyCode.RUB);
        Money actualMoney = serviceUnderTest.convertMoney(moneyRub, CurrencyCode.USD);

        assertThat(actualMoney)
                .hasAmount(ONE)
                .hasCurrency(CurrencyCode.USD);
    }

    @Test
    public void convertMoney_fromUsdToYnd_success() {
        Money moneyRub = Money.valueOf(ONE, CurrencyCode.USD);
        Money actualMoney = serviceUnderTest.convertMoney(moneyRub, CurrencyCode.YND_FIXED);

        assertThat(actualMoney)
                .hasAmount(TWO)
                .hasCurrency(CurrencyCode.YND_FIXED);
    }

    @Test(expected = IllegalStateException.class)
    public void convertMoney_fromRubToUnknownCurrency_fail() {
        Money moneyRub = Money.valueOf(BigDecimal.valueOf(60), CurrencyCode.RUB);

        // в mock-репозитории эта валюта неизвестна
        CurrencyCode absentCurrency = CurrencyCode.TRY;

        @SuppressWarnings("unused")
        Money actualMoney = serviceUnderTest.convertMoney(moneyRub, absentCurrency);
    }

    @Test
    public void convertMoney_twoIdenticalRequest_singleRequestToRepository() {
        Money moneyRub = Money.valueOf(BigDecimal.valueOf(60), CurrencyCode.RUB);
        serviceUnderTest.convertMoney(moneyRub, CurrencyCode.USD);
        verify(currencyRateRepository).getCurrencyRate(eq(CurrencyCode.USD), any());
        serviceUnderTest.convertMoney(moneyRub, CurrencyCode.USD);
        verifyNoMoreInteractions(currencyRateRepository);
    }

}
