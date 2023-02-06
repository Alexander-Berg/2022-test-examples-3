package ru.yandex.direct.intapi.entity.auction;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.auction.container.bs.TrafaretBidItem;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;
import ru.yandex.direct.intapi.configuration.IntApiTest;
import ru.yandex.direct.intapi.entity.auction.model.AuctionItem;
import ru.yandex.direct.intapi.entity.auction.model.AuctionValuesConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.intapi.entity.auction.model.AuctionValuesConverter.roundAuctionItemsValues;

@IntApiTest
public class AuctionValuesConverterTest {
    @Test
    public void toTrafaretBidItems() {
        List<AuctionItem> auctionItems = Collections.singletonList(
                new AuctionItem().withBid(111_111_111L).withAmnestyPrice(112_111_111L).withTrafficVolume(51_000L));
        List<TrafaretBidItem> trafaretBidItems =
                AuctionValuesConverter.toTrafaretBidItems(auctionItems, CurrencyCode.USD);
        assertThat(trafaretBidItems).hasSize(1);

        TrafaretBidItem expectedItem = new TrafaretBidItem()
                .withBid(Money.valueOfMicros(111_111_111L, CurrencyCode.USD).roundToAuctionStepUp())
                .withPositionCtrCorrection(51_000L)
                .withPrice(Money.valueOfMicros(112_111_111L, CurrencyCode.USD).roundToAuctionStepUp());
        assertThat(trafaretBidItems.get(0)).isEqualToComparingFieldByField(expectedItem);
    }

    @Test
    public void toAuctionItems() {

        List<TrafaretBidItem> trafaretBidItems = Collections.singletonList(new TrafaretBidItem()
                .withBid(Money.valueOfMicros(111_111_111L, CurrencyCode.USD))
                .withPositionCtrCorrection(51_000L)
                .withPrice(Money.valueOfMicros(112_111_111L, CurrencyCode.USD)));
        List<AuctionItem> auctionItems =
                AuctionValuesConverter.toAuctionItems(trafaretBidItems);
        assertThat(trafaretBidItems).hasSize(1);
        AuctionItem expectedItem =
                new AuctionItem()
                        .withBid(111_111_111L)
                        .withAmnestyPrice(112_111_111L)
                        .withTrafficVolume(51_000L);

        assertThat(auctionItems.get(0)).isEqualToComparingFieldByField(expectedItem);
    }

    @Test
    public void roundPhraseAuctionItemsPoints() {
        List<AuctionItem> auctionItems =
                Collections.singletonList(new AuctionItem().withBid(1_000_111L).withAmnestyPrice(1_000_111L)
                        .withTrafficVolume(1_000_111L));
        List<AuctionItem> result = roundAuctionItemsValues(auctionItems, CurrencyCode.USD);
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(
                new AuctionItem().withBid(1_010_000L).withAmnestyPrice(1_010_000L).withTrafficVolume(1_000_000L));
    }
}
