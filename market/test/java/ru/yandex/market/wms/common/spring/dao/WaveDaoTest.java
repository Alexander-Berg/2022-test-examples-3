package ru.yandex.market.wms.common.spring.dao;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.WaveInProcessStatus;
import ru.yandex.market.wms.common.spring.IntegrationTest;
import ru.yandex.market.wms.common.spring.dao.entity.Wave;
import ru.yandex.market.wms.common.spring.dao.implementation.WaveDao;
import ru.yandex.market.wms.common.spring.enums.StartType;
import ru.yandex.market.wms.common.spring.enums.WaveState;
import ru.yandex.market.wms.common.spring.enums.WaveType;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DatabaseSetup("/db/dao/wave/data.xml")
public class WaveDaoTest extends IntegrationTest {

    @Autowired
    private WaveDao dao;

    @Test
    public void getWaveTypeHappyAll() {
        Optional<WaveType> type = dao.findWaveTypeByWaveKey("123");
        assertions.assertThat(type).isPresent();
        assertions.assertThat(type.get()).isEqualTo(WaveType.ALL);
    }

    @Test
    public void getWaveTypeHappySingle() {
        Optional<WaveType> type = dao.findWaveTypeByWaveKey("124");
        assertions.assertThat(type).isPresent();
        assertions.assertThat(type.get()).isEqualTo(WaveType.SINGLE);
    }

    @Test
    public void getWaveTypeNull() {
        Optional<WaveType> type = dao.findWaveTypeByWaveKey("126");
        assertions.assertThat(type).isEmpty();
    }

    @Test
    public void getWaveTypeNotExisting() {
        Optional<WaveType> type = dao.findWaveTypeByWaveKey("125");
        assertions.assertThat(type).isEmpty();
    }

    @Test
    @DatabaseSetup("/db/dao/wave/3/before.xml")
    @ExpectedDatabase(value = "/db/dao/wave/3/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void createWaves() {
        Wave wave = Wave.builder()
                .waveKey("127")
                .batchKey("B000334")
                .inProcessStatus(WaveInProcessStatus.RESERVATION_COMPLETED)
                .state(WaveState.ALLOCATED)
                .sortationStationKey("S01")
                .startType(StartType.DEFAULT)
                .waveType(WaveType.ALL)
                .comment("comment")
                .build();

        dao.create(Collections.singletonList(wave), "TEST");
    }

    @Test
    @DatabaseSetup("/db/dao/wave/1/before.xml")
    @ExpectedDatabase(value = "/db/dao/wave/1/after.xml", assertionMode = NON_STRICT_UNORDERED)
    public void updateWaves() {
        Wave wave = Wave.builder()
                .waveKey("127")
                .batchKey("B000334")
                .inProcessStatus(WaveInProcessStatus.RESERVATION_COMPLETED)
                .state(WaveState.ALLOCATED)
                .sortationStationKey("S01")
                .startType(StartType.DEFAULT)
                .waveType(WaveType.ALL)
                .comment("modified comment")
                .build();

        dao.updateWaves(Collections.singletonList(wave), "TEST");
    }

    @Test
    @DatabaseSetup("/db/dao/wave/2/immutable.xml")
    @ExpectedDatabase(value = "/db/dao/wave/2/immutable.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getWavesByKeys() {
        List<Wave> actualWaves =  dao.findWavesByKeys(List.of("127", "128"));

        assertSoftly(assertions -> {
            assertions.assertThat(actualWaves.size()).isEqualTo(2);

            Wave firstWave = actualWaves.get(0);
            assertions.assertThat(firstWave.getWaveKey()).isEqualTo("127");
            assertions.assertThat(firstWave.getBatchKey()).isNull();
            assertions.assertThat(firstWave.getSortationStationKey()).isNull();
            assertions.assertThat(firstWave.getState()).isEqualTo(WaveState.NOT_STARTED);
            assertions.assertThat(firstWave.getState()).isEqualTo(WaveState.NOT_STARTED);
            assertions.assertThat(firstWave.getInProcessStatus()).isEqualTo(WaveInProcessStatus.CREATED);
            assertions.assertThat(firstWave.getWaveType()).isEqualTo(WaveType.ALL);
            assertions.assertThat(firstWave.getStartType()).isEqualTo(StartType.DEFAULT);

            Wave secondWave = actualWaves.get(1);
            assertions.assertThat(secondWave.getWaveKey()).isEqualTo("128");
            assertions.assertThat(secondWave.getBatchKey()).isEqualTo("B000334");
            assertions.assertThat(secondWave.getSortationStationKey()).isEqualTo("S01");
            assertions.assertThat(secondWave.getState()).isEqualTo(WaveState.ALLOCATED);
            assertions.assertThat(secondWave.getInProcessStatus()).isEqualTo(WaveInProcessStatus.RESERVATION_COMPLETED);
            assertions.assertThat(secondWave.getWaveType()).isEqualTo(WaveType.WITHDRAWAL);
            assertions.assertThat(secondWave.getStartType()).isEqualTo(StartType.MANUAL);
        });
    }

    @Test
    @DatabaseSetup("/db/dao/wave/4/data.xml")
    @ExpectedDatabase(value = "/db/dao/wave/4/data.xml", assertionMode = NON_STRICT_UNORDERED)
    public void getWavesCount() {
        int withStation = dao.getWavesCount(WaveState.ACTIVE_STATES, Set.of(WaveType.ALL), true);
        int withOutStation = dao.getWavesCount(WaveState.ACTIVE_STATES, Set.of(WaveType.ALL), false);
        assertions.assertThat(withStation).isEqualTo(3);
        assertions.assertThat(withOutStation).isEqualTo(3);
    }
}
