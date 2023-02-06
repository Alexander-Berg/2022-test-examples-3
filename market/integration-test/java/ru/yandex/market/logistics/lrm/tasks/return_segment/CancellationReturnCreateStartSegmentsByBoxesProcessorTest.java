package ru.yandex.market.logistics.lrm.tasks.return_segment;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import one.util.streamex.EntryStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lrm.AbstractIntegrationTest;
import ru.yandex.market.logistics.lrm.config.LocalsConfiguration;
import ru.yandex.market.logistics.lrm.config.locals.UuidGenerator;
import ru.yandex.market.logistics.lrm.queue.payload.ReturnCreateStartSegmentsByBoxesPayload;
import ru.yandex.market.logistics.lrm.queue.processor.CreateStartSegmentsByBoxesProcessor;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.partner.SearchPartnerFilter;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Создание стартовых сегментов по коробкам для невыкупов")
class CancellationReturnCreateStartSegmentsByBoxesProcessorTest extends AbstractIntegrationTest {

    @Autowired
    private CreateStartSegmentsByBoxesProcessor processor;

    @Autowired
    private LMSClient lmsClient;

    @Autowired
    private UuidGenerator uuidGenerator;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient);
        verifyNoMoreInteractions(uuidGenerator);
    }

    @Test
    @DisplayName("Успех, невыкуп в ПВЗ")
    @DatabaseSetup("/database/tasks/return-segment/create-by-boxes/before/cancellation_return_pickup.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-by-boxes/after/success_cancellation_return_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancellationReturnPickup() throws Exception {
        try (
            var ignored0 = mockUuidGenerator();
            var ignored1 = mockGetLogisticPoints(Set.of(10001700279L));
            var ignored2 = mockGetPartners(Map.of(75735L, PartnerType.SORTING_CENTER))
        ) {
            process(List.of(
                ReturnCreateStartSegmentsByBoxesPayload.WaybillSegmentPayload.builder()
                    .partnerId(75735L)
                    .logisticsPointId(10001700279L)
                    .build()
            ));
        }
    }

    @Test
    @DisplayName("Успех, невыкуп")
    @DatabaseSetup("/database/tasks/return-segment/create-by-boxes/before/cancellation_return.xml")
    @ExpectedDatabase(
        value = "/database/tasks/return-segment/create-by-boxes/after/success_cancellation_return.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void cancellationReturn() throws Exception {
        try (
            var ignored0 = mockUuidGenerator();
            var ignored1 = mockGetLogisticPoints(Set.of(10001700279L, 10001686443L));
            var ignored2 = mockGetPartners(
                Map.of(
                    75735L, PartnerType.SORTING_CENTER,
                    55468L, PartnerType.DELIVERY
                )
            )
        ) {
            process(List.of(
                ReturnCreateStartSegmentsByBoxesPayload.WaybillSegmentPayload.builder()
                    .partnerId(75735L)
                    .logisticsPointId(10001700279L)
                    .build(),
                ReturnCreateStartSegmentsByBoxesPayload.WaybillSegmentPayload.builder()
                    .partnerId(55468L)
                    .logisticsPointId(10001686443L)
                    .build()
            ));
        }
    }

    @Nonnull
    private AutoCloseable mockUuidGenerator() {
        when(uuidGenerator.get()).thenReturn(
            UUID.fromString(LocalsConfiguration.TEST_UUID),
            UUID.fromString(LocalsConfiguration.TEST_UUID2)
        );
        return () -> verify(uuidGenerator, times(2)).get();
    }

    @Nonnull
    private AutoCloseable mockGetLogisticPoints(Set<Long> logisticsPoints) {
        LogisticsPointFilter filter = LogisticsPointFilter.newBuilder().ids(logisticsPoints).build();
        when(lmsClient.getLogisticsPoints(filter))
            .thenReturn(
                logisticsPoints.stream()
                    .map(logisticsPointId ->
                        LogisticsPointResponse.newBuilder()
                            .id(logisticsPointId)
                            .externalId("logistics-point-external-id-" + logisticsPointId)
                            .name("logistics-point-name-" + logisticsPointId)
                            .build()
                    )
                    .toList()
            );

        return () -> verify(lmsClient).getLogisticsPoints(filter);
    }

    @Nonnull
    private AutoCloseable mockGetPartners(Map<Long, PartnerType> partners) {
        SearchPartnerFilter filter = SearchPartnerFilter.builder().setIds(partners.keySet()).build();
        when(lmsClient.searchPartners(filter))
            .thenReturn(
                EntryStream.of(partners)
                    .mapKeyValue((partnerId, partnerType) ->
                        PartnerResponse.newBuilder()
                            .id(partnerId)
                            .partnerType(partnerType)
                            .build()
                    )
                    .toList()
            );
        return () -> verify(lmsClient).searchPartners(filter);
    }

    private void process() {
        process(List.of());
    }

    private void process(List<ReturnCreateStartSegmentsByBoxesPayload.WaybillSegmentPayload> sortingCenters) {
        processor.execute(
            ReturnCreateStartSegmentsByBoxesPayload.builder()
                .returnId(1L)
                .sortingCenters(sortingCenters)
                .build()
        );
    }
}
