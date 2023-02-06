package ru.yandex.market.tpl.tms.logbroker.consumer.lrm;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.logistics.lrm.event_model.ReturnEvent;
import ru.yandex.market.logistics.lrm.event_model.ReturnEventType;
import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LrmCustomerOrderItemsChangedProcessorUnitTest {

    public static final String EXISTS_EXTERNAL_ORDER_ID = "EXISTS_EXTERNAL_ORDER_ID";
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderCommandService orderCommandService;
    @InjectMocks
    private LrmCustomerOrderItemsChangedProcessor tplLrmReturnEventProcessor;

    @Test
    void processSuccess() {
        //given
        Order order = mock(Order.class);

        ReturnEvent returnEvent = ReturnEvent.builder()
                .eventType(ReturnEventType.CUSTOMER_ORDER_ITEMS_CHANGED)
                .orderExternalId(EXISTS_EXTERNAL_ORDER_ID)
                .build();

        when(orderRepository.findByExternalOrderId(EXISTS_EXTERNAL_ORDER_ID)).thenReturn(Optional.of(order));

        //when
        tplLrmReturnEventProcessor.process(returnEvent);

        //then
        verify(orderCommandService, times(1)).updateFlowStatus(any(), any());
    }

    @Test
    void processFailure_whenOrderNotExists() {
        //given
        ReturnEvent returnEvent = ReturnEvent.builder()
                .eventType(ReturnEventType.CUSTOMER_ORDER_ITEMS_CHANGED)
                .orderExternalId(EXISTS_EXTERNAL_ORDER_ID)
                .build();

        when(orderRepository.findByExternalOrderId(EXISTS_EXTERNAL_ORDER_ID)).thenReturn(Optional.empty());

        //when
        assertThrows(Exception.class, () -> tplLrmReturnEventProcessor.process(returnEvent));

        //then
        verify(orderCommandService, never()).updateFlowStatus(any(), any());
    }

    @Test
    void processSkip_whenOrderPvz() {
        //given
        Order order = mock(Order.class);

        when(order.isPickup()).thenReturn(true);

        ReturnEvent returnEvent = ReturnEvent.builder()
                .eventType(ReturnEventType.CUSTOMER_ORDER_ITEMS_CHANGED)
                .orderExternalId(EXISTS_EXTERNAL_ORDER_ID)
                .build();

        when(orderRepository.findByExternalOrderId(EXISTS_EXTERNAL_ORDER_ID)).thenReturn(Optional.of(order));

        //when
        tplLrmReturnEventProcessor.process(returnEvent);

        //then
        verify(orderCommandService, never()).updateFlowStatus(any(), any());
    }
}
