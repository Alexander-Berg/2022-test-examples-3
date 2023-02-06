package ru.yandex.direct.core.entity.bids.utils;

import java.util.Map;

import org.junit.Test;

import ru.yandex.direct.core.entity.bids.container.KeywordBidPokazometerData;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.pokazometer.PhraseRequest;
import ru.yandex.direct.pokazometer.PhraseResponse;
import ru.yandex.direct.testing.currency.MoneyAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.currency.CurrencyCode.YND_FIXED;
import static ru.yandex.direct.pokazometer.PhraseResponse.Coverage.HIGH;
import static ru.yandex.direct.pokazometer.PhraseResponse.Coverage.LOW;
import static ru.yandex.direct.pokazometer.PhraseResponse.Coverage.MEDIUM;

public class PokazometerUtilsTest {

    private static final long KEYWORD_ID = 1L;

    @Test
    public void convertPhraseResponse_success() {
        PhraseResponse phraseResponse = PhraseResponse.on(new PhraseRequest("phrase text", null));
        phraseResponse.setContextCoverage(50L);
        // цены в микрофишках
        phraseResponse.setPriceByCoverage(LOW, 1_000_000L);
        phraseResponse.setPriceByCoverage(MEDIUM, 5_000_000L);
        phraseResponse.setPriceByCoverage(HIGH, 10_000_000L);

        Keyword keyword = new Keyword().withId(KEYWORD_ID);
        KeywordBidPokazometerData actual = PokazometerUtils.convertPhraseResponse(keyword, phraseResponse);

        assertThat(actual.getKeywordId()).isEqualTo(KEYWORD_ID);
        Map<PhraseResponse.Coverage, Money> coverageWithPrices = actual.getCoverageWithPrices();
        assertThat(coverageWithPrices)
                .isNotNull()
                .containsKeys(LOW, MEDIUM, HIGH);
        // Так как пока что Money нельзя сравнивать по equals, используем специальный MoneyAssert
        MoneyAssert.assertThat(coverageWithPrices.get(LOW))
                .isEqualTo(money(1.0));
        MoneyAssert.assertThat(coverageWithPrices.get(MEDIUM))
                .isEqualTo(money(5.0));
        MoneyAssert.assertThat(coverageWithPrices.get(HIGH))
                .isEqualTo(money(10.0));
    }

    private static Money money(double amount) {
        return Money.valueOf(amount, YND_FIXED);
    }

}
