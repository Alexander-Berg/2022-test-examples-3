package ru.yandex.market.ff.repository;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.CalendarBookingEntity;
import ru.yandex.market.ff.util.query.count.JpaQueriesCount;

public class CalendarBookingRepositoryTest extends IntegrationTest {

    @Autowired
    private CalendarBookingRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/calendar_booking/before.xml")
    @JpaQueriesCount(1)
    void findAll() {
        List<CalendarBookingEntity> all = repository.findAll();
        assertions.assertThat(all.size()).isEqualTo(1);
        CalendarBookingEntity calendarBookingEntity = all.get(0);
        assertions.assertThat(calendarBookingEntity.getRequest().getId()).isEqualTo(1L);
        assertions.assertThat(calendarBookingEntity.getBookingId()).isEqualTo(200L);
    }
}
