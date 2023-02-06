package ru.yandex.market.wms.autostart.strategy;

import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.wms.autostart.strategy.manual.waveprocessing.sortstationselect.NonSortWaveSortStationSelectionStrategy;
import ru.yandex.market.wms.common.spring.BaseTest;
import ru.yandex.market.wms.common.spring.dao.entity.Wave;
import ru.yandex.market.wms.common.spring.enums.WaveType;

public class NonSortWaveSortStationSelectionStrategyTest extends BaseTest {

    private static final String EMPTY_STRING = "";

    @Test
    public void specifyStationWithoutForce() {
        var strat = new NonSortWaveSortStationSelectionStrategy();
        var waves = List.of(createSingleWave("wave1"), createSingleWave("wave2"));

        strat.select(waves, "Some_station_key", false);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();
    }

    @Test
    public void specifyEmptyStationKeyWithoutForce() {
        var strat = new NonSortWaveSortStationSelectionStrategy();
        var waves = List.of(createSingleWave("wave1"), createSingleWave("wave2"));

        strat.select(waves, EMPTY_STRING, false);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();
    }

    @Test
    public void specifyNullStationKeyWithoutForce() {
        var strat = new NonSortWaveSortStationSelectionStrategy();
        var waves = List.of(createSingleWave("wave1"), createSingleWave("wave2"));

        strat.select(waves, null, false);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();
    }

    @Test
    public void forceEmptyStationByEmptyStationName() {
        var strat = new NonSortWaveSortStationSelectionStrategy();
        var waves = List.of(createSingleWave("wave1"), createSingleWave("wave2"));

        strat.select(waves, EMPTY_STRING, true);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();
    }

    @Test
    public void forceNullStationByEmptyStationName() {
        var strat = new NonSortWaveSortStationSelectionStrategy();
        var waves = List.of(createSingleWave("wave1"), createSingleWave("wave2"));

        strat.select(waves, null, true);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();
    }

    @Test
    public void forcedStationDoesNotExist() {
        var strat = new NonSortWaveSortStationSelectionStrategy();
        var waves = List.of(createSingleWave("wave1"), createSingleWave("wave2"));

        strat.select(waves, "some_station", true);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();
    }

    @Test
    public void forceStation() {
        final String station = "forced_station";
        var strat = new NonSortWaveSortStationSelectionStrategy();
        var waves = List.of(createSingleWave("wave1"), createSingleWave("wave2"));
        for (var wave : waves) {
            wave.setComment("INITIAL COMMENT");
        }

        strat.select(waves, station, true);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();

        assertions.assertThat(waves.get(0).getWaveType()).isEqualTo(WaveType.SINGLE);
        assertions.assertThat(waves.get(1).getWaveType()).isEqualTo(WaveType.SINGLE);

        assertions.assertThat(waves.get(0).getComment()).isEqualTo("INITIAL COMMENT");
        assertions.assertThat(waves.get(1).getComment()).isEqualTo("INITIAL COMMENT");
    }

    @Test
    public void preSelectStationsHappyPass() {
        var strategy = new NonSortWaveSortStationSelectionStrategy();
        var waves = List.of(createSingleWave("wave1"), createSingleWave("wave2"));

        strategy.preSelect(waves);

        assertions.assertThat(waves.get(0).getSortationStationKey()).isNull();
        assertions.assertThat(waves.get(1).getSortationStationKey()).isNull();
    }

    private static Wave createSingleWave(String waveKey) {
        return Wave.builder()
                .waveKey(waveKey)
                .waveType(WaveType.SINGLE)
                .build();
    }
}
