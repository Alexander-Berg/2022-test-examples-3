package ru.yandex.market.delivery.transport_manager.repository.mappers;

import java.time.ZonedDateTime;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TimeSlot;

class BookedTimeSlotMapperTest extends AbstractContextualTest {
    @Autowired
    private BookedTimeSlotMapper mapper;

    @DatabaseSetup("/repository/slot/slot.xml")
    @Test
    void getById() {
        softly.assertThat(mapper.getById(1L))
            .isEqualTo(
                new TimeSlot()
                    .setId(1L)
                    .setCalendaringServiceId(100L)
                    .setGateId(10L)
                    .setZoneId("Europe/Moscow")
                    .setFromDate(date("2021-06-07T12:00:00+00:00").toLocalDateTime())
                    .setToDate(date("2021-06-07T13:00:00+00:00").toLocalDateTime())
            );
    }

    @ExpectedDatabase(
        value = "/repository/slot/after/insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void persist() {
        mapper.persist(new TimeSlot()
            .setCalendaringServiceId(100L)
            .setGateId(10L)
            .setZoneId("+5")
            .setFromDate(date("2021-06-07T12:00:00+05:00").toLocalDateTime())
            .setToDate(date("2021-06-07T13:00:00+05:00").toLocalDateTime())
        );
        TimeSlot slot = mapper.getById(1);
        softly.assertThat(slot.getZonedFromDate()).isEqualTo("2021-06-07T12:00:00+05:00");
        softly.assertThat(slot.getZonedToDate()).isEqualTo("2021-06-07T13:00:00+05:00");
    }

    @DatabaseSetup("/repository/slot/slot.xml")
    @Test
    void getByCalendaringServiceId() {
        softly.assertThat(mapper.getByCalendaringServiceId(100L))
            .isEqualTo(
                new TimeSlot()
                    .setId(1L)
                    .setCalendaringServiceId(100L)
                    .setGateId(10L)
                    .setZoneId("Europe/Moscow")
                    .setFromDate(date("2021-06-07T12:00:00+00:00").toLocalDateTime())
                    .setToDate(date("2021-06-07T13:00:00+00:00").toLocalDateTime())
            );
    }

    @DatabaseSetup({
        "/repository/slot/slot.xml",
        "/repository/slot/transportation_unit.xml",
    })
    @Test
    void getByTransportationUnitId() {
        softly.assertThat(mapper.getByTransportationUnitId(101L))
            .isEqualTo(
                new TimeSlot()
                    .setId(1L)
                    .setCalendaringServiceId(100L)
                    .setGateId(10L)
                    .setZoneId("Europe/Moscow")
                    .setFromDate(date("2021-06-07T12:00:00+00:00").toLocalDateTime())
                    .setToDate(date("2021-06-07T13:00:00+00:00").toLocalDateTime())
            );
    }

    @DatabaseSetup("/repository/slot/slot.xml")
    @ExpectedDatabase(
        value = "/repository/slot/after/remove.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void remove() {
        mapper.remove(Set.of(1L));
    }

    @DatabaseSetup("/repository/slot/slot.xml")
    @ExpectedDatabase(
        value = "/repository/slot/after/update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void update() {
        mapper.update(new TimeSlot()
            .setId(1L)
            .setCalendaringServiceId(0L) // should not be changed
            .setGateId(2L)
            .setFromDate(date("2021-06-07T10:00:00+00:00").toLocalDateTime())
            .setToDate(date("2021-06-07T16:00:00+00:00").toLocalDateTime()));
    }

    private ZonedDateTime date(String value) {
        return ZonedDateTime.parse(value);
    }
}
