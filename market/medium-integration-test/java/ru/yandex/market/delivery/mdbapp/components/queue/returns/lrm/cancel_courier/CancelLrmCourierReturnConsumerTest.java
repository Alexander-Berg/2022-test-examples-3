package ru.yandex.market.delivery.mdbapp.components.queue.returns.lrm.cancel_courier;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.mdbapp.AbstractMediumContextualTest;
import ru.yandex.market.delivery.mdbapp.components.service.lrm.LogisticReturnService;
import ru.yandex.market.logistics.mdb.lrm.client.api.ReturnsApi;
import ru.yandex.money.common.dbqueue.api.QueueShardId;
import ru.yandex.money.common.dbqueue.api.Task;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.delivery.mdbapp.integration.utils.ReturnRequestTestUtils.RETURN_ID;

class CancelLrmCourierReturnConsumerTest extends AbstractMediumContextualTest {

    @Autowired
    private LogisticReturnService logisticReturnService;
    @Autowired
    private ReturnsApi returnsApi;

    private CancelLrmCourierReturnConsumer consumer;

    @BeforeEach
    void setup() {
        consumer = new CancelLrmCourierReturnConsumer(logisticReturnService);
    }

    @AfterEach
    void verifyMocks() {
        verifyNoMoreInteractions(returnsApi);
    }

    @Test
    @DisplayName("Успех")
    void success() {
        assertResult(TaskExecutionResult.finish());

        verify(returnsApi).cancelCourierReturn(RETURN_ID);
    }

    @Test
    @DisplayName("Ошибка LRM")
    void lrmError() {
        doThrow(new RuntimeException("Some error")).when(returnsApi).cancelCourierReturn(RETURN_ID);

        assertResult(TaskExecutionResult.fail());

        verify(returnsApi).cancelCourierReturn(RETURN_ID);
    }

    private void assertResult(TaskExecutionResult result) {
        softly.assertThat(consumer.execute(new Task<>(
                new QueueShardId("id"),
                new CancelLrmCourierReturnDto(RETURN_ID),
                1L,
                ZonedDateTime.now(),
                null,
                null
            )))
            .isEqualTo(result);
    }

}
