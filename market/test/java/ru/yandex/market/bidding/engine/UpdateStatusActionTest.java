package ru.yandex.market.bidding.engine;

import java.sql.SQLException;
import java.util.List;

import org.hamcrest.CustomTypeSafeMatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.hamcrest.MockitoHamcrest;
import org.mockito.runners.MockitoJUnitRunner;

import ru.yandex.market.bidding.BiddingApi;
import ru.yandex.market.bidding.ExchangeProtos;
import ru.yandex.market.bidding.TimeUtils;
import ru.yandex.market.bidding.engine.model.BidBuilder;
import ru.yandex.market.bidding.engine.model.OfferBid;
import ru.yandex.market.bidding.engine.model.OfferBidBuilder;
import ru.yandex.market.bidding.engine.status.UpdateBuffer;
import ru.yandex.market.bidding.engine.status.UpdateStatusAction;
import ru.yandex.market.bidding.engine.storage.BasicOracleStorage;
import ru.yandex.market.bidding.model.Place;
import ru.yandex.market.bidding.model.PublicationStatus;

import static ru.yandex.market.bidding.engine.OfferKeys.SYNTHETIC_FEED;

/**
 * Created by kudrale on 19.06.17.
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdateStatusActionTest extends BasicShopTestCase {

    private static int TEST_TIMEOUT = 1;
    private static long TEST_GENERATION = 1;
    private static int TEST_PUB_TIME = 100;
    @Mock
    BasicOracleStorage basicOracleStorage;

    @Test
    public void emptyUpdateShouldUpdateStatus() throws SQLException, InterruptedException {
        BasicPartner partner = shop(1).

                addCategoryBid(new OfferBidBuilder().feedId(SYNTHETIC_FEED).setId(String.valueOf(BiddingApi.CATEGORY_BOOK)).
                        setPlaceBid(Place.SEARCH, new BidBuilder.PlaceBid(Place.SEARCH.stopValue()).setStatus(PublicationStatus.PENDING)).build(OfferBid.class)).
                build();

        UpdateBuffer update = UpdateBuffer.acquire(UpdateBuffer.Kind.SNAPSHOT, TEST_TIMEOUT);
        update.shop(partner);

        Action action = new UpdateStatusAction(update,
                TEST_GENERATION, TEST_PUB_TIME, false);
        action.prepare();
        Mockito.doNothing().when(basicOracleStorage).updateStatuses(Mockito.anyLong(), Mockito.anyList(), Mockito.anyList(), Mockito.anyLong());
        action.persist(basicOracleStorage);

        Mockito.verify(basicOracleStorage).updateStatuses(Mockito.anyLong(),
                MockitoHamcrest.argThat(new CustomTypeSafeMatcher<List<OfferBid>>("should be one category") {
                    @Override
                    protected boolean matchesSafely(List<OfferBid> item) {
                        return item.size() == 1;
                    }
                }),
                Mockito.anyList(), Mockito.anyLong());
    }

    @Test
    public void statusShouldBeUpdatedForAllCategoryBids() throws SQLException, InterruptedException {
        int now = TimeUtils.nowUnixTime();
        BasicPartner partner = shop(1).

                addCategoryBid(new OfferBidBuilder().feedId(SYNTHETIC_FEED).setId(String.valueOf(BiddingApi.CATEGORY_BOOK)).
                        setPlaceBid(Place.SEARCH, new BidBuilder.PlaceBid((short) 16).setStatus(PublicationStatus.PENDING)
                                .setModTime(now - 120).setPubValue((short) 15).setPubTime(now - 140)).build(OfferBid.class)).
                addCategoryBid(new OfferBidBuilder().feedId(SYNTHETIC_FEED).setId(String.valueOf(BiddingApi.CATEGORY_ALL)).
                        setPlaceBid(Place.SEARCH, new BidBuilder.PlaceBid(Place.SEARCH.stopValue()).setStatus(PublicationStatus.PENDING)).build(OfferBid.class)).
                build();

        UpdateBuffer update = UpdateBuffer.acquire(UpdateBuffer.Kind.SNAPSHOT, TEST_TIMEOUT);
        update.shop(partner);
        update.setPublicationTime(now - 110);
        ExchangeProtos.Bid.Builder bid = ExchangeProtos.Bid.newBuilder();
        bid.setDomainType(ExchangeProtos.Bid.DomainType.CATEGORY_ID);
        bid.setPartnerType(ExchangeProtos.Bid.PartnerType.SHOP);
        bid.setPartnerId(1);
        bid.setDomainId(String.valueOf(BiddingApi.CATEGORY_BOOK));
        bid.addDomainIds("1");
        bid.addDomainIds(String.valueOf(BiddingApi.CATEGORY_BOOK));
        ExchangeProtos.Bid.Value.Builder details = ExchangeProtos.Bid.Value.newBuilder();
        details.setValue(16);
        details.setDeltaOperation(ExchangeProtos.Bid.DeltaOperation.MODIFY);
        details.setModificationTime(now - 120);
        details.setPublicationStatus(ExchangeProtos.Bid.PublicationStatus.APPLIED);
        bid.setValueForSearch(details.build());
        update.add(bid.build());


        Action action = new UpdateStatusAction(update,
                TEST_GENERATION, TEST_PUB_TIME, false);
        action.prepare();
        Mockito.doNothing().when(basicOracleStorage).updateStatuses(Mockito.anyLong(), Mockito.anyList(), Mockito.anyList(), Mockito.anyLong());
        action.persist(basicOracleStorage);
        Mockito.verify(basicOracleStorage).updateStatuses(Mockito.anyLong(),
                MockitoHamcrest.argThat(new CustomTypeSafeMatcher<List<OfferBid>>("should be two categories") {
                    @Override
                    protected boolean matchesSafely(List<OfferBid> item) {
                        return item.size() == 2;
                    }
                }),
                Mockito.anyList(), Mockito.anyLong());
    }

    @Test
    public void allBidsShouldBeWithId() throws SQLException, InterruptedException {
        int now = TimeUtils.nowUnixTime();
        BasicPartner partner = shop(1).

                addCategoryBid(new OfferBidBuilder().feedId(SYNTHETIC_FEED).setId(String.valueOf(BiddingApi.CATEGORY_BOOK)).
                        setPlaceBid(Place.SEARCH, new BidBuilder.PlaceBid((short) 16).setStatus(PublicationStatus.PENDING)
                                .setModTime(now - 120).setPubValue((short) 15).setPubTime(now - 140)).build(OfferBid.class)).
                addCategoryBid(new OfferBidBuilder().feedId(SYNTHETIC_FEED).setId(String.valueOf(BiddingApi.CATEGORY_ALL)).
                        setPlaceBid(Place.SEARCH, new BidBuilder.PlaceBid(Place.SEARCH.stopValue()).setStatus(PublicationStatus.PENDING)).build(OfferBid.class)).
                build();

        UpdateBuffer update = UpdateBuffer.acquire(UpdateBuffer.Kind.SNAPSHOT, TEST_TIMEOUT);
        update.shop(partner);
        update.setPublicationTime(now - 110);
        ExchangeProtos.Bid.Builder bid = ExchangeProtos.Bid.newBuilder();
        bid.setDomainType(ExchangeProtos.Bid.DomainType.CATEGORY_ID);
        bid.setPartnerType(ExchangeProtos.Bid.PartnerType.SHOP);
        bid.setPartnerId(1);
        bid.setDomainId(String.valueOf(BiddingApi.CATEGORY_BOOK));
        bid.addDomainIds("1");
        bid.addDomainIds(String.valueOf(BiddingApi.CATEGORY_BOOK));
        ExchangeProtos.Bid.Value.Builder details = ExchangeProtos.Bid.Value.newBuilder();
        details.setValue(16);
        details.setDeltaOperation(ExchangeProtos.Bid.DeltaOperation.MODIFY);
        details.setModificationTime(now - 120);
        details.setPublicationStatus(ExchangeProtos.Bid.PublicationStatus.APPLIED);
        bid.setValueForSearch(details.build());
        update.add(bid.build());


        Action action = new UpdateStatusAction(update,
                TEST_GENERATION, TEST_PUB_TIME, false);
        action.prepare();
        Mockito.doNothing().when(basicOracleStorage).updateStatuses(Mockito.anyLong(), Mockito.anyList(), Mockito.anyList(), Mockito.anyLong());
        action.persist(basicOracleStorage);
        Mockito.verify(basicOracleStorage).updateStatuses(Mockito.anyLong(),
                MockitoHamcrest.argThat(new CustomTypeSafeMatcher<List<OfferBid>>("should be two categories") {
                    @Override
                    protected boolean matchesSafely(List<OfferBid> item) {
                        for (OfferBid bid : item) {
                            if (bid.key().id() == null) {
                                return false;
                            }
                        }
                        return true;
                    }
                }),
                Mockito.anyList(), Mockito.anyLong());
    }
}
