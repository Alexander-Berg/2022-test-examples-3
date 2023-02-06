package ru.yandex.market.delivery.transport_manager.service.event.ffwf;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.event.unit.status.listener.AcceptedRequestListener;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.inbound.PutInboundRegisterProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.outbound.PutOutboundRegisterProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.xdoc.CreateXDocOutboundPlanProducer;
import ru.yandex.market.delivery.transport_manager.service.checker.PartnerMethodsCheckService;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;
import ru.yandex.market.delivery.transport_manager.util.UnitStatusReceivedEventFactory;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class AcceptedRequestListenerTest {

    private static final long REQUEST_ID = 123L;
    private static final long UNIT_ID = 2L;

    RegisterService registerService;
    PartnerMethodsCheckService partnerMethodsCheckService;
    PutOutboundRegisterProducer putOutboundRegisterProducer;
    PutInboundRegisterProducer putInboundRegisterProducer;
    CreateXDocOutboundPlanProducer xDocOutboundPlanProducer;

    private AcceptedRequestListener acceptedRequestListener;

    @BeforeEach
    public void init() {
        registerService = Mockito.mock(RegisterService.class);
        partnerMethodsCheckService = Mockito.mock(PartnerMethodsCheckService.class);
        putOutboundRegisterProducer = Mockito.mock(PutOutboundRegisterProducer.class);
        putInboundRegisterProducer = Mockito.mock(PutInboundRegisterProducer.class);
        xDocOutboundPlanProducer = Mockito.mock(CreateXDocOutboundPlanProducer.class);

        acceptedRequestListener = new AcceptedRequestListener(
            registerService,
            partnerMethodsCheckService,
            putOutboundRegisterProducer,
            putInboundRegisterProducer,
            xDocOutboundPlanProducer
        );
    }

    @Test
    void testSendRegistryForInterWarehouse() {
        Transportation transportation =
            UnitStatusReceivedEventFactory.transportation(TransportationType.INTERWAREHOUSE);
        TransportationUnit unit = UnitStatusReceivedEventFactory.unit(TransportationUnitType.OUTBOUND, "not null");

        when(partnerMethodsCheckService.supportsPutOutboundRegistryMethod(transportation.getId(), unit.getPartnerId()))
            .thenReturn(true);
        acceptedRequestListener
            .checkAndSendTasks(UnitStatusReceivedEventFactory.event(transportation, unit, true));
        verify(putOutboundRegisterProducer).produce(eq(UNIT_ID));

        when(partnerMethodsCheckService.supportsPutOutboundRegistryMethod(transportation.getId(), unit.getPartnerId()))
            .thenReturn(false);
        acceptedRequestListener
            .checkAndSendTasks(UnitStatusReceivedEventFactory.event(transportation, unit, true));
        verifyNoMoreInteractions(putOutboundRegisterProducer);
    }

    @Test
    void testSendRegistryForAnomalyLinehaul() {
        Transportation transportation =
            UnitStatusReceivedEventFactory.transportation(TransportationType.ANOMALY_LINEHAUL);
        TransportationUnit unit = UnitStatusReceivedEventFactory.unit(TransportationUnitType.INBOUND, null);

        when(partnerMethodsCheckService.supportsPutInboundRegistryMethod(transportation.getId(), unit.getPartnerId()))
            .thenReturn(true);
        acceptedRequestListener.checkAndSendTasks(
            UnitStatusReceivedEventFactory.event(transportation, unit, true)
        );
        verify(putInboundRegisterProducer).produce(eq(UNIT_ID));

        when(partnerMethodsCheckService.supportsPutInboundRegistryMethod(transportation.getId(), unit.getPartnerId()))
            .thenReturn(false);
        acceptedRequestListener.checkAndSendTasks(
            UnitStatusReceivedEventFactory.event(transportation, unit, true)
        );
        verifyNoMoreInteractions(putInboundRegisterProducer);
    }

    @Test
    void testCreateOutboundPlanXDoc() {
        Transportation transportation =
            UnitStatusReceivedEventFactory.transportation(TransportationType.XDOC_TRANSPORT);
        TransportationUnit unit = UnitStatusReceivedEventFactory.unit(TransportationUnitType.OUTBOUND, "not null");

        when(partnerMethodsCheckService.supportsPutOutboundRegistryMethod(transportation.getId(), unit.getPartnerId()))
            .thenReturn(true);
        acceptedRequestListener.checkAndSendTasks(
            UnitStatusReceivedEventFactory.event(transportation, unit, true)
        );
        verify(xDocOutboundPlanProducer).enqueue(eq(transportation));

        when(partnerMethodsCheckService.supportsPutOutboundRegistryMethod(transportation.getId(), unit.getPartnerId()))
            .thenReturn(false);
        acceptedRequestListener.checkAndSendTasks(
            UnitStatusReceivedEventFactory.event(transportation, unit, true)
        );
        verifyNoMoreInteractions(xDocOutboundPlanProducer);
    }

    @Test
    void testMarkNoNeedToSendOutbound() {
        final long registerId = 1000L;

        when(registerService.findRecentFirstPlan(UNIT_ID))
            .thenReturn(List.of(
                new Register().setId(registerId)
            ));

        acceptedRequestListener.checkAndSendTasks(UnitStatusReceivedEventFactory.event(
            UnitStatusReceivedEventFactory.transportation(TransportationType.ORDERS_OPERATION),
            UnitStatusReceivedEventFactory.unit(TransportationUnitType.OUTBOUND, null),
            true
        ));

        verify(registerService).updateStatusAndDate(eq(registerId), eq(RegisterStatus.DO_NOT_NEED_TO_SEND), isNull());
    }

    @Test
    void testMarkNoNeedToSendInbound() {
        final long registerId = 1000L;

        when(registerService.findRecentFirstPlan(UNIT_ID))
            .thenReturn(List.of(
                new Register().setId(registerId)
            ));

        acceptedRequestListener.checkAndSendTasks(UnitStatusReceivedEventFactory.event(
            UnitStatusReceivedEventFactory.transportation(TransportationType.ORDERS_OPERATION),
            UnitStatusReceivedEventFactory.unit(TransportationUnitType.INBOUND, null),
            true
        ));

        verify(registerService).updateStatusAndDate(eq(registerId), eq(RegisterStatus.DO_NOT_NEED_TO_SEND), isNull());
    }
}
