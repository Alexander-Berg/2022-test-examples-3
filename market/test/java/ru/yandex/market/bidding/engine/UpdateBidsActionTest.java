package ru.yandex.market.bidding.engine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.bidding.BiddingApi;
import ru.yandex.market.bidding.engine.model.BidBuilder;
import ru.yandex.market.bidding.engine.model.OfferBid;
import ru.yandex.market.bidding.engine.model.OfferBidBuilder;
import ru.yandex.market.bidding.engine.storage.BasicOracleStorage;
import ru.yandex.market.bidding.model.Place;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static ru.yandex.market.bidding.engine.OfferKeys.SYNTHETIC_FEED;

/**
 * Created by kudrale on 25.05.17.
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateBidsActionTest extends BasicShopTestCase {
    private static final String OFFER_ID_1 = "AAA";
    private static final String OFFER_ID_2 = "BBB";
    private static final int FEED_ID_10 = 10;
    private static final int FEED_ID_15 = 15;

    private static final int BID_10 = 10;
    private static final int BID_15 = 15;

    private static final int SHOP_ID = 1;

    @Mock
    BasicOracleStorage basicOracleStorage;
    @Captor
    ArgumentCaptor<UpdateOfferBidsLogbrokerEvent> eventCaptor;
    @Mock
    private LogbrokerEventPublisher<UpdateOfferBidsLogbrokerEvent> logbrokerService;

    @Test
    public void updateCategoryBidsTests() throws SQLException {
        BasicPartner partner = shop(SHOP_ID)
                .addCategoryBid(new OfferBidBuilder()
                        .feedId(SYNTHETIC_FEED)
                        .setId(String.valueOf(BiddingApi.CATEGORY_BOOK))
                        .setPlaceBid(Place.SEARCH, new BidBuilder.PlaceBid((short) BID_15))
                        .build(OfferBid.class))
                .build();
        Action action = new UpdateBidsAction(true, partner,
                categoryBidChanges((long) BiddingApi.CATEGORY_BOOK, (long) BiddingApi.CATEGORY_ALL),
                Collections.emptyList(), null, false);
        action.prepare();
        doNothing().when(basicOracleStorage).updateBids(anyLong(), anyList(), anyList(), any(), anyBoolean());
        action.persist(basicOracleStorage);
    }

    @Test
    public void updateOfferBidsTests() {
        OfferBid bidBefore = idBid(OFFER_ID_1, FEED_ID_10, BID_15);
        OfferBid bidAfter = idBid(OFFER_ID_1, FEED_ID_10, BID_10);
        BasicPartner partner = shop(1).
                addOfferBid(bidBefore).
                build();
        assertSame(bidBefore, partner.key(0));
        assertSame(0, partner.idx(bidBefore));

        Action action = new UpdateBidsAction(true, partner,
                Collections.emptyList(),
                bidChanges(FEED_ID_10, OFFER_ID_1), null, false);
        action.prepare();
        action.onSuccess();
        assertEquals(bidAfter, partner.key(0));
        assertEquals(0, partner.idx(bidAfter));
    }

    @Test
    @DisplayName("Тест на то, что правильные данные уходят в логброкер")
    public void sendViaLogbrokerServiceCheck() {
        BasicPartner partner = shop(SHOP_ID)
                .addOfferBid(idBid(OFFER_ID_1, FEED_ID_10, BID_15))
                .build();

        List<BidChange> bidChangeList = new ArrayList<>();
        bidChangeList.addAll(bidChanges(FEED_ID_10, OFFER_ID_1));
        bidChangeList.addAll(bidChanges(FEED_ID_15, OFFER_ID_2));

        UpdateBidsAction action = new UpdateBidsAction(true, partner,
                Collections.emptyList(),
                bidChangeList, null, false);
        action.setLogbrokerService(logbrokerService);
        action.prepare();
        action.onSuccess();

        verify(logbrokerService, times(1)).publishEvent(eventCaptor.capture());

        assertSame(partner, eventCaptor.getValue().getPartner());
        assertThat(eventCaptor.getAllValues(), hasSize(1));

        List<OfferBid> offerBids = eventCaptor.getValue().getOfferBids();
        assertTrue(bidChangeList.removeAll(offerBids));
        assertThat(bidChangeList, hasSize(0));

        verifyNoMoreInteractions(logbrokerService);
    }

    @Test
    @DisplayName("Тест с передачей null в качестве logbrokerService")
    public void testWithNullLogbrokerService() {
        BasicPartner partner = shop(SHOP_ID)
                .addOfferBid(idBid(OFFER_ID_1, FEED_ID_10, BID_15))
                .build();

        Action action = new UpdateBidsAction(true, partner,
                Collections.emptyList(),
                bidChanges(FEED_ID_10, OFFER_ID_1), null, false);
        action.prepare();
        Assertions.assertDoesNotThrow(action::onSuccess);
        verifyNoMoreInteractions(logbrokerService);
    }

    @Test
    @DisplayName("Тест с передачей пустой коллекции offerBids")
    public void testWithEmptyOfferBids() {
        BasicPartner partner = shop(SHOP_ID)
                .addOfferBid(idBid(OFFER_ID_1, FEED_ID_10, BID_15))
                .build();

        UpdateBidsAction action = new UpdateBidsAction(true, partner,
                bidChanges(FEED_ID_10, OFFER_ID_1),
                Collections.emptyList(), null, false);
        action.setLogbrokerService(logbrokerService);
        action.prepare();
        action.onSuccess();
        verifyZeroInteractions(logbrokerService);
    }
}
