package ru.yandex.market.delivery.mdbapp.integration.endpoint;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;

import ru.yandex.market.delivery.mdbapp.AbstractTest;
import ru.yandex.market.logistics.lom.model.dto.AbstractOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.ChangeOrderRequestDto;
import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.enums.OrderStatus;

public class AbstractLogisticsOrderCreateHandlerTest extends AbstractTest {

    private final AbstractLogisticsOrderCreateHandler<AbstractOrderRequestDto> handler = new DummyHandler();

    @DisplayName("Проверка состояния заказа")
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @EnumSource(
        value = OrderStatus.class,
        mode = EnumSource.Mode.EXCLUDE,
        names = {"VALIDATION_ERROR", "PROCESSING_ERROR", "UNKNOWN"}
    )
    public void testSkipStatus(OrderStatus orderStatus) {
        OrderDto order = Mockito.mock(OrderDto.class);
        AbstractOrderRequestDto requestDto = Mockito.mock(AbstractOrderRequestDto.class);

        Mockito.when(order.getStatus()).thenReturn(orderStatus);
        handler.processOrder(order, requestDto);
    }

    private class DummyHandler extends AbstractLogisticsOrderCreateHandler<AbstractOrderRequestDto> {
        protected DummyHandler() {
            super(null, null, null);
        }

        @Override
        OrderDto createOrderWithAutocommit(AbstractOrderRequestDto order) {
            return null;
        }

        @Override
        OrderDto confirmChangeOrderDeliveryOption(
            long lomOrderId,
            ChangeOrderRequestDto changeRequest,
            AbstractOrderRequestDto order
        ) {
            softly.fail("Should not change delivery option");
            return null;
        }
    }
}
