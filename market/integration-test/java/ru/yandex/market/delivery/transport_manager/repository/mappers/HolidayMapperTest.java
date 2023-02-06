package ru.yandex.market.delivery.transport_manager.repository.mappers;

import java.time.LocalDate;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Holiday;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationConfig;

class HolidayMapperTest extends AbstractContextualTest {
    @Autowired
    private HolidayMapper holidayMapper;

    @DatabaseSetup("/repository/schedule/setup/transportation_with_schedule_after_update.xml")
    @Test
    void getAll() {
        List<Holiday> actual = holidayMapper.getAll();
        softly.assertThat(actual).containsExactlyInAnyOrder(
            new Holiday()
                .setScheduleId(2L)
                .setDay(LocalDate.of(2021, 5, 9))
                .setPartnerId(10L),
            new Holiday()
                .setScheduleId(2L)
                .setDay(LocalDate.of(2021, 5, 10))
                .setPartnerId(30L)
        );
    }

    @DatabaseSetup("/repository/schedule/setup/transportation_with_schedule_after_update.xml")
    @Test
    void getByConfig() {
        List<Holiday> actual = holidayMapper.getByConfig(new TransportationConfig().setId(2L));
        softly.assertThat(actual).containsExactlyInAnyOrder(
            new Holiday()
                .setScheduleId(2L)
                .setDay(LocalDate.of(2021, 5, 9))
                .setPartnerId(10L),
            new Holiday()
                .setScheduleId(2L)
                .setDay(LocalDate.of(2021, 5, 10))
                .setPartnerId(30L)
        );
    }

    @DatabaseSetup("/repository/schedule/setup/transportation_with_schedule_after_update.xml")
    @Test
    void getByConfigEmpty() {
        List<Holiday> actual = holidayMapper.getByConfig(new TransportationConfig().setId(1L));
        softly.assertThat(actual).isEmpty();
    }
}
