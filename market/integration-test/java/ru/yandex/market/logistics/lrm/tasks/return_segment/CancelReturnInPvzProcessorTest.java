package ru.yandex.market.logistics.lrm.tasks.return_segment;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.queue.payload.CancelReturnInPvzPayload;
import ru.yandex.market.logistics.lrm.queue.processor.CancelReturnInPvzProcessor;
import ru.yandex.market.pvz.client.logistics.PvzLogisticsClient;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Отмена возврата в ПВЗ")
@ParametersAreNonnullByDefault
class CancelReturnInPvzProcessorTest extends AbstractIntegrationTest {

    private static final String RETURN_ID = "123";

    @Autowired
    private CancelReturnInPvzProcessor cancelPvzSegmentProcessor;

    @Autowired
    private PvzLogisticsClient pvzLogisticsClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(pvzLogisticsClient);
    }

    @Test
    @DisplayName("Успешная отмена")
    void success() {
        cancelPvzSegmentProcessor.execute(payload());

        verify(pvzLogisticsClient).cancelReturnRequest(eq(RETURN_ID));
    }

    @Nonnull
    private CancelReturnInPvzPayload payload() {
        return CancelReturnInPvzPayload.builder()
            .returnExternalId(RETURN_ID)
            .build();
    }
}
