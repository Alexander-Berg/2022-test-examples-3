package ru.yandex.market.wms.autostart.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;
import ru.yandex.market.wms.autostart.autostartlogic.nonsort.NonSortService;
import ru.yandex.market.wms.autostart.core.model.dto.StationType;
import ru.yandex.market.wms.autostart.model.entity.StationToCarrier;
import ru.yandex.market.wms.common.spring.dao.implementation.ConsolidationLocationDao;
import ru.yandex.market.wms.common.spring.dto.LocationBalanceDto;
import ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType;
import ru.yandex.market.wms.common.spring.enums.LinkedToDsType;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType.OVERSIZE;
import static ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType.SINGLES;

public class NonSortServiceTest extends AutostartIntegrationTest {

    @Autowired
    private NonSortService nonSortService;

    @Autowired
    private ConsolidationLocationDao consolidationLocationDao;

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/nonsort/consolidation-location/setup.xml")
    public void getLocationBalancesTest() {
        Map<ConsolidationLocationType, List<LocationBalanceDto>> balances =
                nonSortService.getConsolidationLocationBalances(null);

        assertThat(balances.get(OVERSIZE)).isNotEmpty();
        assertThat(balances.get(ConsolidationLocationType.SINGLES)).isNotEmpty();

        List<LocationBalanceDto> oversizeBalances = balances.get(OVERSIZE);
        assertThat(oversizeBalances.size()).isEqualTo(4);
        LocationBalanceDto firstLine = oversizeBalances.stream()
                .filter(a -> a.getLocation().equals("NS-CONS-1"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(""));

        LocationBalanceDto secondLine = oversizeBalances.stream()
                .filter(a -> a.getLocation().equals("NS-CONS-2"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(""));

        assertThat(firstLine.getQuantity()).isEqualTo(3);
        assertThat(secondLine.getQuantity()).isEqualTo(7);

        List<LocationBalanceDto> singlesBalances = balances.get(ConsolidationLocationType.SINGLES);
        assertThat(singlesBalances.size()).isEqualTo(1);
        assertThat(singlesBalances.get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/nonsort/consolidation-location/setup.xml")
    public void getLocationBalancesNotLinkedToDsMode() {
        StationToCarrierFilterByStation stationToCarrierFilterByStation =
                buildStationToCarrierFilter(LinkedToDsType.NO_LINK_TO_DS);
        Map<ConsolidationLocationType, List<LocationBalanceDto>> balances =
                nonSortService.getConsolidationLocationBalances(stationToCarrierFilterByStation, null);

        assertThat(balances.get(OVERSIZE)).isNotEmpty();
        assertThat(balances.get(ConsolidationLocationType.SINGLES)).isNotEmpty();

        List<LocationBalanceDto> oversizeBalances = balances.get(OVERSIZE);
        assertThat(oversizeBalances.size()).isEqualTo(4);
        LocationBalanceDto firstLine = oversizeBalances.stream()
                .filter(a -> a.getLocation().equals("NS-CONS-1"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(""));

        LocationBalanceDto secondLine = oversizeBalances.stream()
                .filter(a -> a.getLocation().equals("NS-CONS-2"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(""));

        assertThat(firstLine.getQuantity()).isEqualTo(3);
        assertThat(secondLine.getQuantity()).isEqualTo(7);

        List<LocationBalanceDto> singlesBalances = balances.get(ConsolidationLocationType.SINGLES);
        assertThat(singlesBalances.size()).isEqualTo(1);
        assertThat(singlesBalances.get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/nonsort/consolidation-location/setup.xml")
    public void getLocationBalancesToLinkedDsMode() {
        StationToCarrierFilterByStation stationToCarrierFilterByStation =
                buildStationToCarrierFilter(LinkedToDsType.TO_LINKED_DS);
        Map<ConsolidationLocationType, List<LocationBalanceDto>> balances =
                nonSortService.getConsolidationLocationBalances(stationToCarrierFilterByStation, null);

        List<LocationBalanceDto> oversizeBalances = balances.get(OVERSIZE);
        assertThat(oversizeBalances.size()).isEqualTo(2);
        LocationBalanceDto firstLine = oversizeBalances.stream()
                .filter(a -> a.getLocation().equals("NS-CONS-1"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(""));

        LocationBalanceDto secondLine = oversizeBalances.stream()
                .filter(a -> a.getLocation().equals("NS-CONS-2"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(""));

        assertThat(firstLine.getQuantity()).isEqualTo(3);
        assertThat(secondLine.getQuantity()).isEqualTo(7);

        List<LocationBalanceDto> singlesBalances = balances.get(ConsolidationLocationType.SINGLES);
        assertThat(singlesBalances.size()).isEqualTo(1);
        assertThat(singlesBalances.get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/nonsort/consolidation-location/setup.xml")
    public void getLocationBalancesToUnlinkedDsMode() {
        StationToCarrierFilterByStation stationToCarrierFilterByStation =
                buildStationToCarrierFilter(LinkedToDsType.TO_UNLINKED_DS);
        Map<ConsolidationLocationType, List<LocationBalanceDto>> balances =
                nonSortService.getConsolidationLocationBalances(stationToCarrierFilterByStation, null);

        List<LocationBalanceDto> oversizeBalances = balances.get(OVERSIZE);
        assertThat(oversizeBalances.size()).isEqualTo(2);
        LocationBalanceDto firstLine = oversizeBalances.stream()
                .filter(a -> a.getLocation().equals("NS-CONS-3"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(""));

        LocationBalanceDto secondLine = oversizeBalances.stream()
                .filter(a -> a.getLocation().equals("NS-CONS-4"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(""));

        assertThat(firstLine.getQuantity()).isEqualTo(0);
        assertThat(secondLine.getQuantity()).isEqualTo(0);

        List<LocationBalanceDto> singlesBalances = balances.get(ConsolidationLocationType.SINGLES);
        assertThat(singlesBalances).isNull();
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/nonsort/consolidation-location/setup.xml")
    @DatabaseSetup(value = "/fixtures/autostart/nonsort/consolidation-location/oversize-on.xml")
    public void canReserveTest() {
        var balances = nonSortService.getConsolidationLocationBalances(null);
        assertThat(balances).containsKey(OVERSIZE);
        assertThat(balances).containsKey(SINGLES);
        var reserver = nonSortService.getAosReserver(null);
        assertThat(reserver.tryReserve(OVERSIZE, 1)).isTrue();
        assertThat(reserver.tryReserve(SINGLES, 1)).isFalse();
        assertThat(reserver.forceReserve(OVERSIZE, 1)).isPresent();
        assertThat(reserver.forceReserve(SINGLES, 1)).isEmpty();
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/nonsort/consolidation-location/setup.xml")
    public void canNotReserveTest() {
        var balances = nonSortService.getConsolidationLocationBalances(null);
        assertThat(balances).containsKey(OVERSIZE);
        assertThat(balances).containsKey(SINGLES);
        var reserver = nonSortService.getAosReserver(null);
        assertThat(reserver.tryReserve(OVERSIZE, 1)).isFalse();
        assertThat(reserver.tryReserve(SINGLES, 1)).isFalse();
        assertThat(reserver.forceReserve(OVERSIZE, 1)).isEmpty();
        assertThat(reserver.forceReserve(SINGLES, 1)).isEmpty();
    }

    @Test
    @DatabaseSetup(value = "/fixtures/autostart/nonsort/consolidation-location/no-avaliable-pickdetails.xml")
    public void noAvailablePickDetailsTest() {
        assertThat(nonSortService.getConsolidationLocationBalances(null).keySet().size()).isEqualTo(2);
        nonSortService.getConsolidationLocationBalances(null).forEach((type, balances) -> {
            assertThat(balances).isNotEmpty();
            balances.forEach(balance -> assertThat(balance.getQuantity()).isEqualTo(0));
        });
    }

    @Test
    public void noDataTest() {
        assertThat(nonSortService.getConsolidationLocationBalances(null).entrySet()).isEmpty();
        Arrays.stream(ConsolidationLocationType.values()).forEach(type ->
                assertThat(consolidationLocationDao.getConsolidationLocationsWithType(null, type)).isEmpty()
        );
    }

    private StationToCarrierFilterByStation buildStationToCarrierFilter(LinkedToDsType linkedToDsType) {
        List<StationToCarrier> stationToCarriers = List.of(
                StationToCarrier.builder()
                        .carrierCode("C1").stationKey("NS-CONS-1").type(StationType.CONSOLIDATION).build(),
                StationToCarrier.builder()
                        .carrierCode("C1").stationKey("NS-CONS-2").type(StationType.CONSOLIDATION).build(),
                StationToCarrier.builder()
                        .carrierCode("C1").stationKey("SORT-1").type(StationType.SORT).build()
        );
        return new StationToCarrierFilterByStation(stationToCarriers, linkedToDsType, "C1");
    }
}
