package ru.yandex.market.checkout.checkouter.tasks.queuedcalls.processors.track;

import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import ru.yandex.market.checkout.checkouter.pay.ReturnService;
import ru.yandex.market.checkout.checkouter.pay.validation.ReturnDeliveryStatusValidationException;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus;
import ru.yandex.market.queuedcalls.ExecutionResult;
import ru.yandex.market.queuedcalls.QueuedCallProcessor;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static ru.yandex.market.checkout.checkouter.returns.ReturnDeliveryStatus.SENDER_SENT;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReturnDeliveryStatusQCProcessorTest {

    @Mock
    private ReturnService returnService;
    private ReturnDeliveryStatusQCProcessor returnDeliveryStatusQCProcessor;

    @BeforeAll
    public void setUp() {
        initMocks(this);
        returnDeliveryStatusQCProcessor = new ReturnDeliveryStatusQCProcessor(returnService, 10, 10);
    }

    @Test
    @DisplayName("Обработка QC не валится при невалидном payload")
    public void doNotFailOnInvalidExecutionPayload() {
        assertEquals(ExecutionResult.SUCCESS, assertDoesNotThrow(() -> returnDeliveryStatusQCProcessor.process(
                new QueuedCallProcessor.QueuedCallExecution(1L, null, 0, Instant.now(), 1L))));
        assertEquals(ExecutionResult.SUCCESS, assertDoesNotThrow(() -> returnDeliveryStatusQCProcessor.process(
                new QueuedCallProcessor.QueuedCallExecution(1L, "INVALID", 0, Instant.now(), 1L))));
    }

    @Test
    @DisplayName("Обработка QC не валится, если валидация перехода в статус дает ошибку")
    public void doNotFailOnStatusValidationException() {
        doThrow(new ReturnDeliveryStatusValidationException(new Return(), SENDER_SENT))
                .when(returnService).updateReturnDeliveryStatusByReturnDeliveryId(anyLong(), any());

        assertEquals(ExecutionResult.SUCCESS, assertDoesNotThrow(() -> returnDeliveryStatusQCProcessor.process(
                new QueuedCallProcessor.QueuedCallExecution(1L, SENDER_SENT.name(), 0, Instant.now(), 1L))));
    }

    @Test
    @DisplayName("Успешная обработка QC")
    public void processReturnDeliveryStatusSuccessfully() {
        assertEquals(ExecutionResult.SUCCESS, returnDeliveryStatusQCProcessor.process(
                new QueuedCallProcessor.QueuedCallExecution(1L, SENDER_SENT.name(), 0, Instant.now(), 1L)
        ));
        ArgumentCaptor<ReturnDeliveryStatus> statusArgumentCaptor = ArgumentCaptor.forClass(ReturnDeliveryStatus.class);
        verify(returnService)
                .updateReturnDeliveryStatusByReturnDeliveryId(eq(1L), statusArgumentCaptor.capture());
        assertEquals(SENDER_SENT, statusArgumentCaptor.getValue());
    }
}
