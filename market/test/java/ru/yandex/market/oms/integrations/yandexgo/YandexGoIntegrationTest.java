package ru.yandex.market.oms.integrations.yandexgo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.oms.AbstractFunctionalTest;
import ru.yandex.market.oms.util.DbTestUtils;
import ru.yandex.mj.generated.client.self.api.YandexGoApiClient;

@ActiveProfiles("functionalTest")
public class YandexGoIntegrationTest extends AbstractFunctionalTest {

    @Autowired
    private YandexGoApiClient api;

    @Autowired
    private DbTestUtils db;

    private final Long userId = 1L;

    private final List<Long> orderIds = List.of(1L, 2L, 3L, 4L);
    private final List<Long> deliveryIds = List.of(1L, 2L, 3L, 4L);
    private final List<Long> addressIds = List.of(1L, 2L, 3L, 4L);

    private final List<Long> pvzOrderIds = List.of(88L, 99L);
    private final List<Long> pvzDeliveryIds = List.of(88L, 99L);
    private final List<Long> pvzAddressIds = List.of(88L, 99L);

    private final List<Long> nonActiveOrderIds = List.of(888L, 999L);
    private final List<Long> nonActiveDeliveryIds = List.of(888L, 999L);
    private final List<Long> nonActiveAddressIds = List.of(888L, 999L);


    @BeforeEach
    public void initDb() {
        // inserting target active orders
        for (int i = 0; i < orderIds.size(); i++) {
            db.insertOrder(orderIds.get(i), userId, deliveryIds.get(i), OrderStatus.PROCESSING);
            db.insertOrderDelivery(orderIds.get(i), deliveryIds.get(i), addressIds.get(i), new Integer[]{});
        }
        // inserting pvz orders that should be ignored (feature contains 3)
        for (int i = 0; i < pvzOrderIds.size(); i++) {
            db.insertOrder(pvzOrderIds.get(i), userId, pvzDeliveryIds.get(i), OrderStatus.PROCESSING);
            db.insertOrderDelivery(pvzOrderIds.get(i), pvzDeliveryIds.get(i), pvzAddressIds.get(i), new Integer[]{3});
        }
        // inserting non active orders
        for (int i = 0; i < nonActiveOrderIds.size(); i++) {
            var status = i % 2 == 0 ? OrderStatus.DELIVERED : OrderStatus.CANCELLED;
            db.insertOrder(nonActiveOrderIds.get(i), userId, nonActiveDeliveryIds.get(i), status);
            db.insertOrderDelivery(nonActiveOrderIds.get(i), nonActiveDeliveryIds.get(i),
                    nonActiveAddressIds.get(i), new Integer[]{});
        }
    }

    @AfterEach
    public void clearDb() {
        // deleting target active orders
        for (int i = 0; i < orderIds.size(); i++) {
            db.deleteOrderDelivery(deliveryIds.get(i), addressIds.get(i));
            db.deleteOrder(orderIds.get(i));
        }
        // deleting pvz orders that should be ignored (feature contains 3)
        for (int i = 0; i < pvzOrderIds.size(); i++) {
            db.deleteOrderDelivery(pvzDeliveryIds.get(i), pvzAddressIds.get(i));
            db.deleteOrder(pvzOrderIds.get(i));
        }
        // deleting non active order
        for (int i = 0; i < nonActiveOrderIds.size(); i++) {
            db.deleteOrderDelivery(nonActiveDeliveryIds.get(i), nonActiveAddressIds.get(i));
            db.deleteOrder(nonActiveOrderIds.get(i));
        }
    }


    @Test
    public void getAllByUidTest() throws ExecutionException, InterruptedException {
        //when
        var orders = api
                .integrationsOrdersByUidUidGet(userId, null)
                .schedule();

        //then
        Assertions.assertEquals(orderIds.size(), orders.get().size());
        for (int i = 0; i < orders.get().size(); i++) {
            Assertions.assertNotNull(orders.get().get(i).getDelivery());
            Assertions.assertNotNull(orders.get().get(i).getDelivery().getOutlet());
        }
    }

    @Test
    public void getByUidAndIdsTest() throws ExecutionException, InterruptedException {
        //given
        int halfOfOrders = orderIds.size() / 2;

        //when
        var orders = api
                .integrationsOrdersByUidUidGet(userId, orderIds.subList(0, halfOfOrders))
                .schedule();

        //then
        Assertions.assertEquals(halfOfOrders, orders.get().size());
    }

    @Test
    @DisplayName("Если запросить список заказов, но не все из запрошенных существуют, должны вернуться только " +
            "существующие")
    public void getByUidAndIdsWithFewIncorrectIdsTest() throws ExecutionException, InterruptedException {
        //given
        int halfOfOrders = orderIds.size() / 2;
        List<Long> incorrectOrderIds = List.of(231324L, 342412L, 2344124L);
        for (Long incorrectId : incorrectOrderIds) {
            Assertions.assertFalse(orderIds.contains(incorrectId));
        }

        //when
        List<Long> mixedList = new ArrayList<>(orderIds.subList(0, halfOfOrders));
        mixedList.addAll(incorrectOrderIds);
        var orders = api
                .integrationsOrdersByUidUidGet(userId, mixedList)
                .schedule();

        //then
        Assertions.assertEquals(halfOfOrders, orders.get().size());
    }

    @Test
    public void getByInvalidUid4xxTest() throws InterruptedException, ExecutionException {
        //when
        long invalidUid = 2L;
        Assertions.assertNotEquals(invalidUid, userId);
        var orders = api
                .integrationsOrdersByUidUidGet(invalidUid, null)
                .schedule();

        //then
        Assertions.assertEquals(0, orders.get().size());
    }
}
