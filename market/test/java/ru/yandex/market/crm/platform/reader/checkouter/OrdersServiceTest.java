package ru.yandex.market.crm.platform.reader.checkouter;

import java.util.Set;

import org.junit.Test;
import org.mockito.stubbing.Answer;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderNotFoundException;
import ru.yandex.market.checkout.checkouter.request.OrderRequest;
import ru.yandex.market.checkout.checkouter.request.RequestClientInfo;
import ru.yandex.market.crm.platform.commons.Uid;
import ru.yandex.market.crm.platform.commons.UidType;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OrdersServiceTest {
    private static final Long ORDER_ID = 12345L;
    private static final Long BUYER_UID = 1111L;

    /**
     * Если заказ заархивирован, то сервис пробует получить его из архива чекаутера
     */
    @Test
    public void testIfOrderArchivedServiceTryGetHimFromArchive() {
        CheckouterAPI checkouterAPI = mock(CheckouterAPI.class);
        OrdersService ordersService = new OrdersService(checkouterAPI);

        Answer<Order> answer = invocation -> {
            OrderRequest orderRequest = invocation.getArgument(1, OrderRequest.class);
            if (orderRequest.isArchived()) {
                Order order = new Order();
                order.setBuyer(new Buyer(BUYER_UID));;
                return order;
            } else {
                throw new OrderNotFoundException(ORDER_ID);
            }
        };

        when(checkouterAPI.getOrder(any(RequestClientInfo.class), any(OrderRequest.class))).thenAnswer(answer);

        Set<Uid> uids = ordersService.getOrderUserIds(ORDER_ID);
        assertEquals(1, uids.size());

        Uid actualUid = uids.iterator().next();
        Uid expectedUid = Uid.newBuilder()
                .setType(UidType.PUID)
                .setIntValue(BUYER_UID)
                .build();

        assertEquals(actualUid, expectedUid);
        verify(checkouterAPI, times(2))
                .getOrder(any(RequestClientInfo.class), any(OrderRequest.class));
    }
}
