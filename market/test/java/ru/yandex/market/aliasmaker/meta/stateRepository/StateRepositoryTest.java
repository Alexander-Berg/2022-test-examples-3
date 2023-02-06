package ru.yandex.market.aliasmaker.meta.stateRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.aliasmaker.meta.IntegrationTests.IntegrationTestConfig;
import ru.yandex.market.aliasmaker.meta.be.CurrentStateResponse;
import ru.yandex.market.aliasmaker.meta.be.ShardInfo;
import ru.yandex.market.aliasmaker.meta.be.ShardKey;
import ru.yandex.market.aliasmaker.meta.be.ShardSetSnapshot;
import ru.yandex.market.aliasmaker.meta.be.ShardSetState;
import ru.yandex.market.aliasmaker.meta.peristence.IMetaStateDAO;
import ru.yandex.market.mboc.common.utils.PGaaSZonkyInitializer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = IntegrationTestConfig.class,
        initializers = PGaaSZonkyInitializer.class)
@Transactional
public class StateRepositoryTest {
    @Autowired
    private IMetaStateDAO stateRepository;
    private static final String TEST_DC_NAME = "vlu";
    private static final String TEST_SERVICE_NAME = "testServiceName";

    @Test
    public void canAddState() {
        ShardSetSnapshot snapshotTest = getSnapshotForTest(TEST_SERVICE_NAME);
        stateRepository.addState(snapshotTest);
        var result = stateRepository.getCurrentSnapshot(true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).isEqualToComparingFieldByFieldRecursively(snapshotTest);
    }

    @Test
    public void whenAddingMoreThanOneStateThenLastIsUsed() {
        ShardSetSnapshot snapshotTest = getSnapshotForTest(TEST_SERVICE_NAME);
        ShardSetSnapshot snapshotTest2 = getSnapshotForTest(TEST_SERVICE_NAME);
        stateRepository.addState(snapshotTest);
        stateRepository.addState(snapshotTest2);
        var result = stateRepository.getCurrentSnapshot(true);
        Assertions.assertThat(result).isNotNull();
        Assertions.assertThat(result).isEqualToComparingFieldByFieldRecursively(snapshotTest2);
    }

    @Test
    public void cantAddDisabledState() {
        var snapshot = getSnapshotForTest(TEST_SERVICE_NAME);

        stateRepository.addState(snapshot);
        Assertions.assertThat(stateRepository.getCommonState().getState().containsKey(TEST_SERVICE_NAME)).isTrue();

        stateRepository.manuallySwitchByName(TEST_SERVICE_NAME, false);
        Assertions.assertThat(stateRepository.getCommonState().getState().containsKey(TEST_SERVICE_NAME)).isFalse();

        stateRepository.addState(getSnapshotForTest(TEST_SERVICE_NAME));
        Assertions.assertThat(stateRepository.getCommonState().getState().containsKey(TEST_SERVICE_NAME)).isFalse();
    }

    @Test
    public void canDisableByName() {
        var snapshot = getSnapshotForTest(TEST_SERVICE_NAME);

        stateRepository.addState(snapshot);
        Assertions.assertThat(stateRepository.getCurrentSnapshot(true)).isNotNull();

        stateRepository.manuallySwitchByName(TEST_SERVICE_NAME, false);
        Assertions.assertThat(stateRepository.getCommonState().getState().containsKey(TEST_SERVICE_NAME)).isFalse();
    }

    @Test
    public void canDisableByDCName() {
        var snapshot = getSnapshotForTest(TEST_SERVICE_NAME, TEST_DC_NAME);

        stateRepository.addState(snapshot);
        Assertions.assertThat(stateRepository.getCurrentSnapshot(true)).isNotNull();

        stateRepository.manuallySwitchByDCName(TEST_DC_NAME, false);
        Assertions.assertThat(stateRepository.getCommonState().getState().containsKey(TEST_SERVICE_NAME)).isFalse();
    }

    @Test
    public void canEnableByName() {
        var snapshot = getSnapshotForTest(TEST_SERVICE_NAME);

        stateRepository.addState(snapshot);
        stateRepository.manuallySwitchByName(TEST_SERVICE_NAME, false);
        Assertions.assertThat(stateRepository.getCommonState().getState().containsKey(TEST_SERVICE_NAME)).isFalse();

        stateRepository.manuallySwitchByName(TEST_SERVICE_NAME, true);
        Assertions.assertThat(stateRepository.getCommonState().getState().containsKey(TEST_SERVICE_NAME)).isTrue();
    }

    @Test
    public void canEnableByDCName() {
        var snapshot = getSnapshotForTest(TEST_SERVICE_NAME, TEST_DC_NAME);

        stateRepository.addState(snapshot);
        stateRepository.manuallySwitchByDCName(TEST_DC_NAME, false);
        Assertions.assertThat(stateRepository.getCommonState().getState().containsKey(TEST_SERVICE_NAME)).isFalse();

        stateRepository.manuallySwitchByDCName(TEST_DC_NAME, true);
        Assertions.assertThat(stateRepository.getCommonState().getState().containsKey(TEST_SERVICE_NAME)).isTrue();
    }

    @Test
    public void canResetState() {
        var snapshot = getSnapshotForTest(TEST_SERVICE_NAME);

        stateRepository.addState(snapshot);
        stateRepository.manuallySwitchByName(TEST_SERVICE_NAME, false);
        Assertions.assertThat(stateRepository.getCommonState().getState().containsKey(TEST_SERVICE_NAME)).isFalse();

        stateRepository.addState(getSnapshotForTest(TEST_SERVICE_NAME));
        Assertions.assertThat(stateRepository.getCommonState().getState().containsKey(TEST_SERVICE_NAME)).isFalse();

        stateRepository.resetState();
        stateRepository.addState(getSnapshotForTest(TEST_SERVICE_NAME));
        Assertions.assertThat(stateRepository.getCommonState().getState().containsKey(TEST_SERVICE_NAME)).isTrue();
    }

    private ShardSetSnapshot getSnapshotForTest(String serviceName) {
        return getSnapshotForTest(serviceName, "testDC");
    }

    private ShardSetSnapshot getSnapshotForTest(String serviceName, String dcName) {
        List<String> itags = new Vector<>();
        itags.add("a_dc_" + dcName);

        Map<ShardKey, ShardInfo> pair = new HashMap<>();
        Map<String, Map<ShardKey, ShardInfo>> shardState = new HashMap<>();
        pair.put(new ShardKey("testHost", "testContainerHost", 15, serviceName),
                new ShardInfo(new CurrentStateResponse.InstancePart("testContainerHost", "testEngine",
                        "testHost", itags, "networkSettings", 15), serviceName, 0, false, null));
        shardState.put(serviceName, pair);
        ShardSetSnapshot snapshotTest = new ShardSetSnapshot(new ShardSetState(shardState), new ShardSetState());

        return snapshotTest;
    }
}
