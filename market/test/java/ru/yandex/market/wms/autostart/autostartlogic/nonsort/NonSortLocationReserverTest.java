package ru.yandex.market.wms.autostart.autostartlogic.nonsort;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.common.spring.dto.LocationBalanceDto;
import ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType;

import static ru.yandex.market.wms.common.spring.enums.ConsolidationLocationType.OVERSIZE;

public class NonSortLocationReserverTest {

    @Test
    public void fairDistribution() {
        Map<ConsolidationLocationType, List<LocationBalanceDto>> balances = new HashMap<>();

        List<LocationBalanceDto> locBalances = new ArrayList<>();
        locBalances.add(new LocationBalanceDto("A", 1));
        locBalances.add(new LocationBalanceDto("B", 2));
        locBalances.add(new LocationBalanceDto("C", 3));

        balances.put(OVERSIZE, locBalances);
        NonSortLocationReserver reserver = new NonSortLocationReserver(balances, 5);

        int refCapacity = 5 * locBalances.size() - locBalances.stream().mapToInt(LocationBalanceDto::getQuantity).sum();
        Assertions.assertThat(reserver.getCapacity(OVERSIZE)).isEqualTo(refCapacity);

        for (int i = 0; i < 6; i++) {
            Assertions.assertThat(reserver.tryReserve(OVERSIZE, 1)).isTrue();
        }

        List<String> reservedLocs = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            reservedLocs.add(reserver.getReserved(OVERSIZE, 1).get());
        }

        Assertions.assertThat(reservedLocs).containsExactlyInAnyOrder("A", "A", "A", "B", "B", "C");
    }

    @Test
    public void fairDistributionWithCommonLocations() {
        Map<ConsolidationLocationType, List<LocationBalanceDto>> balances = new HashMap<>();

        List<LocationBalanceDto> locBalances = new ArrayList<>();
        locBalances.add(new LocationBalanceDto("A", 1));
        locBalances.add(new LocationBalanceDto("B", 2));
        locBalances.add(new LocationBalanceDto("C", 3));

        List<LocationBalanceDto> locBalances2 = new ArrayList<>();
        locBalances2.add(new LocationBalanceDto("A", 1));
        locBalances2.add(new LocationBalanceDto("B", 2));
        locBalances2.add(new LocationBalanceDto("C", 3));

        balances.put(OVERSIZE, locBalances);
        balances.put(ConsolidationLocationType.SINGLES, locBalances2);
        NonSortLocationReserver reserver = new NonSortLocationReserver(balances, 5);
        for (int i = 0; i < 3; i++) {
            Assertions.assertThat(reserver.tryReserve(OVERSIZE, 1)).isTrue();
            Assertions.assertThat(reserver.tryReserve(ConsolidationLocationType.SINGLES, 1)).isTrue();
        }

        List<String> reservedLocs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            reservedLocs.add(reserver.getReserved(OVERSIZE, 1).get());
            reservedLocs.add(reserver.getReserved(ConsolidationLocationType.SINGLES, 1).get());
        }

        Assertions.assertThat(reservedLocs).containsExactlyInAnyOrder("A", "A", "A", "B", "B", "C");
    }

    @Test
    public void fairDistributionWithCommonAndNotCommonLocations() {
        Map<ConsolidationLocationType, List<LocationBalanceDto>> balances = new HashMap<>();

        List<LocationBalanceDto> locBalances = new ArrayList<>();
        locBalances.add(new LocationBalanceDto("A", 10));
        locBalances.add(new LocationBalanceDto("B", 12));
        locBalances.add(new LocationBalanceDto("C", 30));

        List<LocationBalanceDto> locBalances2 = new ArrayList<>();
        locBalances2.add(new LocationBalanceDto("A", 10));
        locBalances2.add(new LocationBalanceDto("D", 2));
        locBalances2.add(new LocationBalanceDto("E", 4));

        balances.put(OVERSIZE, locBalances);
        balances.put(ConsolidationLocationType.SINGLES, locBalances2);
        NonSortLocationReserver reserver = new NonSortLocationReserver(balances, 50);
        for (int i = 0; i < 4; i++) {
            Assertions.assertThat(reserver.tryReserve(OVERSIZE, 1)).isTrue();
            Assertions.assertThat(reserver.tryReserve(ConsolidationLocationType.SINGLES, 1)).isTrue();
        }

        List<String> reservedLocs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            reservedLocs.add(reserver.getReserved(OVERSIZE, 1).get());
            reservedLocs.add(reserver.getReserved(ConsolidationLocationType.SINGLES, 1).get());
        }

        Assertions.assertThat(reservedLocs).containsExactlyInAnyOrder("A", "A", "B", "A", "D", "D", "D", "E");
    }

    @Test
    public void noLocationsByType() {
        Map<ConsolidationLocationType, List<LocationBalanceDto>> balances = new HashMap<>();

        List<LocationBalanceDto> locBalances = new ArrayList<>();
        locBalances.add(new LocationBalanceDto("A", 10));
        locBalances.add(new LocationBalanceDto("B", 20));
        locBalances.add(new LocationBalanceDto("C", 30));
        balances.put(OVERSIZE, locBalances);
        NonSortLocationReserver reserver = new NonSortLocationReserver(balances, 10);
        for (int i = 0; i < 3; i++) {
            Assertions.assertThat(reserver.tryReserve(ConsolidationLocationType.SINGLES, 1)).isFalse();
            Assertions.assertThat(reserver.tryReserve(ConsolidationLocationType.WITHDRAWAL, 1)).isFalse();
        }

        for (int i = 0; i < 3; i++) {
            Assertions.assertThat(reserver.getReserved(ConsolidationLocationType.SINGLES, 1)).isEmpty();
            Assertions.assertThat(reserver.getReserved(ConsolidationLocationType.WITHDRAWAL, 1)).isEmpty();
        }
    }

    @Test
    public void allToLocationWithMaxCapacity() {
        Map<ConsolidationLocationType, List<LocationBalanceDto>> balances = new HashMap<>();

        List<LocationBalanceDto> locBalances = new ArrayList<>();
        locBalances.add(new LocationBalanceDto("A", 10));
        locBalances.add(new LocationBalanceDto("B", 15));
        locBalances.add(new LocationBalanceDto("C", 25));
        balances.put(OVERSIZE, locBalances);
        NonSortLocationReserver reserver = new NonSortLocationReserver(balances, 30);
        for (int i = 0; i < 5; i++) {
            Assertions.assertThat(reserver.tryReserve(OVERSIZE, 1)).isTrue();
        }

        List<String> reservedLocs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            reservedLocs.add(reserver.getReserved(OVERSIZE, 1).get());
        }

        Assertions.assertThat(reservedLocs).containsExactlyInAnyOrder("A", "A", "A", "A", "A");
    }

    @Test
    public void forceReserveTest() {
        Map<ConsolidationLocationType, List<LocationBalanceDto>> balances = new HashMap<>();

        List<LocationBalanceDto> locBalances = new ArrayList<>();
        locBalances.add(new LocationBalanceDto("A", 10));
        locBalances.add(new LocationBalanceDto("B", 15));
        locBalances.add(new LocationBalanceDto("C", 25));
        balances.put(OVERSIZE, locBalances);

        NonSortLocationReserver reserver = new NonSortLocationReserver(balances, 30);
        for (int i = 0; i < 20; i++) {
            Optional<String> reserved = reserver.forceReserve(OVERSIZE, 100);
            Assertions.assertThat(reserved).isPresent();
            Assertions.assertThat(reserved.get()).isEqualTo(locBalances.get(i % locBalances.size()).getLocation());
        }

    }
}
