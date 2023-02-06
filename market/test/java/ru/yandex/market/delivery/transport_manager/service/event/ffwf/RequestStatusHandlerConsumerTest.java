package ru.yandex.market.delivery.transport_manager.service.event.ffwf;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.transport_manager.converter.ffwf.RequestSubtypeIds;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnitType;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationSubtype;
import ru.yandex.market.delivery.transport_manager.domain.enums.TransportationType;
import ru.yandex.market.delivery.transport_manager.dto.xdoc.XDocRequestStatus;
import ru.yandex.market.delivery.transport_manager.queue.task.xdoc.transportations.cancel.XDocCancelProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.xdoc.transportations.dc.XDocCreateDcProducer;
import ru.yandex.market.delivery.transport_manager.queue.task.xdoc.transportations.ff.XDocCreateFfProducer;
import ru.yandex.market.ff.client.dto.RequestStatusChangeDto;
import ru.yandex.market.ff.client.dto.RequestStatusChangesDto;
import ru.yandex.market.ff.client.enums.RequestStatus;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public class RequestStatusHandlerConsumerTest {

    private static final int SUPPLY_TO_FF_TYPE_ID = 0;
    private static final int X_DOC_PARTNER_SUPPLY_TO_FF_TYPE_ID = 21;
    private static final int LINEHAUL_TYPE_ID = 11;
    public static final Integer BREAK_BULK_X_DOC_PARTNER_SUPPLY_TO_FF_TYPE_ID = RequestSubtypeIds.id(
        TransportationType.XDOC_PARTNER_SUPPLY_TO_FF,
        TransportationSubtype.BREAK_BULK_XDOCK,
        TransportationUnitType.INBOUND
    );

    @Test
    void accept() {
        RequestStatusHandlerService requestStatusHandlerService = mock(RequestStatusHandlerService.class);
        XDocCreateFfProducer xDocCreateFfProducer = mock(XDocCreateFfProducer.class);
        XDocCreateDcProducer xDocCreateDcProducer = mock(XDocCreateDcProducer.class);
        XDocCancelProducer xDocCancelProducer = mock(XDocCancelProducer.class);
        RequestStatusHandlerConsumer consumer = new RequestStatusHandlerConsumer(
            requestStatusHandlerService,
            xDocCreateFfProducer,
            xDocCreateDcProducer,
            xDocCancelProducer
        );

        var xdocCreated = dto(1, X_DOC_PARTNER_SUPPLY_TO_FF_TYPE_ID, RequestStatus.VALIDATED);
        var xdocAccepted = dto(2, X_DOC_PARTNER_SUPPLY_TO_FF_TYPE_ID, RequestStatus.ACCEPTED_BY_SERVICE);
        var xdocCanel = dto(3, X_DOC_PARTNER_SUPPLY_TO_FF_TYPE_ID, RequestStatus.CANCELLED);
        var statusChange = dto(4, SUPPLY_TO_FF_TYPE_ID, RequestStatus.ACCEPTED_BY_SERVICE);
        var sentToService = dto(5, SUPPLY_TO_FF_TYPE_ID, RequestStatus.SENT_TO_SERVICE);

        var breakBulkXdocCreated = dto(6, BREAK_BULK_X_DOC_PARTNER_SUPPLY_TO_FF_TYPE_ID, RequestStatus.VALIDATED);
        var breakBulkXdocAccepted = dto(
            7, BREAK_BULK_X_DOC_PARTNER_SUPPLY_TO_FF_TYPE_ID, RequestStatus.ACCEPTED_BY_SERVICE
        );
        var breakBulkXdocCancelled = dto(8, BREAK_BULK_X_DOC_PARTNER_SUPPLY_TO_FF_TYPE_ID, RequestStatus.CANCELLED);

        var linehaulProcessed = dto(9, LINEHAUL_TYPE_ID, RequestStatus.PROCESSED, false);
        var linehaulProcessedWithDetails = dto(9, LINEHAUL_TYPE_ID, RequestStatus.PROCESSED, true);
        var linehaulFinishedWithDetails = dto(9, LINEHAUL_TYPE_ID, RequestStatus.FINISHED, true);


        consumer.accept(List.of(new RequestStatusChangesDto(List.of(
            xdocCreated,
            breakBulkXdocCreated,
            xdocAccepted,
            breakBulkXdocAccepted,
            xdocCanel,
            breakBulkXdocCancelled,
            statusChange,
            sentToService,
            linehaulProcessed,
            linehaulProcessedWithDetails,
            linehaulFinishedWithDetails
        ))));

        verify(requestStatusHandlerService).processRawBatchRequestStatuses(eq(List.of(
            xdocAccepted,
            breakBulkXdocAccepted,
            xdocCanel,
            breakBulkXdocCancelled,
            statusChange,
            linehaulProcessed,
            linehaulProcessedWithDetails
        )));
        verify(xDocCreateFfProducer).enqueue(xdocCreated);
        verify(xDocCreateFfProducer).enqueue(breakBulkXdocCreated);
        verify(xDocCreateDcProducer).enqueue(
            xdocAccepted.getRequestId(),
            XDocRequestStatus.ACCEPTED_BY_SERVICE,
            null
        );
        verify(xDocCancelProducer).enqueue(xdocCanel.getRequestId());
        verify(xDocCancelProducer).enqueue(breakBulkXdocCancelled.getRequestId());

        verifyNoMoreInteractions(
            requestStatusHandlerService,
            xDocCreateFfProducer,
            xDocCreateDcProducer,
            xDocCancelProducer
        );
    }

    private RequestStatusChangeDto dto(long id, int type, RequestStatus status) {
        return dto(id, type, status, false);
    }

    private RequestStatusChangeDto dto(long id, int type, RequestStatus status, boolean detailsLoaded) {
        return new RequestStatusChangeDto()
            .setRequestId(id)
            .setRequestType(type)
            .setNewStatus(status)
            .setDetailsLoaded(detailsLoaded);
    }
}
