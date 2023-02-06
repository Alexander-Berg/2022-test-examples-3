package ru.yandex.market.wms.autostart.strategy;

import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.autostartlogic.service.SortingStationService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStation;
import ru.yandex.market.wms.autostart.exception.SortStationNotEnoughForWaveException;
import ru.yandex.market.wms.autostart.exception.SortStationNotFoundException;
import ru.yandex.market.wms.autostart.exception.SortStationSelectionException;
import ru.yandex.market.wms.autostart.strategy.manual.waveprocessing.sortstationselect.WithdrawalSortStationSelectionStrategy;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Wave;
import ru.yandex.market.wms.common.spring.dto.AutoStartSortingStationDto;
import ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WithdrawalSortStationSelectionStrategyTest extends BaseTest {

    private static final String EMPTY_STRING = "";

    @Test
    public void forceEmptyStationByEmptyStationName() {
        var strat = new WithdrawalSortStationSelectionStrategy(null);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        strat.select(waves, EMPTY_STRING, true);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isEqualTo(EMPTY_STRING);
        assertions.assertThat(waves.get(1).getSortationStationKey()).isEqualTo(EMPTY_STRING);
    }

    @Test
    public void forceEmptyStationByBlankStationName() {
        var strat = new WithdrawalSortStationSelectionStrategy(null);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        strat.select(waves, "     ", true);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isEqualTo(EMPTY_STRING);
        assertions.assertThat(waves.get(1).getSortationStationKey()).isEqualTo(EMPTY_STRING);
    }

    @Test
    public void forceEmptyStationByNullStationName() {
        var strat = new WithdrawalSortStationSelectionStrategy(null);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        strat.select(waves, null, true);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isEqualTo(EMPTY_STRING);
        assertions.assertThat(waves.get(1).getSortationStationKey()).isEqualTo(EMPTY_STRING);
    }

    @Test
    public void forceStation() {
        final String station = "forced_station";
        var sortingStationService = mock(SortingStationService.class);
        when(sortingStationService.getStation(anyString()))
                .thenReturn(createStation(station, AutoStartSortingStationMode.WITHDRAWALS));
        when(sortingStationService.getModeForWaveType(WaveType.WITHDRAWAL))
                .thenReturn(AutoStartSortingStationMode.WITHDRAWALS);
        var strat = new WithdrawalSortStationSelectionStrategy(sortingStationService);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        strat.select(waves, station, true);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isEqualTo(station);
        assertions.assertThat(waves.get(1).getSortationStationKey()).isEqualTo(station);
    }

    @Test
    public void forceNonExistingStation() {
        final String station = "forced_station";
        var sortingStationService = mock(SortingStationService.class);
        when(sortingStationService.getStation(anyString()))
                .thenReturn(null);
        when(sortingStationService.getModeForWaveType(WaveType.WITHDRAWAL))
                .thenReturn(AutoStartSortingStationMode.WITHDRAWALS);
        var strat = new WithdrawalSortStationSelectionStrategy(sortingStationService);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        assertions.assertThatThrownBy(() -> strat.select(waves, station, true))
                .isExactlyInstanceOf(SortStationNotFoundException.class)
                .hasMessage("400 BAD_REQUEST \"Указанная станция forced_station не найдена\"");
        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();
    }

    @Test
    public void specifyStationWithoutForce() {
        final String station = "forced_station";
        var strat = new WithdrawalSortStationSelectionStrategy(null);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        assertions.assertThatThrownBy(() -> strat.select(waves, station, false))
                .isExactlyInstanceOf(SortStationSelectionException.class)
                .hasMessage("400 BAD_REQUEST \"Указана станция forced_station без принудительного запуска\"");
        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();
    }

    @Test
    public void notEnoughStations() {
        var sortingStationService = mock(SortingStationService.class);
        when(sortingStationService.getModeForWaveType(WaveType.WITHDRAWAL))
                .thenReturn(AutoStartSortingStationMode.WITHDRAWALS);
        when(sortingStationService.stationsAndUsableSlots(true, AutoStartSortingStationMode.WITHDRAWALS))
                .thenReturn(new LinkedHashSet<>(List.of(station("station1", 0))));
        var strat = new WithdrawalSortStationSelectionStrategy(sortingStationService);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        assertions.assertThatThrownBy(() -> strat.select(waves, null, false))
                .isExactlyInstanceOf(SortStationNotEnoughForWaveException.class)
                .hasMessage("400 BAD_REQUEST \"Недостаточно свободных станций сортировки\"");
        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();
    }

    @Test
    public void enoughStations() {
        var sortingStationService = mock(SortingStationService.class);
        when(sortingStationService.getModeForWaveType(WaveType.WITHDRAWAL))
                .thenReturn(AutoStartSortingStationMode.WITHDRAWALS);
        var stations = new LinkedHashSet<>(
                List.of(station("station1", 0), station("station2", 1)));
        when(sortingStationService.stationsAndUsableSlots(true, AutoStartSortingStationMode.WITHDRAWALS))
                .thenReturn(stations);
        var strat = new WithdrawalSortStationSelectionStrategy(sortingStationService);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        strat.select(waves, null, false);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isEqualTo("station1");
        assertions.assertThat(waves.get(1).getSortationStationKey()).isEqualTo("station2");
    }

    @Test
    public void preSelectStationsHappyPass() {
        var sortingStationService = mock(SortingStationService.class);
        when(sortingStationService.getModeForWaveType(WaveType.WITHDRAWAL))
                .thenReturn(AutoStartSortingStationMode.WITHDRAWALS);
        when(sortingStationService.stationsSortedByOccupancy(AutoStartSortingStationMode.WITHDRAWALS))
                .thenReturn(List.of("station1", "station2"));
        var strategy = new WithdrawalSortStationSelectionStrategy(sortingStationService);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        strategy.preSelect(waves);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isEqualTo("station1");
        assertions.assertThat(waves.get(1).getSortationStationKey()).isEqualTo("station2");
    }

    private static Wave createWave(String waveKey) {
        return Wave.builder()
                .waveKey(waveKey)
                .build();
    }

    private static AutoStartSortingStationDto createStation(String name, AutoStartSortingStationMode mode) {
        return AutoStartSortingStationDto.builder()
                .station(name)
                .mode(mode)
                .build();
    }

    private CandidateSortStation station(String name, int capacity) {
        return CandidateSortStation.builder().name(name).capacity(capacity).build();
    }
}
