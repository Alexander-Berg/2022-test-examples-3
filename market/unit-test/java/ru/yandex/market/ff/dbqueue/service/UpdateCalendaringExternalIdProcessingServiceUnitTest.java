package ru.yandex.market.ff.dbqueue.service;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ru.yandex.market.ff.model.AppInfo;
import ru.yandex.market.ff.model.dbqueue.UpdateCalendaringExternalIdPayload;
import ru.yandex.market.ff.model.entity.CalendarBookingEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.service.CalendaringServiceClientWrapperService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;
import ru.yandex.market.ff.service.implementation.PublishToLogbrokerCalendarShopRequestChangeService;
import ru.yandex.market.ff.service.timeslot.CalendarBookingService;
import ru.yandex.market.logistics.calendaring.client.dto.UpdateExternalIdRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class UpdateCalendaringExternalIdProcessingServiceUnitTest {

    private static final long BOOKING_ID = 123;
    private static final long OLD_REQUEST_ID = 234;
    private static final long NEW_REQUEST_ID = 345;
    private static final long OTHER_BOOKING_ID = 456;
    private static final String CALENDARING_SERVICE_SOURCE = "FFWF";

    private final CalendaringServiceClientWrapperService csClientWrapperService =
            mock(CalendaringServiceClientWrapperService.class);
    private final CalendarBookingService calendarBookingService = mock(CalendarBookingService.class);

    private final PublishToLogbrokerCalendarShopRequestChangeService publishChangeToCalendaringService =
            mock(PublishToLogbrokerCalendarShopRequestChangeService.class);

    private final ShopRequestFetchingService shopRequestFetchingService = mock(ShopRequestFetchingService.class);

    private final UpdateCalendaringExternalIdProcessingService service =
            new UpdateCalendaringExternalIdProcessingService(
                    csClientWrapperService,
                    calendarBookingService,
                    new AppInfo("HOST", CALENDARING_SERVICE_SOURCE),
                    publishChangeToCalendaringService,
                    shopRequestFetchingService
            );

    @Test
    public void successIfBookingAlreadyUpdated() {
        when(calendarBookingService.findByRequestIdWithLock(OLD_REQUEST_ID)).thenReturn(Optional.empty());
        when(calendarBookingService.findByRequestId(NEW_REQUEST_ID))
                .thenReturn(Optional.of(new CalendarBookingEntity(BOOKING_ID,
                        createRequestWithId(NEW_REQUEST_ID), LocalDateTime.now(), false)));
        service.processPayload(createPayload());
        verify(calendarBookingService).findByRequestIdWithLock(OLD_REQUEST_ID);
        verify(calendarBookingService).findByRequestId(NEW_REQUEST_ID);
        verifyNoMoreInteractions(calendarBookingService);
        verifyZeroInteractions(csClientWrapperService);
    }

    @Test
    public void failedIfBookingNotExistsAndNotAlreadyUpdated() {
        when(calendarBookingService.findByRequestIdWithLock(OLD_REQUEST_ID)).thenReturn(Optional.empty());
        when(calendarBookingService.findByRequestId(NEW_REQUEST_ID)).thenReturn(Optional.empty());
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.processPayload(createPayload()));
        assertEquals("Failed to update calendar booking because no booking id DB", exception.getMessage());
        verify(calendarBookingService).findByRequestIdWithLock(OLD_REQUEST_ID);
        verify(calendarBookingService).findByRequestId(NEW_REQUEST_ID);
        verifyNoMoreInteractions(calendarBookingService);
        verifyZeroInteractions(csClientWrapperService);
    }

    @Test
    public void failedIfUpdatedButMappingOnAnotherBooking() {
        when(calendarBookingService.findByRequestIdWithLock(OLD_REQUEST_ID)).thenReturn(Optional.empty());
        when(calendarBookingService.findByRequestId(NEW_REQUEST_ID))
                .thenReturn(Optional.of(new CalendarBookingEntity(OTHER_BOOKING_ID,
                        createRequestWithId(NEW_REQUEST_ID), LocalDateTime.now(), false)));
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.processPayload(createPayload()));
        assertEquals("Request id " + NEW_REQUEST_ID +
                " is in mapping with other booking id " + OTHER_BOOKING_ID, exception.getMessage());
        verify(calendarBookingService).findByRequestIdWithLock(OLD_REQUEST_ID);
        verify(calendarBookingService).findByRequestId(NEW_REQUEST_ID);
        verifyNoMoreInteractions(calendarBookingService);
        verifyZeroInteractions(csClientWrapperService);
    }

    @Test
    public void failedInCaseUpdateBookingIdFailedWithException() {
        when(calendarBookingService.findByRequestIdWithLock(OLD_REQUEST_ID)).thenReturn(Optional.of(
                new CalendarBookingEntity(BOOKING_ID, createRequestWithId(OLD_REQUEST_ID), LocalDateTime.now(), false)
        ));
        UpdateExternalIdRequest updateExternalIdRequest = createExternalRequest();
        when(csClientWrapperService.updateBookingExternalId(updateExternalIdRequest)).thenReturn(Optional.empty());
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.processPayload(createPayload()));
        assertEquals("Failed to change calendar booking because of exception", exception.getMessage());
        verify(calendarBookingService).findByRequestIdWithLock(OLD_REQUEST_ID);
        verify(csClientWrapperService).updateBookingExternalId(updateExternalIdRequest);
        verifyNoMoreInteractions(calendarBookingService);
        verifyNoMoreInteractions(csClientWrapperService);
    }

    @Test
    public void failedIfNothingWasChangedDuringUpdateBooking() {
        when(calendarBookingService.findByRequestIdWithLock(OLD_REQUEST_ID)).thenReturn(Optional.of(
                new CalendarBookingEntity(BOOKING_ID, createRequestWithId(OLD_REQUEST_ID), LocalDateTime.now(), false)
        ));
        UpdateExternalIdRequest updateExternalIdRequest = createExternalRequest();
        when(csClientWrapperService.updateBookingExternalId(updateExternalIdRequest)).thenReturn(Optional.of(false));
        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.processPayload(createPayload()));
        assertEquals("Failed to calendar booking without exception", exception.getMessage());
        verify(calendarBookingService).findByRequestIdWithLock(OLD_REQUEST_ID);
        verify(csClientWrapperService).updateBookingExternalId(updateExternalIdRequest);
        verifyNoMoreInteractions(calendarBookingService);
        verifyNoMoreInteractions(csClientWrapperService);
    }

    @Test
    public void successfulFlowWithUpdateBooking() {
        when(calendarBookingService.findByRequestIdWithLock(OLD_REQUEST_ID)).thenReturn(Optional.of(
                new CalendarBookingEntity(BOOKING_ID, createRequestWithId(OLD_REQUEST_ID), LocalDateTime.now(), false)
        ));
        UpdateExternalIdRequest updateExternalIdRequest = createExternalRequest();
        when(csClientWrapperService.updateBookingExternalId(updateExternalIdRequest)).thenReturn(Optional.of(true));
        service.processPayload(createPayload());
        verify(calendarBookingService).findByRequestIdWithLock(OLD_REQUEST_ID);
        verify(csClientWrapperService).updateBookingExternalId(updateExternalIdRequest);
        ArgumentCaptor<CalendarBookingEntity> bookingCaptor = ArgumentCaptor.forClass(CalendarBookingEntity.class);
        verify(calendarBookingService).update(bookingCaptor.capture());
        CalendarBookingEntity booking = bookingCaptor.getValue();
        assertEquals(BOOKING_ID, booking.getBookingId());
        assertEquals(NEW_REQUEST_ID, booking.getRequest().getId());
        verifyNoMoreInteractions(calendarBookingService);
        verifyNoMoreInteractions(csClientWrapperService);
    }

    private UpdateCalendaringExternalIdPayload createPayload() {
        return new UpdateCalendaringExternalIdPayload(
                BOOKING_ID,
                OLD_REQUEST_ID,
                NEW_REQUEST_ID
        );
    }

    private UpdateExternalIdRequest createExternalRequest() {
        return new UpdateExternalIdRequest(
                BOOKING_ID,
                String.valueOf(OLD_REQUEST_ID),
                CALENDARING_SERVICE_SOURCE,
                String.valueOf(NEW_REQUEST_ID)
        );
    }

    private ShopRequest createRequestWithId(long id) {
        ShopRequest request = new ShopRequest();
        request.setId(id);
        return request;
    }
}
