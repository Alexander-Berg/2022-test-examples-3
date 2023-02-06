package ru.yandex.market.loyalty.core.trigger.restrictions.region;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.UnresolvableException;
import ru.yandex.market.loyalty.core.service.trigger.TriggerDataCache;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.EventFactory;
import ru.yandex.market.loyalty.core.utils.PromoUtils;
import ru.yandex.market.loyalty.core.utils.TriggersFactory;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.core.utils.EventFactory.withDeliveryRegion;

public class RegionRestrictionTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final Promo PROMO = PromoUtils.Coupon.defaultSingleUse().basePromo();

    @Autowired
    private RegionRestrictionFactory regionRestrictionFactory;
    @Autowired
    private TriggersFactory triggersFactory;

    @Test
    public void testOneRegion() throws UnresolvableException {
        long region = 213L;

        RegionRestriction restrictionWithRegion = regionRestrictionFactory.create(1L, Long.toString(region));

        OrderStatusUpdatedEvent eventWithRegion = EventFactory.orderStatusUpdated(withDeliveryRegion(region));

        assertTrue(restrictionWithRegion.fitCondition(eventWithRegion,
                triggersFactory.createDefaultPromoTriggerEventData(PROMO), new TriggerDataCache()).isMatched());

        OrderStatusUpdatedEvent eventWithAnotherRegion = EventFactory.orderStatusUpdated(
                withDeliveryRegion(region + 1));

        assertFalse(restrictionWithRegion.fitCondition(eventWithAnotherRegion,
                triggersFactory.createDefaultPromoTriggerEventData(PROMO), new TriggerDataCache()).isMatched());

    }

    @Test
    public void testSeveralRegions() throws UnresolvableException {
        Set<Long> regions = Stream.of(213L, 2L, 10174L).collect(Collectors.toSet());
        Long randomRegionInRegions = regions.iterator().next();

        RegionRestriction restrictionWithRegions = regionRestrictionFactory.create(1L,
                regions.stream().map(Object::toString).collect(Collectors.joining(",")));

        OrderStatusUpdatedEvent eventWithRegion = EventFactory.orderStatusUpdated(
                withDeliveryRegion(randomRegionInRegions));

        assertTrue(restrictionWithRegions.fitCondition(eventWithRegion,
                triggersFactory.createDefaultPromoTriggerEventData(PROMO), new TriggerDataCache()).isMatched());
    }

    @Test
    public void testAnyRegion() throws UnresolvableException {
        long randomRegion = 213L;
        RegionRestriction restrictionWithRegions = regionRestrictionFactory.create(1L, "");

        OrderStatusUpdatedEvent eventWithRegion = EventFactory.orderStatusUpdated(withDeliveryRegion(randomRegion));

        assertTrue(restrictionWithRegions.fitCondition(eventWithRegion,
                triggersFactory.createDefaultPromoTriggerEventData(PROMO), new TriggerDataCache()).isMatched());
    }

}
