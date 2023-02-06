package ru.yandex.market.checkout.checkouter.events;

import java.util.Collections;
import java.util.stream.Collectors;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.auth.AuthInfo;
import ru.yandex.market.checkout.checkouter.client.OrderFilter;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvents;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.AuthHelper;
import ru.yandex.market.checkout.helpers.EventsQueueGetHelper;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

/**
 * Created by asafev on 12/09/2017.
 */
public class EventsControllerOrderFilterTest extends AbstractWebTestBase {

    @Autowired
    private EventsQueueGetHelper eventsQueueGetHelper;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private OrderServiceHelper orderServiceHelper;

    @Epic(Epics.GET_ORDER)
    @DisplayName("Чекаутер должен фильтровать заказы по признаку fulfilment")
    @Test
    public void filterFulfilmentOrders() throws Exception {
        orderServiceHelper.createOrder(false, true, Color.WHITE, null);
        orderServiceHelper.createOrder(true, false, Color.BLUE, null);

        OrderHistoryEvents response = eventsQueueGetHelper.getOrderHistoryEvents(
                null, Collections.singleton(HistoryEventType.ORDER_STATUS_UPDATED), null,
                OrderFilter.builder().setFulfilment(true).setRgb(Color.BLUE).build()
        );

        assertThat(response.getContent(), not(empty()));
        assertTrue(response.getContent().stream().allMatch(event -> event.getOrderAfter().isFulfilment()));
    }


    @Epic(Epics.GET_ORDER)
    @DisplayName("Чекаутер должен фильтровать заказы по признаку global")
    @Test
    public void filterGlobalOrders() throws Exception {
        orderServiceHelper.createOrder(false, true, Color.WHITE, null);
        orderServiceHelper.createOrder(true, false, Color.BLUE, null);

        OrderHistoryEvents response = eventsQueueGetHelper.getOrderHistoryEvents(
                null, Collections.singleton(HistoryEventType.ORDER_STATUS_UPDATED), null,
                OrderFilter.builder().setGlobal(true).setRgb(Color.WHITE).build()
        );

        assertThat(response.getContent(), not(empty()));
        assertTrue(response.getContent().stream().allMatch(event -> event.getOrderAfter().isGlobal()));
    }

    @Epic(Epics.GET_ORDER)
    @DisplayName("Чекаутер должен фильтровать заказы по признаку shopIds")
    @Story(Stories.ORDERS_EVENTS)
    @Test
    public void filterByShopIds() throws Exception {
        long shopId1 = 2431412L;
        long shopId2 = 9878768L;

        orderServiceHelper.createOrder(false, true, Color.WHITE, shopId1);
        orderServiceHelper.createOrder(true, false, Color.BLUE, shopId2);

        OrderFilter orderFilter = OrderFilter.builder().setShopIds(shopId1).setRgb(Color.WHITE).build();
        OrderHistoryEvents response = eventsQueueGetHelper.getOrderHistoryEvents(
                null, Collections.singleton(HistoryEventType.ORDER_STATUS_UPDATED), null, orderFilter
        );

        assertThat(
                response.getContent().stream()
                        .filter(e -> e.getOrderAfter().getShopId() != shopId1)
                        .collect(Collectors.toList()),
                empty()
        );
    }

    @Epic(Epics.GET_ORDER)
    @DisplayName("Чекаутер должен фильтровать заказы по признаку rgb, если он указан")
    @Story(Stories.ORDERS_EVENTS)
    @Test
    public void filterByColorIfSetSpecified() throws Exception {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.getRgb(), is(Color.WHITE));

        Parameters parameters2 = defaultBlueOrderParameters();
        parameters2.setColor(Color.BLUE);
        Order order2 = orderCreateHelper.createOrder(parameters2);
        assertThat(order2.getRgb(), is(Color.BLUE));

        OrderFilter filter1 = new OrderFilter();
        filter1.setRgb(new Color[]{Color.WHITE});

        OrderHistoryEvents response1 = eventsQueueGetHelper.getOrderHistoryEvents(
                null, Collections.singleton(HistoryEventType.ORDER_STATUS_UPDATED), null, filter1
        );

        assertThat(response1.getContent(), not(empty()));
        assertTrue(response1.getContent().stream().allMatch(ohe -> ohe.getOrderBefore().getRgb() == Color.WHITE));

        OrderFilter filter2 = new OrderFilter();
        filter2.setRgb(new Color[]{Color.WHITE, Color.BLUE});

        OrderHistoryEvents response2 = eventsQueueGetHelper.getOrderHistoryEvents(
                null, Collections.singleton(HistoryEventType.ORDER_STATUS_UPDATED), null, filter2
        );

        assertThat(response2.getContent(), not(empty()));
        assertTrue(response2.getContent().stream().anyMatch(ohe -> ohe.getOrderBefore().getRgb() == Color.WHITE));
        assertTrue(response2.getContent().stream().anyMatch(ohe -> ohe.getOrderBefore().getRgb() == Color.BLUE));
        assertTrue(response2.getContent().stream().noneMatch(ohe -> {
            Color color = ohe.getOrderBefore().getRgb();
            return color != Color.BLUE && color != Color.WHITE;
        }));
    }

    @Test
    public void shouldFilterByNoAuth() throws Exception {
        Parameters parameters = defaultBlueOrderParameters();
        Order order = orderCreateHelper.createOrder(parameters);
        assertThat(order.isNoAuth(), CoreMatchers.is(false));

        AuthInfo authInfo = authHelper.getAuthInfo();
        Parameters parameters2 = defaultBlueOrderParameters();
        parameters2.getOrder().getBuyer().setUid(authInfo.getMuid());
        Order order2 = orderCreateHelper.createOrder(parameters2);
        assertThat(order2.isNoAuth(), CoreMatchers.is(true));

        OrderFilter orderFilter = new OrderFilter();
        orderFilter.setRgb(new Color[]{Color.BLUE});
        orderFilter.setNoAuth(true);

        OrderHistoryEvents events = eventsQueueGetHelper.getOrderHistoryEvents(
                null, Collections.singleton(HistoryEventType.ORDER_STATUS_UPDATED), false, orderFilter
        );

        assertThat(events.getContent(), not(empty()));
        assertThat(events.getContent().stream().allMatch(ohe -> ohe.getOrderAfter().isNoAuth()), is(true));
        assertThat(events.getContent().stream().allMatch(ohe -> ohe.getOrderBefore().isNoAuth()), is(true));

    }
}
