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
public class SearchPriceByMaxTrafficVolumeWizardTest {
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
                        bidItem(80_0000L, 80),
                        bidItem(70_0000L, 70),
                        bidItem(5_5000L, 5.5)
                ));
    }

    @SuppressWarnings("unused")
    private Object[] searchByValueParameters() {
        return new Object[][]{
                {30, 100. * (1. + 30. / 100.)},
                {0, 100. * (1. + 0. / 100.)},
                {80, 100. * (1. + 80. / 100.)}
        };
    }

    @Test
    @Parameters(method = "searchByValueParameters")
    public void calcNewSearchPriceByValue(int increasePercent, double expectedPrice) {
        SearchPriceByMaxTrafficVolumeWizard searchPriceWizard =
                SearchPriceByMaxTrafficVolumeWizard.createWizard(increasePercent);

        BigDecimal actualPrice = searchPriceWizard.calcPrice(trafaretDataData);

        assertThat(actualPrice).isCloseTo(
                BigDecimal.valueOf(expectedPrice), Percentage.withPercentage(0.01)
        );
    }

}
