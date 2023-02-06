package ru.yandex.market.delivery.transport_manager.repository.mappers;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;

import static org.assertj.core.api.Assertions.assertThat;

class TransportationScheduleMapperTest extends AbstractContextualTest {
    @Autowired
    private TransportationScheduleMapper transportationScheduleMapper;

    @Test
    @DatabaseSetup("/repository/transportation/transportation_schedule.xml")
    void countScheduledTransportationsTest() {
        assertThat(transportationScheduleMapper.countTransportationSchedules())
            .isEqualTo(2);
    }
}
