package ru.yandex.market.checkout.checkouter.controller;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.checkout.application.AbstractArchiveWebTestBase;
import ru.yandex.market.checkout.checkouter.client.ClientInfo;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.MoveOrderResponse;
import ru.yandex.market.checkout.checkouter.order.MoveOrderStatus;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderSearchRequest;
import ru.yandex.market.checkout.checkouter.order.archive.ArchiveContext;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;
import ru.yandex.market.queuedcalls.QueuedCallService;

import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.MOVE_ORDER_TO_UID;
import static ru.yandex.market.checkout.checkouter.tasks.queuedcalls.CheckouterQCType.UNARCHIVE_AND_MOVE_ORDER_TO_UID;
import static ru.yandex.market.checkout.test.providers.BuyerProvider.UID;
import static ru.yandex.market.checkout.test.providers.BuyerProvider.getDefaultBuyer;
import static ru.yandex.market.checkout.test.providers.BuyerProvider.getSberIdBuyer;

public class MoveAllUserOrdersTest extends AbstractArchiveWebTestBase {

    @Autowired
    private QueuedCallService queuedCallService;

    @DisplayName("Перенос всех заказов пользователя со СберИД на Яндекс и обратно")
    @Test
    public void moveOrdersFromSberIDToYandex() {
        Buyer sberIdBuyer = getSberIdBuyer();
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(sberIdBuyer);
        Order order1 = orderCreateHelper.createOrder(parameters);
        Order order2 = orderCreateHelper.createOrder(parameters);
        Long orderId1 = order1.getId();
        Long orderId2 = order2.getId();

        // Move from SberID to Yandex UID
        client.moveUserOrders(sberIdBuyer.getUid(), BuyerProvider.UID, ClientRole.SYSTEM, 0L);

        queuedCallService.executeQueuedCallSynchronously(MOVE_ORDER_TO_UID, orderId1);
        queuedCallService.executeQueuedCallSynchronously(MOVE_ORDER_TO_UID, orderId2);

        order1 = client.getOrder(orderId1, ClientRole.SYSTEM, 0L);
        Assertions.assertEquals(UID, (long) order1.getUid());
        order2 = client.getOrder(orderId2, ClientRole.SYSTEM, 0L);
        Assertions.assertEquals(UID, (long) order2.getUid());

        // And back, from Yandex UID to SberID
        client.moveUserOrders(BuyerProvider.UID, sberIdBuyer.getUid(), ClientRole.SYSTEM, 0L);

        queuedCallService.executeQueuedCallSynchronously(MOVE_ORDER_TO_UID, orderId1);
        queuedCallService.executeQueuedCallSynchronously(MOVE_ORDER_TO_UID, orderId2);

        order1 = client.getOrder(orderId1, ClientRole.SYSTEM, 0L);
        Assertions.assertEquals(sberIdBuyer.getUid(), order1.getUid());
        order2 = client.getOrder(orderId2, ClientRole.SYSTEM, 0L);
        Assertions.assertEquals(sberIdBuyer.getUid(), order2.getUid());
    }

    @DisplayName("Перенос заказов пользователя, если заказов нет")
    @Test
    public void moveFromSberIDToYandexWhenUserHasNoOrders() {
        Buyer sberIdBuyer = getSberIdBuyer();

        client.moveUserOrders(sberIdBuyer.getUid(), BuyerProvider.UID, ClientRole.SYSTEM, 0L);
    }

    @DisplayName("Перенос заказов пользователя, если актуальных заказов нет")
    @Test
    public void moveWhenUserHasNoActualOrders() throws SQLException {
        Order order = orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters(getDefaultBuyer(
                12345L)));
        archiveOrders(Set.of(order.getId()));
        for (int i = 0; i < partitionsCount; i++) {
            orderCreateHelper.createOrder(BlueParametersProvider.defaultBlueOrderParameters());
        }
        var initialUid = order.getUid();

        //Making sure that the only order the user has is archived
        Assertions.assertNotNull(getArchivedOrder(order));
        Assertions.assertEquals(1, getOrdersByUid(initialUid).size());

        Assertions.assertEquals(order.getUid(), initialUid);
        moveArchivedOrders();


        client.moveUserOrders(initialUid, UID, ClientRole.SYSTEM, 0L);

        queuedCallService.executeQueuedCallSynchronously(UNARCHIVE_AND_MOVE_ORDER_TO_UID, order.getId());

        Order movedOrder1 = orderService.getOrder(order.getId());
        Assertions.assertEquals(UID, movedOrder1.getUid());
    }

    @DisplayName("Перенос заказов пользователя, если заказов уже привязался к другому пользователю")
    @Test
    public void moveFromSberIDToYandexWhenOrderAlreadyBoundToAnotherUser() {
        checkouterFeatureWriter.writeValue(BooleanFeatureType.THROW_ERROR_ON_WRONG_USER_MUID, true);
        Buyer sberIdBuyer = getSberIdBuyer();
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters(sberIdBuyer);
        Order order1 = orderCreateHelper.createOrder(parameters);
        Order order2 = orderCreateHelper.createOrder(parameters);
        Long orderId1 = order1.getId();
        Long orderId2 = order2.getId();

        // Move from SberID to Yandex UID
        client.moveUserOrders(sberIdBuyer.getUid(), BuyerProvider.UID, ClientRole.SYSTEM, 0L);
        List<MoveOrderResponse> moveOrderResponses = client.moveOrders(
                Collections.singleton(orderId2),
                sberIdBuyer.getUid(), UID + 1,
                ClientRole.SYSTEM, 0L,
                null,
                new Color[]{Color.BLUE}
        );
        Assertions.assertEquals(MoveOrderStatus.SUCCESS, Iterables.getOnlyElement(moveOrderResponses).getStatus());

        queuedCallService.executeQueuedCallSynchronously(MOVE_ORDER_TO_UID, orderId1);
        Assertions.assertThrows(RuntimeException.class,
                () -> queuedCallService.executeQueuedCallSynchronously(MOVE_ORDER_TO_UID, orderId2));

        order1 = client.getOrder(orderId1, ClientRole.SYSTEM, 0L);
        Assertions.assertEquals(UID, (long) order1.getUid());
        order2 = client.getOrder(orderId2, ClientRole.SYSTEM, 0L);
        Assertions.assertEquals(UID + 1, (long) order2.getUid());
    }


    private Order getArchivedOrder(Order order) {
        ArchiveContext.setArchived(Boolean.TRUE);
        try {
            return orderService.getOrder(order.getId());
        } finally {
            ArchiveContext.setArchived(Boolean.FALSE);
        }
    }

    private Collection<Order> getOrdersByUid(Long buyerUid) {
        Collection<Order> result = new ArrayList<>();
        var request = OrderSearchRequest.builder().withUserId(buyerUid).build();
        ArchiveContext.setArchived(Boolean.TRUE);
        try {
            result.addAll(orderService.getOrders(request, ClientInfo.SYSTEM).getItems());
        } finally {
            ArchiveContext.setArchived(Boolean.FALSE);
        }
        result.addAll(orderService.getOrders(request, ClientInfo.SYSTEM).getItems());
        return result;
    }
}
