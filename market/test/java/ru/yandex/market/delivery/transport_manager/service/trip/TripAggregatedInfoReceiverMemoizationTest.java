package ru.yandex.market.delivery.transport_manager.service.trip;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;

import ru.yandex.market.delivery.transport_manager.domain.dto.trip_included_outbounds.TripAggregatedInfo;
import ru.yandex.market.delivery.transport_manager.domain.entity.Movement;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.service.TransportationService;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TripAggregatedInfoReceiverMemoizationTest {

    @Test
    void testMemoization() {
        TransportationService transportationService = Mockito.mock(TransportationService.class);
        TripAggregatedInfoReceiver receiver = new TripAggregatedInfoReceiver(transportationService);
        when(transportationService.search(Mockito.any(), Mockito.any()))
            .thenReturn(new PageImpl<>(List.of(
                new Transportation()
                    .setMovement(new Movement().setPartnerId(1L))
                    .setOutboundUnit(new TransportationUnit()
                        .setLogisticPointId(1L)
                        .setPlannedIntervalStart(LocalDateTime.of(2022, 5, 1, 10, 0))
                        .setPlannedIntervalEnd(LocalDateTime.of(2022, 5, 1, 11, 0))
                    )
                    .setInboundUnit(new TransportationUnit()
                        .setLogisticPointId(2L)
                        .setPlannedIntervalStart(LocalDateTime.of(2022, 5, 1, 12, 0))
                        .setPlannedIntervalEnd(LocalDateTime.of(2022, 5, 1, 13, 0))
                    )
            )));
        Supplier<TripAggregatedInfo> supplier = receiver.getTripAggregatedInfo(2L);

        TripAggregatedInfo firstResult = supplier.get();
        TripAggregatedInfo secondResult = supplier.get();

        assertSame(firstResult, secondResult, "Not the same object returned by two calls!");

        verify(transportationService).search(Mockito.any(), Mockito.any());

        verifyNoMoreInteractions(transportationService);
    }
}
