package ru.yandex.market.checkout.checkouter.order;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.delivery.Recipient;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.helpers.OrderCreateHelper;
import ru.yandex.market.checkout.helpers.OrderGetHelper;
import ru.yandex.market.checkout.helpers.OrderHistoryEventsTestHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.RecipientProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.common.util.date.DateUtil.SIMPLE_DATE_FORMAT;

public class CheckoutRecipientEmailTest extends AbstractWebTestBase {

    @Autowired
    private OrderCreateHelper orderCreateHelper;
    @Autowired
    private OrderGetHelper orderGetHelper;

    @Autowired
    private OrderHistoryEventsTestHelper historyEventsTestHelper;

    @Test
    public void shouldSaveEmailInAddress() throws Exception {

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Recipient defaultRecipient = RecipientProvider.getDefaultRecipient();
        parameters.getOrder().getDelivery().setRecipient(defaultRecipient);

        MultiOrder newMultiOrder = checkoutOrder(parameters);

        Order newOrder = newMultiOrder.getCarts().get(0);
        assertEquals(defaultRecipient.getEmail(), newOrder.getDelivery().getRecipient().getEmail());
    }

    @Test
    public void shouldGetEmailFromOrderByUser() throws Exception {

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Recipient defaultRecipient = RecipientProvider.getDefaultRecipient();
        parameters.getOrder().getDelivery().setRecipient(defaultRecipient);

        MultiOrder newMultiOrder = checkoutOrder(parameters);
        Order newOrder = newMultiOrder.getCarts().get(0);

        Order order = orderGetHelper.getOrder(newOrder.getId(), ClientInfo.SYSTEM);
        assertEquals(defaultRecipient.getEmail(), order.getDelivery().getRecipient().getEmail());
    }

    @Test
    public void shouldGetEmailFromOrders() throws Exception {

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Recipient defaultRecipient = RecipientProvider.getDefaultRecipient();
        parameters.getOrder().getDelivery().setRecipient(defaultRecipient);

        checkoutOrder(parameters);

        String fromDate = SIMPLE_DATE_FORMAT.format(new Date(getClock().instant().toEpochMilli()));
        String toDate = SIMPLE_DATE_FORMAT.format(new Date(getClock().instant().plus(1, ChronoUnit.DAYS)
                .toEpochMilli()));
        PagedOrders order = orderGetHelper.getOrders(ClientInfo.SYSTEM, fromDate, toDate);
        Collection<Order> items = order.getItems();
        assertEquals(1, items.size());
        assertEquals(defaultRecipient.getEmail(), items.iterator().next().getDelivery().getRecipient().getEmail());
    }

    @Test
    public void shouldGetEmailFromHistoryOrders() throws Exception {

        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        Recipient defaultRecipient = RecipientProvider.getDefaultRecipient();
        parameters.getOrder().getDelivery().setRecipient(defaultRecipient);

        MultiOrder multiOrder = checkoutOrder(parameters);
        Order order = multiOrder.getOrders().get(0);

        List<OrderHistoryEvent> allEvents = historyEventsTestHelper.getAllEvents(order.getId());
        allEvents.stream()
                .map(OrderHistoryEvent::getOrderBefore)
                .filter(Objects::nonNull)
                .map(Order::getDelivery)
                .map(Delivery::getRecipient)
                .map(Recipient::getEmail)
                .forEach(email -> assertEquals(defaultRecipient.getEmail(), email));
        allEvents.stream()
                .map(OrderHistoryEvent::getOrderAfter)
                .filter(Objects::nonNull)
                .map(Order::getDelivery)
                .map(Delivery::getRecipient)
                .map(Recipient::getEmail)
                .forEach(email -> assertEquals(defaultRecipient.getEmail(), email));
    }

    private MultiOrder checkoutOrder(Parameters parameters) throws Exception {
        MultiCart multiCart = orderCreateHelper.cart(parameters);
        multiCart.getCarts().get(0).setDelivery(
                multiCart.getCarts().get(0).getDeliveryOptions().get(0)
        );
        multiCart.getCarts().get(0).getDelivery().setRecipient(parameters.getOrder().getDelivery().getRecipient());

        return orderCreateHelper.checkout(multiCart, parameters);
    }
}
