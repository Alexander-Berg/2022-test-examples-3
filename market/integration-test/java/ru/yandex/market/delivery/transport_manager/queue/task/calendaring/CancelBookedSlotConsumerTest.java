package ru.yandex.market.delivery.transport_manager.queue.task.calendaring;

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
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.cancelation.CancelBookedSlotConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.cancelation.CancelBookedSlotDto;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationUnitMapper;
import ru.yandex.market.delivery.transport_manager.util.DbQueueUtils;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
class CancelBookedSlotConsumerTest extends AbstractContextualTest {

    @Autowired
    CancelBookedSlotConsumer cancelBookedSlotConsumer;
    @Autowired
    CalendaringServiceClientApi calendaringServiceClient;
    @Autowired
    TransportationUnitMapper transportationUnitMapper;

    @Test
    @DatabaseSetup({"/repository/facade/transportation_booking_slot_task/cancellation/before/transportation.xml"})
    @ExpectedDatabase(
            value = "/repository/facade/transportation_booking_slot_task/cancellation/after/transportation.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelBookedSlotSuccess() {
        DbQueueUtils.assertExecutedSuccessfully(cancelBookedSlotConsumer, new CancelBookedSlotDto().setTmBookingId(1L));

        verify(calendaringServiceClient, times(1)).cancelSlot(eq(1L));
        TransportationUnit withId1 = transportationUnitMapper.getById(1L);
        TransportationUnit withId2 = transportationUnitMapper.getById(2L);
        softly.assertThat(withId1.getBookedTimeSlot()).isNull();
        softly.assertThat(withId2.getBookedTimeSlot()).isNotNull();
    }

    @Test
    @DatabaseSetup({"/repository/facade/transportation_booking_slot_task/cancellation/before/transportation.xml"})
    @ExpectedDatabase(
            value = "/repository/facade/transportation_booking_slot_task/cancellation/after/transportation.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void cancelBookedSlotFail() {
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.REQUEST_TIMEOUT)).when(calendaringServiceClient)
                .cancelSlot(1L);

        DbQueueUtils.assertExecutedWithFailure(
                cancelBookedSlotConsumer,
                new CancelBookedSlotDto().setTmBookingId(1L)
        );

        verify(calendaringServiceClient, times(1)).cancelSlot(eq(1L));
        TransportationUnit withId1 = transportationUnitMapper.getById(1L);
        TransportationUnit withId2 = transportationUnitMapper.getById(2L);
        softly.assertThat(withId1.getBookedTimeSlot()).isNull();
        softly.assertThat(withId2.getBookedTimeSlot()).isNotNull();
    }
}
