package ru.yandex.market.checkout.pushapi.service;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.Address;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.checkout.pushapi.client.entity.Cart;
import ru.yandex.market.checkout.pushapi.client.entity.Region;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.shop.entity.ExternalCart;
import ru.yandex.market.checkout.pushapi.shop.entity.ShopOrder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RequestDetailsServiceTest {
    
    private RequestDetailsService requestDetailsService = new RequestDetailsService();
    private GeoService geoService = mock(GeoService.class);

    @Before
    public void setUp() throws Exception {
        requestDetailsService.setGeoService(geoService);
    }

    @Test
    public void testCreateExternalCartWithAddress() throws Exception {
        final Address address = mock(Address.class);
        final Region region = mock(Region.class);
        final Items items = createItems();

        final Delivery delivery = new Delivery() {{
            setAddress(address);
            setRegionId(213l);
        }};
        
        final Cart cart = new Cart() {{
            setCurrency(Currency.AED);
            setDelivery(delivery);
            setItems(items.offerItems);
        }};

        when(geoService.getRegion((long) 213)).thenReturn(region);

        final ExternalCart actual = requestDetailsService.createExternalCart(cart);
        
        assertEquals(cart.getCurrency(), actual.getCurrency());
        assertEquals(items.offerItems, actual.getItems());
        assertEquals(address, actual.getDeliveryWithRegion().getAddress());
        assertEquals(region, actual.getDeliveryWithRegion().getRegion());
        assertNull(actual.getDeliveryWithRegion().getRegionId());
        assertNull(actual.getDelivery());
    }

    private void fillItems(
        OrderItem offerItem, OrderItem cartItem,
        final Long feedId, final String offerId, final String offerName, final String feedCategoryId,
        final BigDecimal price, final int count
    ) {
        offerItem.setFeedId(feedId);
        offerItem.setOfferId(offerId);
        offerItem.setOfferName(offerName);
        offerItem.setFeedCategoryId(feedCategoryId);
        offerItem.setPrice(price);
        offerItem.setCount(count);
        cartItem.setFeedId(feedId);
        cartItem.setOfferId(offerId);
        cartItem.setOfferName(offerName);
        cartItem.setFeedCategoryId(feedCategoryId);
        cartItem.setPrice(price);
        cartItem.setCount(count);
    }

    @Test
    public void testCreateExternalCartWithoutAddress() throws Exception {
        final Items items = createItems();
        final Region region = mock(Region.class);

        final Delivery delivery = new Delivery() {{
            setRegionId(213l);
        }};

        final Cart cart = new Cart() {{
            setCurrency(Currency.AFN);
            setDelivery(delivery);

            setItems(items.offerItems);
        }};

        when(geoService.getRegion((long) 213)).thenReturn(region);

        final ExternalCart actual = requestDetailsService.createExternalCart(cart);

        assertEquals(cart.getCurrency(), actual.getCurrency());
        assertEquals(items.offerItems, actual.getItems());
        assertEquals(region, actual.getDeliveryWithRegion().getRegion());
        assertNull(actual.getDeliveryWithRegion().getRegionId());
        assertNull(actual.getDelivery());
    }

    @Test
    public void testCreateShopOrder() throws Exception {
        final OrderItem item1 = mock(OrderItem.class);
        final OrderItem item2 = mock(OrderItem.class);
        final Address address = mock(Address.class);
        final Items items = createItems();

        final Order order = new Order() {{
            setId(1234l);
            setCurrency(Currency.AOA);
            setPaymentType(PaymentType.PREPAID);
            setItems(items.offerItems);
            setDelivery(new Delivery() {{
                setType(DeliveryType.DELIVERY);
                setServiceName("2345");
                setPrice(new BigDecimal(3456));
                setDeliveryDates(new DeliveryDates(
                        XmlTestUtil.date("2013-05-20"), XmlTestUtil.date("2013-05-23")
                ));
                setRegionId(213l);
                setAddress(address);
                setOutletId(4567l);
            }});
        }};

        final Region region = mock(Region.class);
        when(geoService.getRegion(213)).thenReturn(region);

        final ShopOrder shopOrder = requestDetailsService.createShopOrder(order);
        assertEquals(order.getId(), shopOrder.getId());
        assertEquals(order.getCurrency(), shopOrder.getCurrency());
        assertEquals(order.getPaymentType(), shopOrder.getPaymentType());
        assertEquals(order.getItems(), shopOrder.getItems());
        assertEquals(order.getDelivery().getType(), shopOrder.getDeliveryWithRegion().getType());
        assertEquals(order.getDelivery().getServiceName(), shopOrder.getDeliveryWithRegion().getServiceName());
        assertEquals(order.getDelivery().getPrice(), shopOrder.getDeliveryWithRegion().getPrice());
        assertEquals(order.getDelivery().getDeliveryDates(), shopOrder.getDeliveryWithRegion().getDeliveryDates());
        assertEquals(order.getDelivery().getAddress(), shopOrder.getDeliveryWithRegion().getAddress());
        assertEquals(order.getDelivery().getOutletId(), shopOrder.getDeliveryWithRegion().getOutletId());
        assertEquals(region, shopOrder.getDeliveryWithRegion().getRegion());
        assertNull(shopOrder.getDelivery());
        assertNull(shopOrder.getDeliveryWithRegion().getRegionId());

    }

    private void assertCartItemsList(List<OrderItem> expected, List<OrderItem> actual) {
        assertEquals(expected.size(), actual.size());
        for(int i = 0; i < actual.size(); i++) {
            final OrderItem eItem = expected.get(i);
            final OrderItem aItem = actual.get(i);

            assertEquals(eItem.getFeedId(), aItem.getFeedId());
            assertEquals(eItem.getOfferId(), aItem.getOfferId());
            assertEquals(eItem.getOfferName(), aItem.getOfferName());
            assertEquals(eItem.getFeedCategoryId(), aItem.getFeedCategoryId());
            assertEquals(eItem.getPrice(), aItem.getPrice());
            assertEquals(eItem.getCount(), aItem.getCount());
        }
    }

    private class Items {
        private List<OrderItem> cartItems;
        private List<OrderItem> offerItems;

        public List<OrderItem> getCartItems() {
            return cartItems;
        }

        public List<OrderItem> getOrderItems() {
            return offerItems;
        }
    }

    private Items createItems() {
        final OrderItem offerItem1 = new OrderItem();
        final OrderItem cartItem1 = new OrderItem();
        fillItems(
            offerItem1, cartItem1,
            1234l, "2345", "iphone", "3456", new BigDecimal(4567), 2
        );
        final OrderItem offerItem2 = new OrderItem();
        final OrderItem cartItem2 = new OrderItem();
        fillItems(
            offerItem2, cartItem2,
            5687l, "6789", "htc one", "7890", new BigDecimal(8901), 3
        );
        final Items items = new Items();
        items.cartItems = Arrays.asList(cartItem1, cartItem2);
        items.offerItems = Arrays.asList(offerItem1, offerItem2);
        return items;
    }
}
