package ru.yandex.market.ff.dbqueue.service;

import java.util.Collections;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import ru.yandex.market.ff.model.dbqueue.PushRightVerdictMeasurementPayload;
import ru.yandex.market.ff.model.dto.dbqueue.ItemIdentifierDTO;
import ru.yandex.market.ff.service.IrisMeasurementApiService;
import ru.yandex.market.ff.service.implementation.IrisMeasurementApiServiceImpl;
import ru.yandex.market.logistics.iris.client.api.MeasurementApiClient;
import ru.yandex.market.logistics.iris.client.model.request.PutPositiveVerdictRequest;
import ru.yandex.market.logistics.iris.core.domain.item.ItemIdentifier;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PushRightVerdictMeasurementProcessingServiceTest {

    private static final long REQUEST_ID = 3L;

    private final ItemIdentifierDTO firstIdentifier = new ItemIdentifierDTO(1L, "sku1");
    private final ItemIdentifierDTO secondIdentifier = new ItemIdentifierDTO(2L, "sku2");

    private PushRightVerdictMeasurementProcessingService processingService;

    private MeasurementApiClient measurementApiClient;

    private SoftAssertions assertions;

    @BeforeEach
    public void init() {
        measurementApiClient = mock(MeasurementApiClient.class);

        IrisMeasurementApiService irisMeasurementApiService = new IrisMeasurementApiServiceImpl(measurementApiClient);
        processingService = new PushRightVerdictMeasurementProcessingService(irisMeasurementApiService);
        assertions = new SoftAssertions();
    }

    @Test
    public void shouldSuccessProcessPayload() {
        PushRightVerdictMeasurementPayload payload = new PushRightVerdictMeasurementPayload(
                REQUEST_ID,
                ImmutableList.of(firstIdentifier, secondIdentifier)
        );

        processingService.processPayload(payload);

        ArgumentCaptor<PutPositiveVerdictRequest> captor =
                ArgumentCaptor.forClass(PutPositiveVerdictRequest.class);

        verify(measurementApiClient, times(1)).putPositiveVerdictOfMeasurement(captor.capture());
        PutPositiveVerdictRequest request = captor.getValue();

        assertions.assertThat(request).isNotNull();
        assertions.assertThat(request.getItems()).isNotNull();
        assertions.assertThat(request.getItems().size()).isEqualTo(2);

        ItemIdentifier firstActualIdentifier = request.getItems().get(0);
        assertions.assertThat(firstActualIdentifier.getPartnerId()).isEqualTo(firstIdentifier.getSupplierId());
        assertions.assertThat(firstActualIdentifier.getPartnerSku()).isEqualTo(firstIdentifier.getShopSku());

        ItemIdentifier secondActualIdentifier = request.getItems().get(1);
        assertions.assertThat(secondActualIdentifier.getPartnerId()).isEqualTo(secondIdentifier.getSupplierId());
        assertions.assertThat(secondActualIdentifier.getPartnerSku()).isEqualTo(secondIdentifier.getShopSku());
    }

    @Test
    public void shouldNotProcessPayloadIfIdentifiersListIsEmpty() {
        PushRightVerdictMeasurementPayload payload = new PushRightVerdictMeasurementPayload(
                REQUEST_ID,
                Collections.emptyList()
        );

        processingService.processPayload(payload);

        ArgumentCaptor<PutPositiveVerdictRequest> captor =
                ArgumentCaptor.forClass(PutPositiveVerdictRequest.class);

        verify(measurementApiClient, times(1)).putPositiveVerdictOfMeasurement(captor.capture());
        PutPositiveVerdictRequest request = captor.getValue();

        assertions.assertThat(request).isNotNull();
        assertions.assertThat(request.getItems()).isNotNull();
        assertions.assertThat(request.getItems().isEmpty()).isTrue();
    }
}
