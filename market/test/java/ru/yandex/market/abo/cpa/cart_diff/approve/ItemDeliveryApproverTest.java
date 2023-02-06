package ru.yandex.market.abo.cpa.cart_diff.approve;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.abo.api.entity.offer.hidden.details.HidingDetails;
import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.abo.cpa.cart_diff.CartDiffPushApiResponseWrapper;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.pushapi.client.entity.CartResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * created on 29.06.17.
 */
@ExtendWith(MockitoExtension.class)
public class ItemDeliveryApproverTest {
    @InjectMocks
    private ItemDeliveryApprover itemDeliveryApprover;
    @Mock
    private Offer offer;
    @Mock
    private CartResponse cartResponse;
    @Mock
    private OrderItem orderItem;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(cartResponse.getItems()).thenReturn(Collections.singletonList(orderItem));
    }

    @Test
    public void approve() throws Exception {
        when(orderItem.getDelivery()).thenReturn(false);
        HidingDetails details = itemDeliveryApprover.approveDiff(offer, wrap(cartResponse), null);
        assertNotNull(details);
        assertTrue(details.getStockComparison().getMarketParam());
        assertFalse(details.getStockComparison().getShopParam());
    }

    @Test
    public void notApprove() throws Exception {
        when(orderItem.getDelivery()).thenReturn(true);
        assertNull(itemDeliveryApprover.approveDiff(offer, wrap(cartResponse), null));
    }

    @Test
    public void noItems() throws Exception {
        when(cartResponse.getItems()).thenReturn(null);
        assertNull(itemDeliveryApprover.approveDiff(offer, wrap(cartResponse), null));
    }

    private static CartDiffPushApiResponseWrapper wrap(CartResponse cartResponse) {
        return new CartDiffPushApiResponseWrapper(cartResponse);
    }
}
