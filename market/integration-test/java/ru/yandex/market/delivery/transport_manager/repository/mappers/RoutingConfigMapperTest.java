package ru.yandex.market.delivery.transport_manager.repository.mappers;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationScheduleRoutingConfig;
import ru.yandex.market.delivery.transport_manager.domain.enums.DimensionsClass;

class RoutingConfigMapperTest extends AbstractContextualTest {
    @Autowired
    private RoutingConfigMapper mapper;

    @DatabaseSetup("/repository/schedule/setup/transportation_with_schedule.xml")
    @ExpectedDatabase(
        value = "/repository/schedule/expected/insert_routing_config.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void insert() {
        mapper.insert(new TransportationScheduleRoutingConfig(
            1L,
            true,
            DimensionsClass.BULKY_CARGO,
            1.1D,
            false,
            "DEFAULT"
        ));
    }
}
