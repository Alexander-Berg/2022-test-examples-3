package ru.yandex.market.delivery.transport_manager.facade.transportation;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.service.TransportationService;

class RepairTransportationFacadeTest extends AbstractContextualTest {

    @Autowired
    private RepairTransportationFacade repairTransportationFacade;

    @Autowired
    private TransportationService transportationService;

    @BeforeEach
    void init() {
        clock.setFixed(Instant.parse("2020-11-29T19:00:00.00Z"), ZoneId.of("UTC"));
    }

    @DatabaseSetup("/repository/transportation/linehaul_failed_on_wms.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/after/linehaul_failed_on_wms_reparied.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void doErrorRollback() {
        Transportation t = transportationService.getById(11L);
        repairTransportationFacade.repairTransportationUnitsIfRequired(t);
    }

    @DatabaseSetup("/repository/transportation/interwarehouse/interwarehouse_error_outbound.xml")
    @ExpectedDatabase(
        value = "/repository/transportation/interwarehouse/interwarehouse_error_outbound_repair.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void checkPlannedLaunchTimeNotChanged() {
        Transportation t = transportationService.getById(11L);
        repairTransportationFacade.repairTransportationUnitsIfRequired(t);
    }
}
