package ru.yandex.market.api.partner.controllers.auction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.partner.controllers.auction.model.Settings;
import ru.yandex.market.core.auction.AuctionService;
import ru.yandex.market.core.auction.BidLimits;
import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionCategoryBid;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.core.auction.model.MarketCategory;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuctionControllerV2Test {
    public static final long DATASOURCE_ID = 1L;
    private static final int BID_MIN_VALUE = 10;
    private static final int BOOK_BID_MIN_VALUE = 5;
    private static final int MIN_FEE = 100;
    private static final AuctionCategoryBid BOOK_BIDS = new AuctionCategoryBid(
            DATASOURCE_ID, MarketCategory.BOOK.getId(), AuctionBidValues.
            fromSameBids(BOOK_BID_MIN_VALUE).toBuilder().bid(BidPlace.MARKET_PLACE, MIN_FEE).build());
    private static final AuctionCategoryBid ROOT_BIDS = new AuctionCategoryBid(
            DATASOURCE_ID, MarketCategory.ALL.getId(), AuctionBidValues.
            fromSameBids(BID_MIN_VALUE).toBuilder().bid(BidPlace.MARKET_PLACE, MIN_FEE).build());
    private static final List<AuctionCategoryBid> ALL_CATEGORY_BIDS = Arrays.asList(ROOT_BIDS, BOOK_BIDS);

    @Mock
    private Settings settings;

    @Mock
    private BidLimits bidLimits;

    @Mock
    private AuctionService auctionService;

    @InjectMocks
    private AuctionControllerV2 controller = new AuctionControllerV2();

    @Test
    void testDefaultBids() {
        AuctionControllerV2.DefaultBidsCalculator calculator = controller.new DefaultBidsCalculator(DATASOURCE_ID, settings);
        when(auctionService.getCategoryBids(DATASOURCE_ID)).thenReturn(ALL_CATEGORY_BIDS);
        calculator.setDefaultBids();

        verify(settings).setDefaultBid(eq(ROOT_BIDS.getValues()));
        verify(settings).setDefaultBookBid(eq(BOOK_BIDS.getValues()));

        Mockito.verifyNoMoreInteractions(settings);
    }

    @Test
    void testDefaultBidsOnlyRootBids() {
        checkDefaultBidsOnly(ROOT_BIDS);
    }

    @Test
    void testDefaultBidsOnlyBookBids() {
        checkDefaultBidsOnly(BOOK_BIDS);
    }

    private void checkDefaultBidsOnly(AuctionCategoryBid bookBids) {
        AuctionControllerV2.DefaultBidsCalculator calculator = controller.new DefaultBidsCalculator(DATASOURCE_ID, settings);
        when(auctionService.getCategoryBids(DATASOURCE_ID)).thenReturn(Collections.singletonList(bookBids));
        when(bidLimits.minBids(MarketCategory.BOOK)).thenReturn(BOOK_BIDS.getValues());
        when(bidLimits.minBids(MarketCategory.ALL)).thenReturn(ROOT_BIDS.getValues());

        calculator.setDefaultBids();

        verify(settings).setDefaultBid(eq(ROOT_BIDS.getValues()));
        verify(settings).setDefaultBookBid(eq(BOOK_BIDS.getValues()));
        Mockito.verifyNoMoreInteractions(settings);
    }

    @Test
    void testDefaultBidsNoBids() {
        AuctionControllerV2.DefaultBidsCalculator calculator = controller.new DefaultBidsCalculator(DATASOURCE_ID, settings);
        when(auctionService.getCategoryBids(DATASOURCE_ID)).thenReturn(Collections.emptyList());
        when(bidLimits.minBids(MarketCategory.BOOK)).thenReturn(BOOK_BIDS.getValues());
        when(bidLimits.minBids(MarketCategory.ALL)).thenReturn(ROOT_BIDS.getValues());

        calculator.setDefaultBids();

        verify(settings).setDefaultBid(eq(ROOT_BIDS.getValues()));
        verify(settings).setDefaultBookBid(eq(BOOK_BIDS.getValues()));
        Mockito.verifyNoMoreInteractions(settings);
    }

}
