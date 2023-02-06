package ru.yandex.market.supportwizard.load;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.client.CheckouterAPI;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.event.HistoryEventType;
import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderStatus;
import ru.yandex.market.checkout.checkouter.order.OrderSubstatus;
import ru.yandex.market.supportwizard.storage.EnvironmentEntity;
import ru.yandex.market.supportwizard.storage.EnvironmentRepository;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.sdk.userinfo.service.UidConstants.NO_SIDE_EFFECT_UID;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class LoadTestOrdersListenerTest {

    CheckouterAPI checkouterAPI = mock(CheckouterAPI.class);
    EnvironmentRepository env = mock(EnvironmentRepository.class);
    LoadTestOrdersListener listener = new LoadTestOrdersListener(checkouterAPI, new ObjectMapper(), () -> true);

    @BeforeEach
    void before() {
        reset(checkouterAPI, env);
        when(env.findEnvironmentByName(anyString())).thenReturn(new EnvironmentEntity("", "true"));
    }

    @Test
    void testPositiveStatus() {
        listener.process(makeEvent(NO_SIDE_EFFECT_UID, HistoryEventType.ORDER_STATUS_UPDATED, false,
                OrderStatus.PROCESSING, OrderSubstatus.STARTED));
        verify(checkouterAPI).updateOrderStatus(
                eq(1L),
                eq(ClientRole.SHOP),
                eq(10L),
                eq(10L),
                eq(OrderStatus.PROCESSING),
                eq(OrderSubstatus.READY_TO_SHIP)
        );
    }

    @Test
    void testPositiveSubstatus() {
        listener.process(makeEvent(NO_SIDE_EFFECT_UID, HistoryEventType.ORDER_SUBSTATUS_UPDATED, false,
                OrderStatus.PROCESSING, OrderSubstatus.STARTED));
        verify(checkouterAPI).updateOrderStatus(
                eq(1L),
                eq(ClientRole.SHOP),
                eq(10L),
                eq(10L),
                eq(OrderStatus.PROCESSING),
                eq(OrderSubstatus.READY_TO_SHIP)
        );
    }

    @Test
    void notLoadTestOrderShouldBeIgnored() {
        listener.process(makeEvent(1L, HistoryEventType.ORDER_STATUS_UPDATED, false,
                OrderStatus.PROCESSING, OrderSubstatus.STARTED));
        verifyNoInteractions(checkouterAPI);
    }

    @Test
    void fulfillmentOrderShouldBeIgnored() {
        listener.process(makeEvent(NO_SIDE_EFFECT_UID, HistoryEventType.ORDER_STATUS_UPDATED, true,
                OrderStatus.PROCESSING, OrderSubstatus.STARTED));
        verifyNoInteractions(checkouterAPI);
    }


    @Test
    void testWrongStatus() {
        listener.process(makeEvent(NO_SIDE_EFFECT_UID, HistoryEventType.ORDER_STATUS_UPDATED, false,
                OrderStatus.PROCESSING, OrderSubstatus.READY_TO_SHIP));
        listener.process(makeEvent(NO_SIDE_EFFECT_UID, HistoryEventType.ORDER_STATUS_UPDATED, false,
                OrderStatus.PENDING, OrderSubstatus.PREORDER));
        verifyNoInteractions(checkouterAPI);
    }

    @Test
    void testWrongEventType() {
        listener.process(makeEvent(NO_SIDE_EFFECT_UID, HistoryEventType.ORDER_RETURN_CREATED, false,
                OrderStatus.PROCESSING, OrderSubstatus.STARTED));
        verifyNoInteractions(checkouterAPI);
    }

    private OrderHistoryEvent makeEvent(long uid, HistoryEventType type, boolean isFulfillment,
                                        OrderStatus status, OrderSubstatus substatus) {
        var evt = new OrderHistoryEvent();
        evt.setOrderAfter(new Order());
        evt.getOrderAfter().setBuyer(new Buyer(uid));
        evt.setType(type);
        evt.getOrderAfter().setStatus(status);
        evt.getOrderAfter().setSubstatus(substatus);
        evt.getOrderAfter().setId(1L);
        evt.getOrderAfter().setShopId(10L);
        evt.getOrderAfter().setFulfilment(isFulfillment);
        return evt;
    }

}
