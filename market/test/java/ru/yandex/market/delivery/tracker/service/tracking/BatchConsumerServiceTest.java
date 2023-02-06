package ru.yandex.market.delivery.tracker.service.tracking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.tracker.configuration.properties.DeliveryServiceReqLimitProperties;
import ru.yandex.market.delivery.tracker.dao.repository.SourceDao;
import ru.yandex.market.delivery.tracker.dao.repository.TrackRequestMetaDao;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryService;
import ru.yandex.market.delivery.tracker.domain.entity.SourceMeta;
import ru.yandex.market.delivery.tracker.domain.entity.TrackRequestMeta;
import ru.yandex.market.delivery.tracker.domain.entity.TrackingBatch;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceRole;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.RequestType;
import ru.yandex.market.delivery.tracker.domain.enums.SurveyType;
import ru.yandex.market.delivery.tracker.service.logger.BatchConsumerDelayTskvLogger;
import ru.yandex.market.delivery.tracker.service.tracking.batching.BatchingService;
import ru.yandex.market.delivery.tracker.service.tracking.batching.consumer.AbstractBatchConsumer;
import ru.yandex.market.delivery.tracker.service.tracking.batching.consumer.BatchConsumerProvider;
import ru.yandex.market.delivery.tracker.service.tracking.batching.consumer.BatchConsumerService;
import ru.yandex.market.delivery.tracker.service.tracking.batching.consumer.TrackingBatchConsumer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BatchConsumerServiceTest {

    private static int id = 1;
    private static final int OTHER_NOT_DEFAULT_PRIORITY = 10;

    private final SourceDao sourceDao = Mockito.mock(SourceDao.class);
    private final TrackingBatchConsumer consumer = Mockito.mock(TrackingBatchConsumer.class);
    private final BatchingService batchingService = Mockito.mock(BatchingService.class);
    private final BatchConsumerProvider provider = Mockito.mock(BatchConsumerProvider.class);
    private final DeliveryServiceFetcher deliveryServiceFetcher = Mockito.mock(DeliveryServiceFetcher.class);
    private TrackRequestMetaDao requestMetaDao = Mockito.mock(TrackRequestMetaDao.class);
    private final BatchConsumerDelayTskvLogger batchConsumerDelayTskvLogger =
        Mockito.mock(BatchConsumerDelayTskvLogger.class);

    private final BatchConsumerService service = new BatchConsumerService(
        sourceDao,
        batchingService,
        provider,
        deliveryServiceFetcher,
        requestMetaDao,
        batchConsumerDelayTskvLogger
    );

    @Test
    void testDoNothingWhenSourceIsNotFound() {
        when(sourceDao.fetchFreeSourcesByPriority(anyInt())).thenReturn(List.of());

        service.processFreeSourceBatchByPriority(DeliveryService.DEFAULT_PRIORITY);
        service.processFreeSourceBatchByPriority(OTHER_NOT_DEFAULT_PRIORITY);

        verify(batchingService, never()).fetchEarliestBatches(any(SourceMeta.class));
        verify(provider, never()).provideFor(any(TrackRequestMeta.class));
        verify(consumer, never()).acceptBatches(any(List.class), any(TrackRequestMeta.class));
        verify(sourceDao, never()).sourceUpdated(anyInt());
        verify(batchConsumerDelayTskvLogger, never()).logDelay(
            any(TrackingBatch.class),
            any(DeliveryServiceRole.class),
            anyInt()
        );
    }

    @Test
    void testUpdateSourceOnlyWhenNoBatchesForSourceFound() {
        SourceMeta sourceMeta = createSourceMeta();
        when(sourceDao.fetchFreeSourcesByPriority(DeliveryService.DEFAULT_PRIORITY)).thenReturn(List.of(sourceMeta));
        when(batchingService.fetchEarliestBatches(sourceMeta)).thenReturn(List.of());

        service.processFreeSourceBatchByPriority(DeliveryService.DEFAULT_PRIORITY);

        verify(provider, never()).provideFor(any(TrackRequestMeta.class));
        verify(consumer, never()).acceptBatches(any(List.class), any(TrackRequestMeta.class));
        verify(sourceDao).sourceUpdated(sourceMeta.getSourceId());
        verify(batchConsumerDelayTskvLogger, never()).logDelay(
            any(TrackingBatch.class),
            any(DeliveryServiceRole.class),
            anyInt()
        );
    }

    @Test
    void testProcessBatchFound() {
        SourceMeta sourceMeta = createSourceMeta();
        when(sourceDao.fetchFreeSourcesByPriority(DeliveryService.DEFAULT_PRIORITY)).thenReturn(List.of(sourceMeta));
        TrackingBatch b1 = createTrackingBatch(sourceMeta);
        when(batchingService.fetchEarliestBatches(sourceMeta)).thenReturn(List.of(b1));
        DeliveryService ds1 = createDeliveryService(sourceMeta);
        when(deliveryServiceFetcher.getDeliveryService(b1.getServiceId())).thenReturn(ds1);

        TrackRequestMeta requestMeta = createTrackRequestMeta();

        when(requestMetaDao.getTrackRequestMeta(anyLong(), any(RequestType.class), eq(ApiVersion.FF)))
            .thenReturn(Optional.of(requestMeta));
        when(provider.provideFor(requestMeta)).thenReturn(consumer);

        service.processFreeSourceBatchByPriority(DeliveryService.DEFAULT_PRIORITY);

        verify(sourceDao, never()).sourceUpdated(sourceMeta.getSourceId());
        verify(batchConsumerDelayTskvLogger).logDelay(eq(b1), eq(ds1.getRole()), anyInt());
    }

    @Test
    void testBatchProcessingTakesNoLessThanHalfSecond() {
        AbstractBatchConsumer batchConsumer = new AbstractBatchConsumer(new DeliveryServiceReqLimitProperties()) {
            @Override
            protected void doProcessBatches(List<TrackingBatch> batches, TrackRequestMeta requestMeta) {
                // empty body
            }
        };

        SourceMeta sourceMeta = createSourceMeta();
        TrackingBatch batch = createTrackingBatch(sourceMeta);
        TrackRequestMeta requestMeta = createTrackRequestMeta();

        long start = System.currentTimeMillis();
        batchConsumer.acceptBatches(List.of(batch), requestMeta);
        long duration = System.currentTimeMillis() - start;
        assertThat(duration, greaterThanOrEqualTo(500L));
    }

    private TrackingBatch createTrackingBatch(SourceMeta sourceMeta) {
        return new TrackingBatch(
            getNextId(),
            sourceMeta.getSourceId(),
            sourceMeta.getServiceId(),
            null,
            SurveyType.PULL,
            EntityType.ORDER,
            ApiVersion.FF,
            LocalDateTime.of(2021, 9, 7, 12, 0),
            null
        );
    }

    private DeliveryService createDeliveryService(SourceMeta sourceMeta) {
        return new DeliveryService(sourceMeta.getServiceId(), "Test Service", DeliveryServiceType.UNKNOWN);
    }

    private SourceMeta createSourceMeta() {
        return new SourceMeta(getNextId(), (long) getNextId());
    }

    private TrackRequestMeta createTrackRequestMeta() {
        return new TrackRequestMeta()
            .setServiceId(1)
            .setToken("")
            .setUrl("")
            .setName("")
            .setType(RequestType.ORDER_HISTORY)
            .setVersion(ApiVersion.FF);
    }

    private int getNextId() {
        return id++;
    }
}
