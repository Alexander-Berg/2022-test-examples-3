package ru.yandex.market.abo.cpa.cart_diff.approve;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.api.entity.offer.hidden.details.HidingDetails;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.core.region.Regions;
import ru.yandex.market.abo.cpa.cart_diff.CartDiffPushApiResponseWrapper;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;
import ru.yandex.market.checkout.pushapi.client.entity.DeliveryResponse;
import ru.yandex.market.common.report.indexer.model.OfferDetails;
import ru.yandex.market.common.report.indexer.model.RegionalDeliveryOption;
import ru.yandex.market.common.report.model.LocalDeliveryOption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static ru.yandex.market.checkout.checkouter.delivery.DeliveryType.DELIVERY;

public class DeliveryCostApproverTest {
    protected static final Random RND = new Random();
    private static final long OWN_REGION = Regions.MOSCOW;
    private static final BigDecimal MARKET_DELIVERY_PRICE = new BigDecimal(100);
    private static final int FEED_PRICE = RND.nextInt(100500);

    @InjectMocks
    private DeliveryCostApprover deliveryCostApprover;
    @Mock
    private OfferDetails offerDetails;
    @Mock
    private Offer offer;
    @Mock
    private LocalDeliveryOption localDeliveryOption;

    private CartResponse cartResponse = new CartResponse();
    private CartDiffPushApiResponseWrapper responseWrapper = new CartDiffPushApiResponseWrapper(cartResponse);

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        when(offerDetails.getDefaultDeliveryOption()).thenReturn(initRegionalOption(FEED_PRICE));
        cartResponse.setDeliveryOptions(Collections.singletonList(initCartOption(FEED_PRICE, null)));

        when(offer.getClassifierMagicId()).thenReturn("cm_id " + RND.nextInt());
        when(offer.getLocalDelivery()).thenReturn(Collections.singletonList(localDeliveryOption));
        when(localDeliveryOption.getPrice()).thenReturn(MARKET_DELIVERY_PRICE);
    }

    @Test
    public void testFailCheck() throws Exception {
        cartResponse.setDeliveryOptions(Collections.singletonList(initCartOption(FEED_PRICE + 2, null)));
        assertTrue(deliveryCostApprover.approveDiff(offerDetails, RND.nextLong(), cartResponse, OWN_REGION));
    }

    @Test
    public void testPassCheck() throws Exception {
        cartResponse.setDeliveryOptions(Collections.singletonList(initCartOption(FEED_PRICE, null)));
        assertFalse(deliveryCostApprover.approveDiff(offerDetails, RND.nextLong(), cartResponse, OWN_REGION));
    }

    @Test
    public void testRejectEmptyFeedOptionsCheck() throws Exception {
        when(offerDetails.getLocalDelivery()).thenReturn(new ArrayList<>());
        assertFalse(deliveryCostApprover.approveDiff(offerDetails, RND.nextLong(), cartResponse, OWN_REGION));
    }

    @Test
    public void approveUsualCartDiff() throws Exception {
        int SHOP_PRICE = MARKET_DELIVERY_PRICE.intValue() + 10;
        cartResponse.setDeliveryOptions(Collections.singletonList(initCartOption(SHOP_PRICE, null)));

        HidingDetails details = deliveryCostApprover.approveDiff(offer, responseWrapper, null);
        assertNotNull(details);
        assertEquals(details.getDeliveryComparison().getMarketParam().getPrice().intValue(), MARKET_DELIVERY_PRICE.intValue());
        assertEquals(details.getDeliveryComparison().getShopParam().getPrice().intValue(), SHOP_PRICE);
    }

    @Test
    public void noCartOptions() throws Exception {
        cartResponse.setDeliveryOptions(null);
        HidingDetails details = deliveryCostApprover.approveDiff(offer, responseWrapper, null);
        assertNotNull(details);
        assertEquals(details.getDeliveryComparison().getMarketParam().getPrice().intValue(), MARKET_DELIVERY_PRICE.intValue());
        assertNull(details.getDeliveryComparison().getShopParam());
    }

    private static RegionalDeliveryOption initRegionalOption(int price) {
        RegionalDeliveryOption option = new RegionalDeliveryOption();
        option.setPrice(new BigDecimal(price));
        option.setRegionId((long) Regions.MOSCOW);
        return option;
    }

    static DeliveryResponse initCartOption(Integer price, Date toDate) {
        DeliveryResponse response = new DeliveryResponse();
        response.setPrice(new BigDecimal(price != null ? price : 0));
        response.setDeliveryDates(new DeliveryDates(null, toDate));
        response.setType(DELIVERY);
        return response;
    }
}
