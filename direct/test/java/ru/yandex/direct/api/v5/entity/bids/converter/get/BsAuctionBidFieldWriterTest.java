package ru.yandex.direct.api.v5.entity.bids.converter.get;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.yandex.direct.api.v5.bids.BidFieldEnum;
import com.yandex.direct.api.v5.bids.BidGetItem;
import org.junit.Test;

import ru.yandex.direct.core.entity.auction.container.bs.KeywordBidBsAuctionData;
import ru.yandex.direct.core.entity.bids.container.CompleteBidData;
import ru.yandex.direct.core.entity.bids.container.KeywordBidDynamicData;
import ru.yandex.direct.core.entity.bids.model.Bid;

import static java.util.Collections.singletonList;

public class BsAuctionBidFieldWriterTest {

    @Test
    public void write_success_whenNoBsData() throws Exception {
        List<CompleteBidData<KeywordBidBsAuctionData>> completeBidData =
                singletonList(new CompleteBidData<KeywordBidBsAuctionData>().withBid(new Bid().withId(1L))
                        .withDynamicData(new KeywordBidDynamicData<KeywordBidBsAuctionData>()
                                .withBsAuctionData(new KeywordBidBsAuctionData())));
        Set<BidFieldEnum> requiredFields = EnumSet.of(
                BidFieldEnum.MIN_SEARCH_PRICE,
                BidFieldEnum.CURRENT_SEARCH_PRICE,
                BidFieldEnum.SEARCH_PRICES,
                BidFieldEnum.AUCTION_BIDS,
                BidFieldEnum.SEARCH_PRICES
        );
        BsAuctionBidFieldWriter writer =
                BsAuctionBidFieldWriter.createBsAuctionBidFieldWriter(requiredFields, completeBidData);
        // assert no exception
        long absentId = 2L;
        writer.write(new BidGetItem(), absentId);
    }

}
