package ru.yandex.market.delivery.transport_manager.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationConfig;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationConfigHashKey;
import ru.yandex.market.delivery.transport_manager.domain.enums.ConfigTransportationType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.ScheduleMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationConfigMapper;

class TransportationConfigMapperTest extends AbstractContextualTest {

    @Autowired
    TransportationConfigMapper mapper;

    @Autowired
    ScheduleMapper scheduleMapper;

    @Test
    void save() {
        TransportationConfig newConfig = createConfig();
        Long savedScheduleId = mapper.persist(newConfig);
        TransportationConfig savedConfig = mapper.getById(savedScheduleId);
        assertThatModelEquals(savedConfig, newConfig);
    }

    @Test
    @DatabaseSetup(value = "/repository/schedule/setup/transportation.xml")
    void saveWithConflictingUpdate() {
        TransportationConfig newConfig = createConfig()
            .setVolume(101);
        Long savedScheduleId = mapper.persist(newConfig);
        TransportationConfig savedConfig = mapper.getById(savedScheduleId);

        softly.assertThat(mapper.countRows()).isEqualTo(1);
        softly.assertThat(savedConfig.getUpdated()).isNotEqualTo(LocalDateTime.parse("2020-07-10T20:00:00"));
        softly.assertThat(savedConfig.getCreated()).isEqualTo(LocalDateTime.parse("2020-07-10T20:00:00"));
    }

    @Test
    @DatabaseSetup(value = "/repository/schedule/setup/transportation_with_schedule.xml")
    void getHashKeys() {
        Set<TransportationConfigHashKey> allHashKeys = mapper.getAllHashKeys();
        TransportationConfigHashKey expected = new TransportationConfigHashKey()
            .setHash("hash1")
            .setInboundLogisticPointId(30L)
            .setInboundPartnerId(3L)
            .setMovingPartnerId(2L)
            .setOutboundLogisticPointId(10L)
            .setType(ConfigTransportationType.ORDERS_OPERATION)
            .setOutboundPartnerId(1L);
        softly.assertThat(allHashKeys).containsExactly(expected);
    }

    @Test
    @DatabaseSetup("/repository/schedule/setup/for_deletion.xml")
    @ExpectedDatabase(
        value = "/repository/schedule/expected/after_deletion.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deleteByHashKeys() {
        TransportationConfigHashKey key = new TransportationConfigHashKey()
            .setInboundLogisticPointId(30L)
            .setInboundPartnerId(3L)
            .setMovingPartnerId(2L)
            .setType(ConfigTransportationType.ORDERS_OPERATION)
            .setOutboundLogisticPointId(10L)
            .setOutboundPartnerId(100L);

        mapper.delete(Set.of(key));
        System.out.println();
    }

    private TransportationConfig createConfig() {
        return new TransportationConfig()
            .setTransportationSchedule(List.of())
            .setOutboundPartnerId(1L)
            .setOutboundLogisticPointId(10L)
            .setMovingPartnerId(2L)
            .setMovementSegmentId(200L)
            .setInboundPartnerId(3L)
            .setInboundLogisticPointId(30L)
            .setDuration(24)
            .setVolume(100)
            .setWeight(10)
            .setTransportationType(ConfigTransportationType.ORDERS_OPERATION)
            .setHash("hash");
    }
}
