package ru.yandex.market.delivery.transport_manager.queue.task.calendaring.approve;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.logistics.calendaring.client.CalendaringServiceClientApi;
import ru.yandex.market.logistics.calendaring.client.dto.UpdateExpiresAtRequest;

class ApproveBookedSlotConsumerTest extends AbstractContextualTest {
    @Autowired
    private ApproveBookedSlotConsumer consumer;
    @Autowired
    private CalendaringServiceClientApi csClient;

    @BeforeEach
    void setUp() {
        Mockito.reset(csClient);
    }

    @AfterEach
    void tearDown() {
        Mockito.verifyNoMoreInteractions(csClient);
    }

    @DatabaseSetup("/repository/slot/slot.xml")
    @Test
    void approveBooking() {
        consumer.approveBooking(1L);
        Mockito.verify(csClient).updateExpiresAt(Mockito.eq(new UpdateExpiresAtRequest(
            100L,
            null
        )));
    }
}
