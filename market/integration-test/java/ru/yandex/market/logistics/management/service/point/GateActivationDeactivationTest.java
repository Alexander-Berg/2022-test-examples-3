package ru.yandex.market.logistics.management.service.point;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.entity.LogisticsPointGate;
import ru.yandex.market.logistics.management.repository.LogisticsPointGateRepository;
import ru.yandex.market.logistics.management.service.client.LogisticsPointGateService;
import ru.yandex.market.logistics.management.util.CleanDatabase;

import static org.assertj.core.api.Assertions.assertThat;

@CleanDatabase
@Sql("/data/service/point/gate_data.sql")
public class GateActivationDeactivationTest extends AbstractContextualTest {

    @Autowired
    private LogisticsPointGateService logisticsPointGateService;

    @Autowired
    private LogisticsPointGateRepository logisticsPointGateRepository;

    @Test
    void disableGates() {
        Set<Long> gateIds = Set.of(1L, 2L);
        logisticsPointGateService.disable(gateIds);

        assertThat(logisticsPointGateRepository.findAllById(gateIds))
            .extracting(LogisticsPointGate::getEnabled)
            .containsOnly(false);
    }

    @Test
    void enableGates() {
        Set<Long> gateIds = Set.of(3L, 4L);
        logisticsPointGateService.enable(gateIds);

        assertThat(logisticsPointGateRepository.findAllById(gateIds))
            .extracting(LogisticsPointGate::getEnabled)
            .containsOnly(true);
    }
}

