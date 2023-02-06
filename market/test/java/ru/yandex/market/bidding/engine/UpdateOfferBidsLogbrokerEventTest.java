package ru.yandex.market.bidding.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferBids;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Timestamp;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;

import ru.yandex.market.bidding.TimeUtils;
import ru.yandex.market.bidding.engine.model.BidBuilder;
import ru.yandex.market.bidding.engine.model.OfferBid;
import ru.yandex.market.bidding.engine.model.OfferBidBuilder;
import ru.yandex.market.bidding.model.Place;
import ru.yandex.market.core.shop.BeruVirtualShop;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты для {@link UpdateOfferBidsLogbrokerEvent}
 */
public class UpdateOfferBidsLogbrokerEventTest extends BasicShopTestCase {

    private static final String OFFER_KEY_ID = "AAA";
    private static final int FEED_ID = 10;
    private static final int SHOP_ID = 1;
    private static final int BID_VALUE = 15;
    private static final int DONT_PULL_UP_BIDS_VALUE = 1;
    private static final int BUILD_EVENT_BID_VALUE = 100;
    private static final int BUILD_EVENT_DONT_PULL_VALUE = -1;
    private static final int BUILD_EVENT_FEE_VALUE = 200;
    private BasicPartner basicPartner;
    private BasicPartner beruPartner;

    @BeforeEach
    void setUp() {
        basicPartner = shop(SHOP_ID)
                .addOfferBid(new OfferBidBuilder()
                        .setId(OFFER_KEY_ID)
                        .feedId(FEED_ID)
                        .setPlaceBid(Place.SEARCH, new BidBuilder.PlaceBid((short) BID_VALUE))
                        .setPlaceBid(Place.FLAG_DONT_PULL_UP_BIDS, new BidBuilder.PlaceBid((short) DONT_PULL_UP_BIDS_VALUE))
                        .build(OfferBid.class))
                .build();
        beruPartner = shop(BeruVirtualShop.ID)
                .addOfferBid(new OfferBidBuilder()
                        .setId(OFFER_KEY_ID)
                        .feedId(FEED_ID)
                        .setPlaceBid(Place.SEARCH, new BidBuilder.PlaceBid((short) BID_VALUE))
                        .setPlaceBid(Place.FLAG_DONT_PULL_UP_BIDS, new BidBuilder.PlaceBid((short) DONT_PULL_UP_BIDS_VALUE))
                        .build(OfferBid.class))
                .build();
    }

    @DisplayName("Проверка, что компонента dontPullUp не изменится и передастся как была, если мы поменяем только BID компоненту")
    @Test
    void testComponentChanged() {
        final int newBidValue = 200;
        UpdateBidsAction updateBidsAction = createAction(ImmutableMap.of(
                Place.SEARCH, (short) newBidValue
        ));
        //это в секундах значение вписывается в ставку как время изменения
        int tick = (int) ReflectionUtils.readFieldValue(UpdateBidsAction.class, "tick", updateBidsAction).orElse(-1);

        List<OfferBid> offerBids = updateBidsAction.prepareBids();
        assertEquals(1, offerBids.size());

        UpdateOfferBidsLogbrokerEvent event = new UpdateOfferBidsLogbrokerEvent(basicPartner, offerBids, false);
        SyncChangeOffer.ChangeOfferRequest changeOfferRequest = event.buildEvent();
        assertThat(changeOfferRequest.getOfferList(), hasSize(1));
        assertThat(
                changeOfferRequest.getOffer(0).getBids(),
                MbiMatchers.<DataCampOfferBids.OfferBids>newAllOfBuilder()
                        .add(
                                DataCampOfferBids.OfferBids::getBid,
                                MbiMatchers.<DataCampOfferMeta.Ui32Value>newAllOfBuilder()
                                        .add(
                                                DataCampOfferMeta.Ui32Value::getValue,
                                                newBidValue,
                                                "bid"
                                        )
                                        .add(
                                                ui32 -> ui32.getMeta().getTimestamp(),
                                                equalTo(Timestamp.newBuilder().setSeconds(tick).build()),
                                                "bidTs"
                                        )
                                        .build()
                        )
                        .add(
                                DataCampOfferBids.OfferBids::getFlagDontPullUpBids,
                                MbiMatchers.<DataCampOfferMeta.Flag>newAllOfBuilder()
                                        .add(
                                                DataCampOfferMeta.Flag::getFlag,
                                                true,
                                                "dontPullUpBids"
                                        )
                                        .add(
                                                flag -> flag.getMeta().getTimestamp(),
                                                Timestamp.newBuilder().setSeconds(0).build(),
                                                "dontPullUpBidsTs"
                                        )
                                        .build()
                        )
                        .build()
        );
    }

    @Test
    @DisplayName("Проверка что ивент построился как надо")
    void testCheckBuildEvent() {
        UpdateBidsAction updateBidsAction = createAction(ImmutableMap.of(
                Place.SEARCH, (short) BUILD_EVENT_BID_VALUE,                        //bid
                Place.FLAG_DONT_PULL_UP_BIDS, (short) BUILD_EVENT_DONT_PULL_VALUE,  //dont pull ups
                Place.MARKET_PLACE, (short) BUILD_EVENT_FEE_VALUE                   //fee
        ));

        List<OfferBid> offerBids = updateBidsAction.prepareBids();
        assertEquals(1, offerBids.size());

        UpdateOfferBidsLogbrokerEvent event = new UpdateOfferBidsLogbrokerEvent(basicPartner, offerBids, true);
        SyncChangeOffer.ChangeOfferRequest changeOfferRequest = event.buildEvent();

        checkOfferEvent(changeOfferRequest);
    }

    @Test
    void testBidTimestamps() {
        long startTime = TimeUtils.nowUnixTime();
        UpdateBidsAction updateBidsAction = createAction(ImmutableMap.of(
                Place.SEARCH, (short) 100       //bid
        ));

        List<OfferBid> offerBids = updateBidsAction.prepareBids();
        assertEquals(1, offerBids.size());

        UpdateOfferBidsLogbrokerEvent event = new UpdateOfferBidsLogbrokerEvent(basicPartner, offerBids, true);
        SyncChangeOffer.ChangeOfferRequest changeOfferRequest = event.buildEvent();
        DataCampOffer.Offer offer = changeOfferRequest.getOffer(0);

        assertTrue(offer.getMeta().getModificationTs() >= startTime);
        assertTrue(offer.getBids().getMeta().getTimestamp().getSeconds() >= startTime);
        assertTrue(offer.getBids().getBid().getMeta().getTimestamp().getSeconds() >= startTime);

    }

    @Test
    void testBeruColor() {
        UpdateBidsAction updateBidsAction = createAction(ImmutableMap.of(
                Place.SEARCH, (short) BUILD_EVENT_BID_VALUE,                        //bid
                Place.FLAG_DONT_PULL_UP_BIDS, (short) BUILD_EVENT_DONT_PULL_VALUE,  //dont pull ups
                Place.MARKET_PLACE, (short) BUILD_EVENT_FEE_VALUE                   //fee
        ));

        List<OfferBid> offerBids = updateBidsAction.prepareBids();

        UpdateOfferBidsLogbrokerEvent event = new UpdateOfferBidsLogbrokerEvent(beruPartner, offerBids, true);
        SyncChangeOffer.ChangeOfferRequest changeOfferRequest = event.buildEvent();
        assertThat(changeOfferRequest.getOffer(0).getMeta().getRgb(), is(DataCampOfferMeta.MarketColor.BLUE));
    }

    private void checkOfferEvent(SyncChangeOffer.ChangeOfferRequest offerEvent) {
        Instant instantNow = Instant.now();

        Matcher<Number> positiveNumber = MbiMatchers.satisfy(ts -> ts.longValue() > 0);

        Matcher<DataCampOfferMeta.UpdateMeta> updateMetaMatcher = MbiMatchers.<DataCampOfferMeta.UpdateMeta>newAllOfBuilder()
                .add(
                        DataCampOfferMeta.UpdateMeta::getSource,
                        DataCampOfferMeta.DataSource.MARKET_MBI,
                        "updateMetaDataSource"
                )
                .build();
        Matcher<DataCampOfferIdentifiers.OfferIdentifiers> offerIdentifiersMatcher = MbiMatchers.<DataCampOfferIdentifiers.OfferIdentifiers>newAllOfBuilder()
                .add(
                        DataCampOfferIdentifiers.OfferIdentifiers::getShopId,
                        SHOP_ID,
                        "shopId"
                )
                .add(
                        DataCampOfferIdentifiers.OfferIdentifiers::getFeedId,
                        FEED_ID,
                        "feedId"
                )
                .add(
                        DataCampOfferIdentifiers.OfferIdentifiers::getOfferId,
                        OFFER_KEY_ID,
                        "offerId"
                )
                .build();

        Matcher<DataCampOfferMeta.OfferMeta> offerMetaMatcher = MbiMatchers.<DataCampOfferMeta.OfferMeta>newAllOfBuilder()
                .add(
                        DataCampOfferMeta.OfferMeta::getModificationTs,
                        positiveNumber,
                        "offerMetaModificationTs"
                )
                .add(
                        DataCampOfferMeta.OfferMeta::getRgb,
                        DataCampOfferMeta.MarketColor.WHITE,
                        "offerMetaRGB"
                )
                .build();

        Matcher<DataCampOfferMeta.UpdateMeta> updateMetaTimestampMatcher = MbiMatchers.<DataCampOfferMeta.UpdateMeta>newAllOfBuilder()
                .add(
                        DataCampOfferMeta.UpdateMeta::getTimestamp,
                        MbiMatchers.satisfy(timestamp ->
                                Duration.between(
                                        Instant.ofEpochSecond(timestamp.getSeconds()),
                                        instantNow
                                ).getSeconds() < 10
                        ),
                        "timestamp"
                )
                .build();
        Matcher<DataCampOfferMeta.Ui32Value> bidMatcher = MbiMatchers.<DataCampOfferMeta.Ui32Value>newAllOfBuilder()
                .add(
                        DataCampOfferMeta.Ui32Value::getMeta,
                        allOf(updateMetaMatcher, updateMetaTimestampMatcher),
                        "bidMeta"
                )
                .add(
                        DataCampOfferMeta.Ui32Value::getValue,
                        BUILD_EVENT_BID_VALUE,
                        "bidValue"
                )
                .build();

        Matcher<DataCampOfferMeta.Flag> flagMatcher = MbiMatchers.<DataCampOfferMeta.Flag>newAllOfBuilder()
                .add(
                        DataCampOfferMeta.Flag::getMeta,
                        allOf(updateMetaMatcher, updateMetaTimestampMatcher),
                        "flagMeta"
                )
                .add(
                        DataCampOfferMeta.Flag::getFlag,
                        false,
                        "flagValue"
                )
                .build();

        Matcher<DataCampOfferMeta.Ui32Value> feeMatcher = MbiMatchers.<DataCampOfferMeta.Ui32Value>newAllOfBuilder()
                .add(
                        DataCampOfferMeta.Ui32Value::getMeta,
                        allOf(updateMetaMatcher, updateMetaTimestampMatcher),
                        "feeMeta"
                )
                .add(
                        DataCampOfferMeta.Ui32Value::getValue,
                        BUILD_EVENT_FEE_VALUE,
                        "feeValue"
                )
                .build();

        assertThat(offerEvent.getOfferList(), hasSize(1));
        assertThat(offerEvent.getOffer(0),
                MbiMatchers.<DataCampOffer.Offer>newAllOfBuilder()
                        .add(DataCampOffer.Offer::getIdentifiers, offerIdentifiersMatcher)
                        .add(DataCampOffer.Offer::getMeta, offerMetaMatcher)
                        .add(DataCampOffer.Offer::getBids,
                                MbiMatchers.<DataCampOfferBids.OfferBids>newAllOfBuilder()
                                        .add(DataCampOfferBids.OfferBids::getMeta, updateMetaMatcher)
                                        .add(DataCampOfferBids.OfferBids::getBid, bidMatcher)
                                        .add(DataCampOfferBids.OfferBids::getFlagDontPullUpBids, flagMatcher)
                                        .add(DataCampOfferBids.OfferBids::getFee, feeMatcher)
                                        .build()
                        )
                        .build());
    }

    private UpdateBidsAction createAction(ImmutableMap<Place, Short> values) {
        final List<BidChange> bidChangeList = Collections.singletonList(
                new BidChange(OfferKeys.of(FEED_ID, OFFER_KEY_ID), values, null, null, null));

        return new UpdateBidsAction(true, basicPartner,
                Collections.emptyList(),
                bidChangeList, null, false);
    }
}
