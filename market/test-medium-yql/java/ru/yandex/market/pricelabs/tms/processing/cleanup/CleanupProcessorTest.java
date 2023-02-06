package ru.yandex.market.pricelabs.tms.processing.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.Offer;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.tms.processing.offers.AbstractOffersProcessorTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.offer;

public class CleanupProcessorTest extends AbstractOffersProcessorTest {

    @Test
    void testCleanupOffers() {
        var active = getInstant().minusMillis(TimeUnit.DAYS.toMillis(OffersCleanupProcessor.CLEANUP_OFFERS_DAYS));
        var obsolete = active.minusMillis(1);

        var shopId = shop.getShop_id();

        var toKeep = List.of(
                offer("1.1", o -> {
                    o.setShop_id(shopId);
                    o.setUpdated_at(active);
                    o.setStatus(Status.ACTIVE);
                    o.setFeed_id(3);
                }),
                offer("1.2", o -> {
                    o.setShop_id(shopId);
                    o.setUpdated_at(active);
                    o.setStatus(Status.ACTIVE);
                }),
                offer("1.3", o -> {
                    o.setShop_id(shopId);
                    o.setUpdated_at(active);
                    o.setStatus(Status.DELETED);
                    o.setFeed_id(3);
                }),
                offer("1.4", o -> {
                    o.setShop_id(shopId);
                    o.setUpdated_at(active);
                    o.setStatus(Status.DELETED);
                }),
                offer("1.5", o -> {
                    o.setShop_id(shopId);
                    o.setUpdated_at(active);
                    o.setStatus(Status.ACTIVE);
                    o.setFeed_id(3);
                }),
                offer("1.6", o -> {
                    o.setShop_id(shopId);
                    o.setUpdated_at(active);
                    o.setStatus(Status.ACTIVE);
                }),
                offer("1.7", o -> {
                    o.setShop_id(shopId);
                    o.setUpdated_at(active);
                    o.setStatus(Status.DELETED);
                    o.setFeed_id(3);
                }),
                offer("1.8", o -> {
                    o.setShop_id(shopId);
                    o.setUpdated_at(active);
                    o.setStatus(Status.DELETED);
                }),
                offer("1.9", o -> {
                    o.setShop_id(shopId);
                    o.setUpdated_at(active);
                    o.setStatus(Status.ACTIVE);
                    o.setFeed_id(3);
                })
        );

        var toDelete = List.of(
                offer("2.1", o -> {
                    o.setShop_id(shopId);
                    o.setUpdated_at(obsolete);
                    o.setStatus(Status.ACTIVE);
                }),
                offer("2.2", o -> {
                    o.setShop_id(shopId);
                    o.setUpdated_at(obsolete);
                    o.setStatus(Status.DELETED);
                    o.setFeed_id(3);
                }),
                offer("2.3", o -> {
                    o.setShop_id(shopId);
                    o.setUpdated_at(obsolete);
                    o.setStatus(Status.DELETED);
                })
        );

        List<Offer> all = new ArrayList<>();
        all.addAll(toKeep);
        all.addAll(toDelete);

        executor.insert(all);

        shop.setFeeds(Set.of(3L, 4L, 6L));
        testControls.saveShop(shop);

        offersCleanupProcessor.cleanup();
        var actualOfferId = Utils.toMap(executor.selectTargetRows(), Offer::getOffer_id).keySet();
        var expectOfferId = toKeep.stream().map(Offer::getOffer_id).collect(Collectors.toSet());
        assertEquals(expectOfferId, actualOfferId);
    }
}
