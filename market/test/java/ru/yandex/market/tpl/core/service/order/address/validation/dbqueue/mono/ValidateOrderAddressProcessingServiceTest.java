package ru.yandex.market.tpl.core.service.order.address.validation.dbqueue.mono;

import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.core.domain.order.Order;
import ru.yandex.market.tpl.core.domain.order.OrderCommand;
import ru.yandex.market.tpl.core.domain.order.OrderCommandService;
import ru.yandex.market.tpl.core.domain.order.OrderRepository;
import ru.yandex.market.tpl.core.service.order.validator.OrderAddressValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidateOrderAddressProcessingServiceTest {

    private static final long EXISTED_ORDER_ID = 1L;
    private static final long DS_ID = 10L;

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderAddressValidator orderAddressValidator;
    @Mock
    private OrderCommandService orderCommandService;
    @Mock
    private Clock clock;
    @InjectMocks
    private ValidateOrderAddressProcessingService validateService;

    @Test
    void validate_when_ENABLED() {
        //given
        doReturn(buildMockedOrder(EXISTED_ORDER_ID, DS_ID, null)).when(orderRepository).findByIdOrThrow(EXISTED_ORDER_ID);
        when(orderAddressValidator.isEnabledForDS(DS_ID)).thenReturn(true);
        boolean expectedValidStatus = true;
        doReturn(expectedValidStatus).when(orderAddressValidator).isGeoValid(EXISTED_ORDER_ID);

        ArgumentCaptor<OrderCommand.UpdateAddressValidation> commandCaptor =
                ArgumentCaptor.forClass(OrderCommand.UpdateAddressValidation.class);
        //when
        //
        validateService.processPayload(new ValidateOrderAddressPayload("qwr", EXISTED_ORDER_ID));

        //then
        verify(orderCommandService, times(1)).updateIsAddressValidStatus(commandCaptor.capture());

        assertNotNull(commandCaptor.getValue());
        assertEquals(EXISTED_ORDER_ID, commandCaptor.getValue().getOrderId());
        assertEquals(expectedValidStatus, commandCaptor.getValue().getIsAddressValid());
    }

    @Test
    void skipValidation_when_DISABLE() {
        //given
        Order mockedOrder = mock(Order.class);
        doReturn(DS_ID).when(mockedOrder).getDeliveryServiceId();
        doReturn(mockedOrder).when(orderRepository).findByIdOrThrow(EXISTED_ORDER_ID);
        when(orderAddressValidator.isEnabledForDS(DS_ID)).thenReturn(false);


        //when
        validateService.processPayload(new ValidateOrderAddressPayload("qwr", EXISTED_ORDER_ID));

        //then
        verify(orderCommandService, never()).updateIsAddressValidStatus(any());
    }

    @Test
    void skipValidation_when_alreadyValidated() {
        //given
        doReturn(buildMockedOrder(null, DS_ID, true)).when(orderRepository).findByIdOrThrow(EXISTED_ORDER_ID);
        when(orderAddressValidator.isEnabledForDS(DS_ID)).thenReturn(true);


        //when
        validateService.processPayload(new ValidateOrderAddressPayload("qwr", EXISTED_ORDER_ID));

        //then
        verify(orderCommandService, never()).updateIsAddressValidStatus(any());
    }

    private Order buildMockedOrder(Long id, Long dsId, Boolean isAddressValid) {
        Order mockedOrder = mock(Order.class);
        if(id != null) {
            doReturn(id).when(mockedOrder).getId();
        }
        doReturn(dsId).when(mockedOrder).getDeliveryServiceId();
        doReturn(isAddressValid).when(mockedOrder).getIsAddressValid();

        return mockedOrder;
    }
}
