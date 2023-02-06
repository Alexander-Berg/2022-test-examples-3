package ru.yandex.market.checkout.helpers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import ru.yandex.common.util.id.IdsUtils;
import ru.yandex.market.checkout.checkouter.client.CheckouterClientParams;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.order.OfferItemKey;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderItem;
import ru.yandex.market.checkout.checkouter.order.OrderItems;
import ru.yandex.market.checkout.checkouter.order.OrderService;
import ru.yandex.market.checkout.checkouter.order.OrderUpdateService;
import ru.yandex.market.checkout.common.WebTestHelper;
import ru.yandex.market.checkout.common.rest.ErrorCodeException;
import ru.yandex.market.checkout.helpers.utils.MockMvcAware;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.test.providers.OrderItemProvider;
import ru.yandex.market.checkout.util.report.ReportConfigurer;
import ru.yandex.market.checkout.util.serialization.TestSerializationService;
import ru.yandex.market.common.report.model.FeedOfferId;
import ru.yandex.market.common.report.model.MarketReportPlace;

import static java.math.BigDecimal.ZERO;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.test.providers.OrderItemProvider.OTHER_WARE_MD5;
import static ru.yandex.market.checkout.util.ClientHelper.shopUserClientFor;
import static ru.yandex.market.checkout.util.matching.NumberMatcher.numberEqualsTo;

/**
 * @author : poluektov
 * date: 21.02.2019.
 */
@WebTestHelper
public class ChangeOrderItemsHelper extends MockMvcAware {

    private static final Random RANDOM = new Random(100555L);
    @Autowired
    private TestSerializationService serializationService;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderUpdateService orderUpdateService;
    @Autowired
    protected ReportConfigurer reportConfigurer;

    public ChangeOrderItemsHelper(WebApplicationContext webApplicationContext,
                                  TestSerializationService testSerializationService) {
        super(webApplicationContext, testSerializationService);
    }

    public ResultActions changeOrderItems(Collection<OrderItem> newItems, ClientInfo client, long orderId) throws
            Exception {
        return mockMvc.perform(
                put("/orders/{orderId}/items?clientRole={role}&clientId={clientId}&shopId={shopId}",
                        orderId, client.getRole(), client.getId(), client.getShopId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(serializationService.serializeCheckouterObject(new OrderItems(newItems)))
        );
    }

    public ResultActions putOrderItemInstances(String body, ClientInfo client, long orderId) throws
            Exception {
        return mockMvc.perform(
                put("/orders/{orderId}/items/instances?clientRole={role}&clientId={clientId}",
                        orderId, client.getRole(), client.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
        );
    }

    public ResultActions getItemsHistory(long orderId, long historyId, ClientInfo client) throws Exception {
        return mockMvc.perform(
                get("/orders/{orderId}/items/history/{historyId}?clientRole={role}&clientId={clientId}&shopId={shopId}",
                        orderId, historyId, client.getRole(), client.getId(), client.getShopId()));
    }

    public ResultActions checkChangeOrderItemsResponse(ResultActions result, Order orderBefore,
                                                       Map<OfferItemKey, Integer> itemsNewCount,
                                                       Collection<OrderItem> expectedItems) throws Exception {
        String response = result
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Collection<OrderItem> resultItems = serializationService.deserializeCheckouterObject(
                response,
                OrderItems.class
        ).getContent();
        checkEqualItemSet(resultItems, expectedItems);
        Order orderAfter = orderService.getOrder(orderBefore.getId());
        checkChangeItemsPrices(orderBefore, orderAfter, itemsNewCount);
        return result;
    }

    public static void checkChangeItemsPrices(Order orderBefore,
                                              Order orderAfter,
                                              Map<OfferItemKey, Integer> itemsNewCount) throws Exception {
        BigDecimal buyerSumDiff = itemsNewCount.entrySet().stream()
                .map(e -> {
                    OrderItem item = orderBefore.getItem(e.getKey());
                    return mul(item.getBuyerPrice(), e.getValue() - item.getCount());
                })
                .reduce(ZERO, BigDecimal::add);
        assertThat(orderAfter.getTotal(), numberEqualsTo(orderBefore.getTotal().add(buyerSumDiff)));
        assertThat(orderAfter.getItemsTotal(), numberEqualsTo(orderBefore.getItemsTotal().add(buyerSumDiff)));
        assertThat(orderAfter.getBuyerTotal(), numberEqualsTo(orderBefore.getBuyerTotal().add(buyerSumDiff)));
        assertThat(orderAfter.getBuyerItemsTotal(), numberEqualsTo(orderBefore.getBuyerItemsTotal().add(buyerSumDiff)));

        BigDecimal feeDiff = itemsNewCount.entrySet().stream().map(e -> {
            OrderItem item = orderBefore.getItem(e.getKey());
            return mul(item.getFeeSum(), e.getValue() - item.getCount()).setScale(2, RoundingMode.HALF_UP);
        }).reduce(ZERO, BigDecimal::add);
        assertThat(orderAfter.getFeeTotal(), numberEqualsTo(orderBefore.getFeeTotal().add(feeDiff)));
    }

    public static void checkEqualItemSet(Collection<OrderItem> actualItems, Collection<OrderItem> expectedItems) {
        expectedItems = ridOfZeroCounts(expectedItems);
        assertThat(actualItems, hasSize(expectedItems.size()));
        Map<Long, OrderItem> expItemsById = IdsUtils.toIdsMap(expectedItems);
        actualItems.forEach(i -> {
            OrderItem expectedItem = expItemsById.get(i.getId());
            assertThat(expectedItem, notNullValue());
            assertThat(i.getCount(), equalTo(expectedItem.getCount()));
        });
    }

    public static Collection<OrderItem> ridOfZeroCounts(Collection<OrderItem> items) {
        return items.stream().filter(i -> i.getCount() > 0).collect(toList());
    }

    public static BigDecimal mul(BigDecimal num, long multiplier) {
        return num.multiply(new BigDecimal(multiplier));
    }

    public OrderItem addNewItem(Order order) {
        OrderItem newItem = OrderItemProvider.buildOrderItem("333", -1L, 1, null);
        newItem.setMsku(Math.abs(RANDOM.nextLong()));
        newItem.setSku(newItem.getMsku().toString());
        newItem.setShopSku(String.valueOf(Math.abs(RANDOM.nextLong())));
        newItem.setWareMd5(OTHER_WARE_MD5);
        newItem.setSupplierId(order.getItems().stream()
                .findAny()
                .orElseThrow(() -> new RuntimeException("No items!"))
                .getSupplierId());
        mockReportForUpsale(order, newItem);
        return newItem;
    }

    public OrderItem addNewItem(Order order, FeedOfferId feedOfferId,
                                BigDecimal price) {
        OrderItem newItem = OrderItemProvider.buildOrderItem(feedOfferId.getId());

        newItem.setMsku(Math.abs(RANDOM.nextLong()));
        newItem.setSku(newItem.getMsku().toString());
        newItem.setShopSku(String.valueOf(Math.abs(RANDOM.nextLong())));
        newItem.setWareMd5(OTHER_WARE_MD5);
        newItem.setSupplierId(order.getItems().stream()
                .findAny()
                .orElseThrow(() -> new RuntimeException("No items!"))
                .getSupplierId());

        newItem.setPrice(price);
        newItem.setQuantPrice(price);
        newItem.setBuyerPrice(price);
        newItem.setFeedOfferId(feedOfferId);
        mockReportForUpsale(order, newItem);
        return newItem;
    }

    public OrderItem addBareItem(Order order) {
        OrderItem item = addNewItem(order);
        OrderItem newItem = new OrderItem(item.getFeedOfferId(), BigDecimal.valueOf(100), 1);
        newItem.setId(item.getId());
        newItem.setWareMd5(OTHER_WARE_MD5);
        return newItem;
    }

    private void mockReportForUpsale(Order order, OrderItem... newItems) {
        Order fakeOrder = order.clone();
        fakeOrder.setItems(Arrays.asList(newItems));
        Parameters parameters = new Parameters(fakeOrder);
        reportConfigurer.mockReportPlace(MarketReportPlace.OFFER_INFO, parameters.getReportParameters());
        reportConfigurer.mockReportPlace(MarketReportPlace.MODEL_INFO, parameters.getReportParameters());
    }

    public void fillShipmentDimensions(Order order) {
        Delivery delivery = order.getDelivery();
        delivery.getParcels().forEach(s -> {
            s.setWeight(randomLong(10, 500));
            s.setWidth(randomLong(5, 50));
            s.setHeight(randomLong(10, 70));
            s.setDepth(randomLong(3, 30));
            s.setParcelItems(null);
        });
        delivery.setAddress(null);
        orderUpdateService.updateOrderDelivery(order.getId(), delivery, shopUserClientFor(order));
    }

    private long randomLong(long rangeFrom, long rangeTo) {
        return rangeFrom + (long) (Math.random() * (rangeTo - rangeFrom));
    }

    public static Collection<OrderItem> reduceOneItem(Collection<OrderItem> items) {
        ArrayList<OrderItem> newItems = new ArrayList<>(items);
        newItems.get(0).setCount(newItems.get(0).getCount() - 1);
        return newItems;
    }

    public static ResultActions checkSuccessResponse(ResultActions resultActions) throws Exception {
        return resultActions
                .andExpect(status().is2xxSuccessful());
    }

    public static ResultActions checkErroneousResponse(ResultActions resultActions, ErrorCodeException expectedExc)
            throws Exception {
        return resultActions
                .andExpect(status().is(expectedExc.getStatusCode()))
                .andExpect(jsonPath("$.status").value(expectedExc.getStatusCode()))
                .andExpect(jsonPath("$.code").value(expectedExc.getCode()));
    }

    public ResultActions getOrderItems(long orderId) throws Exception {
        return mockMvc.perform(
                get("/orders/{orderId}/items", orderId)
                        .param(CheckouterClientParams.CLIENT_ROLE, ClientRole.SYSTEM.name()))
                .andExpect(status().isOk());
    }
}
