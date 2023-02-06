package ru.yandex.market.checkout.checkouter.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.ItemService;
import ru.yandex.market.checkout.checkouter.order.ItemServiceStatus;
import ru.yandex.market.checkout.checkouter.order.OptionalOrderPart;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.checkout.checkouter.storage.itemservice.ItemServiceDao;
import ru.yandex.market.checkout.util.services.OrderServiceHelper;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author zagidullinri
 * @date 24.08.2021
 */
public class CheckouterClientItemServicePartialTest extends AbstractWebTestBase {

    private static final RequestClientInfo SYSTEM_CLIENT = new RequestClientInfo(ClientRole.SYSTEM, null);
    private static final BigDecimal ITEM_SERVICE_PRICE = BigDecimal.valueOf(150L);
    private static final int ORDER_ITEMS_COUNT = 3;
    private static final BigDecimal TOTAL_ITEM_SERVICES_AMOUNT = ITEM_SERVICE_PRICE
            .multiply(BigDecimal.valueOf(ORDER_ITEMS_COUNT));

    @Autowired
    private OrderServiceHelper orderServiceHelper;
    @Autowired
    private ItemServiceDao itemServiceDao;

    @Test
    public void getOrderShouldFetchItemServicesWhenNeeded() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SINGLE_SERVICE_PER_MULTIPLE_ORDER_ITEMS, false);

        Order order = prepareOrder(ORDER_ITEMS_COUNT, TOTAL_ITEM_SERVICES_AMOUNT);
        ItemService itemService = getFirstItemService(order);
        OrderRequest orderRequest = OrderRequest.builder(order.getId())
                .withPartials(Collections.singleton(OptionalOrderPart.ITEM_SERVICES))
                .build();

        Order actualOrder = client.getOrder(SYSTEM_CLIENT, orderRequest);
        ItemService actualItemService = getFirstItemService(actualOrder);

        checkItemService(itemService, actualItemService);
        assertEquals(order.getTotal().compareTo(actualOrder.getTotal()), 0);
        assertEquals(order.getBuyerTotal().compareTo(actualOrder.getBuyerTotal()), 0);
        assertEquals(order.getServicesTotal().compareTo(actualOrder.getServicesTotal()), 0);
        assertEquals(TOTAL_ITEM_SERVICES_AMOUNT.compareTo(actualOrder.getServicesTotal()), 0);
    }

    @Test
    public void getOrderShouldFetchItemServicesWhenNeededWithSingleServicePerItems() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SINGLE_SERVICE_PER_MULTIPLE_ORDER_ITEMS, true);

        Order order = prepareOrder(ORDER_ITEMS_COUNT, ITEM_SERVICE_PRICE);
        ItemService itemService = getFirstItemService(order);
        OrderRequest orderRequest = OrderRequest.builder(order.getId())
                .withPartials(Collections.singleton(OptionalOrderPart.ITEM_SERVICES))
                .build();

        Order actualOrder = client.getOrder(SYSTEM_CLIENT, orderRequest);
        ItemService actualItemService = getFirstItemService(actualOrder);

        checkItemService(itemService, actualItemService);
        assertEquals(order.getTotal().compareTo(actualOrder.getTotal()), 0);
        assertEquals(order.getBuyerTotal().compareTo(actualOrder.getBuyerTotal()), 0);
        assertEquals(order.getServicesTotal().compareTo(actualOrder.getServicesTotal()), 0);
        assertEquals(ITEM_SERVICE_PRICE.compareTo(actualOrder.getServicesTotal()), 0);
    }

    @Test
    public void getOrderShouldNotFetchItemServicesWhenNotNeeded() {
        Order order = prepareOrder(1, TOTAL_ITEM_SERVICES_AMOUNT);
        OrderRequest orderRequest = OrderRequest.builder(order.getId()).build();

        Order actualOrder = client.getOrder(SYSTEM_CLIENT, orderRequest);
        Set<ItemService> actualItemServices = actualOrder.getItems()
                .iterator()
                .next()
                .getServices();

        assertThat(actualItemServices, empty());
        assertEquals(order.getTotal().compareTo(actualOrder.getTotal()), 0);
        assertEquals(order.getBuyerTotal().compareTo(actualOrder.getBuyerTotal()), 0);
        assertNull(actualOrder.getServicesTotal());
    }

    @Test
    public void getOrdersByUserShouldFetchItemServicesWhenNeeded() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SINGLE_SERVICE_PER_MULTIPLE_ORDER_ITEMS, false);

        Order order = prepareOrder(ORDER_ITEMS_COUNT, TOTAL_ITEM_SERVICES_AMOUNT);
        ItemService itemService = getFirstItemService(order);
        OrderSearchRequest orderSearchRequest = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .withPartials(new OptionalOrderPart[]{OptionalOrderPart.ITEM_SERVICES})
                .build();

        Order actualOrder = client.getOrdersByUser(orderSearchRequest, order.getBuyer().getUid())
                .getItems()
                .iterator()
                .next();
        ItemService actualItemService = getFirstItemService(actualOrder);

        checkItemService(itemService, actualItemService);
        assertEquals(order.getTotal().compareTo(actualOrder.getTotal()), 0);
        assertEquals(order.getBuyerTotal().compareTo(actualOrder.getBuyerTotal()), 0);
        assertEquals(order.getServicesTotal().compareTo(actualOrder.getServicesTotal()), 0);
        assertEquals(TOTAL_ITEM_SERVICES_AMOUNT.compareTo(actualOrder.getServicesTotal()), 0);
    }

    @Test
    public void getOrdersByUserShouldFetchItemServicesWhenNeededWithSingleServicePerItems() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.SINGLE_SERVICE_PER_MULTIPLE_ORDER_ITEMS, true);

        Order order = prepareOrder(ORDER_ITEMS_COUNT, ITEM_SERVICE_PRICE);
        ItemService itemService = getFirstItemService(order);
        OrderSearchRequest orderSearchRequest = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .withPartials(new OptionalOrderPart[]{OptionalOrderPart.ITEM_SERVICES})
                .build();

        Order actualOrder = client.getOrdersByUser(orderSearchRequest, order.getBuyer().getUid())
                .getItems()
                .iterator()
                .next();
        ItemService actualItemService = getFirstItemService(actualOrder);

        checkItemService(itemService, actualItemService);
        assertEquals(order.getTotal().compareTo(actualOrder.getTotal()), 0);
        assertEquals(order.getBuyerTotal().compareTo(actualOrder.getBuyerTotal()), 0);
        assertEquals(order.getServicesTotal().compareTo(actualOrder.getServicesTotal()), 0);
        assertEquals(ITEM_SERVICE_PRICE.compareTo(actualOrder.getServicesTotal()), 0);
    }

    @Test
    public void getOrdersByUserShouldNotFetchItemServicesWhenNotNeeded() {
        Order order = prepareOrder(1, TOTAL_ITEM_SERVICES_AMOUNT);
        OrderSearchRequest orderSearchRequest = OrderSearchRequest.builder()
                .withRgbs(Color.BLUE)
                .build();

        Order actualOrder = client.getOrdersByUser(orderSearchRequest, order.getBuyer().getUid())
                .getItems()
                .iterator()
                .next();
        Set<ItemService> actualItemServices = actualOrder.getItems()
                .iterator()
                .next()
                .getServices();

        assertThat(actualItemServices, empty());
        assertEquals(order.getTotal().compareTo(actualOrder.getTotal()), 0);
        assertEquals(order.getBuyerTotal().compareTo(actualOrder.getBuyerTotal()), 0);
        assertNull(actualOrder.getServicesTotal());
    }

    private ItemService getFirstItemService(Order order) {
        return order.getItems()
                .iterator()
                .next()
                .getServices()
                .iterator()
                .next();
    }

    private void checkItemService(ItemService expected, ItemService actual) {
        assertEquals(expected.getServiceId(), actual.getServiceId());
        assertEquals(expected.getTitle(), actual.getTitle());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getDate(), actual.getDate());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getPrice().compareTo(actual.getPrice()), 0);
    }

    private Order prepareOrder(int itemCnt, BigDecimal servicesTotal) {
        Order order = orderServiceHelper.createPostOrder(it -> it.getItems().iterator().next().setCount(itemCnt));
        order.setServicesTotal(servicesTotal);
        final ItemService itemService = buildItemService();
        itemService.setOrderId(order.getId());
        Iterator<OrderItem> iterator = order.getItems().iterator();
        OrderItem orderItem = iterator.next();
        transactionTemplate.execute(ts -> {
            itemServiceDao.insert(order.getId(), singletonMap(orderItem.getId(),
                    Collections.singletonList(itemService)));
            return null;
        });
        itemServiceDao.loadToOrders(singletonList(order));
        return order;
    }

    private ItemService buildItemService() {
        ItemService itemService = new ItemService();
        itemService.setServiceId(10L);
        itemService.setTitle("test_service_title");
        itemService.setDescription("test_service_description");
        itemService.setDate(Date.from(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS).plusDays(2)
                .atZone(getClock().getZone()).toInstant()));
        itemService.setStatus(ItemServiceStatus.NEW);
        itemService.setPrice(ITEM_SERVICE_PRICE);
        itemService.setCount(1);
        return itemService;
    }
}
