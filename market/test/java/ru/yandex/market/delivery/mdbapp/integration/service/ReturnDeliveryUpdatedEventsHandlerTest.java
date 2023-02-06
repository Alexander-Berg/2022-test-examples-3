package ru.yandex.market.delivery.mdbapp.integration.service;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.delivery.mdbapp.AbstractTest;
import ru.yandex.market.delivery.mdbapp.components.queue.returns.lrm.create.CreateLrmReturnEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterReturnService;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnRequestState;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ORDER_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ID_STR;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.checkouterCourierReturn;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.checkouterReturnWithDelivery;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.orderHistoryEvent;

@DisplayName("Обработка события обновления данных доставки возврата")
class ReturnDeliveryUpdatedEventsHandlerTest extends AbstractTest {

    @Mock
    CheckouterReturnService checkouterReturnService;
    @Mock
    ReturnRequestService returnRequestService;
    @Mock
    CreateLrmReturnEnqueueService createLrmReturnEnqueueService;

    @InjectMocks
    ReturnDeliveryUpdatedEventsHandler handler;

    @Test
    @DisplayName("Возврат курьером")
    void courierReturn() {
        when(checkouterReturnService.getReturn(ORDER_ID, RETURN_ID))
            .thenReturn(checkouterCourierReturn());
        when(returnRequestService.findByReturnId(RETURN_ID_STR))
            .thenReturn(Optional.of(
                new ReturnRequest()
                    .setExternalOrderId(ORDER_ID)
                    .setReturnId(RETURN_ID_STR)
                    .setState(ReturnRequestState.AWAITING_FOR_DATA)
            ));

        handler.handle(orderHistoryEvent());

        verify(createLrmReturnEnqueueService).enqueue(RETURN_ID_STR);
    }

    @Test
    @DisplayName("Возврат курьером уже отправлен")
    void courierReturnSent() {
        when(checkouterReturnService.getReturn(ORDER_ID, RETURN_ID))
            .thenReturn(checkouterCourierReturn());
        when(returnRequestService.findByReturnId(RETURN_ID_STR))
            .thenReturn(Optional.of(
                new ReturnRequest()
                    .setExternalOrderId(ORDER_ID)
                    .setReturnId(RETURN_ID_STR)
                    .setState(ReturnRequestState.FINAL)
            ));

        handler.handle(orderHistoryEvent());

        verify(createLrmReturnEnqueueService, never()).enqueue(RETURN_ID_STR);
    }

    @Test
    @DisplayName("Возврат в ПВЗ")
    void pickupReturn() {
        Return checkouterReturn = checkouterReturnWithDelivery();
        ReturnRequest returnRequest = new ReturnRequest()
            .setExternalOrderId(ORDER_ID)
            .setReturnId(RETURN_ID_STR)
            .setBuyerName("return-buyer-name")
            .setState(ReturnRequestState.AWAITING_FOR_DATA);

        when(checkouterReturnService.getReturn(ORDER_ID, RETURN_ID))
            .thenReturn(checkouterReturn);
        when(returnRequestService.findByReturnId(RETURN_ID_STR))
            .thenReturn(Optional.of(returnRequest));

        handler.handle(orderHistoryEvent());

        verify(returnRequestService).addReturnRequestToPickupPoint(returnRequest, checkouterReturn.getDelivery());
    }

}
