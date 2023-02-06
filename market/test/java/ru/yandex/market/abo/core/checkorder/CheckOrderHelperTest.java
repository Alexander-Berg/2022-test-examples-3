package ru.yandex.market.abo.core.checkorder;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang.time.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.abo.core.offer.report.Offer;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryDates;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.shipment.Parcel;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * @author artemmz
 * @date 09/04/2020.
 */
class CheckOrderHelperTest {
    private static final int DAYS_TO_DELIVER = 3;
    private static final Date NOW = new Date();
    private static final PaymentMethod PAYMENT_METHOD = PaymentMethod.CASH_ON_DELIVERY;

    @Mock
    Order order;
    @Mock
    Delivery fastDeliveryOpt;
    @Mock
    Delivery slowDeliveryOpt;
    @Mock
    CreateOrderParam createOrderParam;
    @Mock
    Offer offer;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        doReturn(List.of(fastDeliveryOpt, slowDeliveryOpt)).when(order).getDeliveryOptions();
        when(fastDeliveryOpt.getDeliveryDates()).thenReturn(new DeliveryDates(NOW, NOW));
        Date slowDeliveryDate = DateUtils.addDays(NOW, DAYS_TO_DELIVER);
        when(slowDeliveryOpt.getDeliveryDates()).thenReturn(new DeliveryDates(slowDeliveryDate, slowDeliveryDate));
        when(createOrderParam.getPaymentMethod()).thenReturn(PAYMENT_METHOD);
        when(offer.getPrice()).thenReturn(BigDecimal.ONE);
        Stream.of(slowDeliveryOpt, fastDeliveryOpt)
                .forEach(opt -> when(opt.getPaymentOptions()).thenReturn(Set.of(PAYMENT_METHOD)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void getDeliveryByMinDateFrom(boolean needSlowDelivery) {
        when(createOrderParam.getMinDeliveryFromDay()).thenReturn(needSlowDelivery ? DAYS_TO_DELIVER : null);
        Delivery deliveryOpt = CheckOrderHelper.getDelivery(order, createOrderParam).orElseThrow();
        assertEquals(needSlowDelivery ? slowDeliveryOpt : fastDeliveryOpt, deliveryOpt);
    }

    @Test
    void paymentMethod() {
        when(slowDeliveryOpt.getPaymentOptions()).thenReturn(Set.of(PaymentMethod.APPLE_PAY));
        when(fastDeliveryOpt.getPaymentOptions()).thenReturn(Set.of(PAYMENT_METHOD));
        Delivery deliveryOpt = CheckOrderHelper.getDelivery(order, createOrderParam).orElseThrow();
        assertEquals(fastDeliveryOpt, deliveryOpt);
    }

    @ParameterizedTest
    @CsvSource({"1, 1", "2, 1"})
    void cartCount(int neededOfferCnt, int actualOfferCnt) {
        CreateOrderParam orderParam = new CreateOrderParam();
        orderParam.setOffersCount(neededOfferCnt);
        int itemsPerOffer = 2;
        orderParam.setItemsPerOffer(itemsPerOffer);

        Collection<OrderItem> items = CheckOrderHelper.createOrder(List.of(offer), orderParam).getItems();
        assertEquals(actualOfferCnt, items.size());
        assertEquals(itemsPerOffer * neededOfferCnt, items.stream().mapToInt(OrderItem::getCount).sum());
    }

    @Test
    void createMulticartFake() {
        long shopId = 123L;
        String notes = "This is fake order";

        CreateOrderParam orderParam = new CreateOrderParam();
        orderParam.setOffersCount(1);
        orderParam.setRegionId(213L);
        orderParam.setShopId(shopId);
        orderParam.setRgb(Color.WHITE);
        orderParam.setNotes(notes);
        int itemsPerOffer = 2;
        orderParam.setItemsPerOffer(itemsPerOffer);

        when(offer.getWeight()).thenReturn(BigDecimal.valueOf(1));
        when(offer.getWidth()).thenReturn(BigDecimal.valueOf(2));
        when(offer.getHeight()).thenReturn(BigDecimal.valueOf(3));
        when(offer.getDepth()).thenReturn(BigDecimal.valueOf(4));

        Order order = CheckOrderHelper.createOrderFake(List.of(offer), orderParam);

        assertTrue(order.isFake());
        assertEquals(shopId, order.getShopId());
        assertEquals(notes, order.getNotes());
        assertEquals(Color.WHITE, order.getRgb());

        Delivery expectedDelivery = new Delivery();
        expectedDelivery.setRegionId(213L);
        expectedDelivery.setDeliveryServiceId(99L);
        expectedDelivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        expectedDelivery.setServiceName("Доставка");

        Parcel parcel = new Parcel();
        parcel.setWeight(1L);
        parcel.setWidth(2L);
        parcel.setHeight(3L);
        parcel.setDepth(4L);

        expectedDelivery.setParcels(List.of(parcel));

        assertEquals(expectedDelivery, order.getDelivery());

        assertEquals(1, order.getItems().size());
        assertEquals(itemsPerOffer, order.getItems().stream().mapToInt(OrderItem::getCount).sum());
    }

}
