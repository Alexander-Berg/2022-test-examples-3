package ru.yandex.market.partner.auction.servantlet.category;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;

import ru.yandex.market.core.auction.model.AuctionBidValues;
import ru.yandex.market.core.auction.model.AuctionCategoryBid;
import ru.yandex.market.core.auction.model.BidPlace;
import ru.yandex.market.partner.auction.AuctionCategoryBidsServantlet;
import ru.yandex.market.partner.auction.servantlet.AuctionServantletMockBase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_222;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_CENTS_333;
import static ru.yandex.market.partner.auction.AuctionBulkCommon.BID_KEEP_OLD;

/**
 * Тесты для {@link AuctionCategoryBidsServantlet}.
 * Без контекста.
 */
@ExtendWith(MockitoExtension.class)
class CategoryBidsManagementTest extends AuctionServantletMockBase {

    private static final BigInteger RESET_BID_VALUE = null;
    private static final AuctionBidValues RESET_CATEGORY_BIDS = AuctionBidValues.createKeepOldBids().toBuilder()
            .bid(BidPlace.CARD, RESET_BID_VALUE)
            .bid(BidPlace.MARKET_PLACE, RESET_BID_VALUE)
            .bid(BidPlace.SEARCH, RESET_BID_VALUE)
            .build();

    @InjectMocks
    private AuctionCategoryBidsServantlet servantlet;

    static Stream<Arguments> setBidsArgs() {
        return Stream.of(
                Arguments.of("сброс с явным указанием 0",
                        "categoryId=1" +
                                "&fee=0" +
                                "&cbid=0" +
                                "&bid=0",
                        RESET_CATEGORY_BIDS.getPlaceBids()
                ),
                Arguments.of("установка значений",
                        "categoryId=1" +
                                "&fee=1.11" +
                                "&cbid=2.22" +
                                "&bid=3.33",
                        AuctionBidValues.createKeepOldBids().toBuilder()
                                .bid(BidPlace.MARKET_PLACE, RESET_BID_VALUE)
                                .bid(BidPlace.CARD, BID_CENTS_222)
                                .bid(BidPlace.SEARCH, BID_CENTS_333)
                                .build().getPlaceBids()
                ),
                Arguments.of("установка значений (bid only)",
                        "categoryId=1" +
                                "&bid=3.33",
                        AuctionBidValues.createKeepOldBids().toBuilder()
                                .bid(BidPlace.MARKET_PLACE, RESET_BID_VALUE)
                                .bid(BidPlace.CARD, BID_KEEP_OLD)
                                .bid(BidPlace.SEARCH, BID_CENTS_333)
                                .build().getPlaceBids()
                ),
                Arguments.of("установка значений unified (bid only)",
                        "categoryId=1" +
                                "&bid=3.33" +
                                "&unified=true",
                        AuctionBidValues.createKeepOldBids().toBuilder()
                                .bid(BidPlace.MARKET_PLACE, RESET_BID_VALUE)
                                .bid(BidPlace.CARD, BID_CENTS_333)
                                .bid(BidPlace.SEARCH, BID_CENTS_333)
                                .build().getPlaceBids()
                ),
                Arguments.of("установка значений unified",
                        "categoryId=1" +
                                "&bid=3.33" +
                                "&cbid=2.22" +
                                "&unified=true",
                        AuctionBidValues.createKeepOldBids().toBuilder()
                                .bid(BidPlace.MARKET_PLACE, RESET_BID_VALUE)
                                .bid(BidPlace.CARD, BID_CENTS_333)
                                .bid(BidPlace.SEARCH, BID_CENTS_333)
                                .build().getPlaceBids()
                )

        );
    }

    @BeforeEach
    void beforeEach() {
        mockRegionsAndTariff();
        mockServRequestCrudActionUPDATE();
        mockServRequestIdentificationParams();
    }

    @DisplayName("Установка и сброс категорийных ставок")
    @MethodSource("setBidsArgs")
    @ParameterizedTest(name = "{0}")
    void test_categorySetBids(String description, String args, Map<BidPlace, BigInteger> expectedPlaceBids) {

        mockServantletPassedArgs(args);

        servantlet.process(servRequest, servResponse);

        List<AuctionCategoryBid> actualCategoryBids = extractAuctionSetCategoryBids();
        assertThat(actualCategoryBids, hasSize(1));
        AuctionCategoryBid categoryBid = actualCategoryBids.get(0);
        assertThat(categoryBid.getValues().getPlaceBids(), is(expectedPlaceBids));
    }

}
