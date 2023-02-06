package ru.yandex.market.aliasmaker.meta.IntegrationTests;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.aliasmaker.meta.BaseTest;
import ru.yandex.market.aliasmaker.meta.be.CurrentStateResponse;
import ru.yandex.market.aliasmaker.meta.be.ShardInfo;
import ru.yandex.market.aliasmaker.meta.be.ShardSetState;
import ru.yandex.market.aliasmaker.meta.controllers.SwitcherController;
import ru.yandex.market.aliasmaker.meta.discovery.DiscoveryWorker;
import ru.yandex.market.aliasmaker.meta.discovery.NannyDiscoverer;
import ru.yandex.market.aliasmaker.meta.heartbeat.AlivenessChecker;
import ru.yandex.market.aliasmaker.meta.heartbeat.RealsAlivenessWorker;
import ru.yandex.market.aliasmaker.meta.peristence.IMetaStateDAO;
import ru.yandex.market.aliasmaker.meta.picker.HashPickerService;
import ru.yandex.market.aliasmaker.meta.picker.SizeCategoryPickerService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

public class MetaPipelineTests extends BaseTest {

    private static final int BIG_CATEGORY = 100001;
    private HashPickerService hashPickerService;
    private SizeCategoryPickerService sizeCategoryPickerService;
    //Discoverer замоканы потому что у нас всего 2 тестовых инстанса АМ
    //Мета помирает если в живых остается только 1 инстанс АМ, а мы в тестах выключаем-включаем их
    private NannyDiscoverer nannyDiscoverer;
    @Autowired
    private IMetaStateDAO stateRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private RealsAlivenessWorker realsAlivenessWorker;
    private DiscoveryWorker discoveryWorker;
    private final String TEST_SAS_SERVICE_NAME1 = "testSASService1";
    private final String TEST_SAS_SERVICE_NAME2 = "testSASService2";
    private final String TEST_SAS_SERVICE_NAME3 = "testSASService3";
    private final String TEST_VLA_SERVICE_NAME1 = "testVLAService1";
    private final String TEST_VLA_SERVICE_NAME2 = "testVLAService2";
    private final String TEST_VLA_SERVICE_NAME3 = "testVLAService3";
    private final String SAS_DC_NAME = "sas";
    private final String VLA_DC_NAME = "vla";
    private SwitcherController switcherController;
    @Autowired
    private IMetaStateDAO metaStateDAO;
    @Autowired
    private StorageKeyValueService storageKeyValueService;


    @Before
    public void setUp() {
        AlivenessChecker mockedAlivenessChecker = Mockito.mock(AlivenessChecker.class);
        Mockito.when(mockedAlivenessChecker.check(Mockito.any())).thenAnswer(i ->
        {
            ShardSetState arg = i.getArgument(0);
            ShardSetState.Builder builder = new ShardSetState.Builder();

            long currentTimestamp = System.currentTimeMillis();
            for (var serviceName : arg.getState().keySet()) {
                for (var serviceInfo : arg.getState().get(serviceName).values()) {
                    builder.updateShard(serviceName, new ShardInfo(
                            serviceInfo.getInstancePart(), serviceName, currentTimestamp, true,
                            null));
                }
            }
            return builder.build();
        });
        realsAlivenessWorker = new RealsAlivenessWorker(1, stateRepository, mockedAlivenessChecker);
        hashPickerService = new HashPickerService(Collections.singleton(BIG_CATEGORY), 1, 100,
                storageKeyValueService);
        sizeCategoryPickerService = new SizeCategoryPickerService(1, metaStateDAO);
        nannyDiscoverer = buildNannyDiscovererMock();
        discoveryWorker = new DiscoveryWorker(nannyDiscoverer);
        discoveryWorker.addOnSuccessHandler(i -> realsAlivenessWorker.forceUpdateSynchronously(i));
        realsAlivenessWorker.addOnSuccessHandler(i -> {
            hashPickerService.updateFrom(stateRepository.getCurrentSnapshot(true));
        });
        switcherController = new SwitcherController(jdbcTemplate, stateRepository, realsAlivenessWorker);
        switcherController = Mockito.spy(switcherController);
        Mockito.doAnswer(invocation -> {
            realsAlivenessWorker.forceUpdateSynchronously(stateRepository.getCommonState());
            return null;
        }).when(switcherController).updateAliveness();
        stateRepository.resetState();
    }

    @Test
    public void canDisableServiceByNameViaController() {
        discoveryWorker.discover();
        var state = stateRepository.getCommonState();
        Assertions.assertThat(state).isNotNull();

        var shard = hashPickerService.pickShard(1);
        var shard2 = hashPickerService.pickShard(BIG_CATEGORY / 2);
        var shard3 = hashPickerService.pickShard(BIG_CATEGORY);

        Set<String> usedServices = new HashSet<>();
        usedServices.add(shard.getShardInfo().getServiceName());
        usedServices.add(shard2.getShardInfo().getServiceName());
        usedServices.add(shard3.getShardInfo().getServiceName());

        Assertions.assertThat(usedServices.size()).isGreaterThan(1);

        String serviceToDisable = usedServices.stream().findFirst().orElseThrow();
        usedServices.clear();

        switcherController.switchService(serviceToDisable, false);

        shard = hashPickerService.pickShard(1);
        shard2 = hashPickerService.pickShard(BIG_CATEGORY / 2);
        shard3 = hashPickerService.pickShard(BIG_CATEGORY);
        usedServices.add(shard.getShardInfo().getServiceName());
        usedServices.add(shard2.getShardInfo().getServiceName());
        usedServices.add(shard3.getShardInfo().getServiceName());

        discoveryWorker.discover();
        Assertions.assertThat(usedServices).doesNotContain(serviceToDisable);
    }

    @Test
    public void canDisableServiceByDCNameViaController() {
        discoveryWorker.discover();
        Assertions.assertThat(stateRepository.getCommonState()).isNotNull();

        switcherController.switchServicesByDC(SAS_DC_NAME, false);

        var shard = hashPickerService.pickShard(1);
        var shard2 = hashPickerService.pickShard(BIG_CATEGORY / 2);
        var shard3 = hashPickerService.pickShard(BIG_CATEGORY);
        Set<String> usedServices = new HashSet<>();
        usedServices.add(shard.getShardInfo().getServiceName());
        usedServices.add(shard2.getShardInfo().getServiceName());
        usedServices.add(shard3.getShardInfo().getServiceName());

        discoveryWorker.discover();
        Assertions.assertThat(usedServices).doesNotContain(TEST_SAS_SERVICE_NAME1, TEST_SAS_SERVICE_NAME2,
                TEST_SAS_SERVICE_NAME3);
        Assertions.assertThat(usedServices).size().isGreaterThan(1);
    }

    @Test
    public void canEnableServiceByDCNameViaController() {
        discoveryWorker.discover();
        var state = stateRepository.getCommonState();
        Assertions.assertThat(state).isNotNull();

        switcherController.switchServicesByDC(SAS_DC_NAME, false);
        discoveryWorker.discover();
        var shard = hashPickerService.pickShard(1);
        var shard2 = hashPickerService.pickShard(BIG_CATEGORY / 2);
        var shard3 = hashPickerService.pickShard(BIG_CATEGORY);
        Set<String> usedServices = new HashSet<>();
        usedServices.add(shard.getShardInfo().getServiceName());
        usedServices.add(shard2.getShardInfo().getServiceName());
        usedServices.add(shard3.getShardInfo().getServiceName());

        Assertions.assertThat(usedServices).doesNotContain(TEST_SAS_SERVICE_NAME1, TEST_SAS_SERVICE_NAME2,
                TEST_SAS_SERVICE_NAME3);

        usedServices.clear();
        switcherController.switchServicesByDC(SAS_DC_NAME, true);
        discoveryWorker.discover();
        shard = hashPickerService.pickShard(1);
        shard2 = hashPickerService.pickShard(BIG_CATEGORY / 2);
        shard3 = hashPickerService.pickShard(BIG_CATEGORY);
        usedServices.add(shard.getShardInfo().getServiceName());
        usedServices.add(shard2.getShardInfo().getServiceName());
        usedServices.add(shard3.getShardInfo().getServiceName());


        Assertions.assertThat(usedServices.stream()
                .anyMatch(i -> i.equals(TEST_SAS_SERVICE_NAME1)
                        || i.equals(TEST_SAS_SERVICE_NAME2)
                        || i.equals(TEST_SAS_SERVICE_NAME3))).isTrue();
    }

    @Test
    public void canEnableServiceByServiceNameViaController() {
        discoveryWorker.discover();
        var state = stateRepository.getCommonState();
        Assertions.assertThat(state).isNotNull();

        switcherController.switchService(TEST_SAS_SERVICE_NAME1, false);
        switcherController.switchService(TEST_SAS_SERVICE_NAME2, false);
        switcherController.switchService(TEST_SAS_SERVICE_NAME3, false);
        discoveryWorker.discover();
        var shard = hashPickerService.pickShard(1);
        var shard2 = hashPickerService.pickShard(BIG_CATEGORY / 2);
        var shard3 = hashPickerService.pickShard(BIG_CATEGORY);
        Set<String> usedServices = new HashSet<>();
        usedServices.add(shard.getShardInfo().getServiceName());
        usedServices.add(shard2.getShardInfo().getServiceName());
        usedServices.add(shard3.getShardInfo().getServiceName());

        Assertions.assertThat(usedServices).doesNotContain(TEST_SAS_SERVICE_NAME1, TEST_SAS_SERVICE_NAME2,
                TEST_SAS_SERVICE_NAME3);

        usedServices.clear();
        switcherController.switchService(TEST_SAS_SERVICE_NAME1, true);
        switcherController.switchService(TEST_SAS_SERVICE_NAME2, true);
        switcherController.switchService(TEST_SAS_SERVICE_NAME3, true);
        discoveryWorker.discover();
        shard = hashPickerService.pickShard(1);
        shard2 = hashPickerService.pickShard(BIG_CATEGORY / 2);
        shard3 = hashPickerService.pickShard(BIG_CATEGORY);
        usedServices.add(shard.getShardInfo().getServiceName());
        usedServices.add(shard2.getShardInfo().getServiceName());
        usedServices.add(shard3.getShardInfo().getServiceName());

        Assertions.assertThat(usedServices.stream()
                .anyMatch(i -> i.equals(TEST_SAS_SERVICE_NAME1)
                        || i.equals(TEST_SAS_SERVICE_NAME2)
                        || i.equals(TEST_SAS_SERVICE_NAME3))).isTrue();
    }

    @Test
    public void canResetDisabledServicesViaController() {
        discoveryWorker.discover();
        Assertions.assertThat(stateRepository.getCommonState()).isNotNull();

        switcherController.switchServicesByDC(SAS_DC_NAME, false);
        var shard = hashPickerService.pickShard(1);
        var shard2 = hashPickerService.pickShard(BIG_CATEGORY / 2);
        var shard3 = hashPickerService.pickShard(BIG_CATEGORY);
        Set<String> usedServices = new HashSet<>();
        usedServices.add(shard.getShardInfo().getServiceName());
        usedServices.add(shard2.getShardInfo().getServiceName());
        usedServices.add(shard3.getShardInfo().getServiceName());

        discoveryWorker.discover();
        Assertions.assertThat(usedServices).doesNotContain(TEST_SAS_SERVICE_NAME1, TEST_SAS_SERVICE_NAME2,
                TEST_SAS_SERVICE_NAME3);

        switcherController.resetServiceState();

        discoveryWorker.discover();

        shard = hashPickerService.pickShard(1);
        shard2 = hashPickerService.pickShard(BIG_CATEGORY / 2);
        shard3 = hashPickerService.pickShard(BIG_CATEGORY);
        usedServices.add(shard.getShardInfo().getServiceName());
        usedServices.add(shard2.getShardInfo().getServiceName());
        usedServices.add(shard3.getShardInfo().getServiceName());
        Assertions.assertThat(usedServices.stream()
                .anyMatch(i -> i.equals(TEST_SAS_SERVICE_NAME1)
                        || i.equals(TEST_SAS_SERVICE_NAME2)
                        || i.equals(TEST_SAS_SERVICE_NAME3))).isTrue();
    }

    private NannyDiscoverer buildNannyDiscovererMock() {
        Map<String, CurrentStateResponse> nannyResponse = new HashMap<>();
        nannyResponse.put(TEST_SAS_SERVICE_NAME1, getTestStateResponse(SAS_DC_NAME, 1));
        nannyResponse.put(TEST_SAS_SERVICE_NAME2, getTestStateResponse(SAS_DC_NAME, 2));
        nannyResponse.put(TEST_SAS_SERVICE_NAME3, getTestStateResponse(SAS_DC_NAME, 3));
        nannyResponse.put(TEST_VLA_SERVICE_NAME1, getTestStateResponse(VLA_DC_NAME, 1));
        nannyResponse.put(TEST_VLA_SERVICE_NAME2, getTestStateResponse(VLA_DC_NAME, 2));
        nannyResponse.put(TEST_VLA_SERVICE_NAME3, getTestStateResponse(VLA_DC_NAME, 3));
        var result = Mockito.mock(NannyDiscoverer.class);
        Mockito.when(result.discover()).thenReturn(nannyResponse);
        return result;
    }

    private CurrentStateResponse getTestStateResponse(String dcName, Integer id) {
        return new CurrentStateResponse(Collections.emptyList(),
                Collections.singletonList(
                        new CurrentStateResponse.InstancePart(dcName + ".qqq-w.yandex.ru" + id,
                                "",
                                "vlaHostName",
                                Collections.singletonList("a_dc_" + dcName),
                                "MTN_ENABLED",
                                5000)));
    }
}

