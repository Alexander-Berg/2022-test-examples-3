package ru.yandex.market.delivery.transport_manager.service.event.ffwf;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.EntityType;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheme;
import ru.yandex.market.delivery.transport_manager.event.unit.status.listener.AcceptedRequestListener;
import ru.yandex.market.delivery.transport_manager.queue.task.request.external_id.RequestExternalIdQueueProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.request.status.RequestStatusQueueDto;
import ru.yandex.market.delivery.transport_manager.queue.task.request.status.RequestStatusQueueProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.ticket.StartrekErrorType;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.FetchRegisterProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.external_id.EnrichRegisterExternalIdProducer;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.service.PartnerInfoService;
import ru.yandex.market.delivery.transport_manager.service.ticket.service.StEntityErrorTicketService;
import ru.yandex.market.ff.client.dto.RequestStatusChangeDto;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.logistics.management.entity.type.PartnerType;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class RequestStatusHandlerServiceTest extends AbstractContextualTest {

    private static final int SUPPLY_TYPE_ID = 0;
    private static final int WITHDRAW_TYPE_ID = 1;
    private static final int MOVEMENT_SUPPLY_TYPE_ID = 16;
    private static final int MOVEMENT_WITHDRAW_TYPE_ID = 17;

    @Autowired
    private RequestStatusHandlerService requestStatusHandlerService;
    @Autowired
    private FetchRegisterProducer fetchRegisterProducer;
    @Autowired
    private RequestExternalIdQueueProducer requestExternalIdQueueProducer;
    @Autowired
    private EnrichRegisterExternalIdProducer enrichRegisterExternalIdProducer;
    @Autowired
    private RequestStatusQueueProducer requestStatusQueueProducer;
    @Autowired
    private StEntityErrorTicketService stEntityErrorTicketService;
    @Autowired
    private AcceptedRequestListener acceptedRequestListener;
    @Autowired
    private TransportationMapper transportationMapper;
    @Autowired
    private PartnerInfoService partnerInfoService;

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2020-03-02T00:00:00Z"), ZoneOffset.UTC);
        Mockito.doReturn(PartnerType.FULFILLMENT).when(partnerInfoService)
            .getPartnerType(4L, 1L);
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation_and_fact_register.xml")
    @ExpectedDatabase(value = "/repository/service/event/after/transportation_unit_actual_date_time_with_register.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void processProcessedMovementWithdrawWithLoadedDetailsAndFetchedRegister() {
        RequestStatusQueueDto statusAccepted = new RequestStatusQueueDto(1L, true, false,
            MOVEMENT_WITHDRAW_TYPE_ID,
            now(clock),
            RequestStatus.PROCESSED);
        requestStatusHandlerService.processRequestStatusEvent(statusAccepted);
        verify(fetchRegisterProducer, never()).produce(any(), any());
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/interwarehouse.xml")
    void processProcessedMovementWithdrawWithLoadedDetailsAndNotFetchedRegister() {
        RequestStatusQueueDto statusAccepted = new RequestStatusQueueDto(1L, true, false,
            MOVEMENT_WITHDRAW_TYPE_ID,
            now(clock),
            RequestStatus.PROCESSED);
        requestStatusHandlerService.processRequestStatusEvent(statusAccepted);
        verify(fetchRegisterProducer).produce(
            2L,
            false
        );
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/interwarehouse.xml")
    @ExpectedDatabase(value = "/repository/service/event/after/transportation_unit_ready_to_withdraw.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void processReadyForWithdrawMovementWithdrawWithLoadedDetails() {
        RequestStatusQueueDto statusFinished = new RequestStatusQueueDto(1L, true, false,
            MOVEMENT_WITHDRAW_TYPE_ID,
            now(clock),
            RequestStatus.READY_TO_WITHDRAW);
        requestStatusHandlerService.processRequestStatusEvent(statusFinished);
        verify(fetchRegisterProducer).produce(
            2L,
            false
        );
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/interwarehouse.xml")
    @ExpectedDatabase(value = "/repository/service/event/after/transportation_unit_ready_to_withdraw.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void processReadyForWithdrawMovementWithdrawWithoutLoadedDetails() {
        RequestStatusQueueDto statusFinished = new RequestStatusQueueDto(1L, false, false,
            MOVEMENT_WITHDRAW_TYPE_ID,
            now(clock),
            RequestStatus.READY_TO_WITHDRAW);
        requestStatusHandlerService.processRequestStatusEvent(statusFinished);
        verify(fetchRegisterProducer, never()).produce(any(), any());
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation.xml")
    @ExpectedDatabase(value = "/repository/service/event/after/transportation_unit_accepted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void processRequestStatusEventTest_accepted() {
        RequestStatusQueueDto statusAccepted = new RequestStatusQueueDto(15L, false, false,
            MOVEMENT_SUPPLY_TYPE_ID,
            now(),
            RequestStatus.ACCEPTED_BY_SERVICE);
        requestStatusHandlerService.processRequestStatusEvent(statusAccepted);
        verify(acceptedRequestListener).checkAndSendTasks(any());
        verify(requestExternalIdQueueProducer).produce(15L);
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation_interwarehouse.xml")
    @ExpectedDatabase(value = "/repository/service/event/after/transportation_unit_inprogress.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void processRequestStatusEventTest_inprogress() {
        RequestStatusQueueDto statusAccepted = new RequestStatusQueueDto(15L, false, false,
            MOVEMENT_SUPPLY_TYPE_ID,
            now(clock),
            RequestStatus.IN_PROGRESS
        );
        requestStatusHandlerService.processRequestStatusEvent(statusAccepted);
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation.xml")
    @ExpectedDatabase(value = "/repository/service/event/after/transportation_unit_actual_date_time.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void processRequestStatusEventTest_processed() {
        RequestStatusQueueDto statusAccepted = new RequestStatusQueueDto(15L, false, false,
            MOVEMENT_SUPPLY_TYPE_ID,
            now(clock),
            RequestStatus.PROCESSED);
        requestStatusHandlerService.processRequestStatusEvent(statusAccepted);
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation.xml")
    @ExpectedDatabase(value = "/repository/service/event/after/transportation_unit_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void processRequestStatusEventTest_error() {
        RequestStatusQueueDto statusInvalid = new RequestStatusQueueDto(15L, false, false,
            MOVEMENT_SUPPLY_TYPE_ID,
            now(),
            RequestStatus.INVALID);
        requestStatusHandlerService.processRequestStatusEvent(statusInvalid);
        verifyNoInteractions(fetchRegisterProducer);
    }

    @Test
    void processRequestStatusEventTest_noTransportationUnit() {
        RequestStatusQueueDto statusFinished = new RequestStatusQueueDto(15L, false, false,
            SUPPLY_TYPE_ID,
            now(),
            RequestStatus.FINISHED);
        assertThrows(RuntimeException.class,
            () -> requestStatusHandlerService.processRequestStatusEvent(statusFinished));
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation.xml")
    void processRawBatchRequestStatuses_success() {
        RequestStatusChangeDto requestStatusChangeDto =
            new RequestStatusChangeDto(
                15L,
                SUPPLY_TYPE_ID,
                false,
                false,
                now(),
                now(),
                RequestStatus.ACCEPTED_BY_SERVICE,
                RequestStatus.FINISHED
            );
        requestStatusHandlerService.processRawBatchRequestStatuses(List.of(requestStatusChangeDto));
        verify(requestStatusQueueProducer).produce(any());
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation.xml")
    void processRawBatchRequestStatuses_wrongRequestId() {
        RequestStatusChangeDto requestStatusChangeDto =
            new RequestStatusChangeDto(
                1111L,
                SUPPLY_TYPE_ID,
                false,
                false,
                now(),
                now(),
                RequestStatus.ACCEPTED_BY_SERVICE,
                RequestStatus.FINISHED
            );
        requestStatusHandlerService.processRawBatchRequestStatuses(List.of(requestStatusChangeDto));
        verify(requestStatusQueueProducer, never()).produce(any());
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation.xml")
    void processRawBatchRequestStatuses_emptyResponse_noAction() {
        RequestStatusChangeDto requestStatusChangeDto =
            new RequestStatusChangeDto(
                0,
                SUPPLY_TYPE_ID,
                false,
                false,
                now(),
                now(),
                RequestStatus.ACCEPTED_BY_SERVICE,
                RequestStatus.FINISHED
            );
        requestStatusHandlerService.processRawBatchRequestStatuses(List.of(requestStatusChangeDto));
        verify(requestStatusQueueProducer, never()).produce(any());
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation.xml")
    @ExpectedDatabase(
        value = "/repository/service/event/after/new_inbound_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void newSchemeInboundSuccessCallback() {
        transportationMapper.setScheme(1L, TransportationScheme.NEW);
        RequestStatusQueueDto status = new RequestStatusQueueDto(15L, false, false,
            MOVEMENT_SUPPLY_TYPE_ID, now(), RequestStatus.ACCEPTED_BY_SERVICE);
        requestStatusHandlerService.processRequestStatusEvent(status);
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/service/event/before/transportation.xml",
        "/repository/service/event/before/methods.xml"
    })
    @ExpectedDatabase(
        value = "/repository/service/event/after/new_inbound_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void newSchemeInboundFailureCallback() {
        transportationMapper.setScheme(1L, TransportationScheme.NEW);
        RequestStatusQueueDto status = new RequestStatusQueueDto(15L, false, false,
            MOVEMENT_SUPPLY_TYPE_ID, now(), RequestStatus.REJECTED_BY_SERVICE);
        requestStatusHandlerService.processRequestStatusEvent(status);
        verify(stEntityErrorTicketService).createErrorTicket(
            eq(EntityType.TRANSPORTATION),
            eq(1L),
            argThat(dto -> dto.getErrorType() == StartrekErrorType.UNIT_ERROR)
        );
    }

    @Test
    @DatabaseSetup(value = {
        "/repository/service/event/before/transportation.xml",
        "/repository/service/event/before/methods.xml"
    })
    @ExpectedDatabase(
        value = "/repository/service/event/after/new_outbound_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void newSchemeOutboundSuccessCallback() {
        transportationMapper.setScheme(1L, TransportationScheme.NEW);
        RequestStatusQueueDto status = new RequestStatusQueueDto(1L, false, false,
            MOVEMENT_SUPPLY_TYPE_ID, now(), RequestStatus.ACCEPTED_BY_SERVICE);
        requestStatusHandlerService.processRequestStatusEvent(status);
        verify(acceptedRequestListener).checkAndSendTasks(any());
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation.xml")
    @ExpectedDatabase(
        value = "/repository/service/event/after/new_outbound_fail.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void newSchemeOutboundFailureCallback() {
        transportationMapper.setScheme(1L, TransportationScheme.NEW);
        RequestStatusQueueDto status = new RequestStatusQueueDto(1L, false, false,
            MOVEMENT_SUPPLY_TYPE_ID, now(), RequestStatus.REJECTED_BY_SERVICE);
        requestStatusHandlerService.processRequestStatusEvent(status);
        verify(stEntityErrorTicketService).createErrorTicket(
            eq(EntityType.TRANSPORTATION),
            eq(1L),
            argThat(dto -> dto.getErrorType() == StartrekErrorType.UNIT_ERROR)
        );
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation_and_register.xml")
    @ExpectedDatabase(value = "/repository/service/event/after/transportation_unit_register_created.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void newRequestStatusRegisterCreated() {
        RequestStatusQueueDto planRegisterCreated = new RequestStatusQueueDto(15L, false, false,
            MOVEMENT_SUPPLY_TYPE_ID, now(), RequestStatus.PLAN_REGISTRY_CREATED);
        requestStatusHandlerService.processRequestStatusEvent(planRegisterCreated);
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation_and_register.xml")
    @ExpectedDatabase(value = "/repository/service/event/after/transportation_unit_register_sent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void newRequestStatusRegisterSent() {
        RequestStatusQueueDto planRegisterSent = new RequestStatusQueueDto(15L, false, false,
            MOVEMENT_SUPPLY_TYPE_ID, now(), RequestStatus.PLAN_REGISTRY_SENT);
        requestStatusHandlerService.processRequestStatusEvent(planRegisterSent);
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation_and_register.xml")
    @ExpectedDatabase(value = "/repository/service/event/after/transportation_unit_register_accepted.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void newRequestStatusRegisterAccepted() {
        RequestStatusQueueDto planRegisterAccepted = new RequestStatusQueueDto(15L, false, false,
            MOVEMENT_SUPPLY_TYPE_ID, now(), RequestStatus.PLAN_REGISTRY_ACCEPTED);
        requestStatusHandlerService.processRequestStatusEvent(planRegisterAccepted);

        verify(enrichRegisterExternalIdProducer).produce(15L);
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation_interwarehouse.xml")
    @ExpectedDatabase(
        value = "/repository/service/event/after/transportation_unit_register_accepted_interwarehouse.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void newRequestStatusRegisterAcceptedInterwarehouse() {
        RequestStatusQueueDto planRegisterAccepted = new RequestStatusQueueDto(15L, false, false,
            MOVEMENT_SUPPLY_TYPE_ID, now(), RequestStatus.PLAN_REGISTRY_ACCEPTED);
        requestStatusHandlerService.processRequestStatusEvent(planRegisterAccepted);

        verify(enrichRegisterExternalIdProducer).produce(15L);
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation_and_register.xml")
    @ExpectedDatabase(value = "/repository/service/event/after/transportation_unit_actual_date_time.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void newRequestStatusProcessed() {
        RequestStatusQueueDto processed = new RequestStatusQueueDto(15L, false, false,
            MOVEMENT_SUPPLY_TYPE_ID, now(clock), RequestStatus.PROCESSED);
        requestStatusHandlerService.processRequestStatusEvent(processed);
    }

    @Test
    @DatabaseSetup("/repository/register/sent_outbound_plan_register.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/after_outbound_plan_register_ffwf_error.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void registerError() {
        RequestStatusQueueDto rejected = new RequestStatusQueueDto(147L, false, false,
            WITHDRAW_TYPE_ID, now(clock), RequestStatus.REJECTED_BY_SERVICE);
        requestStatusHandlerService.processRequestStatusEvent(rejected);
        verify(stEntityErrorTicketService).createErrorTicket(
            eq(EntityType.TRANSPORTATION),
            eq(1L),
            argThat(dto -> dto.getErrorType() == StartrekErrorType.REGISTER_ERROR)
        );
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation.xml")
    void testNoInteractionFor7ShopRequestStatusWithoutDetails() {
        RequestStatusQueueDto statusAccepted = new RequestStatusQueueDto(1L, false, false,
            WITHDRAW_TYPE_ID,
            now(clock),
            RequestStatus.PROCESSED);
        requestStatusHandlerService.processRequestStatusEvent(statusAccepted);
        verifyNoInteractions(fetchRegisterProducer);
    }

    @Test
    @DatabaseSetup("/repository/service/event/before/transportation.xml")
    void testFetchRegisterFor7ShopRequestStatus() {
        RequestStatusQueueDto statusAccepted = new RequestStatusQueueDto(1L, true, false,
            WITHDRAW_TYPE_ID,
            now(clock),
            RequestStatus.PROCESSED);
        requestStatusHandlerService.processRequestStatusEvent(statusAccepted);
        verify(fetchRegisterProducer).produce(
            2L,
            false
        );
    }

    @Test
    @DatabaseSetup("/repository/service/event/after/transportation_unit_ready_to_withdraw.xml")
    void testFetchPreparedRegisterStatus() {
        RequestStatusQueueDto statusAccepted = new RequestStatusQueueDto(1L, false, true,
            WITHDRAW_TYPE_ID,
            now(clock),
            RequestStatus.READY_TO_WITHDRAW);
        requestStatusHandlerService.processRequestStatusEvent(statusAccepted);
        verify(fetchRegisterProducer).produce(
            2L,
            true
        );
    }

}
