package ru.yandex.market.checkout.checkouter.itemservice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderCreateService;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.PagedOrders;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.ItemServiceProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ItemServiceOrderSearchRequestTest extends AbstractWebTestBase {

    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderCreateService orderCreateService;

    @Test
    public void test() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        ItemService itemService = ItemServiceProvider.builder()
                .configure(ItemServiceProvider::applyDefaults)
                .configure(builder -> builder
                        .id(null)
                        .date(Date.from(LocalDate.now().plusDays(2).atStartOfDay().atZone(getClock().getZone())
                                .toInstant()))
                )
                .build();

        parameters.getOrder().getItems().iterator().next().addService(itemService);

        parameters.getOrder().setItemsTotal(BigDecimal.valueOf(10L));
        parameters.getOrder().setBuyerItemsTotal(BigDecimal.valueOf(10L));
        parameters.getOrder().setBuyerTotal(BigDecimal.valueOf(10L));
        parameters.getOrder().setFeeTotal(BigDecimal.valueOf(10L));
        parameters.getOrder().setTotal(BigDecimal.valueOf(10L));

        parameters.getOrder().setServicesTotal(BigDecimal.valueOf(10L));

        long orderId = orderCreateService.createOrder(parameters.getOrder(), ClientInfo.SYSTEM);

        OrderSearchRequest searchRequest = OrderSearchRequest.builder()
                .withOrderIds(new Long[]{orderId})
                .withJoinOrderItem(true)
                .withPartials(new OptionalOrderPart[]{OptionalOrderPart.ITEM_SERVICES})
                .build();
        PagedOrders actualResult = orderService.getOrders(searchRequest, ClientInfo.SYSTEM);
        Collection<Order> orders = actualResult.getItems();
        assertThat(orders, hasSize(1));

        Order order = orders.iterator().next();
        assertThat(order.getItems(), hasSize(1));

        OrderItem orderItem = order.getItems().iterator().next();
        assertThat(orderItem.getServices(), hasSize(1));

        ItemService actualItemService = orderItem.getServices().iterator().next();
        checkItemService(itemService, actualItemService);
    }

    private void checkItemService(ItemService expected, ItemService actual) {
        assertEquals(expected.getServiceId(), actual.getServiceId());
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getDate(), actual.getDate());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getPrice().compareTo(actual.getPrice()), 0);
    }
}
