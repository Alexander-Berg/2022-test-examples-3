package ru.yandex.market.delivery.rupostintegrationapp.service.converter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.common.util.currency.Bank;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.currency.CurrencyRate;
import ru.yandex.common.util.currency.RateSource;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.currencyexchange.CurrencyException;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.currencyexchange.CurrencyRates;

class CurrencyRatesConverterTest extends BaseTest {

    @Test
    void testEmptyBanks() {
        softly.assertThatThrownBy(
            () -> CurrencyRatesConverter.convert(
                new CurrencyRates(),
                BigDecimal.valueOf(100),
                Bank.CBRF,
                Currency.EUR,
                Bank.CBRF.getNationalCurrency(),
                2
            )
        ).isInstanceOf(CurrencyException.class);
    }

    @Test
    void testEmptyBankRates() {
        Map<Bank, Map<Currency, CurrencyRate>> myrates = new HashMap<>();
        myrates.put(Bank.CBRF, new HashMap<>());

        softly.assertThatThrownBy(
            () -> CurrencyRatesConverter.convert(
                new CurrencyRates(myrates, LocalDateTime.now(), LocalDateTime.now()),
                BigDecimal.valueOf(100),
                Bank.CBRF,
                Currency.EUR,
                Bank.CBRF.getNationalCurrency(),
                2
            )
        ).isInstanceOf(CurrencyException.class);
    }

    private CurrencyRates fillRates() {
        Map<Bank, Map<Currency, CurrencyRate>> myrates = new HashMap<>();

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
        myrates.put(Bank.CBRF, bank1);


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
        myrates.put(Bank.ECB, bank2);

        return new CurrencyRates(myrates, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void testCBRFConvert() {
        CurrencyRates rates = fillRates();
        softly.assertThat(
            CurrencyRatesConverter.convert(
                rates,
                BigDecimal.valueOf(100),
                Bank.CBRF,
                Currency.EUR,
                Currency.RUR,
                1
            ).doubleValue()
        )
            .as("Assertions that cbrf convert of 100 eur to rur is valid")
            .isEqualTo(10000.00);
        softly.assertThat(
            CurrencyRatesConverter.convert(
                rates,
                BigDecimal.valueOf(100),
                Bank.CBRF,
                Currency.RUR,
                Currency.EUR,
                1
            ).intValue()
        )
            .as("Assertions that cbrf convert of 100 rur to eur is valid")
            .isEqualTo(1);

        softly.assertThat(
            CurrencyRatesConverter.convert(
                rates,
                BigDecimal.valueOf(100),
                Bank.CBRF,
                Bank.CBRF.getNationalCurrency(),
                Bank.CBRF.getNationalCurrency(),
                1
            ).doubleValue()
        )
            .as("Assertions that cbrf convert of 100 CBRF.nationalCurrency to CBRF.nationalCurrency is valid")
            .isEqualTo(100.00);
    }

    @Test
    void testECBConvert() {
        CurrencyRates rates = fillRates();

        // 100 евро = 100 евро
        softly.assertThat(CurrencyRatesConverter.convert(
            rates,
            BigDecimal.valueOf(100),
            Bank.ECB,
            Currency.EUR,
            Currency.EUR,
            1
        ).doubleValue())
            .as("Assertions that 100 eur to eur is valid")
            .isEqualTo(100.00);

        // 100 евро = 10000 рублей (100 / 0.01)
        softly.assertThat(CurrencyRatesConverter.convert(
            rates,
            BigDecimal.valueOf(100),
            Bank.ECB,
            Currency.EUR,
            Currency.RUR,
            1
        ).doubleValue())
            .as("Assertions that 100 eur to rur is valid")
            .isEqualTo(10000.00);

        // 100 рублей = 1 евро (100 * 0.01)
        softly.assertThat(CurrencyRatesConverter.convert(
            rates,
            BigDecimal.valueOf(100),
            Bank.ECB,
            Currency.RUR,
            Currency.EUR,
            1
        ).doubleValue())
            .as("Assertions that 100 rur to eur is valid")
            .isEqualTo(1);
    }

    @Test
    void testConvertRub2Eur() {
        CurrencyRates rates = fillRates();
        // 1 рубль = 0.01 евро
        softly.assertThat(CurrencyRatesConverter.convertRub2Eur(rates, BigDecimal.valueOf(1)).doubleValue())
            .as("Assertions that 1 eur to rur is valid")
            .isEqualTo(0.01);

        // 100 рублей = 1 евро
        softly.assertThat(CurrencyRatesConverter.convertRub2Eur(rates, BigDecimal.valueOf(100)).intValue())
            .as("Assertions that 100 rur to eur is valid")
            .isEqualTo(1);
    }
}
