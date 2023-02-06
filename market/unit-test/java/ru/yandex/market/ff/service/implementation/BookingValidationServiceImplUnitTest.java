package ru.yandex.market.ff.service.implementation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.ff.base.SoftAssertionSupport;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.enrichment.RequestValidationErrorType;
import ru.yandex.market.ff.enrichment.RequestValidationResult;
import ru.yandex.market.ff.enrichment.ValidationProfile;
import ru.yandex.market.ff.model.entity.CalendarBookingEntity;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.repository.CalendarBookingRepository;
import ru.yandex.market.ff.service.CalendaringServiceClientWrapperService;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponseV2;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;

import static org.mockito.ArgumentMatchers.eq;

public class BookingValidationServiceImplUnitTest extends SoftAssertionSupport {

    private final CalendarBookingRepository calendarBookingRepository = Mockito.mock(CalendarBookingRepository.class);
    private final CalendaringServiceClientWrapperService
            calendaringServiceClientWrapperService = Mockito.mock(CalendaringServiceClientWrapperService.class);


    private final BookingValidationServiceImpl service =
            new BookingValidationServiceImpl(calendarBookingRepository, calendaringServiceClientWrapperService);


    @Test
    void doNotValidateIfNoLinkToBookingTest() {

        RequestValidationResult requestValidationResult =
                RequestValidationResult.of(ValidationProfile.builder().build(),
                        Set.of(), Map.of());

        long requestId = 1L;


        Mockito.when(calendarBookingRepository.findByRequestId(eq(requestId))).thenReturn(Optional.empty());

        requestValidationResult =
                service.validateBookingIsActive(requestId, requestValidationResult);

        assertions.assertThat(requestValidationResult.getValidationErrors().size()).isEqualTo(0);
    }

    @Test
    void activeBookingFoundTest() {

        RequestValidationResult requestValidationResult =
                RequestValidationResult.of(ValidationProfile.builder().build(),
                        Set.of(), Map.of());

        long requestId = 1L;
        long bookingId = 1L;

        CalendarBookingEntity calendarBookingEntity = new CalendarBookingEntity();
        calendarBookingEntity.setBookingId(bookingId);

        BookingResponseV2 bookingResponse = new BookingResponseV2(
                1L,
                "FFWF",
                "1",
                null,
                1L,
                ZonedDateTime.of(2021, 1, 1, 10, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2021, 1, 1, 11, 0, 0, 0, ZoneId.of("UTC")),
                BookingStatus.ACTIVE,
                LocalDateTime.of(2021, 1, 1, 9, 0, 0),
                100L
        );


        Mockito.when(calendarBookingRepository.findByRequestId(eq(requestId))).thenReturn(
                Optional.of(calendarBookingEntity)
        );

        Mockito.when(calendaringServiceClientWrapperService.getSlot(eq(bookingId))).thenReturn(
                Optional.of(bookingResponse)
        );

        requestValidationResult =
                service.validateBookingIsActive(requestId, requestValidationResult);

        assertions.assertThat(requestValidationResult.getValidationErrors().size()).isEqualTo(0);
    }

    @Test
    void activeBookingNotFoundWhenBookingInactiveTest() {

        RequestValidationResult requestValidationResult =
                RequestValidationResult.of(ValidationProfile.builder().build(),
                        Set.of(), Map.of(), new HashSet<>());

        long requestId = 1L;
        long bookingId = 1L;

        CalendarBookingEntity calendarBookingEntity = new CalendarBookingEntity();
        calendarBookingEntity.setBookingId(bookingId);

        BookingResponseV2 bookingResponse = new BookingResponseV2(
                1L,
                "FFWF",
                "1",
                null,
                1L,
                ZonedDateTime.of(2021, 1, 1, 10, 0, 0, 0, ZoneId.of("UTC")),
                ZonedDateTime.of(2021, 1, 1, 11, 0, 0, 0, ZoneId.of("UTC")),
                BookingStatus.CANCELLED,
                LocalDateTime.of(2021, 1, 1, 9, 0, 0),
                100L
        );


        Mockito.when(calendarBookingRepository.findByRequestId(eq(requestId))).thenReturn(
                Optional.of(calendarBookingEntity)
        );

        Mockito.when(calendaringServiceClientWrapperService.getSlot(eq(bookingId))).thenReturn(
                Optional.of(bookingResponse)
        );

        requestValidationResult =
                service.validateBookingIsActive(requestId, requestValidationResult);

        assertions.assertThat(requestValidationResult.getValidationErrors()
                .contains(RequestValidationErrorType.ACTIVE_BOOKING_NOT_FOUND)).isEqualTo(true);
    }


    @Test
    void validateLimitIsActualWithdrawNextDay() {

        long requestId = 1L;
        long bookingId = 1L;

        LocalDate limitDate = LocalDate.of(2021, 1, 2);

        ShopRequest request = new ShopRequest();
        request.setId(requestId);
        request.setCreatedAt(LocalDateTime.of(2021, 1, 1, 10, 0));
        request.setType(RequestType.MOVEMENT_WITHDRAW);

        CalendarBookingEntity calendarBookingEntity = new CalendarBookingEntity();
        calendarBookingEntity.setBookingId(bookingId);

        RequestValidationResult requestValidationResult =
                RequestValidationResult.of(ValidationProfile.builder().build(),
                        Set.of(), Map.of(), new HashSet<>());

        Mockito.when(calendarBookingRepository.findByRequestId(eq(requestId))).thenReturn(
                Optional.of(calendarBookingEntity)
        );

        Mockito.when(calendaringServiceClientWrapperService.getBookingLimit(eq(bookingId))).thenReturn(
                limitDate
        );

        requestValidationResult =
                service.validateLimitIsActual(request, requestValidationResult);

        assertions.assertThat(requestValidationResult.getValidationErrors()
                .contains(RequestValidationErrorType.LIMIT_DATE_EXPIRED)).isEqualTo(false);

    }

    @Test
    void validateLimitIsActualWithdrawSameDay() {

        long requestId = 1L;
        long bookingId = 1L;

        LocalDate limitDate = LocalDate.of(2021, 1, 1);

        ShopRequest request = new ShopRequest();
        request.setId(requestId);
        request.setCreatedAt(LocalDateTime.of(2021, 1, 1, 10, 0));
        request.setType(RequestType.MOVEMENT_WITHDRAW);

        CalendarBookingEntity calendarBookingEntity = new CalendarBookingEntity();
        calendarBookingEntity.setBookingId(bookingId);

        RequestValidationResult requestValidationResult =
                RequestValidationResult.of(ValidationProfile.builder().build(),
                        Set.of(), Map.of(), new HashSet<>());

        Mockito.when(calendarBookingRepository.findByRequestId(eq(requestId))).thenReturn(
                Optional.of(calendarBookingEntity)
        );

        Mockito.when(calendaringServiceClientWrapperService.getBookingLimit(eq(bookingId))).thenReturn(
                limitDate
        );

        requestValidationResult =
                service.validateLimitIsActual(request, requestValidationResult);

        assertions.assertThat(requestValidationResult.getValidationErrors()
                .contains(RequestValidationErrorType.LIMIT_DATE_EXPIRED)).isEqualTo(true);

    }

    @Test
    void validateLimitIsActualSupplySameDay() {

        long requestId = 1L;
        long bookingId = 1L;

        LocalDate limitDate = LocalDate.of(2021, 1, 1);

        ShopRequest request = new ShopRequest();
        request.setId(requestId);
        request.setCreatedAt(LocalDateTime.of(2021, 1, 1, 10, 0));
        request.setType(RequestType.MOVEMENT_SUPPLY);

        CalendarBookingEntity calendarBookingEntity = new CalendarBookingEntity();
        calendarBookingEntity.setBookingId(bookingId);

        RequestValidationResult requestValidationResult =
                RequestValidationResult.of(ValidationProfile.builder().build(),
                        Set.of(), Map.of(), new HashSet<>());

        Mockito.when(calendarBookingRepository.findByRequestId(eq(requestId))).thenReturn(
                Optional.of(calendarBookingEntity)
        );

        Mockito.when(calendaringServiceClientWrapperService.getBookingLimit(eq(bookingId))).thenReturn(
                limitDate
        );

        requestValidationResult =
                service.validateLimitIsActual(request, requestValidationResult);

        assertions.assertThat(requestValidationResult.getValidationErrors()
                .contains(RequestValidationErrorType.LIMIT_DATE_EXPIRED)).isEqualTo(false);

    }

}
