package ru.yandex.common.util.currency.exchange;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.junit.Test;
import ru.yandex.common.util.currency.Bank;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.currency.CurrencyRate;
import ru.yandex.common.util.currency.RateSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @date 05/08/2020
 */
public class CurrencyRatesConverterTest extends TestCase {
    private static final CurrencyRates rates = fillRates();

    @Test
    public void testEmptyBanks() {
        assertThatThrownBy(
                () -> CurrencyRatesConverter.convert(
                        new CurrencyRates(Collections.EMPTY_MAP),
                        BigDecimal.valueOf(100),
                        Bank.CBRF,
                        Currency.EUR,
                        Bank.CBRF.getNationalCurrency(),
                        2
                )
        ).isInstanceOf(CurrencyException.class);
    }

    @Test
    public void testEmptyBankRates() {
        Map<Bank, Map<Currency, CurrencyRate>> rates = new HashMap<>();
        rates.put(Bank.CBRF, new HashMap<>());

        assertThatThrownBy(
                () -> CurrencyRatesConverter.convert(
                        new CurrencyRates(rates),
                        BigDecimal.valueOf(100),
                        Bank.CBRF,
                        Currency.EUR,
                        Bank.CBRF.getNationalCurrency(),
                        2
                )
        ).isInstanceOf(CurrencyException.class);
    }

    @Test
    public void testCBRFConvert() {
        assertEquals(
                10000.00,
                CurrencyRatesConverter.convert(
                        rates,
                        BigDecimal.valueOf(100),
                        Bank.CBRF,
                        Currency.EUR,
                        Currency.RUR,
                        1
                ).doubleValue()
        );

        assertEquals(
                1,
                CurrencyRatesConverter.convert(
                        rates,
                        BigDecimal.valueOf(100),
                        Bank.CBRF,
                        Currency.RUR,
                        Currency.EUR,
                        1
                ).intValue()
        );

        assertEquals(
                100.00,
                CurrencyRatesConverter.convert(
                        rates,
                        BigDecimal.valueOf(100),
                        Bank.CBRF,
                        Bank.CBRF.getNationalCurrency(),
                        Bank.CBRF.getNationalCurrency(),
                        1
                ).doubleValue()
        );
    }

    @Test
    public void testECBConvert() {
        assertEquals(
                100.00,
                CurrencyRatesConverter.convert(
                rates,
                BigDecimal.valueOf(100),
                Bank.ECB,
                Currency.EUR,
                Currency.EUR,
                1
        ).doubleValue());

        // 100 евро = 10000 рублей (100 / 0.01)
        assertEquals(
                10000.00,
                CurrencyRatesConverter.convert(
                rates,
                BigDecimal.valueOf(100),
                Bank.ECB,
                Currency.EUR,
                Currency.RUR,
                1
        ).doubleValue());

        // 100 рублей = 1 евро (100 * 0.01)
        assertEquals(
                1.0,
                CurrencyRatesConverter.convert(
                rates,
                BigDecimal.valueOf(100),
                Bank.ECB,
                Currency.RUR,
                Currency.EUR,
                1
        ).doubleValue());
    }

    private static CurrencyRates fillRates() {
        Map<Bank, Map<Currency, CurrencyRate>> rates = new HashMap<>();

        CurrencyRate rate1 = new CurrencyRate(
                Currency.EUR,
                RateSource.CBRF_DAILY,
                1,
                BigDecimal.valueOf(100.0000),
                new Date()
        );
        Map<Currency, CurrencyRate> bank1 = new HashMap<>();
        bank1.put(Currency.EUR, rate1);
        bank1.put(Currency.RUR, rate1);
        rates.put(Bank.CBRF, bank1);


        CurrencyRate rate2 = new CurrencyRate(
                Currency.RUR,
                RateSource.ECB_DAILY,
                1,
                BigDecimal.valueOf(0.0100),
                new Date()
        );
        Map<Currency, CurrencyRate> bank2 = new HashMap<>();
        bank2.put(Currency.EUR, rate2);
        bank2.put(Currency.RUR, rate2);
        rates.put(Bank.ECB, bank2);

        return new CurrencyRates(rates);
    }
}
