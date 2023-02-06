package ru.yandex.market.abo.core.hiding.util.checker;

import java.math.BigDecimal;
import java.util.Date;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.api.entity.offer.hidden.details.DeliveryOption;
import ru.yandex.market.abo.api.entity.offer.hidden.details.HidingDetails;
import ru.yandex.market.abo.core.hiding.util.model.FreshOfferWrapper;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.common.report.indexer.model.OfferDetails;
import ru.yandex.market.common.report.model.LocalDeliveryOption;

import static java.util.Collections.min;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author artemmz
 * created on 27.01.17.
 */
public abstract class DeliveryCheckerTest extends EmptyTest {

    private static final double DELTA = 0.0001;

    static Offer initOfferWithDeliveryOpts(Integer dayTo, BigDecimal cost) {
        Offer offer = new Offer();
        offer.setLocalDelivery(singletonList(initDeliveryOption(dayTo, null, cost)));
        offer.setShopId(RND.nextLong());
        return offer;
    }

    public static FreshOfferWrapper initFeedWithDeliveryOpts(Integer dayTo, BigDecimal cost) {
        OfferDetails offerDetails =
                new OfferDetails(new Date(), new Date(), RND.nextDouble(), RND.nextBoolean(), -RND.nextInt(10000), "foo");
        offerDetails.setLocalDelivery(singletonList(initDeliveryOption(dayTo, null, cost)));
        offerDetails.setOfferId("testOfferId");
        return new FreshOfferWrapper(offerDetails);
    }

    public static LocalDeliveryOption initDeliveryOption(Integer dayTo, Integer orderBefore, BigDecimal cost) {
        return initDeliveryOption(null, dayTo, orderBefore, cost);
    }

    public static LocalDeliveryOption initDeliveryOption(Integer dayFrom, Integer dayTo, Integer orderBefore,
                                                         BigDecimal cost) {
        LocalDeliveryOption option = new LocalDeliveryOption();
        option.setOrderBefore(orderBefore == null ? LocalDeliveryOption.DEFAULT_ORDER_BEFORE : orderBefore);
        option.setDayFrom(dayFrom);
        option.setDayTo(dayTo);
        option.setCost(cost);
        return option;
    }

    void testRejectDeliveryOptsInPICheck(FeedReportDiff feedReportDiff) throws Exception {
        assertNull(feedReportDiff.diff(
                initOfferWithDeliveryOpts(null, nextBigDecimal()),
                initFeedWithDeliveryOpts(null, nextBigDecimal()),
                null
        ));
    }

    void assertFeedDelivery(HidingDetails details, OfferDetails feed) {
        assertDelivery(details.getDeliveryComparison().getShopParam(), feed.getLocalDelivery().iterator().next());
    }

    void assertOfferDelivery(HidingDetails details, Offer offer) {
        DeliveryOption marketParamToSave = details.getDeliveryComparison().getMarketParam();
        LocalDeliveryOption offerOption = offer.getLocalDelivery().iterator().next();
        assertDelivery(marketParamToSave, offerOption);
    }

    private void assertDelivery(DeliveryOption paramToSave, LocalDeliveryOption initialOption) {
        assertEquals(initialOption.getDayTo(), paramToSave.getDayTo());
        assertEquals(paramToSave.getPrice(), initialOption.getCost().doubleValue(), DELTA);
    }
}
