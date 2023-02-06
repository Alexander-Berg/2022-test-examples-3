package ru.yandex.market.checkout.checkouter.delivery;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.event.PagedEvents;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.order.OrderPropertyType;
import ru.yandex.market.checkout.helpers.EventsGetHelper;
import ru.yandex.market.checkout.helpers.OrderDeliveryHelper;
import ru.yandex.market.checkout.helpers.OrderPayHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.DeliveryUpdateProvider;

public class OrderControllerDeliveryUpdateCRMTest extends AbstractWebTestBase {

    private static final long OPERATOR_UID = 123123L;
    private static final String RECIPIENT = "new recipient";
    private static final String PHONE = "new_phone";
    @Autowired
    private OrderDeliveryHelper orderDeliveryHelper;
    @Autowired
    private OrderPayHelper orderPayHelper;
    @Autowired
    private EventsGetHelper eventsGetHelper;


    @Test
    public void shouldAllowToUpdateRecipientAndPhone() throws Exception {
        Parameters defaultBlueOrderParameters = BlueParametersProvider.defaultBlueOrderParameters();
        defaultBlueOrderParameters.setAcceptMethod(OrderAcceptMethod.WEB_INTERFACE);
        defaultBlueOrderParameters.getOrder().setProperty(OrderPropertyType.ASYNC_OUTLET_ANTIFRAUD, true); // чтобы
        // попасть в PENDING

        Order order = orderCreateHelper.createOrder(defaultBlueOrderParameters);
        orderPayHelper.payForOrder(order);
        Order order2 = orderService.getOrder(order.getId());


        AddressImpl addressRequest = (AddressImpl) ((AddressImpl) order2.getDelivery().getBuyerAddress()).clone();
        addressRequest.setRecipient(RECIPIENT);
        addressRequest.setPhone(PHONE);

        Delivery deliveryUpdate = DeliveryUpdateProvider.createDeliveryUpdate(d -> {
            d.setRegionId(order2.getDelivery().getRegionId());
            d.setAddress(addressRequest);
        });

        Order updatedOrder = orderDeliveryHelper.updateOrderDelivery(
                order2,
                new ClientInfo(ClientRole.CALL_CENTER_OPERATOR, OPERATOR_UID),
                deliveryUpdate
        );

        Address addressResponse = updatedOrder.getDelivery().getShopAddress();
        Assertions.assertNull(updatedOrder.getDelivery().getBuyerAddress());
        Assertions.assertNotNull(addressResponse.getCity());
        Assertions.assertNotNull(addressResponse.getCountry());
        Assertions.assertEquals(RECIPIENT, updatedOrder.getDelivery().getRecipient().getPerson().getFormattedName());
        Assertions.assertEquals(PHONE, addressResponse.getPhone());

        PagedEvents events = eventsGetHelper.getOrderHistoryEvents(updatedOrder.getId());

        OrderHistoryEvent orderHistoryEvent = events.getItems().stream()
                .filter(ohe -> ohe.getType() == HistoryEventType.ORDER_DELIVERY_UPDATED)
                .findAny()
                .orElse(null);

        Assertions.assertNotNull(orderHistoryEvent, "Should generate ORDER_DELIVERY_UPDATED event");

        Order orderBefore = orderHistoryEvent.getOrderBefore();
        Assertions.assertEquals(
                order2.getDelivery().getRecipient(),
                orderBefore.getDelivery().getRecipient());
        Assertions.assertEquals(
                order2.getDelivery().getShopAddress().getPhone(),
                orderBefore.getDelivery().getShopAddress().getPhone()
        );

        Order orderAfter = orderHistoryEvent.getOrderAfter();
        Assertions.assertEquals(
                updatedOrder.getDelivery().getRecipient(),
                orderAfter.getDelivery().getRecipient()
        );
        Assertions.assertEquals(
                updatedOrder.getDelivery().getShopAddress().getPhone(),
                orderAfter.getDelivery().getShopAddress().getPhone()
        );
    }
}
