package ru.yandex.market.delivery.mdbapp.integration.service;

import java.util.Optional;

import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.delivery.mdbapp.components.queue.returns.lrm.create.CreateLrmReturnEnqueueService;
import ru.yandex.market.delivery.mdbapp.components.service.LogisticsOrderService;
import ru.yandex.market.delivery.mdbapp.components.storage.domain.type.ReturnStatus;
import ru.yandex.market.delivery.mdbapp.components.storage.dto.ReturnRequestUpdateDto;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.BARCODE_DROPSHIP_LRM;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.BARCODE_FF_LRM;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.ORDER_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ID;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ID_STR;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.lomOrder;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.returnRequestDtoWithItemsLrmFlow;

@RunWith(MockitoJUnitRunner.class)
public class ReturnRequestUpdateServiceTest {

    @Rule
    public JUnitSoftAssertions softly = new JUnitSoftAssertions();

    @Mock
    ReturnRequestService returnRequestService;
    @Mock
    LogisticsOrderService logisticsOrderService;
    @Mock
    CreateLrmReturnEnqueueService createLrmReturnEnqueueService;

    ReturnRequestUpdateService service;

    @Before
    public void setup() {
        service = new ReturnRequestUpdateService(
            returnRequestService,
            logisticsOrderService,
            createLrmReturnEnqueueService
        );
    }

    @After
    public void after() {
        verifyNoMoreInteractions(
            returnRequestService,
            createLrmReturnEnqueueService
        );
    }

    @Test
    @DisplayName("Обработка клиентского возврата для FBS-заказа через LRM")
    public void successDropshipOrderLrmFlow() {

        // given:
        when(logisticsOrderService.getByCheckouterOrderId(ORDER_ID))
            .thenReturn(Optional.of(lomOrder(PartnerType.DROPSHIP)));

        // when:
        service.makeReturnRequestAndUpdateState(returnRequestDtoWithItemsLrmFlow(true));

        // then:
        verify(returnRequestService).updateReturnRequest(
            ReturnRequestUpdateDto.builder()
                .status(ReturnStatus.NEW)
                .returnId(RETURN_ID_STR)
                .barcode(BARCODE_DROPSHIP_LRM)
                .build()
        );
        verify(createLrmReturnEnqueueService).enqueue(eq(String.valueOf(RETURN_ID)));
    }

    @Test
    @DisplayName("Обработка клиентского возврата для ФФ-заказа через LRM")
    public void successFfOrder() {

        // given:
        when(logisticsOrderService.getByCheckouterOrderId(ORDER_ID))
            .thenReturn(Optional.of(lomOrder(PartnerType.FULFILLMENT)));

        // when:
        service.makeReturnRequestAndUpdateState(returnRequestDtoWithItemsLrmFlow(false));

        // then:
        verify(returnRequestService).updateReturnRequest(
            ReturnRequestUpdateDto.builder()
                .status(ReturnStatus.NEW)
                .returnId(RETURN_ID_STR)
                .barcode(BARCODE_FF_LRM)
                .build()
        );
        verify(createLrmReturnEnqueueService).enqueue(eq(String.valueOf(RETURN_ID)));
    }
}
