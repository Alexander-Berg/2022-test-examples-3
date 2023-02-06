package ru.yandex.market.wms.autostart.service;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.autostart.core.model.dto.StationToCarrierDto;
import ru.yandex.market.wms.autostart.core.model.dto.StationType;
import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StationToCarrierServiceTest extends IntegrationTest {

    @Autowired
    private StationToCarrierService stationToCarrierService;

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/service/station-to-carrier/stations.xml")
    @DatabaseSetup(value = "/fixtures/autostart/service/station-to-carrier/common.xml")
    @ExpectedDatabase(
            value = "/fixtures/autostart/service/station-to-carrier/common.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testFindAll() {
        List<StationToCarrierDto> stationsToCarrier = stationToCarrierService.getStationsToCarrier();
        assertAll(
                () -> assertEquals(4, stationsToCarrier.size()),
                () -> assertEquals(2, stationsToCarrier.stream()
                        .filter(x -> x.getType().equals(StationType.SORT))
                        .count()),
                () -> assertEquals(2, stationsToCarrier.stream()
                        .filter(x -> x.getType().equals(StationType.CONSOLIDATION))
                        .count())
        );
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/service/station-to-carrier/common.xml")
    @ExpectedDatabase(
            value = "/fixtures/autostart/service/station-to-carrier/add-sort-stations/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testAddSortStationsToCarrier() {
        stationToCarrierService.putStations(null, List.of("LOC-5", "LOC-6"), List.of("10"), false);
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/service/station-to-carrier/common.xml")
    @ExpectedDatabase(
            value = "/fixtures/autostart/service/station-to-carrier/add-cons-stations/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testAddConsolidationStationsToCarrier() {
        stationToCarrierService.putStations(List.of("LOC-5", "LOC-6"), null, List.of("10"), true);
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/service/station-to-carrier/common.xml")
    @ExpectedDatabase(
            value = "/fixtures/autostart/service/station-to-carrier/remove-by-carrier-code/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testRemoveByCarrierCode() {
        stationToCarrierService.removeByCarrier("10");
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/service/station-to-carrier/common.xml")
    @ExpectedDatabase(
            value = "/fixtures/autostart/service/station-to-carrier/remove-by-stationkey-type/after.xml",
            assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    public void testRemoveByStationKeyAndType() {
        stationToCarrierService.removeByStationAndType("LOC-1", StationType.SORT);
    }
}
