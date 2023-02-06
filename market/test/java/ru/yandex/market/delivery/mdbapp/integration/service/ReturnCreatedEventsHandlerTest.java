package ru.yandex.market.delivery.mdbapp.integration.service;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.delivery.mdbapp.AbstractTest;
import ru.yandex.market.delivery.mdbapp.components.service.LogisticsOrderService;
import ru.yandex.market.delivery.mdbapp.components.service.PartnerExternalParamsService;
import ru.yandex.market.delivery.mdbapp.components.service.checkouter.client.CheckouterReturnService;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.ReturnRequest;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnRequestState;
import ru.yandex.market.delivery.mdbapp.integration.converter.EnumConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.ReturnRequestItemConverter;
import ru.yandex.market.delivery.mdbapp.integration.converter.ReturnTypeConverter;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ORDER_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ID_STR;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.checkouterCourierReturn;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.checkouterReturn;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.lomOrder;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.lomOrderDropshipWithoutSc;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.orderHistoryEvent;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequestWithItems;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.setJpaIds;

@DisplayName("Обработка события создания возврата")
class ReturnCreatedEventsHandlerTest extends AbstractTest {

    @Captor
    private ArgumentCaptor<ReturnRequest> returnRequestArgumentCaptor;
    @Captor
    private ArgumentCaptor<Order> orderArgumentCaptor;

    @Mock
    private CheckouterReturnService checkouterReturnService;
    @Mock
    private LogisticsOrderService logisticsOrderService;
    @Spy
    private final ReturnRequestItemConverter returnRequestItemConverter = new ReturnRequestItemConverter(
        new ReturnTypeConverter(),
        new EnumConverter()
    );
    @Mock
    private ReturnRequestService returnRequestService;
    @Mock
    private PartnerExternalParamsService partnerExternalParamsService;

    @InjectMocks
    ReturnCreatedEventsHandler returnCreatedEventsHandler;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
            checkouterReturnService,
            returnRequestService,
            logisticsOrderService,
            partnerExternalParamsService
        );
    }

    @Test
    @DisplayName("Создание клиентского возврата для FBS-заказа")
    void testDropship() {
        // given:
        when(checkouterReturnService.getReturn(anyLong(), anyLong()))
            .thenReturn(checkouterReturn());
        when(returnRequestService.findByReturnId(String.valueOf(RETURN_ID)))
            .thenReturn(Optional.empty());
        when(returnRequestService.save(any(ReturnRequest.class)))
            .then(setJpaIds());
        when(logisticsOrderService.getByOrder(any()))
            .thenReturn(Optional.of(lomOrder(PartnerType.DROPSHIP)));

        // when:
        returnCreatedEventsHandler.handle(orderHistoryEvent());

        // then:
        verify(checkouterReturnService).getReturn(ORDER_ID, RETURN_ID);
        verify(logisticsOrderService).getByOrder(orderArgumentCaptor.capture());
        softly.assertThat(orderArgumentCaptor.getValue().getId()).isEqualTo(167802870L);

        verify(returnRequestService).findByReturnId(String.valueOf(RETURN_ID));
        verify(returnRequestService).save(returnRequestArgumentCaptor.capture());
        softly.assertThat(returnRequestArgumentCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(returnRequestWithItems(true).setBarcode(null));

        verify(returnRequestService).addReturnRequestToPickupPoint(
            any(ReturnRequest.class),
            isNull()
        );
    }

    @Test
    @DisplayName("Создание клиентского возврата для FF-заказа")
    void testFf() {
        // given:
        when(checkouterReturnService.getReturn(anyLong(), anyLong()))
            .thenReturn(checkouterReturn());
        when(returnRequestService.findByReturnId(String.valueOf(RETURN_ID)))
            .thenReturn(Optional.empty());
        when(returnRequestService.save(any(ReturnRequest.class)))
            .then(setJpaIds());
        when(logisticsOrderService.getByOrder(any()))
            .thenReturn(Optional.of(lomOrder(PartnerType.FULFILLMENT)));

        // when:
        returnCreatedEventsHandler.handle(orderHistoryEvent());

        // then:
        verify(checkouterReturnService).getReturn(ORDER_ID, RETURN_ID);
        verify(logisticsOrderService).getByOrder(orderArgumentCaptor.capture());
        softly.assertThat(orderArgumentCaptor.getValue().getId()).isEqualTo(167802870L);

        verify(returnRequestService).findByReturnId(String.valueOf(RETURN_ID));
        verify(returnRequestService).save(returnRequestArgumentCaptor.capture());
        softly.assertThat(returnRequestArgumentCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(returnRequestWithItems(false).setBarcode(null));

        verify(returnRequestService).addReturnRequestToPickupPoint(
            any(ReturnRequest.class),
            isNull()
        );
    }

    @Test
    @DisplayName("Создание клиентского возврата для заказа, которого нет в LOM")
    void testNoLomOrder() {
        // given:
        when(checkouterReturnService.getReturn(anyLong(), anyLong()))
            .thenReturn(checkouterReturn());
        when(returnRequestService.findByReturnId(String.valueOf(RETURN_ID)))
            .thenReturn(Optional.empty());
        when(logisticsOrderService.getByOrder(any()))
            .thenReturn(Optional.empty());

        // when:
        returnCreatedEventsHandler.handle(orderHistoryEvent());

        // then:
        verify(checkouterReturnService).getReturn(ORDER_ID, RETURN_ID);
        verify(logisticsOrderService).getByOrder(orderArgumentCaptor.capture());
        softly.assertThat(orderArgumentCaptor.getValue().getId()).isEqualTo(167802870L);

        verify(returnRequestService).findByReturnId(String.valueOf(RETURN_ID));
        verify(returnRequestService, never()).save(any(ReturnRequest.class));
        verify(returnRequestService, never()).addReturnRequestToPickupPoint(any(ReturnRequest.class), isNull());
    }

    @Test
    @DisplayName("Создание клиентского возврата для дропшип заказа без СЦ. Пропускаем обработку.")
    void testDropshipWithoutSc() {
        // given:
        when(checkouterReturnService.getReturn(anyLong(), anyLong()))
            .thenReturn(checkouterReturn());
        when(returnRequestService.findByReturnId(String.valueOf(RETURN_ID)))
            .thenReturn(Optional.empty());
        when(logisticsOrderService.getByOrder(any()))
            .thenReturn(Optional.of(lomOrderDropshipWithoutSc()));

        // when:
        returnCreatedEventsHandler.handle(orderHistoryEvent());

        // then:
        verify(checkouterReturnService).getReturn(ORDER_ID, RETURN_ID);
        verify(logisticsOrderService).getByOrder(orderArgumentCaptor.capture());
        softly.assertThat(orderArgumentCaptor.getValue().getId()).isEqualTo(167802870L);

        verify(returnRequestService).findByReturnId(String.valueOf(RETURN_ID));
        verify(returnRequestService, never()).save(any(ReturnRequest.class));
        verify(returnRequestService, never()).addReturnRequestToPickupPoint(any(ReturnRequest.class), isNull());
    }

    @Test
    @DisplayName("Создание курьерского возврата")
    void testCourierReturn() {
        when(checkouterReturnService.getReturn(ORDER_ID, RETURN_ID))
            .thenReturn(checkouterCourierReturn());
        when(returnRequestService.findByReturnId(String.valueOf(RETURN_ID)))
            .thenReturn(Optional.empty());

        returnCreatedEventsHandler.handle(orderHistoryEvent());

        verify(checkouterReturnService).getReturn(ORDER_ID, RETURN_ID);
        verify(returnRequestService).findByReturnId(String.valueOf(RETURN_ID));
        verify(returnRequestService).save(returnRequestArgumentCaptor.capture());
        softly.assertThat(returnRequestArgumentCaptor.getValue())
            .usingRecursiveComparison()
            .isEqualTo(
                new ReturnRequest()
                    .setExternalOrderId(ORDER_ID)
                    .setReturnId(RETURN_ID_STR)
                    .setState(ReturnRequestState.AWAITING_FOR_DATA)
            );
    }

    @Test
    @DisplayName("При наличии заявки на возврат в базе событие игнорируется")
    void duplicatedEventIgnored() {
        // given:
        when(returnRequestService.findByReturnId(String.valueOf(RETURN_ID)))
            .thenReturn(Optional.of(new ReturnRequest()));

        // when:
        returnCreatedEventsHandler.handle(orderHistoryEvent());

        // then:
        verify(checkouterReturnService, never()).getReturn(anyLong(), anyLong());
        verify(logisticsOrderService, never()).getByOrder(any(Order.class));
        verify(returnRequestService).findByReturnId(String.valueOf(RETURN_ID));
        verify(returnRequestService, never()).save(any(ReturnRequest.class));
        verify(returnRequestService, never()).addReturnRequestToPickupPoint(any(ReturnRequest.class), isNull());
    }
}
