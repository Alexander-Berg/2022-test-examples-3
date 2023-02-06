package ru.yandex.market.delivery.transport_manager.event.register.saved.listener;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.domain.entity.TmPropertyKey;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.event.register.saved.RegisterSavedEvent;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.xdoc.ProcessXdocOutboundFactRegisterProducer;
import ru.yandex.market.delivery.transport_manager.service.IncludedTransportationService;
import ru.yandex.market.delivery.transport_manager.service.TmPropertyService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class IncludedSuppliesOutboundFactListenerTest {
    public static final long REGISTER_ID = 1000L;
    private IncludedSuppliesOutboundFactListener listener;
    private IncludedTransportationService includedTransportationService;
    private ProcessXdocOutboundFactRegisterProducer
        xdocOutboundFactRegisterProducer;
    private TmPropertyService propertyService;

    @BeforeEach
    void setUp() {
        includedTransportationService = Mockito.mock(IncludedTransportationService.class);
        xdocOutboundFactRegisterProducer = Mockito.mock(ProcessXdocOutboundFactRegisterProducer.class);
        propertyService = Mockito.mock(TmPropertyService.class);

        listener = new IncludedSuppliesOutboundFactListener(
            includedTransportationService,
            xdocOutboundFactRegisterProducer,
            propertyService
        );
    }

    @Test
    void listenSuccess() {
        when(propertyService.getBoolean(TmPropertyKey.ENABLE_INTERWAREHOUSE_ASSEMBLAGE_ONLY))
            .thenReturn(true);
        when(propertyService.getList(TmPropertyKey.INTERWAREHOUSE_ASSEMBLAGE_ONLY_WH_IDS))
            .thenReturn(List.of(1L));

        Transportation transportation = new Transportation()
            .setTransportationType(TransportationType.INTERWAREHOUSE)
            .setOutboundUnit(new TransportationUnit().setPartnerId(1L));
        listener.listen(new RegisterSavedEvent(
            transportation,
            transportation,
            new Register().setId(REGISTER_ID).setType(RegisterType.FACT),
            TransportationUnitType.OUTBOUND
        ));
        verify(xdocOutboundFactRegisterProducer).enqueue(eq(REGISTER_ID));
    }

    @Test
    void listenSuccessEmptyWhList() {
        when(propertyService.getBoolean(TmPropertyKey.ENABLE_INTERWAREHOUSE_ASSEMBLAGE_ONLY))
            .thenReturn(true);
        when(propertyService.getList(TmPropertyKey.INTERWAREHOUSE_ASSEMBLAGE_ONLY_WH_IDS))
            .thenReturn(Collections.emptyList());

        Transportation transportation = new Transportation()
            .setTransportationType(TransportationType.INTERWAREHOUSE)
            .setOutboundUnit(new TransportationUnit().setPartnerId(1L));
        listener.listen(new RegisterSavedEvent(
            transportation,
            transportation,
            new Register().setId(REGISTER_ID).setType(RegisterType.FACT),
            TransportationUnitType.OUTBOUND
        ));
        verify(xdocOutboundFactRegisterProducer).enqueue(eq(REGISTER_ID));
    }

    @Test
    void listenUnsupportedTrasportationType() {
        when(propertyService.getBoolean(TmPropertyKey.ENABLE_INTERWAREHOUSE_ASSEMBLAGE_ONLY))
            .thenReturn(true);
        when(propertyService.getList(TmPropertyKey.INTERWAREHOUSE_ASSEMBLAGE_ONLY_WH_IDS))
            .thenReturn(List.of(1L));

        Transportation transportation = new Transportation()
            .setTransportationType(TransportationType.XDOC_TRANSPORT)
            .setOutboundUnit(new TransportationUnit().setPartnerId(1L));
        listener.listen(new RegisterSavedEvent(
            transportation,
            transportation,
            new Register().setId(REGISTER_ID).setType(RegisterType.FACT),
            TransportationUnitType.OUTBOUND
        ));
        verifyNoMoreInteractions(xdocOutboundFactRegisterProducer);
    }

    @Test
    void listenUnsupportedRegisterType() {
        when(propertyService.getBoolean(TmPropertyKey.ENABLE_INTERWAREHOUSE_ASSEMBLAGE_ONLY))
            .thenReturn(true);
        when(propertyService.getList(TmPropertyKey.INTERWAREHOUSE_ASSEMBLAGE_ONLY_WH_IDS))
            .thenReturn(List.of(1L));

        Transportation transportation = new Transportation()
            .setTransportationType(TransportationType.INTERWAREHOUSE)
            .setOutboundUnit(new TransportationUnit().setPartnerId(1L));
        listener.listen(new RegisterSavedEvent(
            transportation,
            transportation,
            new Register().setId(REGISTER_ID).setType(RegisterType.PREPARED),
            TransportationUnitType.OUTBOUND
        ));
        verifyNoMoreInteractions(xdocOutboundFactRegisterProducer);
    }

    @Test
    void listenUnsupportedTransportationUnitType() {
        when(propertyService.getBoolean(TmPropertyKey.ENABLE_INTERWAREHOUSE_ASSEMBLAGE_ONLY))
            .thenReturn(true);
        when(propertyService.getList(TmPropertyKey.INTERWAREHOUSE_ASSEMBLAGE_ONLY_WH_IDS))
            .thenReturn(List.of(1L));

        Transportation transportation = new Transportation()
            .setTransportationType(TransportationType.INTERWAREHOUSE)
            .setOutboundUnit(new TransportationUnit().setPartnerId(1L));
        listener.listen(new RegisterSavedEvent(
            transportation,
            transportation,
            new Register().setId(REGISTER_ID).setType(RegisterType.FACT),
            TransportationUnitType.INBOUND
        ));
        verifyNoMoreInteractions(xdocOutboundFactRegisterProducer);
    }
}
