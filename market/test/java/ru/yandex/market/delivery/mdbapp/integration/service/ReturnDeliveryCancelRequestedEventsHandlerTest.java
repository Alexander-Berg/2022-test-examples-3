package ru.yandex.market.delivery.mdbapp.integration.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.market.delivery.mdbapp.AbstractTest;
import ru.yandex.market.delivery.mdbapp.components.queue.returns.lrm.cancel_courier.CancelLrmCourierReturnEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterReturnService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ORDER_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.checkouterCourierReturn;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.checkouterReturnWithDelivery;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.orderHistoryEvent;

@DisplayName("Обработка события запроса отмены возврата")
public class ReturnDeliveryCancelRequestedEventsHandlerTest extends AbstractTest {

    @Mock
    CheckouterReturnService checkouterReturnService;
    @Mock
    CancelLrmCourierReturnEnqueueService enqueueService;

    @InjectMocks
    ReturnDeliveryCancelRequestedEventsHandler handler;

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(enqueueService);
    }

    @Test
    @DisplayName("Возврат курьером")
    void courierReturn() {
        when(checkouterReturnService.getReturn(ORDER_ID, RETURN_ID))
            .thenReturn(checkouterCourierReturn());

        handler.handle(orderHistoryEvent());

        verify(enqueueService).enqueue(RETURN_ID);
    }

    @Test
    @DisplayName("Возврат в ПВЗ")
    void pickupReturn() {
        when(checkouterReturnService.getReturn(ORDER_ID, RETURN_ID))
            .thenReturn(checkouterReturnWithDelivery());

        handler.handle(orderHistoryEvent());
    }

}
