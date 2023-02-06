package ru.yandex.direct.core.entity.bids.utils.autoprice;

import java.math.BigDecimal;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.data.Percentage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.auction.container.bs.KeywordTrafaretData;
import ru.yandex.direct.core.entity.auction.container.bs.TrafaretBidItem;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class SearchPriceByTrafficVolumeWizardTest {
    private KeywordTrafaretData trafaretDataData;

    private static TrafaretBidItem bidItem(long ctrCorrection, double money) {
        return new TrafaretBidItem()
                .withPositionCtrCorrection(ctrCorrection)
                .withBid(money(money))
                .withPrice(money(money));
    }

    private static Money money(double value) {
        return Money.valueOf(value, CurrencyCode.YND_FIXED);
    }

    @Before
    public void setUp() throws Exception {
        trafaretDataData = new KeywordTrafaretData()
                .withBidItems(asList(
                        bidItem(100_0000L, 100),
                        bidItem(90_0000L, 90),
                        bidItem(80_0000L, 80),
                        bidItem(70_0000L, 70),
                        bidItem(10_0000L, 10),
                        bidItem(7_5000L, 7.5),
                        bidItem(6_2000L, 6.2),
                        bidItem(5_5000L, 5.5)
                ));
    }

    @SuppressWarnings("unused")
    private Object[] searchByValueParameters() {
        return new Object[][]{
                // Попадание в точку из входных данных
                {100, 30, 100. * (1. + 30. / 100.)},
                {90, 30, 90. * (1. + 30. / 100.)},
                {80, 30, 80 * (1. + 30. / 100.)},
                {70, 30, 70 * (1. + 30. / 100.)},
                {10, 30, 10 * (1. + 30. / 100.)},
                {7.5, 30, 7.5 * (1. + 30. / 100.)},
                {6.2, 30, 6.2 * (1. + 30. / 100.)},
                {5.5, 30, 5.5 * (1. + 30. / 100.)},
                // Попадание между точками
                {50, 30, 50. * (1. + 30. / 100.)},
                // Выше максимальной точки -- опорной становится максимальная
                {110, 30, 100. * (1. + 30. / 100.)},
                // Ниже минимальной точки -- опорной становится минимальная
                {0, 30, 5.5 * (1. + 30. / 100.)}
        };
    }

    @Test
    @Parameters(method = "searchByValueParameters")
    public void calcNewSearchPriceByValue(double targetTrafficVolume, int increasePercent, double expectedPrice) {
        SearchPriceByTrafficVolumeWizard searchPriceWizard =
                SearchPriceByTrafficVolumeWizard.createWizard(increasePercent, targetTrafficVolume);

        BigDecimal actualPrice = searchPriceWizard.calcPrice(trafaretDataData);

        assertThat(actualPrice).isCloseTo(
                BigDecimal.valueOf(expectedPrice), Percentage.withPercentage(0.01)
        );
    }

}
