package ru.yandex.market.bidding.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.yandex.market.bidding.engine.model.BidBuilder;
import ru.yandex.market.bidding.engine.model.OfferBid;
import ru.yandex.market.bidding.engine.model.OfferBidBuilder;
import ru.yandex.market.bidding.model.Place;

import static ru.yandex.market.bidding.model.PublicationStatus.APPLIED;
import static ru.yandex.market.bidding.model.PublicationStatus.NOT_ALLOWED;
import static ru.yandex.market.bidding.model.PublicationStatus.NOT_FOUND;
import static ru.yandex.market.bidding.model.PublicationStatus.PENDING;

public abstract class BasicShopTestCase {

    protected BasicPartner.Builder shop(long id) {
        return new BasicPartner.Builder().id(id);
    }


    protected void addOfferBids(BasicPartner.Builder shopBuilder, OfferBid... bids) {
        for (OfferBid bid : bids) {
            shopBuilder.addOfferBid(bid);
        }
    }

    protected OfferBid titleBid(String title, long switchFeedId, String switchOfferId) {
        return appliedTitleBid(title, 0, switchFeedId, switchOfferId);
    }

    protected OfferBid appliedTitleBid(String title, long groupId, long switchFeedId, String switchOfferId) {
        OfferBidBuilder builder = new OfferBidBuilder();
        builder.setPlaceBid(Place.SEARCH, new BidBuilder.PlaceBid((short) 100));
        return titleBid(title, groupId, switchFeedId, switchOfferId, builder);
    }

    protected OfferBid titleBid(String title, long groupId, long switchFeedId,
                                String switchOfferId, OfferBidBuilder builder) {
        builder.setId(title);
        builder.feedId(0);
        builder.setGroupId(groupId);
        builder.setSwitchFeedId(switchFeedId);
        builder.setSwitchOfferId(switchOfferId);
        return builder.build(OfferBid.class);
    }

    protected OfferBid idBid(String offerId, int feedId) {
        return idBid(offerId, feedId, 100);
    }

    protected OfferBid idBid(String offerId, int feedId, int bid) {
        assert feedId > 0;
        OfferBidBuilder builder = new OfferBidBuilder();
        builder.feedId(feedId);
        builder.setId(offerId);
        builder.setPlaceBid(Place.SEARCH, new BidBuilder.PlaceBid((short) bid));
        return builder.build(OfferBid.class);
    }

    protected List<BidChange> categoryBidChanges(Long... ids) {
        List<BidChange> bidChanges = new ArrayList<>();
        for (Long id : ids) {
            Map<Place, Short> placeBids = new HashMap<>();
            placeBids.put(Place.SEARCH, (short) 10);
            bidChanges.add(new BidChange(OfferKeys.fromSingleId(id.intValue()), placeBids, null, null, null));
        }
        return bidChanges;
    }

    protected List<BidChange> bidChanges(int feedId, String... ids) {
        List<BidChange> bidChanges = new ArrayList<>();
        for (String id : ids) {
            Map<Place, Short> placeBids = new HashMap<>();
            placeBids.put(Place.SEARCH, (short) 10);
            bidChanges.add(new BidChange(OfferKeys.of(feedId, id), placeBids, null, null, null));
        }
        return bidChanges;
    }

    protected BasicPartner createShop(int count, BidFactory bidFactory) {
        BasicPartner.Builder shopBuilder = shop(1L);
        for (int i = 0; i < count; i++) {
            // Эти ставки не могут быть переключены, но проблем всё равно не будет
            shopBuilder.addOfferBid(bidFactory.create(i));
        }
        return shopBuilder.build();
    }

    protected BasicPartner createShop(BidFactory... factories) {
        BasicPartner.Builder shopBuilder = shop(1L);
        for (int i = 0; i < factories.length; i++) {
            // Эти ставки не могут быть переключены, но проблем всё равно не будет
            shopBuilder.addOfferBid(factories[i].create(i));
        }
        return shopBuilder.build();
    }

    protected BidBuilder.PlaceBid appliedBid() {
        return new BidBuilder.PlaceBid((short) 1, 1, (short) 1, 2, APPLIED);
    }

    protected BidBuilder.PlaceBid notEverAppliedBid() {
        return new BidBuilder.PlaceBid((short) 2, 3, (short) 0, 0, PENDING);
    }

    protected BidBuilder.PlaceBid notAllowedValueBid() {
        return new BidBuilder.PlaceBid((short) 2, 3, (short) 0, 0, NOT_ALLOWED);
    }

    protected BidBuilder.PlaceBid notFoundBid() {
        return new BidBuilder.PlaceBid((short) 2, 3, (short) 0, 0, NOT_FOUND);
    }

    public interface BidFactory {
        OfferBid create(int idx);
    }
}
