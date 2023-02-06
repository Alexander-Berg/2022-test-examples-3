package ru.yandex.market.delivery.tracker.service.tracking.batching.supplier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ForkJoinPool;

import org.assertj.core.api.JUnitJupiterSoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import ru.yandex.market.delivery.tracker.dao.repository.DeliveryServiceRepository;
import ru.yandex.market.delivery.tracker.domain.dto.PartnerCreatedBatchesDescription;
import ru.yandex.market.delivery.tracker.domain.entity.DeliveryService;
import ru.yandex.market.delivery.tracker.domain.enums.ApiVersion;
import ru.yandex.market.delivery.tracker.domain.enums.DeliveryServiceType;
import ru.yandex.market.delivery.tracker.domain.enums.SurveyType;
import ru.yandex.market.delivery.tracker.service.tracking.batching.BatchingService;
import ru.yandex.market.delivery.tracker.service.tracking.batching.supplier.TrackingBatchSupplierService.ServiceApiVersion;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.delivery.tracker.domain.enums.RequestType.ORDER_HISTORY;

class TrackingBatchSupplierServiceTest {

    @RegisterExtension
    final JUnitJupiterSoftAssertions softly = new JUnitJupiterSoftAssertions();

    private static final int BATCH_SIZE = 10;
    private static final int FETCH_SIZE = 100_000;
    private static final long DS_ID = 1L;
    private static final long TRACK_ID_1 = 11L;
    private static final long TRACK_ID_2 = 12L;
    private static final int SOURCE_ID = 100;

    private final DeliveryServiceRepository deliveryServiceRepository = mock(DeliveryServiceRepository.class);
    private final BatchingService batchingService = mock(BatchingService.class);

    private final TrackingBatchSupplierService trackerService = new TrackingBatchSupplierService(
        batchingService,
        deliveryServiceRepository,
        BATCH_SIZE,
        FETCH_SIZE,
        new ForkJoinPool(2),
        ORDER_HISTORY
    );

    @Test
    void testSupplyTrackerBatches() {
        DeliveryService service = createService();
        when(deliveryServiceRepository.getServiceSettings(DS_ID))
            .thenReturn(service);

        List<Long> trackIds = List.of(TRACK_ID_1, TRACK_ID_2);
        Map<ServiceApiVersion, List<Long>> tracks = Map.of(
            ServiceApiVersion.of(DS_ID, ApiVersion.DS), trackIds
        );
        CreatedBatchesDescription actual = trackerService.supplyBatches(tracks);

        softly.assertThat(actual.getBatchesCreated()).isEqualTo(1);
        softly.assertThat(actual.getPartnerCreatedBatchesDescriptions()).hasSize(1);
        softly.assertThat(actual.getPartnerCreatedBatchesDescriptions())
            .containsOnly(new PartnerCreatedBatchesDescription(BATCH_SIZE, 1, trackIds.size(), DS_ID));
        verify(deliveryServiceRepository).getServiceSettings(DS_ID);
        verify(batchingService)
            .createBatch(SOURCE_ID, DS_ID, trackIds, ORDER_HISTORY, SurveyType.PULL, ApiVersion.DS);
    }

    private DeliveryService createService() {
        DeliveryService service = new DeliveryService(DS_ID, "Name", DeliveryServiceType.DELIVERY);
        service.setSourceId(SOURCE_ID);
        return service;
    }
}
