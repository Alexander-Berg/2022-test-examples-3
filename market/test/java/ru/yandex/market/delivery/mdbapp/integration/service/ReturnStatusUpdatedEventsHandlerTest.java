package ru.yandex.market.delivery.mdbapp.integration.service;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;
import ru.yandex.market.delivery.mdbapp.AbstractTest;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterReturnService;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ORDER_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ID_STR;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.checkouterReturnWithDelivery;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.orderHistoryEvent;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnDelivery;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequestWithItems;

@DisplayName("Обработка события обновления возврата")
class ReturnStatusUpdatedEventsHandlerTest extends AbstractTest {

    @Captor
    private ArgumentCaptor<ReturnDelivery> returnDeliveryArgumentCaptor;

    @Mock
    CheckouterReturnService checkouterReturnService;
    @Mock
    ReturnRequestService returnRequestService;

    @InjectMocks
    ReturnStatusUpdatedEventsHandler returnStatusUpdatedEventsHandler;

    @Test
    @DisplayName("Успех")
    void shouldSucceed() {
        // given:
        when(checkouterReturnService.getReturn(anyLong(), anyLong()))
            .thenReturn(checkouterReturnWithDelivery());
        when(returnRequestService.findByReturnId(anyString()))
            .thenReturn(Optional.of(returnRequestWithItems(false)));

        // when:
        returnStatusUpdatedEventsHandler.handle(orderHistoryEvent());

        // then:
        verify(checkouterReturnService).getReturn(ORDER_ID, RETURN_ID);
        verify(returnRequestService).findByReturnId(RETURN_ID_STR);
        verify(returnRequestService).addReturnRequestToPickupPoint(
            any(ReturnRequest.class),
            returnDeliveryArgumentCaptor.capture()
        );
        softly.assertThat(returnDeliveryArgumentCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(returnDelivery());
    }

    @Test
    @DisplayName("Запрос на возврат не найден")
    void shouldNotAddPickupPoint_whenNoReturnRequest() {
        // given:
        when(checkouterReturnService.getReturn(anyLong(), anyLong()))
            .thenReturn(checkouterReturnWithDelivery());
        when(returnRequestService.findByReturnId(anyString()))
            .thenReturn(Optional.empty());

        // when:
        returnStatusUpdatedEventsHandler.handle(orderHistoryEvent());

        // then:
        verify(checkouterReturnService).getReturn(ORDER_ID, RETURN_ID);
        verify(returnRequestService).findByReturnId(RETURN_ID_STR);
        verifyZeroInteractions(returnRequestService);
    }

}
