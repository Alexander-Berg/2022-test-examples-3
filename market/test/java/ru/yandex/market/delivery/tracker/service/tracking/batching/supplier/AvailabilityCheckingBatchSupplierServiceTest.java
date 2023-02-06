package ru.yandex.market.delivery.tracker.service.tracking.batching.supplier;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.tracker.dao.repository.DeliveryServiceRepository;
import ru.yandex.market.delivery.tracker.dao.repository.TrackRequestMetaDao;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryService;
import ru.yandex.market.delivery.tracker.domain.entity.TrackRequestMeta;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceType;
import ru.yandex.market.delivery.tracker.domain.enums.EntityType;
import ru.yandex.market.delivery.tracker.domain.enums.RequestType;
import ru.yandex.market.delivery.tracker.domain.enums.State;
import ru.yandex.market.delivery.tracker.domain.enums.SurveyType;
import ru.yandex.market.delivery.tracker.service.tracking.DeliveryTrackService;
import ru.yandex.market.delivery.tracker.service.tracking.batching.BatchingService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.tracker.domain.enums.State.CHECKED;

class AvailabilityCheckingBatchSupplierServiceTest {

    private static final int BATCH_SIZE = 1000;
    private static final int FETCH_SIZE = 10000;

    private final DeliveryTrackService deliveryTrackService = mock(DeliveryTrackService.class);
    private final TrackRequestMetaDao requestMetaDao = mock(TrackRequestMetaDao.class);
    private final DeliveryServiceRepository deliveryServiceRepository = mock(DeliveryServiceRepository.class);
    private final BatchingService batchingService = mock(BatchingService.class);
    private final Clock clock = Clock.fixed(Clock.systemDefaultZone().instant(), ZoneId.systemDefault());

    private final AvailabilityCheckingBatchSupplierService checkerService =
        new AvailabilityCheckingBatchSupplierService(
            batchingService,
            deliveryServiceRepository,
            deliveryTrackService,
            requestMetaDao,
            clock,
            BATCH_SIZE,
            FETCH_SIZE,
            new ForkJoinPool(2),
            RequestType.ORDER_STATUS,

            new TrackingBatchSupplierService(
                batchingService,
                deliveryServiceRepository,
                BATCH_SIZE,
                FETCH_SIZE,
                new ForkJoinPool(2),
                RequestType.ORDER_HISTORY
            )
        );

    DeliveryService service = createService();
    PartnerApi partnerApi = new PartnerApi(service, ApiVersion.DS);
    List<Long> batch = Arrays.asList(1L, 2L);

    @Test
    void shouldSupply_WhenServiceHasActiveStatusRequest() {
        mockGetTrackableServiceApis(partnerApi);
        mockGetTrackRequestMeta(service, getRequestMeta(service));
        mockGetTracksForBatch();

        checkerService.supplyBatches();

        verifyGetTrackableServiceApis();
        verifyGetTracksForBatch();
        verifyCreateBatch(RequestType.ORDER_STATUS);
    }

    @Test
    void shouldSupply_WhenServiceHasNotActiveStatusRequest_AndEnabledPullingByGetHistory() {
        service.setPullByGetHistoryEnabled(true);

        mockGetTrackableServiceApis(partnerApi);
        mockGetTrackRequestMeta(service, null);
        mockGetTracksForBatch();

        checkerService.supplyBatches();

        verifyGetTrackableServiceApis();
        verifyGetTracksForBatch();
        verifyCreateBatch(RequestType.ORDER_HISTORY);
    }

    @Test
    void shouldNotSupply_WhenServiceHasNotActiveStatusRequest_AndDisabledPullingByGetHistory() {
        mockGetTrackableServiceApis(partnerApi);
        mockGetTrackRequestMeta(service, null);

        checkerService.supplyBatches();

        verifyGetTrackableServiceApis();
        verifyZeroInteractions(deliveryTrackService);
        verifyNeverCreateBatch();
    }

    private DeliveryService createService() {
        return new DeliveryService(1, "Name", DeliveryServiceType.DELIVERY)
            .setSourceId(1);
    }

    private TrackRequestMeta getRequestMeta(DeliveryService service) {
        return new TrackRequestMeta()
            .setServiceId(service.getId())
            .setToken("token")
            .setUrl("url")
            .setName("name")
            .setType(RequestType.ORDER_STATUS)
            .setVersion(ApiVersion.FF);
    }

    private void mockGetTrackableServiceApis(@Nonnull PartnerApi partnerApi) {
        when(deliveryServiceRepository.getTrackableServiceApis())
            .thenReturn(Collections.singletonList(partnerApi));
    }

    private void verifyGetTrackableServiceApis() {
        verify(deliveryServiceRepository).getTrackableServiceApis();
    }

    private void mockGetTrackRequestMeta(@Nonnull DeliveryService service, @Nullable TrackRequestMeta requestMeta) {
        when(requestMetaDao.getTrackRequestMeta(service.getId(), RequestType.ORDER_STATUS, ApiVersion.DS))
            .thenReturn(Optional.ofNullable(requestMeta));
    }

    private void mockGetTracksForBatch() {
        LocalDateTime timestamp = LocalDateTime.now(clock);
        when(deliveryTrackService.getTracksForBatch(
            eq(service.getId()),
            eq(CHECKED),
            eq(timestamp),
            anyInt(),
            eq(EntityType.ORDER),
            eq(ApiVersion.DS)
        ))
            .thenReturn(batch);
    }

    private void verifyGetTracksForBatch() {
        verify(deliveryTrackService)
            .getTracksForBatch(
                anyLong(),
                any(State.class),
                any(LocalDateTime.class),
                anyInt(),
                eq(EntityType.ORDER),
                eq(ApiVersion.DS)
            );
    }

    private void verifyCreateBatch(@Nonnull RequestType requestType) {
        verify(batchingService)
            .createBatch(service.getSourceId(), service.getId(), batch, requestType, SurveyType.PULL, ApiVersion.DS);
    }

    private void verifyNeverCreateBatch() {
        verify(batchingService, never())
            .createBatch(anyInt(), anyLong(), anyList(), any(), any(), any());
    }
}
