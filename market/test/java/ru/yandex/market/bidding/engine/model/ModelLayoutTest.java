package ru.yandex.market.bidding.engine.model;

import java.util.ArrayList;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.market.TestFixtures;
import ru.yandex.market.bidding.model.Place;

/**
 * Created with IntelliJ IDEA.
 * User: snoop
 * Date: 4/2/15
 * Time: 5:34 PM
 */
public class ModelLayoutTest {
    private static BidFactory bf = MagicTool.DEFAULT;
    private static TestFixtures.Table table = new TestFixtures.Table(
            "Model Layout",
            new TestFixtures.Column("Class", 30, false),
            new TestFixtures.Column("Instance size", 30, true),
            new TestFixtures.Column("Total footprint", 30, true));

    @BeforeClass
    public static void beforeClass() {
        TestFixtures.footprint(new FatBid(), table);

        TestFixtures.footprint(bf.createBid(exampleBid()), table);

        for (OfferBidBuilder example : exampleOfferBids()) {
            TestFixtures.footprint(bf.createOfferBid(example), table);
        }
        table.breakLine();
    }

    @AfterClass
    public static void afterClass() {
//        table.breakLine();
        System.out.println(table.toString());
    }

    static BidBuilder exampleBid(Place place) {
        return exampleBid().setPlaceBid(place, BidBuilder.PlaceBid.undefined());
    }

    static BidBuilder exampleBid() {
        return new BidBuilder();
    }

    static OfferBidBuilder exampleOfferBid(String id, long feed, String switchId) {
        return new OfferBidBuilder().setId(id).feedId(feed).setSwitchOfferId(switchId);
    }

    private static List<OfferBidBuilder> exampleOfferBids() {
        List<OfferBidBuilder> examples = new ArrayList<>();
        examples.add(exampleOfferBid("<OFFER_TITLE>", 0, null));
        examples.add(exampleOfferBid("<OFFER_TITLE>", 0, "<FEED_OFFER_ID>"));
        examples.add(exampleOfferBid("<OFFER_TITLE>", 0, "123"));

        examples.add(exampleOfferBid("<FEED_OFFER_ID>", 1, null));
        examples.add(exampleOfferBid("123", 1, null));

        return examples;
    }

    @Test
    public void testFatBid() {
        FatBid example = new FatBid();
        for (Place place : Place.ALL) {
            example.add(place);
            TestFixtures.footprint(example, table);
        }
        table.breakLine();
    }

    @Test
    public void testBid() {
        BidBuilder example = exampleBid();
        for (Place place : Place.ALL) {
            example.setPlaceBid(place, BidBuilder.PlaceBid.undefined().setValue((short) 10));
            TestFixtures.footprint(bf.createBid(example), table);
        }
        table.breakLine();
    }

    @Test
    public void testOfferBid() {
        final List<OfferBidBuilder> offerBids = exampleOfferBids();
        for (Place place : Place.ALL) {
            for (OfferBidBuilder example : offerBids) {
                example.setPlaceBid(place, BidBuilder.PlaceBid.undefined().setValue((short) 10));
                TestFixtures.footprint(bf.createOfferBid(example), table);
            }
            table.breakLine();
        }
    }
}
