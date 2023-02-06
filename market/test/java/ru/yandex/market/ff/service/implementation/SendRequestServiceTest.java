package ru.yandex.market.ff.service.implementation;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityNotFoundException;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DatabaseSetups;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.dbqueue.producer.SendCommonRequestToServiceQueueProducer;
import ru.yandex.market.ff.dbqueue.producer.SendSupplyRequestToServiceQueueProducer;
import ru.yandex.market.ff.service.RequestSubTypeService;
import ru.yandex.market.ff.service.timeslot.CalendarBookingService;
import ru.yandex.market.logistics.calendaring.client.dto.BookingListResponse;
import ru.yandex.market.logistics.calendaring.client.dto.BookingResponse;
import ru.yandex.market.logistics.calendaring.client.dto.enums.BookingStatus;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ff.client.enums.RequestType.SUPPLY_TYPES_FOR_SEND_TO_SERVICE;

/**
 * Интеграционный тесты для {@link SendRequestService}.
 *
 */
public class SendRequestServiceTest extends IntegrationTest {

    private static final EnumSet<RequestType> COMMON_REQUEST_TYPES_FOR_SENDING_REQUESTS_TO_SERVICE = EnumSet
            .complementOf(SUPPLY_TYPES_FOR_SEND_TO_SERVICE);
    private SendSupplyRequestToServiceQueueProducer supplyProducer;
    private SendCommonRequestToServiceQueueProducer commonProducer;
    @Autowired
    private RequestSubTypeService requestSubTypeService;
    @Autowired
    private CalendarBookingService calendarBookingService;
    private SendRequestService sendRequestService;

    @BeforeEach
    void init() {
        supplyProducer = mock(SendSupplyRequestToServiceQueueProducer.class);
        commonProducer = mock(SendCommonRequestToServiceQueueProducer.class);

        sendRequestService = new SendRequestService(supplyProducer, commonProducer, requestSubTypeService,
                calendarBookingService);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:params/use-market-name.xml"),
            @DatabaseSetup("classpath:tms/send-requests-to-service/before.xml")})
    void testSendSupplyRequestToServiceTasksCreate() {
        processAllRequests();
        Mockito.verify(supplyProducer,
                Mockito.times(getRequestsCount(SUPPLY_TYPES_FOR_SEND_TO_SERVICE))).produceSingle(
                Mockito.any()
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:params/use-market-name.xml"),
            @DatabaseSetup("classpath:tms/send-requests-to-service/before.xml")})
    void testSendCommonRequestToServiceTasksCreate() {
        processAllRequests();
        Mockito.verify(commonProducer,
                Mockito.times(6)).produceSingle(
                Mockito.any()
        );
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:params/use-market-name.xml"),
            @DatabaseSetup("classpath:tms/send-requests-to-service/before-without-booking.xml")})
    @ExpectedDatabase(value = "classpath:tms/send-requests-to-service/before-without-booking.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void testCalendaringRequestNotSentWithoutBookedSlotAndBooking() {
        processAllRequests();
        Mockito.verifyZeroInteractions(supplyProducer);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:params/use-market-name.xml"),
            @DatabaseSetup("classpath:tms/send-requests-to-service/before.xml")})
    void testRequestNotSentWithNotPushableToService() {
        var request = shopRequestFetchingService.getRequest(12)
                .orElseThrow(() -> new EntityNotFoundException("Request[id=" + 12 + "] hasn't been found."));
        sendRequestService.sendIfNeeded(request);
        Mockito.verifyZeroInteractions(supplyProducer);
        Mockito.verifyZeroInteractions(commonProducer);
    }

    @Test
    @DatabaseSetups({
            @DatabaseSetup("classpath:params/use-market-name.xml"),
            @DatabaseSetup("classpath:tms/send-requests-to-service/before-with-booking-in-cs.xml")})
    void testCalendaringRequestSentWithoutBookedSlotButWithBooking() {

        when(csClient.getSlotByExternalIdentifiers(eq(Set.of("1")), anyString(), isNull())).thenReturn(
                new BookingListResponse(List.of(
                        new BookingResponse(199, "FFWF", "1", null, 0,
                                ZonedDateTime.of(2018, 1, 6, 9, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                                ZonedDateTime.of(2018, 1, 6, 10, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                                BookingStatus.UPDATING, null,
                                100L),
                        new BookingResponse(200, "FFWF", "1", null, 0,
                                ZonedDateTime.of(2018, 1, 5, 7, 0, 0, 0, ZoneId.of("Europe/Moscow")),
                                ZonedDateTime.of(2018, 1, 5, 7, 30, 0, 0, ZoneId.of("Europe/Moscow")),
                                BookingStatus.ACTIVE, null,
                                100L)
                ))
        );

        processAllRequests();
        Mockito.verify(supplyProducer, Mockito.times(1)).produceSingle(
                Mockito.any());
    }

    private int getRequestsCount(Collection<RequestType> requestTypes) {
        return shopRequestFetchingService
                .getNotInternalRequestsByStatusAndTypesWithValidCalendaring(RequestStatus.VALIDATED,
                        requestTypes).size();
    }

    private void processAllRequests() {
        shopRequestFetchingService.getRequestsByStatuses(RequestStatus.values())
                .forEach(sendRequestService::sendIfNeeded);
    }
}
