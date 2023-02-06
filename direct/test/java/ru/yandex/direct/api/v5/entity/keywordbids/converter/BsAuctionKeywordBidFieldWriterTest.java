package ru.yandex.direct.api.v5.entity.keywordbids.converter;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.yandex.direct.api.v5.keywordbids.AuctionBidItem;
import com.yandex.direct.api.v5.keywordbids.KeywordBidGetItem;
import one.util.streamex.StreamEx;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;

import ru.yandex.direct.api.v5.entity.keywordbids.KeywordBidAnyFieldEnum;
import ru.yandex.direct.core.entity.auction.container.bs.KeywordTrafaretData;
import ru.yandex.direct.core.entity.auction.container.bs.TrafaretBidItem;
import ru.yandex.direct.core.entity.bids.container.CompleteBidData;
import ru.yandex.direct.core.entity.bids.container.KeywordBidDynamicData;
import ru.yandex.direct.core.entity.bids.model.Bid;
import ru.yandex.direct.currency.CurrencyCode;
import ru.yandex.direct.currency.Money;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.direct.api.v5.entity.keywordbids.converter.BsAuctionKeywordBidFieldWriter.createBsAuctionKeywordBidFieldWriter;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


public class BsAuctionKeywordBidFieldWriterTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Test
    public void write_roundToTheNearest() {
        List<TrafaretBidItem> trafaretBidItems = StreamEx.of(
                1_025_001L, 1_015_000L, 1_014_999L, 1_000_000L, 508_000L, 502_000L, 5_000L)
                .map(x -> new TrafaretBidItem()
                        .withPositionCtrCorrection(x)
                        .withBid(Money.valueOfMicros(1_000_000L, CurrencyCode.RUB))
                        .withPrice(Money.valueOfMicros(1_000_000L, CurrencyCode.RUB)))
                .toList();

        List<AuctionBidItem> expectedBidItems = StreamEx.of(103, 102, 101, 100, 51, 50, 1)
                .map(x -> new AuctionBidItem().withTrafficVolume(x).withBid(1_000_000L))
                .toList();
        writeAndCheckResult(trafaretBidItems, expectedBidItems);
    }

    @Test
    public void write_filterPositive() {
        List<TrafaretBidItem> trafaretBidItems = StreamEx.of(1_000L, 0L, -50_000L, -1L, 4_999L)
                .map(x -> new TrafaretBidItem()
                        .withPositionCtrCorrection(x)
                        .withBid(Money.valueOfMicros(1_000_000L, CurrencyCode.RUB))
                        .withPrice(Money.valueOfMicros(1_000_000L, CurrencyCode.RUB)))
                .toList();

        List<AuctionBidItem> expectedBidItems = emptyList();
        writeAndCheckResult(trafaretBidItems, expectedBidItems);
    }

    @Test
    public void write_removeDuplicates() {
        List<Long> positionCtrCorrections = asList(1_000_000L, 499_000L, 996_000L, 501_000L, 750_000L, 1_004_000L);
        List<Long> bids = asList(10_000_000L, 2_000_000L, 5_000_000L, 1_500_000L, 2_000_000L, 7_000_000L);
        List<TrafaretBidItem> trafaretBidItems = StreamEx.zip(positionCtrCorrections, bids,
                (x, bid) -> new TrafaretBidItem()
                        .withPositionCtrCorrection(x)
                        .withBid(Money.valueOfMicros(bid, CurrencyCode.RUB))
                        .withPrice(Money.valueOfMicros(1_000_000L, CurrencyCode.RUB)))
                .toList();

        List<AuctionBidItem> expectedBidItems = asList(
                new AuctionBidItem().withTrafficVolume(100).withBid(5_000_000L),
                new AuctionBidItem().withTrafficVolume(75).withBid(2_000_000L),
                new AuctionBidItem().withTrafficVolume(50).withBid(1_500_000L));
        writeAndCheckResult(trafaretBidItems, expectedBidItems);
    }

    private void writeAndCheckResult(List<TrafaretBidItem> trafaretBidItems, List<AuctionBidItem> expectedBidItems) {
        Long bidId = 1L;
        CompleteBidData<KeywordTrafaretData> completeBidData =
                new CompleteBidData<KeywordTrafaretData>()
                        .withBid(new Bid().withId(bidId))
                        .withDynamicData(new KeywordBidDynamicData<KeywordTrafaretData>()
                                .withBsAuctionData(new KeywordTrafaretData()
                                        .withBidItems(trafaretBidItems)));

        Set<KeywordBidAnyFieldEnum> requiredFields = EnumSet.of(KeywordBidAnyFieldEnum.SEARCH_AUCTION_BIDS);

        BsAuctionKeywordBidFieldWriter writer = createBsAuctionKeywordBidFieldWriter(
                requiredFields, singletonList(completeBidData));

        KeywordBidGetItem item = new KeywordBidGetItem();
        writer.write(item, bidId);

        List<AuctionBidItem> actualBidItems = item.getSearch().getAuctionBids().getValue().getAuctionBidItems();
        assumeThat(actualBidItems, hasSize(expectedBidItems.size()));

        for (int i = 0; i < expectedBidItems.size(); i++) {
            AuctionBidItem actual = actualBidItems.get(i);
            AuctionBidItem expected = expectedBidItems.get(i);

            softly.assertThat(actual.getTrafficVolume()).describedAs("TrafficVolume")
                    .isEqualTo(expected.getTrafficVolume());
            softly.assertThat(actual.getBid()).describedAs("Bid").isEqualTo(expected.getBid());
        }
    }
}
