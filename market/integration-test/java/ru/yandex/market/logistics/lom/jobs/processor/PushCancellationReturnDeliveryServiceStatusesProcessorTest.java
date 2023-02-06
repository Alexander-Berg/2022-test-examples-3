package ru.yandex.market.logistics.lom.jobs.processor;

import java.time.Instant;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory;
import ru.yandex.market.logistics.lrm.client.api.ReturnsApi;
import ru.yandex.market.logistics.lrm.client.model.LomReturnStatusHistory;
import ru.yandex.market.logistics.lrm.client.model.LomReturnStatuses;
import ru.yandex.market.logistics.lrm.client.model.LomSegmentStatus;
import ru.yandex.market.logistics.lrm.client.model.PushStatusesRequest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DisplayName("Отправка чекпоинтов в LRM вместо обработки и сохранения в БД")
class PushCancellationReturnDeliveryServiceStatusesProcessorTest extends AbstractContextualTest {

    @Autowired
    private PushCancellationReturnDeliveryServiceStatusesProcessor pushStatusesToLrmProcessor;

    @Autowired
    private ReturnsApi returnsApi;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(returnsApi);
    }

    @Test
    @DisplayName("Нет активных возвратов, не пушим статусы в LRM")
    @DatabaseSetup("/jobs/processor/push_statuses_to_lrm/setup_no_order_return.xml")
    void noActiveReturns() {
        pushStatusesToLrmProcessor.processPayload(PayloadFactory.createOrderIdPayload(1L));
    }

    @Test
    @DisplayName("Успешная отправка статусов в LRM")
    @DatabaseSetup("/jobs/processor/push_statuses_to_lrm/setup.xml")
    void success() {
        pushStatusesToLrmProcessor.processPayload(PayloadFactory.createOrderIdPayload(1L));

        ArgumentCaptor<PushStatusesRequest> captor = ArgumentCaptor.forClass(PushStatusesRequest.class);

        verify(returnsApi).pushCancellationReturnDeliveryServiceStatuses(captor.capture());

        softly.assertThat(captor.getValue())
            .usingRecursiveComparison()
            .ignoringCollectionOrder()
            .isEqualTo(
                new PushStatusesRequest()
                    .addStatusesItem(
                        new LomReturnStatuses()
                            .partnerId(102L)
                            .returnId(1L)
                            .addStatusHistoryItem(
                                new LomReturnStatusHistory()
                                    .status(LomSegmentStatus.RETURN_PREPARING)
                                    .timestamp(Instant.parse("2019-08-06T13:40:00Z"))
                            )
                            .addStatusHistoryItem(
                                new LomReturnStatusHistory()
                                    .status(LomSegmentStatus.CANCELLED)
                                    .timestamp(Instant.parse("2019-08-07T13:40:00Z"))
                            )
                    )
            );
    }
}
