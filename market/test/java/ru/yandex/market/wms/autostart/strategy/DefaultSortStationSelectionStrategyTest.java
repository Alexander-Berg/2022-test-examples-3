package ru.yandex.market.wms.autostart.strategy;

import java.util.LinkedHashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.autostartlogic.service.SortingStationService;
import ru.yandex.market.wms.autostart.autostartlogic.waves2.CandidateSortStation;
import ru.yandex.market.wms.autostart.exception.SortStationNotEnoughForWaveException;
import ru.yandex.market.wms.autostart.exception.SortStationNotFoundException;
import ru.yandex.market.wms.autostart.exception.SortStationSelectionException;
import ru.yandex.market.wms.autostart.strategy.manual.waveprocessing.sortstationselect.DefaultSortStationSelectionStrategy;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Wave;
import ru.yandex.market.wms.common.spring.dto.AutoStartSortingStationDto;
import ru.yandex.market.wms.common.spring.enums.AutoStartSortingStationMode;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultSortStationSelectionStrategyTest extends BaseTest {

    private static final String EMPTY_STRING = "";

    @Test
    public void forceEmptyStationByEmptyStationName() {
        var strat = new DefaultSortStationSelectionStrategy(null);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        strat.select(waves, EMPTY_STRING, true);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isEqualTo(EMPTY_STRING);
        assertions.assertThat(waves.get(1).getSortationStationKey()).isEqualTo(EMPTY_STRING);
    }

    @Test
    public void forceEmptyStationByBlankStationName() {
        var strat = new DefaultSortStationSelectionStrategy(null);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        strat.select(waves, "     ", true);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isEqualTo(EMPTY_STRING);
        assertions.assertThat(waves.get(1).getSortationStationKey()).isEqualTo(EMPTY_STRING);
    }

    @Test
    public void forceEmptyStationByNullStationName() {
        var strat = new DefaultSortStationSelectionStrategy(null);
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
                .thenReturn(createStation(station, AutoStartSortingStationMode.ORDERS));
        when(sortingStationService.getModeForWaveType(WaveType.ALL))
                .thenReturn(AutoStartSortingStationMode.ORDERS);
        var strat = new DefaultSortStationSelectionStrategy(sortingStationService);
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
        when(sortingStationService.getModeForWaveType(WaveType.ALL))
                .thenReturn(AutoStartSortingStationMode.ORDERS);
        var strat = new DefaultSortStationSelectionStrategy(sortingStationService);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        assertions.assertThatThrownBy(() -> strat.select(waves, station, true))
                .isExactlyInstanceOf(SortStationNotFoundException.class)
                .hasMessage("400 BAD_REQUEST \"?????????????????? ?????????????? forced_station ???? ??????????????\"");
        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();
    }

    @Test
    public void specifyStationWithoutForce() {
        final String station = "forced_station";
        var strat = new DefaultSortStationSelectionStrategy(null);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        assertions.assertThatThrownBy(() -> strat.select(waves, station, false))
                .isExactlyInstanceOf(SortStationSelectionException.class)
                .hasMessage("400 BAD_REQUEST \"?????????????? ?????????????? forced_station ?????? ?????????????????????????????? ??????????????\"");
        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();
    }

    @Test
    public void notEnoughStations() {
        var sortingStationService = mock(SortingStationService.class);
        when(sortingStationService.getModeForWaveType(WaveType.ALL))
                .thenReturn(AutoStartSortingStationMode.ORDERS);
        when(sortingStationService.stationsAndUsableSlots(true, AutoStartSortingStationMode.ORDERS))
                .thenReturn(new LinkedHashSet<>(List.of(station("station1", 0))));
        var strat = new DefaultSortStationSelectionStrategy(sortingStationService);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        assertions.assertThatThrownBy(() -> strat.select(waves, null, false))
                .isExactlyInstanceOf(SortStationNotEnoughForWaveException.class)
                .hasMessage("400 BAD_REQUEST \"???????????????????????? ?????????????????? ?????????????? ????????????????????\"");
        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();
    }

    @Test
    public void enoughStations() {
        var sortingStationService = mock(SortingStationService.class);
        when(sortingStationService.getModeForWaveType(WaveType.ALL))
                .thenReturn(AutoStartSortingStationMode.ORDERS);
        var stations = new LinkedHashSet<>(List.of(
                station("station1", 0), station("station2", 1)));
        when(sortingStationService.stationsAndUsableSlots(false, AutoStartSortingStationMode.ORDERS))
                .thenReturn(stations);
        var strategy = new DefaultSortStationSelectionStrategy(sortingStationService);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        strategy.select(waves, null, false);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isEqualTo("station1");
        assertions.assertThat(waves.get(1).getSortationStationKey()).isEqualTo("station2");
    }

    @Test
    public void preSelectStationsHappyPass() {
        var sortingStationService = mock(SortingStationService.class);
        when(sortingStationService.getModeForWaveType(WaveType.ALL))
                .thenReturn(AutoStartSortingStationMode.ORDERS);
        when(sortingStationService.stationsSortedByOccupancy(AutoStartSortingStationMode.ORDERS))
                .thenReturn(List.of("station2", "station1"));
        var strategy = new DefaultSortStationSelectionStrategy(sortingStationService);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        strategy.preSelect(waves);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isEqualTo("station2");
        assertions.assertThat(waves.get(1).getSortationStationKey()).isEqualTo("station1");
    }

    @Test
    public void preSelectNotEnoughStations() {
        var sortingStationService = mock(SortingStationService.class);
        when(sortingStationService.getModeForWaveType(WaveType.ALL))
                .thenReturn(AutoStartSortingStationMode.ORDERS);
        when(sortingStationService.stationsSortedByOccupancy(AutoStartSortingStationMode.ORDERS))
                .thenReturn(List.of("station1"));
        var strategy = new DefaultSortStationSelectionStrategy(sortingStationService);
        var waves = List.of(createWave("wave1"), createWave("wave2"));

        assertions.assertThatThrownBy(() -> strategy.preSelect(waves))
                .isExactlyInstanceOf(SortStationNotEnoughForWaveException.class)
                .hasMessage("400 BAD_REQUEST \"???????????????????????? ?????????????????? ?????????????? ????????????????????\"");
        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();
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
