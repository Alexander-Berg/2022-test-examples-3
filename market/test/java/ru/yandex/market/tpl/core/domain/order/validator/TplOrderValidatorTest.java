package ru.yandex.market.tpl.core.domain.order.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.common.util.exception.TplOrderValidationException;
import ru.yandex.market.tpl.core.domain.order.Order;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TplOrderValidatorTest {

    @Test
    void canUpdateDeliveryDate_failure_whenCanUpdateStatusFalse() {
        //given
        Order mockedOrder = buildMockedOrder(false);
        doReturn(OrderFlowStatus.DELIVERED_TO_RECIPIENT).when(mockedOrder).getOrderFlowStatus();

        //when
        assertThrows(TplOrderValidationException.class,
                () -> TplOrderValidator.validateBeforeOperation(mockedOrder, TplOrderOperation.UPDATE_ORDER_DELIVERY));
    }

    @Test
    void canUpdateOrderDelivery_failure_whenCanUpdateStatusFalse() {
        //given
        Order mockedOrder = buildMockedOrder(false);
        doReturn(OrderFlowStatus.DELIVERED_TO_RECIPIENT).when(mockedOrder).getOrderFlowStatus();

        //when
        assertThrows(TplOrderValidationException.class,
                () -> TplOrderValidator.validateBeforeOperation(mockedOrder, TplOrderOperation.UPDATE_ORDER_DELIVERY_DATE));
    }

    @Test
    void canUpdateRecipient_failure_whenCanUpdateStatusFalse() {
        //given
        Order mockedOrder = buildMockedOrder(false);
        doReturn(OrderFlowStatus.DELIVERED_TO_RECIPIENT).when(mockedOrder).getOrderFlowStatus();

        //when
        assertThrows(TplOrderValidationException.class,
                () -> TplOrderValidator.validateBeforeOperation(mockedOrder, TplOrderOperation.UPDATE_RECIPIENT));
    }

    @Test
    void canUpdateRecipient_success_whenCanUpdateStatusTrue() {
        //given
        Order mockedOrder = buildMockedOrder(true);

        //when
        assertDoesNotThrow(() -> TplOrderValidator.validateBeforeOperation(mockedOrder, TplOrderOperation.UPDATE_RECIPIENT));
    }

    private Order buildMockedOrder(boolean canUpdate) {
        Order mockedOrder = mock(Order.class);
        doReturn(canUpdate).when(mockedOrder).canUpdate();
        return mockedOrder;
    }
}
