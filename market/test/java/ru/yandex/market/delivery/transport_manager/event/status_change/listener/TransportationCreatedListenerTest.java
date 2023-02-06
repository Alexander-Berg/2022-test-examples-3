package ru.yandex.market.delivery.transport_manager.event.status_change.listener;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.event.status_change.TransportationStatusChangeEvent;
import ru.yandex.market.delivery.transport_manager.service.AxaptaStatusEventService;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;
import ru.yandex.market.delivery.transport_manager.service.TransportationService;
import ru.yandex.market.delivery.transport_manager.service.ticket.service.StInterwarehouseTicketService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class TransportationCreatedListenerTest {

    private static final TransportationStatusChangeEvent OK_EVENT = new TransportationStatusChangeEvent(
        1L,
        null,
        TransportationStatus.NEW
    );

    private static final TransportationStatusChangeEvent EVENT_FOR_XDOC_TRANSPORT =
        new TransportationStatusChangeEvent(
            1L,
            TransportationStatus.NEW,
            TransportationStatus.SCHEDULED
        );

    private static final TransportationStatusChangeEvent IGNORED_EVENT =
        new TransportationStatusChangeEvent(
            1L,
            TransportationStatus.MOVEMENT_SENT,
            TransportationStatus.WAITING_DEPARTURE
        );

    private TransportationCreatedListener listener;
    private AxaptaStatusEventService axaptaStatusEventService;
    private TransportationService transportationService;
    private StInterwarehouseTicketService ticketService;
    private TmPropertyService propertyService;

    @BeforeEach
    void setUp() {
        axaptaStatusEventService = mock(AxaptaStatusEventService.class);
        transportationService = mock(TransportationService.class);
        ticketService = mock(StInterwarehouseTicketService.class);
        propertyService = mock(TmPropertyService.class);

        listener = new TransportationCreatedListener(
            axaptaStatusEventService,
            transportationService,
            ticketService,
            propertyService
        );
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(
            axaptaStatusEventService,
            transportationService,
            ticketService,
            propertyService
        );
    }

    @ParameterizedTest
    @MethodSource("nonXDockTransport")
    void testDoNothing(TransportationType type) {
        Transportation transportation = new Transportation().setTransportationType(type);
        when(transportationService.getById(eq(1L)))
            .thenReturn(transportation);
        when(propertyService.getList(eq(TmPropertyKey.TYPES_TO_LAUNCH_ALL_AT_ONCE)))
            .thenReturn(List.of(TransportationType.XDOC_TRANSPORT));

        listener.onApplicationEvent(OK_EVENT);

        verify(transportationService).getById(eq(1L));
        verify(propertyService).getList(eq(TmPropertyKey.TYPES_TO_LAUNCH_ALL_AT_ONCE));
        verify(axaptaStatusEventService).createNewTransportationEvent(transportation);
    }

    @ParameterizedTest
    @MethodSource("allTransportationTypes")
    void testDoNothingWrongStatus(TransportationType type) {
        when(transportationService.getById(eq(1L)))
            .thenReturn(new Transportation().setTransportationType(type));

        listener.onApplicationEvent(IGNORED_EVENT);
        verify(transportationService).getById(1L);
    }

    @Test
    void testXDockTransport() {
        Transportation transportation = new Transportation().setTransportationType(TransportationType.XDOC_TRANSPORT);
        when(transportationService.getById(eq(1L))).thenReturn(transportation);
        when(propertyService.getList(eq(TmPropertyKey.TYPES_TO_LAUNCH_ALL_AT_ONCE)))
            .thenReturn(List.of(TransportationType.XDOC_TRANSPORT));

        listener.onApplicationEvent(EVENT_FOR_XDOC_TRANSPORT);

        verify(transportationService).getById(eq(1L));
        verify(axaptaStatusEventService).createNewTransportationEvent(transportation);
        verify(propertyService).getList(eq(TmPropertyKey.TYPES_TO_LAUNCH_ALL_AT_ONCE));
    }

    @Test
    void testXDockTransportSkip() {
        Transportation transportation = new Transportation().setTransportationType(TransportationType.XDOC_TRANSPORT);
        when(transportationService.getById(eq(1L))).thenReturn(transportation);
        when(propertyService.getList(eq(TmPropertyKey.TYPES_TO_LAUNCH_ALL_AT_ONCE)))
            .thenReturn(Collections.emptyList());

        listener.onApplicationEvent(EVENT_FOR_XDOC_TRANSPORT);

        verify(transportationService).getById(eq(1L));
        verify(propertyService).getList(eq(TmPropertyKey.TYPES_TO_LAUNCH_ALL_AT_ONCE));
    }

    public static Stream<Arguments> nonXDockTransport() {
        Set<TransportationType> xdocTransport = EnumSet.of(
            TransportationType.XDOC_TRANSPORT, TransportationType.INTERWAREHOUSE
        );
        return Arrays.stream(TransportationType.values())
            .filter(t -> !xdocTransport.contains(t))
            .map(Arguments::of);
    }

    public static Stream<Arguments> allTransportationTypes() {
        return Arrays.stream(TransportationType.values()).map(Arguments::of);
    }
}
