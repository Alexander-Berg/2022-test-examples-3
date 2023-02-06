package ru.yandex.direct.core.entity.bids.container.interpolator;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.auction.container.bs.TrafaretBidItem;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;


@CoreTest
public class PointConverterTest {
    @Test
    public void unionToTrafaretBidItem_BidsAndAmnestyPricesDifferentSizes() {
        List<Double> bids = asList(1.1, 1.0);
        List<Double> amnestyPrices = Collections.singletonList(0.9);
        Long trafficVolume = 1L;

        CurrencyCode yndFixed = CurrencyCode.YND_FIXED;
        List<TrafaretBidItem> trafaretBidItems =
                PointConverter.unionToTrafaretBidItem(bids, amnestyPrices, trafficVolume, yndFixed);
        assertThat(trafaretBidItems).hasSize(2);
        assertThat(trafaretBidItems.get(0)).isEqualToComparingFieldByField(new TrafaretBidItem()
                .withBid(Money.valueOf(bids.get(0), yndFixed))
                .withPrice(Money.valueOf(amnestyPrices.get(0), yndFixed))
                .withPositionCtrCorrection(trafficVolume));
        assertThat(trafaretBidItems.get(1)).isEqualToComparingFieldByField(new TrafaretBidItem()
                .withBid(Money.valueOf(bids.get(1), yndFixed))
                .withPrice(Money.valueOf(amnestyPrices.get(0), yndFixed))
                .withPositionCtrCorrection(trafficVolume));
    }

    @Test
    public void unionToTrafaretBidItem_BidsAndAmnestyPricesOfSize2() {
        List<Double> bids = asList(1.1, 1.0);
        List<Double> amnestyPrices = asList(0.9, 0.8);
        Long trafficVolume = 1L;

        CurrencyCode yndFixed = CurrencyCode.YND_FIXED;
        List<TrafaretBidItem> trafaretBidItems =
                PointConverter.unionToTrafaretBidItem(bids, amnestyPrices, trafficVolume, yndFixed);
        assertThat(trafaretBidItems).hasSize(2);
        assertThat(trafaretBidItems.get(0)).isEqualToComparingFieldByField(new TrafaretBidItem()
                .withBid(Money.valueOf(bids.get(0), yndFixed))
                .withPrice(Money.valueOf(amnestyPrices.get(0), yndFixed))
                .withPositionCtrCorrection(trafficVolume));
        assertThat(trafaretBidItems.get(1)).isEqualToComparingFieldByField(new TrafaretBidItem()
                .withBid(Money.valueOf(bids.get(1), yndFixed))
                .withPrice(Money.valueOf(amnestyPrices.get(1), yndFixed))
                .withPositionCtrCorrection(trafficVolume));
    }

    @Test
    public void unionToTrafaretBidItem_BidsAndAmnestyPricesOfSize1() {
        List<Double> bids = Collections.singletonList(1.1);
        List<Double> amnestyPrices = Collections.singletonList(0.9);
        Long trafficVolume = 1L;

        CurrencyCode yndFixed = CurrencyCode.YND_FIXED;
        List<TrafaretBidItem> trafaretBidItems =
                PointConverter.unionToTrafaretBidItem(bids, amnestyPrices, trafficVolume, yndFixed);
        assertThat(trafaretBidItems).hasSize(1);
        assertThat(trafaretBidItems.get(0)).isEqualToComparingFieldByField(new TrafaretBidItem()
                .withBid(Money.valueOf(bids.get(0), yndFixed))
                .withPrice(Money.valueOf(amnestyPrices.get(0), yndFixed))
                .withPositionCtrCorrection(trafficVolume));
    }

    @Test
    public void unionToTrafaretBidItem_BidsAndAmnestyPricesOfSize1_BidLessThanAmnestyPrice() {
        List<Double> bids = Collections.singletonList(1.1);
        List<Double> amnestyPrices = Collections.singletonList(1.2);
        Long trafficVolume = 1L;

        CurrencyCode yndFixed = CurrencyCode.YND_FIXED;
        List<TrafaretBidItem> trafaretBidItems =
                PointConverter.unionToTrafaretBidItem(bids, amnestyPrices, trafficVolume, yndFixed);
        assertThat(trafaretBidItems).hasSize(1);
        assertThat(trafaretBidItems.get(0)).isEqualToComparingFieldByField(new TrafaretBidItem()
                .withBid(Money.valueOf(bids.get(0), yndFixed))
                .withPrice(Money.valueOf(bids.get(0), yndFixed))
                .withPositionCtrCorrection(trafficVolume));
    }

    @Test(expected = IllegalArgumentException.class)
    public void unionToTrafaretBidItem_GreaterThanMaxSize() {
        List<Double> bids = asList(1.1, 1.2, 1.3);
        List<Double> amnestyPrices = Collections.singletonList(1.5);
        Long trafficVolume = 1L;

        CurrencyCode yndFixed = CurrencyCode.YND_FIXED;
        PointConverter.unionToTrafaretBidItem(bids, amnestyPrices, trafficVolume, yndFixed);
    }

    @Test(expected = IllegalArgumentException.class)
    public void unionToTrafaretBidItem_LessThanMinSize() {
        List<Double> bids = Collections.emptyList();
        List<Double> amnestyPrices = Collections.singletonList(1.5);
        Long trafficVolume = 1L;

        CurrencyCode yndFixed = CurrencyCode.YND_FIXED;
        PointConverter.unionToTrafaretBidItem(bids, amnestyPrices, trafficVolume, yndFixed);
    }
}
