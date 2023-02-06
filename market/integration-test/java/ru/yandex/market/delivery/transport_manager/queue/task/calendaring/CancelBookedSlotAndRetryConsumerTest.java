package ru.yandex.market.delivery.transport_manager.queue.task.calendaring;

import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationUnit;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.cancelation_and_retry.CancelBookedSlotAndRetryConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.cancelation_and_retry.CancelBookedSlotAndRetryDto;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationUnitMapper;
import ru.yandex.market.delivery.transport_manager.util.DbQueueUtils;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class CancelBookedSlotAndRetryConsumerTest extends AbstractContextualTest {

    @Autowired
    CancelBookedSlotAndRetryConsumer cancelBookedSlotAndRetryConsumer;
    @Autowired
    CalendaringServiceClientApi calendaringServiceClient;
    @Autowired
    TransportationUnitMapper transportationUnitMapper;

    @Test
    @DatabaseSetup({"/repository/facade/transportation_booking_slot_task/cancellation/before/transportation.xml"})
    @ExpectedDatabase(
            value =
                "/repository/facade/transportation_booking_slot_task/cancellation/after/transportation_with_retry.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelBookedSlotAndRetrySuccess() {
        DbQueueUtils.assertExecutedSuccessfully(cancelBookedSlotAndRetryConsumer,
                new CancelBookedSlotAndRetryDto(List.of(1L)));

        verify(calendaringServiceClient, times(1)).cancelSlots(eq(Set.of(1L, 2L)));
        TransportationUnit withId1 = transportationUnitMapper.getById(1L);
        TransportationUnit withId2 = transportationUnitMapper.getById(2L);
        softly.assertThat(withId1.getBookedTimeSlot()).isNull();
        softly.assertThat(withId2.getBookedTimeSlot()).isNull();
    }

    @Test
    @DatabaseSetup({"/repository/facade/transportation_booking_slot_task/cancellation/before/transportation.xml"})
    @ExpectedDatabase(
            value =
                "/repository/facade/transportation_booking_slot_task/cancellation/after/transportation_with_retry.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelBookedSlotAndRetryFail() {
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.REQUEST_TIMEOUT)).when(calendaringServiceClient)
                .cancelSlots(Set.of(1L, 2L));

        DbQueueUtils.assertExecutedWithFailure(
                cancelBookedSlotAndRetryConsumer,
                new CancelBookedSlotAndRetryDto(List.of(1L)));

        verify(calendaringServiceClient, times(1)).cancelSlots(eq(Set.of(1L, 2L)));
        TransportationUnit withId1 = transportationUnitMapper.getById(1L);
        TransportationUnit withId2 = transportationUnitMapper.getById(2L);
        softly.assertThat(withId1.getBookedTimeSlot()).isNotNull();
        softly.assertThat(withId2.getBookedTimeSlot()).isNotNull();
    }
}
