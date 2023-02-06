package ru.yandex.market.ff.repository;

import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.TimeSlotStatus;
import ru.yandex.market.ff.model.entity.BookedTimeSlot;

@ActiveProfiles("BookedTimeSlotsRepositoryTest")
public class BookedTimeSlotsRepositoryTest extends IntegrationTest {

    @Autowired
    private BookedTimeSlotsRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/booked_time_slots/find_booked_slots.xml")
    @ExpectedDatabase(value = "classpath:repository/booked_time_slots/find_booked_slots.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT)
    public void findBookedSlotsWorksCorrect() {
        Optional<BookedTimeSlot> firstBookedTimeSlot = repository.findBookedSlotForRequest(1, TimeSlotStatus.ACTIVE);
        assertions.assertThat(firstBookedTimeSlot).isPresent();
        assertions.assertThat(firstBookedTimeSlot.get().getId()).isEqualTo(3);

        Optional<BookedTimeSlot> secondBookedTimeSlot = repository.findBookedSlotForRequest(2, TimeSlotStatus.ACTIVE);
        assertions.assertThat(secondBookedTimeSlot).isNotPresent();
    }
}
