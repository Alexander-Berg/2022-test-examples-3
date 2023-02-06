package ru.yandex.market.checkout.checkouter.checkout;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryPartnerType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.outlet.OutletPurpose;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.YandexMarketDeliveryHelper;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;

import static ru.yandex.market.checkout.test.providers.DeliveryProvider.MOCK_DELIVERY_SERVICE_ID;
import static ru.yandex.market.checkout.test.providers.DeliveryProvider.RUSPOST_DELIVERY_SERVICE_ID;

public class CreateOrderOutletPurposeTest extends AbstractWebTestBase {

    @Autowired
    private YandexMarketDeliveryHelper yandexMarketDeliveryHelper;

    @Autowired
    private OrderGetHelper orderGetHelper;

    @Autowired
    private EventsGetHelper eventsGetHelper;

    @Test
    public void shouldSaveOutletPurpose() {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        Assertions.assertEquals(DeliveryType.PICKUP, order.getDelivery().getType());
        Assertions.assertNotNull(order.getDelivery().getOutlet());
        Assertions.assertEquals(OutletPurpose.PICKUP, order.getDelivery().getOutletPurpose());
    }

    @Test
    public void shouldGetOutletPurpose() throws Exception {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        order = orderGetHelper.getOrder(order.getId(), ClientInfo.SYSTEM);

        Assertions.assertEquals(DeliveryType.PICKUP, order.getDelivery().getType());
        Assertions.assertNotNull(order.getDelivery().getOutlet());
        Assertions.assertEquals(OutletPurpose.PICKUP, order.getDelivery().getOutletPurpose());
    }

    @Test
    public void shouldGetOutletPurposeFromEvent() throws Exception {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.PICKUP)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPickup(MOCK_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(order.getId());
        OrderHistoryEvent event = Iterables.get(events.getItems(), 0);
        order = event.getOrderAfter();

        Assertions.assertEquals(DeliveryType.PICKUP, order.getDelivery().getType());
        Assertions.assertNotNull(order.getDelivery().getOutlet());
        Assertions.assertEquals(OutletPurpose.PICKUP, order.getDelivery().getOutletPurpose());
    }

    @Test
    public void shouldSaveOutletPurposeForPost() {
        Order order = yandexMarketDeliveryHelper.newMarDoOrderBuilder()
                .withColor(Color.BLUE)
                .withDeliveryType(DeliveryType.POST)
                .withDeliveryPartnerType(DeliveryPartnerType.YANDEX_MARKET)
                .withActualDelivery(ActualDeliveryProvider.builder()
                        .addPost(1, RUSPOST_DELIVERY_SERVICE_ID)
                        .build())
                .build();

        Assertions.assertEquals(DeliveryType.POST, order.getDelivery().getType());
        Assertions.assertNotNull(order.getDelivery().getPostOutlet());
        Assertions.assertEquals(OutletPurpose.POST, order.getDelivery().getOutletPurpose());
    }
}
