package ru.yandex.market.checkout.checkouter.actualization.multicart.processor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.cart.ItemChange;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.OfferItem;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder;
import ru.yandex.market.checkout.checkouter.validation.PromoSampleCartChangeError;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.FoundOffer;

import static java.util.stream.Collectors.toMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SampleOffersProcessorTest {

    private static final String BUNDLE_ID_UNKNOWN = "UNKNOWN";
    private static final String BUNDLE_ID_1 = "1";

    private SampleOffersProcessor sampleOffersProcessor;

    @BeforeEach
    public void setUp() {
        sampleOffersProcessor = new SampleOffersProcessor();
    }

    @Test
    public void process() {
        CheckoutContextHolder.setCheckoutOperation(true);
        // OK     case1: bundle id is present, main item is present (main item has same bundle id)
        FeedOfferId sampleId1 = FeedOfferId.from(11, "id11");
        OrderItem sampleItem1 = orderItem(sampleId1, BUNDLE_ID_1);
        FeedOfferId mainId1 = FeedOfferId.from(12, "id12");
        OrderItem mainItem1 = orderItem(mainId1, BUNDLE_ID_1);
        // NOT OK case 2: bundle id is present, main item is missing (main item must be present in cart)
        FeedOfferId sampleId2 = FeedOfferId.from(21, "id21");
        OrderItem sampleItem2 = orderItem(sampleId2, BUNDLE_ID_UNKNOWN);
        // NOT OK case 3: no bundle id (sample item cannot be ordered without main item)
        FeedOfferId sampleId3 = FeedOfferId.from(31, "id32");
        OrderItem sampleItem3 = orderItem(sampleId3, null);
        //
        MultiCart multiCart = multiCart(mainItem1, sampleItem1, sampleItem2, sampleItem3);
        MultiCartFetchingContext context = multiCartActualizationContext(
                foundOffer(mainId1, false),
                foundOffer(sampleId1, true),
                foundOffer(sampleId2, true),
                foundOffer(sampleId3, true)
        );

        sampleOffersProcessor.process(multiCart, context);

        Map<FeedOfferId, OrderItem> idToItem = multiCart.getCarts().stream()
                .map(Order::getItems)
                .flatMap(Collection::stream)
                .collect(toMap(OfferItem::getFeedOfferId, Function.identity()));
        assertNull(idToItem.get(sampleId1).getChanges());
        assertTrue(idToItem.get(sampleId2).getChanges().contains(ItemChange.SAMPLE_MISSING_MAIN_ITEM));
        assertTrue(idToItem.get(sampleId3).getChanges().contains(ItemChange.SAMPLE_MISSING_MAIN_ITEM));
        assertEquals(PromoSampleCartChangeError.TYPE,
                multiCart.getCarts().get(0).getValidationErrors().get(0).getCode());
    }

    private OrderItem orderItem(FeedOfferId feedOfferId, String bundleId) {
        OrderItem item = new OrderItem();
        item.setFeedOfferId(feedOfferId);
        item.setBundleId(bundleId);

        return item;
    }

    private FoundOffer foundOffer(FeedOfferId feedOfferId, boolean sample) {
        FoundOffer foundOffer = new FoundOffer();
        foundOffer.setShopOfferId(feedOfferId.getId());
        foundOffer.setFeedId(feedOfferId.getFeedId());
        foundOffer.setSample(sample);

        return foundOffer;
    }

    private MultiCart multiCart(OrderItem... items) {
        Order order = new Order();
        order.setItems(List.of(items));

        MultiCart multiCart = new MultiCart();
        multiCart.setCarts(List.of(order));

        return multiCart;
    }

    private MultiCartFetchingContext multiCartActualizationContext(FoundOffer... foundOffers) {
        ImmutableMultiCartContext immutableMultiCartContext = mock(ImmutableMultiCartContext.class);
        when(immutableMultiCartContext.getMultiCartOffers()).thenReturn(ImmutableList.copyOf(foundOffers));

        MultiCartFetchingContext context = mock(MultiCartFetchingContext.class);
        when(context.makeImmutableContext()).thenReturn(immutableMultiCartContext);
        return context;
    }
}
