package ru.yandex.market.checkout.checkouter.controllers;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.BasicOrder;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.shop.ShopService;
import ru.yandex.market.checkout.checkouter.storage.OrderHistoryDao;
import ru.yandex.market.checkout.checkouter.storage.OrderReadingDao;
import ru.yandex.market.checkout.checkouter.tasks.Partition;
import ru.yandex.market.checkout.common.rest.Pager;
import ru.yandex.market.checkout.helpers.OrderInsertHelper;
import ru.yandex.market.checkout.test.providers.OrderProvider;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.BUSINESS_ID;
import static ru.yandex.market.checkout.checkouter.client.CheckouterClientParams.SHOP_ID;

public class OrderMigrateControllerTest extends AbstractWebTestBase {

    @Autowired
    private OrderInsertHelper orderInsertHelper;
    @Autowired
    private ShopService shopService;

    @Autowired
    private OrderReadingDao orderReadingDao;

    @Autowired
    private OrderHistoryDao orderHistoryDao;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final Map<Long, Long> SHOP_BUSINESS = Map.of(
            100L, 1000L,
            200L, 2000L
    );
    private static final int COUNT = 4;


    private Set<Long> generateOrders() {
        final Set<Long> orderIds = new HashSet<>();
        transactionTemplate.execute(tx -> {
            orderIds.clear();
            for (Map.Entry<Long, Long> shop : SHOP_BUSINESS.entrySet()) {
                for (int i = 0; i < COUNT; i++) {
                    Order order = OrderProvider.getBlueOrder();
                    order.setBusinessId(null);
                    order.setShopId(shop.getKey());
                    orderIds.add(orderInsertHelper.insertOrder(order));
                }
                shopService.updateMeta(shop.getKey(), new ShopMetaDataBuilder()
                        .withBusinessId(shop.getValue())
                        .build());

            }
            orderHistoryDao.publishPendingEvents(1000, Partition.NULL);
            return null;
        });
        return orderIds;
    }

    private long getOrderCountWithBusiness(Set<Long> ordersIds, Predicate<BasicOrder> predicate) {
        List<BasicOrder> orders = orderReadingDao.getBasicOrders(
                OrderSearchRequest.builder()
                        .withOrderIds(ordersIds.toArray(new Long[0]))
                        .build(), Pager.fromTo(0, Integer.MAX_VALUE),
                ClientInfo.SYSTEM);
        return orders.stream().filter(predicate).count();
    }

    private long getOrderHistoryCountWithBusiness(Set<Long> ordersIds, Predicate<Order> predicate) {
        Collection<OrderHistoryEvent> orders = orderHistoryDao.getOrdersHistoryEvents(
                ordersIds,
                null,
                EnumSet.of(HistoryEventType.NEW_ORDER),
                false,
                false,
                false,
                null,
                ClientInfo.SYSTEM,
                Set.of()
        );
        return orders.stream().map(OrderHistoryEvent::getOrderAfter)
                .filter(predicate).count();
    }


    @Test
    void testMigrate() throws Exception {
        Set<Long> orderIds = generateOrders();
        int batch = 2;
        // обновляем businessId в пачке заказов
        requestMigrate(batch, false);
        assertEquals(batch, getOrderCountWithBusiness(orderIds, order -> order.getBusinessId() != null
                && order.getBusinessId().equals(SHOP_BUSINESS.get(order.getShopId()))
        ));

        // обновляем businessId в истории заказов
        requestMigrate(batch, true);
        assertEquals(batch, getOrderHistoryCountWithBusiness(orderIds, order -> order.getBusinessId() != null
                && order.getBusinessId().equals(SHOP_BUSINESS.get(order.getShopId()))
        ));

        // обновление ещё одной пачки заказов
        requestMigrate(COUNT, false);
        assertEquals(COUNT + batch, getOrderCountWithBusiness(orderIds, order -> order.getBusinessId() != null
                && order.getBusinessId().equals(SHOP_BUSINESS.get(order.getShopId()))
        ));

        // обновление истории заказов
        requestMigrate(COUNT, true);
        assertEquals(COUNT + batch, getOrderHistoryCountWithBusiness(orderIds, order -> order.getBusinessId() != null
                && order.getBusinessId().equals(SHOP_BUSINESS.get(order.getShopId()))
        ));
    }

    @Test
    void testMoveOrders() throws Exception {
        long defaultShopId = 1000L;
        long srcBusinessId = 11000L;
        long dstBusinessId = 21000L;
        long dstBusinessId2 = 31000L;

        Set<Long> orderIds = new HashSet<>();
        transactionTemplate.execute(tx -> {
            for (int i = 0; i < 2; i++) {
                Order order = OrderProvider.getBlueOrder();
                order.setBusinessId(srcBusinessId);
                order.setShopId(defaultShopId);
                orderIds.add(orderInsertHelper.insertOrder(order));
            }
            orderIds.add(orderInsertHelper.insertOrder(OrderProvider.getBlueOrder()));
            shopService.updateMeta(defaultShopId, new ShopMetaDataBuilder()
                    .withBusinessId(dstBusinessId)
                    .build());
            orderHistoryDao.publishPendingEvents(1000, Partition.NULL);
            return orderIds;
        });

        // обновление заказов
        requestMove(2, defaultShopId, null, false);
        assertEquals(2, getOrderCountWithBusiness(orderIds, order -> order.getBusinessId() != null
                && order.getShopId().equals(defaultShopId)
                && order.getBusinessId().equals(dstBusinessId))
        );

        // помагазинно меняем businessId в истории заказов
        requestMove(2, defaultShopId, null, true);
        assertEquals(2, getOrderHistoryCountWithBusiness(orderIds, order -> order.getBusinessId() != null
                && order.getShopId().equals(defaultShopId)
                && order.getBusinessId().equals(dstBusinessId))
        );


        // обновление заказов
        requestMove(2, defaultShopId, dstBusinessId2, false);
        assertEquals(2, getOrderCountWithBusiness(orderIds, order -> order.getBusinessId() != null
                && order.getShopId().equals(defaultShopId)
                && order.getBusinessId().equals(dstBusinessId2))
        );

        // помагазинно меняем businessId в истории заказов
        requestMove(2, defaultShopId, dstBusinessId2, true);
        assertEquals(2, getOrderHistoryCountWithBusiness(orderIds, order -> order.getBusinessId() != null
                && order.getShopId().equals(defaultShopId)
                && order.getBusinessId().equals(dstBusinessId2))
        );


    }

    public String requestMigrate(int count, boolean history) throws Exception {
        MockHttpServletRequestBuilder builder = post("/migrate/orders")
                .param("count", String.valueOf(count))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE)
                .param("history", String.valueOf(history));
        return mockMvc.perform(builder)
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", equalTo(count)))
                .andExpect(jsonPath("$.duration", greaterThan(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    public String requestMove(int count, Long shopId, Long businessId, boolean history) throws Exception {
        MockHttpServletRequestBuilder builder = post("/move/orders")
                .param("count", String.valueOf(count))
                .param("history", String.valueOf(history))
                .param(SHOP_ID, String.valueOf(shopId))
                .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE);
        if (businessId != null) {
            builder = builder.param(BUSINESS_ID, String.valueOf(businessId));
        }
        return mockMvc.perform(builder)
                .andDo(log())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", equalTo(count)))
                // если перенос шёл меньше 1 мс, то будет 0
                .andExpect(jsonPath("$.duration", greaterThanOrEqualTo(0)))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
