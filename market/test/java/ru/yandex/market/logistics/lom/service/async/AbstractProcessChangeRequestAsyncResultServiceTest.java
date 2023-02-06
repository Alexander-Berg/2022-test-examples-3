package ru.yandex.market.logistics.lom.service.async;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.logistics.lom.entity.BusinessProcessState;
import ru.yandex.market.logistics.lom.entity.BusinessProcessStateEntityId;
import ru.yandex.market.logistics.lom.entity.ChangeOrderSegmentRequest;
import ru.yandex.market.logistics.lom.entity.enums.ChangeOrderSegmentRequestStatus;
import ru.yandex.market.logistics.lom.entity.enums.EntityType;
import ru.yandex.market.logistics.lom.model.async.UpdateOrderErrorDto;
import ru.yandex.market.logistics.lom.service.async.impl.UpdateOrderItemsInstancesSegmentAsyncResultServiceImpl;
import ru.yandex.market.logistics.lom.service.order.ChangeOrderRequestService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Сервис асинхронной обработки запроса на изменение заказа")
class AbstractProcessChangeRequestAsyncResultServiceTest {

    private static final long SEQUENCE_ID = 100L;
    private static final long ENTITY_ID = 200L;
    private static final long SEGMENT_ID = 400L;

    @Mock
    private ChangeOrderRequestService changeOrderRequestService;

    private UpdateOrderItemsInstancesSegmentAsyncResultService service;

    @BeforeEach
    void setup() {
        when(changeOrderRequestService.getChangeOrderSegmentRequest(ENTITY_ID)).thenReturn(
            new ChangeOrderSegmentRequest().setId(SEGMENT_ID)
        );

        service = new UpdateOrderItemsInstancesSegmentAsyncResultServiceImpl(changeOrderRequestService);
    }

    @ParameterizedTest
    @MethodSource("arguments")
    @DisplayName("Генерирует сообщение об ошибке")
    void processError_producesExpectedErrorMessage(String expectedMessage, Integer errorCode, String errorMessage) {
        service.processError(
            new BusinessProcessState().setEntityIds(List.of(
                new BusinessProcessStateEntityId()
                    .setEntityType(EntityType.CHANGE_ORDER_SEGMENT_REQUEST)
                    .setEntityId(ENTITY_ID)
            )),
            new UpdateOrderErrorDto(
                "barcode",
                1L,
                SEQUENCE_ID,
                errorMessage,
                errorCode,
                false
            )
        );

        verify(changeOrderRequestService).processChangeSegmentRequestStatusChanged(
            SEGMENT_ID,
            ChangeOrderSegmentRequestStatus.FAIL,
            expectedMessage
        );
    }

    @Nonnull
    private static Stream<Arguments> arguments() {
        return Stream.of(
            Arguments.of("ERROR [1] error message", 1, "error message"),
            Arguments.of("ERROR error message", null, "error message"),
            Arguments.of("ERROR [1]", 1, null),
            Arguments.of("ERROR", null, null)
        );
    }
}
