package ru.yandex.direct.core.entity.bids.utils.autoprice;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.data.Percentage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.auction.container.bs.Block;
import ru.yandex.direct.core.entity.auction.container.bs.KeywordBidBsAuctionData;
import ru.yandex.direct.core.entity.auction.container.bs.Position;
import ru.yandex.direct.core.entity.keyword.model.Place;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(JUnitParamsRunner.class)
public class SearchPriceWizardTest {
    private KeywordBidBsAuctionData bsAuctionData;

    @Before
    public void setUp() throws Exception {
        bsAuctionData = new KeywordBidBsAuctionData()
                .withPremium(new Block(asList(
                        buildPosition(440, 440),
                        buildPosition(330, 330),
                        buildPosition(220, 220),
                        buildPosition(110, 110)
                )))
                .withGuarantee(new Block(asList(
                        buildPosition(20, 20),
                        buildPosition(16, 16),
                        buildPosition(13, 13),
                        buildPosition(10, 10)
                )));
    }

    @Nonnull
    private Position buildPosition(int price, int amnesty) {
        return new Position(Money.valueOf(amnesty, CurrencyCode.YND_FIXED),
                Money.valueOf(price, CurrencyCode.YND_FIXED));
    }

    @Test
    @Parameters(method = "searchByValueParameters")
    public void calcNewSearchPriceByValue(Place place, int increasePercent, int expectedPrice) throws Exception {
        SearchPriceWizard searchPriceWizard = SearchPriceWizard.byValue(increasePercent, place);

        BigDecimal actualPrice = searchPriceWizard.calcPrice(bsAuctionData);

        assertThat(actualPrice).isCloseTo(
                BigDecimal.valueOf(expectedPrice), Percentage.withPercentage(0.01)
        );
    }

    private Object[] searchByValueParameters() {
        return new Object[][]{
                {Place.PREMIUM1, 30, 440 + 440 * 30 / 100},
                {Place.PREMIUM2, 30, 330 + 330 * 30 / 100},
                {Place.PREMIUM3, 30, 220 + 220 * 30 / 100},
                {Place.PREMIUM4, 30, 110 + 110 * 30 / 100},
                {Place.PREMIUM, 30, 110 + 110 * 30 / 100},

                // Позиции 1-3 Гарантии в параметрах соответствуют первой позиции при расчёте
                {Place.GUARANTEE1, 30, 20 + 20 * 30 / 100},
                {Place.GUARANTEE2, 30, 20 + 20 * 30 / 100},
                {Place.GUARANTEE3, 30, 20 + 20 * 30 / 100},
                {Place.GUARANTEE4, 30, 10 + 10 * 30 / 100},
                {Place.GARANT, 30, 10 + 10 * 30 / 100},
        };
    }


    @Test
    @Parameters(method = "searchByDiffParameters")
    public void calcNewSearchPriceByDiff(Place place, int increasePercent, int expectedPrice) throws Exception {
        SearchPriceWizard searchPriceWizard = SearchPriceWizard.byDiff(increasePercent, place);

        BigDecimal actualPrice = searchPriceWizard.calcPrice(bsAuctionData);

        assertThat(actualPrice).isCloseTo(
                BigDecimal.valueOf(expectedPrice), Percentage.withPercentage(0.01)
        );
    }

    private Object[] searchByDiffParameters() {
        return new Object[][]{
                {Place.PREMIUM1, 30, 440},
                {Place.PREMIUM2, 30, 330 + (440 - 330) * 30 / 100},
                {Place.PREMIUM3, 30, 220 + (330 - 220) * 30 / 100},
                {Place.PREMIUM4, 30, 110 + (220 - 110) * 30 / 100},
                {Place.PREMIUM, 30, 110 + (220 - 110) * 30 / 100},

                {Place.GUARANTEE1, 50, 20 + (110 - 20) * 50 / 100},
                {Place.GUARANTEE2, 50, 20 + (110 - 20) * 50 / 100},
                {Place.GUARANTEE3, 50, 20 + (110 - 20) * 50 / 100},
                {Place.GUARANTEE4, 50, 10 + (20 - 10) * 50 / 100},
                {Place.GARANT, 30, 10 + (20 - 10) * 30 / 100},
        };
    }
}
