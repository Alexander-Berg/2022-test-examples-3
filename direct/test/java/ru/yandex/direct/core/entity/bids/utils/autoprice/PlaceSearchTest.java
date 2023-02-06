package ru.yandex.direct.core.entity.bids.utils.autoprice;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.assertj.core.api.Assertions;
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

@RunWith(JUnitParamsRunner.class)
public class PlaceSearchTest {

    private PlaceSearch placeSearch;

    @Before
    public void setUp() throws Exception {
        KeywordBidBsAuctionData bsAuctionData = new KeywordBidBsAuctionData()
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
        placeSearch = new PlaceSearch(bsAuctionData);
    }

    @Nonnull
    private Position buildPosition(int price, int amnesty) {
        return new Position(Money.valueOf(amnesty, CurrencyCode.YND_FIXED),
                Money.valueOf(price, CurrencyCode.YND_FIXED));
    }

    @Test
    @Parameters({
            "0, ROTATION",
            "9, ROTATION",

            "10, GUARANTEE4",
            "13, GUARANTEE3",
            "16, GUARANTEE2",
            "20, GUARANTEE1",
            "109, GUARANTEE1",

            "110, PREMIUM4",
            "220, PREMIUM3",
            "330, PREMIUM2",
            "440, PREMIUM1",
            "1000, PREMIUM1",
    })
    @TestCaseName("[{index}] placeByPrice({0}) = {1}")
    public void findPlaceByPrice_success(int price, String positionDesc) throws Exception {
        BigDecimal targetPrice = BigDecimal.valueOf(price);
        Place actualPlace = placeSearch.findPlaceByPrice(targetPrice);
        Assertions.assertThat(actualPlace.name()).isEqualTo(positionDesc);
    }

}
