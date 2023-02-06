package ru.yandex.market.delivery.rupostintegrationapp.service.component.currencyexchange;

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.FixtureRepository;

import ru.yandex.common.util.currency.Bank;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.common.util.currency.CurrencyRate;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;

class CurrencyRatesXmlParserTest extends BaseTest {

    private CurrencyRatesXmlParser parser;

    @BeforeEach
    void prepare() throws Exception {
        this.parser = new CurrencyRatesXmlParser();
        this.parser.parseXmlStream(new ByteArrayInputStream(FixtureRepository.getCurrencyRatesXml()));
    }

    @Test
    void testTagParse() {
        Map<Bank, Map<Currency, CurrencyRate>> rates = this.parser.getRates();
        softly.assertThat(rates).isNotNull();

        softly.assertThat(rates.size())
            .as("Asserting that size of currency rates map is valid")
            .isEqualTo(7);
        softly.assertThat(rates.get(Bank.CBRF))
            .as("Asserting that map has Bank map").
            isInstanceOf(Map.class);
        softly.assertThat(rates.get(Bank.CBRF).size())
            .as("Asserting that bank map is valid")
            .isEqualTo(7);
        softly.assertThat(rates.get(Bank.CBRF).get(Currency.RUR).getCurrency())
            .as("Asserting that bank.currency map is valid")
            .isEqualTo(Currency.RUR);
        softly.assertThat(rates.get(Bank.CBRF).get(Currency.RUR).getRateSource())
            .as("Asserting that bank.currency.rateSource is valid")
            .isNull();
        softly.assertThat(rates.get(Bank.CBRF)
            .get(Currency.RUR)
            .getValue().doubleValue())
            .as("Asserting that bank.currency.rur.value is valid")
            .isEqualTo(1.0);

        softly.assertThat(rates.get(Bank.CBRF)
            .get(Currency.USD)
            .getValue().doubleValue())
            .as("Asserting that bank.currency.usd.value is valid")
            .isEqualTo(63.0541);
    }
}
