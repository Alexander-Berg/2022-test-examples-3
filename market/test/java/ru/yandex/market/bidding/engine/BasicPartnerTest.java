package ru.yandex.market.bidding.engine;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.bidding.engine.model.OfferBid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static ru.yandex.common.util.reflect.ReflectionUtils.readPrivateField;

public class BasicPartnerTest extends BasicShopTestCase {

    @Test
    public void testSearch() {
        OfferBid titleBid = titleBid("AB", 0, null);
        OfferBid alphaIdBid = titleBid("ABC", 10, null);
        OfferBid numIdBid = idBid("123", 10);

        BasicPartner.Builder shopBuilder = shop(1L);
        addOfferBids(shopBuilder, titleBid, alphaIdBid, numIdBid);

        BasicPartner shopBids = shopBuilder.build();
        assertSame(titleBid, shopBids.key(0));
        assertSame(alphaIdBid, shopBids.key(1));
        assertSame(numIdBid, shopBids.key(2));

        assertSame(0, shopBids.idx(titleBid));
        assertSame(1, shopBids.idx(alphaIdBid));
        assertSame(2, shopBids.idx(numIdBid));
    }

    @Test
    public void testOrderedBids() {
        OfferBid bid0 = titleBid("AA", 0, null);
        OfferBid bid1 = titleBid("BB", 0, null);
        OfferBid bid2 = titleBid("CC", 0, null);
        OfferBid bid3 = titleBid("DD", 0, null);
        OfferBid bid4 = titleBid("EE", 0, null);

        BasicPartner.Builder shopBuilder = shop(1L);
        // insert out of order
        addOfferBids(shopBuilder, bid4, bid1, bid0, bid2, bid3);

        BasicPartner shop = shopBuilder.build();

        // check ordered
        assertSame(bid0, shop.key(0));
        assertSame(bid1, shop.key(1));
        assertSame(bid2, shop.key(2));
        assertSame(bid3, shop.key(3));
        assertSame(bid4, shop.key(4));

        assertSame(0, shop.idx(bid0));
        assertSame(1, shop.idx(bid1));
        assertSame(2, shop.idx(bid2));
        assertSame(3, shop.idx(bid3));
        assertSame(4, shop.idx(bid4));
    }

    @Test(expected = AssertionError.class)
    public void testDuplicateBids() {
        OfferBid bid1 = titleBid("AB", 0, null);
        // duplicated bids
        OfferBid bid2 = titleBid("ABC", 0, null);
        OfferBid bid3 = titleBid("ABC", 0, null);

        BasicPartner.Builder shopBuilder = shop(1L);
        addOfferBids(shopBuilder, bid1, bid2, bid3);

        shopBuilder.build();
    }

    @Test
    public void testMoveBetweenGroups() {
        // Генерация 4-х ставок в группах 0,1,2 и 3
        BasicPartner.Builder shopBuilder = shop(1L);
        for (int i = 0; i < 4; i++) {
            shopBuilder.addOfferBid(appliedTitleBid("bid" + i, i, 0, null));
        }
        BasicPartner shopBids = shopBuilder.build();

        // Перемещение ставок из 1 в 2
        OfferBid[] bids = shopBids.moveBetweenGroups(1, 2);
        shopBids.setOfferBids(bids);

        // Проверка
        Map<String, OfferBid> bidByTitle = allBidsById(shopBids);
        assertEquals(0L, bidByTitle.get("bid0").groupId());
        assertEquals(2L, bidByTitle.get("bid1").groupId());
        assertEquals(2L, bidByTitle.get("bid2").groupId());
        assertEquals(3L, bidByTitle.get("bid3").groupId());
    }

    protected Map<String, OfferBid> allBidsById(BasicPartner shopBids) {
        Map<String, OfferBid> bidByTitle = new HashMap<>();
        OfferBid[] offerBids = (OfferBid[]) readPrivateField(shopBids, "offerBids");
        for (OfferBid bid : offerBids) {
            bidByTitle.put(bid.id().toString(), bid);
        }
        return bidByTitle;
    }

    /**
     * Проверяем, что магазин переключает ставки и сортирует их по новому ключу
     */
    @Test
    public void testSwitchModeToNumId() {
        final int bidCount = 4;

        // Генерация 4-х ставок в группах 0,1,2 и 3
        BasicPartner.Builder shopBuilder = shop(1L);
        for (int i = 0; i < bidCount; i++) {
            shopBuilder.addOfferBid(appliedTitleBid("bid" + i, i, 10, String.valueOf(bidCount - i)));
        }
        BasicPartner shopBids = shopBuilder.build();

        // Переключаем в fast
        OfferBid[] bids = shopBids.prepareSwitching(true);
        if (bids != null) {
            shopBids.doSwitchMode(true, bids);
        }

        OfferBid[] offerBids = (OfferBid[]) readPrivateField(shopBids, "offerBids");
        assertNotNull(offerBids);
        assertEquals(bidCount, offerBids.length);
        for (int i = 0; i < bidCount; i++) {
            assertNotNull(offerBids[i]);
            assertTrue(offerBids[i].isFast());
            assertEquals(String.valueOf(i + 1), String.valueOf(offerBids[i].id()));
            assertNull(offerBids[i].switchOfferId());
        }

    }

    @Test
    public void testSwitch() {
        // Генерация 4-х ставок в группах 0,1,2 и 3
        BasicPartner.Builder shopBuilder = shop(1L);
        for (int i = 0; i < 4; i++) {
            shopBuilder.addOfferBid(appliedTitleBid("bid" + i, i, 10, "" + i));
        }
        BasicPartner shop = shopBuilder.build();
        OfferBid[] bids = shop.prepareSwitching(true);

        Assert.assertEquals("Some bids were lost", bids.length, 4);
        for (OfferBid bid : bids) {
            assertTrue("Bid was not switched", bid.isFast());
        }
    }

    @Test
    public void testSwitchDuplicates() {
        BasicPartner.Builder shopBuilder = shop(1L);
        for (int i = 0; i < 4; i++) {
            shopBuilder.addOfferBid(appliedTitleBid("bid" + i, i, 10, "1"));
        }
        BasicPartner shop = shopBuilder.build();
        OfferBid[] bids = shop.prepareSwitching(true);

        Assert.assertEquals("Repeated switch_feed_id/switch_offer_id were not deleted", bids.length, 1);
    }

    @Test
    public void testSwitchNonDuplicates() {
        BasicPartner.Builder shopBuilder = shop(1L);
        for (int i = 0; i < 4; i++) {
            // тот же самый offerId, но другой feed
            shopBuilder.addOfferBid(appliedTitleBid("bid" + i, i, i + 1, "1"));
        }
        BasicPartner shop = shopBuilder.build();
        OfferBid[] bids = shop.prepareSwitching(true);

        Assert.assertEquals("Some bids were lost", bids.length, 4);
    }

    /**
     * При переключении с id на title должены быть удалены все ставки
     */
    @Test
    public void testSwitchIdToTitle() {
        BasicPartner.Builder shopBuilder = shop(1L);
        for (int i = 0; i < 4; i++) {
            // тот же самый offerId, но другой feed
            shopBuilder.addOfferBid(idBid("" + i, 1));
        }
        BasicPartner shop = shopBuilder.build();
        OfferBid[] bids = shop.prepareSwitching(false);


    }

}
