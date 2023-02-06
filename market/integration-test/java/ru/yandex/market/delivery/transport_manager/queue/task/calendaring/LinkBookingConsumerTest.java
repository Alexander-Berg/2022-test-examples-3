package ru.yandex.market.delivery.transport_manager.queue.task.calendaring;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.link_booking.LinkBookingToParentConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.calendaring.link_booking.LinkBookingToParentDTO;
import ru.yandex.market.delivery.transport_manager.util.DbQueueUtils;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.LinkBookingsByExternalIdRequest;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class LinkBookingConsumerTest extends AbstractContextualTest {

    @Autowired
    private LinkBookingToParentConsumer linkBookingToParentConsumer;

    @Autowired
    private CalendaringServiceClientApi calendaringServiceClient;

    @Test
    void callCalendaringServiceLinkBookingTest() {

        long bookingId = 2L;
        String source = "TEST";
        LinkBookingToParentDTO payload = new LinkBookingToParentDTO(1L, bookingId, source, List.of("id1", "id2"));

        DbQueueUtils.assertExecutedSuccessfully(linkBookingToParentConsumer, payload);
        var captor = ArgumentCaptor.forClass(LinkBookingsByExternalIdRequest.class);

        verify(calendaringServiceClient, times(1)).linkBookingsByExternalId(eq(bookingId), captor.capture());

        softly.assertThat(captor.getValue().getSource()).isEqualTo(source);
        softly.assertThat(captor.getValue().getExternalIds()).isNotNull();
        softly.assertThat(captor.getValue().getExternalIds().size()).isEqualTo(2);

    }

}
