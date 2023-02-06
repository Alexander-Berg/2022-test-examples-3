package ru.yandex.direct.core.entity.bids.utils.autoprice;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.bids.container.KeywordBidPokazometerData;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.pokazometer.PhraseResponse;

@RunWith(JUnitParamsRunner.class)
public class ContextPriceWizardTest {

    private KeywordBidPokazometerData pokazometerData;

    @Before
    public void setUp() throws Exception {
        Map<PhraseResponse.Coverage, Money> coverageMoneyMap = new HashMap<>();
        coverageMoneyMap.put(
                PhraseResponse.Coverage.LOW,
                Money.valueOf(BigDecimal.valueOf(100), CurrencyCode.USD)
        );
        coverageMoneyMap.put(
                PhraseResponse.Coverage.MEDIUM,
                Money.valueOf(BigDecimal.valueOf(150), CurrencyCode.USD)
        );
        coverageMoneyMap.put(
                PhraseResponse.Coverage.HIGH,
                Money.valueOf(BigDecimal.valueOf(200), CurrencyCode.USD)
        );

        pokazometerData = new KeywordBidPokazometerData(0L, coverageMoneyMap);
    }

    @Test
    @Parameters(method = "contextValues")
    public void calcNewContextPrice(int contextCoverage, int increasePercent, int expectedPrice) throws Exception {
        ContextPriceWizard contextPriceWizard = ContextPriceWizard.createWizard(increasePercent, contextCoverage);

        BigDecimal actualPrice = contextPriceWizard.calcPrice(pokazometerData);
        Assertions.assertThat(actualPrice).isCloseTo(
                BigDecimal.valueOf(expectedPrice), Percentage.withPercentage(1)
        );
    }

    private Object[] contextValues() {
        return new Object[][]{
                // Попадание точно в опорные точки Coverage map
                {20, 0, 100},
                {50, 0, 150},
                {100, 0, 200},

                // Точки между опорными
                {35, 0, 125},
                {75, 0, 175},

                // Ненулевой increasePercent
                {50, 30, 150 + 150 * 30 / 100},
                {70, 30, 221},

                // Меньше Coverage.LOW (20%)
                // Ожидаем, что price будет таким же, как на границе интерполяции при 20% покрытии
                {5, 0, 100},
                {0, 0, 100},
        };
    }

}
