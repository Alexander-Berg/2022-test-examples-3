package ru.yandex.market.aliasmaker.meta.IntegrationTests;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

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
import ru.yandex.market.aliasmaker.meta.picker.DisabledCategoryException;
import ru.yandex.market.aliasmaker.meta.picker.SizeCategoryPickerService;
import ru.yandex.market.aliasmaker.meta.repository.CategorySizeRepository;
import ru.yandex.market.aliasmaker.meta.repository.ShardAllocatedCacheSizeRepository;
import ru.yandex.market.aliasmaker.meta.repository.dto.CategorySizeInfoDTO;
import ru.yandex.market.aliasmaker.meta.repository.dto.ShardAllocatedCacheSizeDTO;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

public class SizePickerMetaPipelineTests extends BaseTest {

    private static final int BIG_CATEGORY = 100001;
    private final String TEST_SAS_SERVICE_NAME1 = "testSASService1";
    private final String TEST_SAS_SERVICE_NAME2 = "testSASService2";
    private final String TEST_SAS_SERVICE_NAME3 = "testSASService3";
    private final String TEST_VLA_SERVICE_NAME1 = "testVLAService1";
    private final String TEST_VLA_SERVICE_NAME2 = "testVLAService2";
    private final String TEST_VLA_SERVICE_NAME3 = "testVLAService3";
    private final String SAS_DC_NAME = "sas";
    private final String VLA_DC_NAME = "vla";
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
    private SwitcherController switcherController;
    @Autowired
    private IMetaStateDAO metaStateDAO;
    @Autowired
    private StorageKeyValueService storageKeyValueService;
    @Autowired
    private CategorySizeRepository categorySizeRepository;
    @Autowired
    private ShardAllocatedCacheSizeRepository shardAllocatedCacheSizeRepository;

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
        sizeCategoryPickerService = new SizeCategoryPickerService(1, metaStateDAO);
        nannyDiscoverer = buildNannyDiscovererMock();
        discoveryWorker = new DiscoveryWorker(nannyDiscoverer);
        discoveryWorker.addOnSuccessHandler(i -> realsAlivenessWorker.forceUpdateSynchronously(i));
        realsAlivenessWorker.addOnSuccessHandler(i -> {
            sizeCategoryPickerService.updateFrom(stateRepository.getCurrentSnapshot(true));
        });
        switcherController = new SwitcherController(jdbcTemplate, stateRepository, realsAlivenessWorker);
        switcherController = Mockito.spy(switcherController);
        Mockito.doAnswer(invocation -> {
            realsAlivenessWorker.forceUpdateSynchronously(stateRepository.getCommonState());
            return null;
        }).when(switcherController).updateAliveness();
        stateRepository.resetState();
        categorySizeRepository.invalidateCategoryInfoCache();
    }

    @Test
    public void canDisableServiceByNameViaController() {
        switcherController.switchServicesByDC(SAS_DC_NAME, false);
        discoveryWorker.discover();
        var state = stateRepository.getCommonState();
        Assertions.assertThat(state).isNotNull();


        var shard = sizeCategoryPickerService.pickShard(1);
        var shard2 = sizeCategoryPickerService.pickShard(BIG_CATEGORY / 2);
        var shard3 = sizeCategoryPickerService.pickShard(BIG_CATEGORY);

        Set<String> usedServices = new HashSet<>();
        usedServices.add(shard.getShardInfo().getServiceName());
        usedServices.add(shard2.getShardInfo().getServiceName());
        usedServices.add(shard3.getShardInfo().getServiceName());

        Assertions.assertThat(usedServices.size()).isGreaterThan(1);

        String serviceToDisable = usedServices.stream().findFirst().orElseThrow();
        switcherController.switchService(serviceToDisable, false);
        discoveryWorker.discover();

        usedServices.clear();


        shard = sizeCategoryPickerService.pickShard(1);
        shard2 = sizeCategoryPickerService.pickShard(BIG_CATEGORY / 2);
        shard3 = sizeCategoryPickerService.pickShard(BIG_CATEGORY);
        usedServices.add(shard.getShardInfo().getServiceName());
        usedServices.add(shard2.getShardInfo().getServiceName());
        usedServices.add(shard3.getShardInfo().getServiceName());

        discoveryWorker.discover();
        Assertions.assertThat(usedServices).doesNotContain(serviceToDisable);
    }

    @Test
    public void chooseShardWithMinLoad() {
        switcherController.switchServicesByDC(SAS_DC_NAME, false);

        discoveryWorker.discover();
        var state = stateRepository.getCommonState();
        Assertions.assertThat(state).isNotNull();

        categorySizeRepository.insertBatch(
                List.of(
                        mockCategoryWithSize(1, 200),
                        mockCategoryWithSize(2, 100),
                        mockCategoryWithSize(3, 300),
                        mockCategoryWithSize(4, 400)
                )
        );
        categorySizeRepository.updateCache();

        var shard = sizeCategoryPickerService.pickShard(1);
        var shard2 = sizeCategoryPickerService.pickShard(2);
        var shard3 = sizeCategoryPickerService.pickShard(3);
        var shard4 = sizeCategoryPickerService.pickShard(4);

        Assertions.assertThat(shard.getShardInfo()).isNotEqualTo(shard2.getShardInfo());
        Assertions.assertThat(shard2.getShardInfo()).isNotEqualTo(shard3.getShardInfo());
        Assertions.assertThat(shard2.getShardInfo()).isEqualTo(shard4.getShardInfo());
    }


    @Test
    public void chooseShardWithMinLoadWithSkutcherFactor() {
        switcherController.switchServicesByDC(SAS_DC_NAME, false);

        discoveryWorker.discover();
        var state = stateRepository.getCommonState();
        Assertions.assertThat(state).isNotNull();

        categorySizeRepository.insertBatch(
                List.of(
                        mockCategoryWithSize(1, 250),
                        mockCategoryWithSize(2, 400, 0.9f),
                        mockCategoryWithSize(3, 300),
                        mockCategoryWithSize(4, 400)
                )
        );
        categorySizeRepository.updateCache();

        var shard = sizeCategoryPickerService.pickShard(1);
        var shard2 = sizeCategoryPickerService.pickShard(2);
        var shard3 = sizeCategoryPickerService.pickShard(3);
        var shard4 = sizeCategoryPickerService.pickShard(4);

        Assertions.assertThat(shard.getShardInfo()).isNotEqualTo(shard2.getShardInfo());
        Assertions.assertThat(shard2.getShardInfo()).isNotEqualTo(shard3.getShardInfo());
        Assertions.assertThat(shard.getShardInfo()).isEqualTo(shard4.getShardInfo());
    }


    @Test
    public void chooseShardForLargeCategory() {
        switcherController.switchServicesByDC(SAS_DC_NAME, false);

        discoveryWorker.discover();
        var state = stateRepository.getCommonState();
        Assertions.assertThat(state).isNotNull();


        categorySizeRepository.insertBatch(
                List.of(
                        mockCategoryWithSize(1, 200),
                        mockCategoryWithSize(2, 100),
                        mockCategoryWithSize(3, 300),
                        mockCategoryWithSize(4, 400, true),
                        mockCategoryWithSize(5, 200)
                )
        );
        categorySizeRepository.updateCache();

        ShardAllocatedCacheSizeDTO shardAllocatedCacheSizeDTO =
                shardAllocatedCacheSizeRepository.findById(TEST_VLA_SERVICE_NAME1);
        shardAllocatedCacheSizeDTO.setLarge(true);
        shardAllocatedCacheSizeRepository.update(shardAllocatedCacheSizeDTO);

        var shard1 = sizeCategoryPickerService.pickShard(1);
        var shard2 = sizeCategoryPickerService.pickShard(2);
        var shard3 = sizeCategoryPickerService.pickShard(3);
        var shard4 = sizeCategoryPickerService.pickShard(4);
        var shard5 = sizeCategoryPickerService.pickShard(5);

        long countOnNonLargeHost = Stream.of(shard1, shard2, shard3, shard4, shard5)
                .map(it -> it.getShardInfo().getServiceName())
                .filter(serviceName -> List.of(TEST_VLA_SERVICE_NAME2, TEST_VLA_SERVICE_NAME3).contains(serviceName))
                .count();
        Assertions.assertThat(countOnNonLargeHost).isEqualTo(4);
        Assertions.assertThat(shard4.getShardInfo().getServiceName()).isEqualTo(TEST_VLA_SERVICE_NAME1);
    }

    @Test
    public void balanceToReserveHost() {
        switcherController.switchServicesByDC(SAS_DC_NAME, false);

        discoveryWorker.discover();
        var state = stateRepository.getCommonState();
        Assertions.assertThat(state).isNotNull();


        categorySizeRepository.insertBatch(
                List.of(
                        mockCategoryWithSize(1, 200)
                )
        );
        categorySizeRepository.updateCache();

        var shard1 = sizeCategoryPickerService.pickShard(1);

        markWithFailedPingService(shard1.getServiceName());

        var shard2 = sizeCategoryPickerService.pickShard(1);
        Assertions.assertThat(shard1.getServiceName()).isNotEqualTo(shard2.getServiceName());

        String reserveAllocatedTo = categorySizeRepository.findById(1L).getReserveAllocatedTo();
        Assertions.assertThat(reserveAllocatedTo).isEqualTo(shard2.getServiceName());

        var shard3 = sizeCategoryPickerService.pickShard(1);
        Assertions.assertThat(reserveAllocatedTo).isEqualTo(shard3.getServiceName());
    }

    @Test
    public void relocateToReloadedHost() {
        switcherController.switchServicesByDC(SAS_DC_NAME, false);

        discoveryWorker.discover();
        var state = stateRepository.getCommonState();
        Assertions.assertThat(state).isNotNull();


        categorySizeRepository.insertBatch(
                List.of(
                        mockCategoryWithSize(1, 200)
                )
        );
        categorySizeRepository.updateCache();

        var shard1 = sizeCategoryPickerService.pickShard(1);

        CategorySizeInfoDTO categoryInfo1 = categorySizeRepository.findById(1L);
        Assertions.assertThat(categoryInfo1.getAllocatedTo()).isEqualTo(shard1.getServiceName());

        markWithFailedPingService(shard1.getShardInfo().getServiceName());

        var shard2 = sizeCategoryPickerService.pickShard(1);
        Assertions.assertThat(shard1.getServiceName()).isNotEqualTo(shard2.getServiceName());

        CategorySizeInfoDTO categoryInfo = categorySizeRepository.findById(1L);
        String reserveAllocatedTo = categoryInfo.getReserveAllocatedTo();
        Assertions.assertThat(reserveAllocatedTo).isEqualTo(shard2.getServiceName());

        discoveryWorker.discover();
        metaStateDAO.resetState();
        var shard3 = sizeCategoryPickerService.pickShard(1);
        Assertions.assertThat(categoryInfo.getAllocatedTo()).isEqualTo(shard3.getServiceName());
    }

    @Test
    public void relocateAfterUnboundOnRecoverHost() {
        switcherController.switchServicesByDC(SAS_DC_NAME, false);

        discoveryWorker.discover();
        var state = stateRepository.getCommonState();
        Assertions.assertThat(state).isNotNull();


        categorySizeRepository.insertBatch(
                List.of(
                        mockCategoryWithSize(1, 200)
                )
        );
        categorySizeRepository.updateCache();

        var shard1 = sizeCategoryPickerService.pickShard(1);

        markWithFailedPingService(shard1.getShardInfo().getServiceName());

        var shard2 = sizeCategoryPickerService.pickShard(1);
        Assertions.assertThat(shard1.getServiceName()).isNotEqualTo(shard2.getServiceName());

        CategorySizeInfoDTO categoryInfo = categorySizeRepository.findById(1L);
        String reserveAllocatedTo = categoryInfo.getReserveAllocatedTo();
        Assertions.assertThat(reserveAllocatedTo).isEqualTo(shard2.getServiceName());

        switcherController.switchService(shard1.getServiceName(), false);

        var shard3 = sizeCategoryPickerService.pickShard(1);
        Assertions.assertThat(reserveAllocatedTo).isEqualTo(shard3.getServiceName());

        categoryInfo = categorySizeRepository.findById(1L);
        Assertions.assertThat(categoryInfo.hasRecoverHost()).isFalse();
        Assertions.assertThat(categoryInfo.getAllocatedTo()).isEqualTo(reserveAllocatedTo);
    }

    @Test
    public void correctSizeCalculationWithReservedHost() {
        switcherController.switchServicesByDC(SAS_DC_NAME, false);
        switcherController.switchService(TEST_VLA_SERVICE_NAME1, false);

        discoveryWorker.discover();
        var state = stateRepository.getCommonState();
        Assertions.assertThat(state).isNotNull();

        categorySizeRepository.insertBatch(
                List.of(
                        CategorySizeInfoDTO.builder().categoryId(1L).categorySize(100L).allocatedTo(TEST_VLA_SERVICE_NAME1).build(),
                        CategorySizeInfoDTO.builder().categoryId(2L).categorySize(200L).allocatedTo(TEST_VLA_SERVICE_NAME2).build(),
                        CategorySizeInfoDTO.builder().categoryId(3L).categorySize(300L).allocatedTo(TEST_VLA_SERVICE_NAME3).build(),
                        CategorySizeInfoDTO.builder().categoryId(4L).categorySize(200L).allocatedTo(TEST_VLA_SERVICE_NAME1).reserveAllocatedTo(TEST_VLA_SERVICE_NAME2).build(),
                        mockCategoryWithSize(5, 200)
                )
        );
        categorySizeRepository.updateCache();

        var shard = sizeCategoryPickerService.pickShard(5);
        Assertions.assertThat(shard.getServiceName()).isEqualTo(TEST_VLA_SERVICE_NAME3);
    }

    @Test(expected = DisabledCategoryException.class)
    public void checkCategoryIsBlocked() {
        switcherController.switchServicesByDC(SAS_DC_NAME, false);

        discoveryWorker.discover();
        var state = stateRepository.getCommonState();
        Assertions.assertThat(state).isNotNull();


        CategorySizeInfoDTO categorySizeInfoDTO = mockCategoryWithSize(1, 200);
        categorySizeInfoDTO.setBlockedFrom(Instant.now().minus(1, ChronoUnit.HOURS));
        categorySizeInfoDTO.setBlockedUntil(Instant.now().plus(1, ChronoUnit.HOURS));
        categorySizeRepository.insertBatch(
                List.of(
                        categorySizeInfoDTO
                )
        );
        categorySizeRepository.updateCache();

        var shard1 = sizeCategoryPickerService.pickShard(1);
    }


    @Test
    public void canDisableServiceByDCNameViaController() {
        discoveryWorker.discover();
        Assertions.assertThat(stateRepository.getCommonState()).isNotNull();

        switcherController.switchServicesByDC(SAS_DC_NAME, false);

        var shard = sizeCategoryPickerService.pickShard(1);
        var shard2 = sizeCategoryPickerService.pickShard(BIG_CATEGORY / 2);
        var shard3 = sizeCategoryPickerService.pickShard(BIG_CATEGORY);
        Set<String> usedServices = new HashSet<>();
        usedServices.add(shard.getShardInfo().getServiceName());
        usedServices.add(shard2.getShardInfo().getServiceName());
        usedServices.add(shard3.getShardInfo().getServiceName());

        discoveryWorker.discover();
        Assertions.assertThat(usedServices).doesNotContain(TEST_SAS_SERVICE_NAME1, TEST_SAS_SERVICE_NAME2,
                TEST_SAS_SERVICE_NAME3);
        Assertions.assertThat(usedServices).size().isGreaterThan(1);
    }


    private NannyDiscoverer buildNannyDiscovererMock() {
        Map<String, CurrentStateResponse> nannyResponse = new HashMap<>();
        addResponse(nannyResponse, TEST_SAS_SERVICE_NAME1, SAS_DC_NAME);
        addResponse(nannyResponse, TEST_SAS_SERVICE_NAME2, SAS_DC_NAME);
        addResponse(nannyResponse, TEST_SAS_SERVICE_NAME3, SAS_DC_NAME);
        addResponse(nannyResponse, TEST_VLA_SERVICE_NAME1, VLA_DC_NAME);
        addResponse(nannyResponse, TEST_VLA_SERVICE_NAME2, VLA_DC_NAME);
        addResponse(nannyResponse, TEST_VLA_SERVICE_NAME3, VLA_DC_NAME);
        var result = Mockito.mock(NannyDiscoverer.class);
        Mockito.when(result.discover()).thenReturn(nannyResponse);
        return result;
    }

    private void addResponse(Map<String, CurrentStateResponse> nannyResponse,
                             String serviceName, String dcName) {
        CurrentStateResponse currentStateResponse = new CurrentStateResponse(Collections.emptyList(),
                Collections.singletonList(
                        new CurrentStateResponse.InstancePart(serviceName,
                                "",
                                "vlaHostName",
                                Collections.singletonList("a_dc_" + dcName),
                                "MTN_ENABLED",
                                5000)));
        nannyResponse.put(serviceName, currentStateResponse);
    }

    private CategorySizeInfoDTO mockCategoryWithSize(long categoryId, long size, Float factor) {
        CategorySizeInfoDTO categorySizeInfoDTO = new CategorySizeInfoDTO();
        categorySizeInfoDTO.setCategoryId(categoryId);
        categorySizeInfoDTO.setCategorySize(size);
        categorySizeInfoDTO.setSkutcherFactor(factor);
        return categorySizeInfoDTO;
    }

    private CategorySizeInfoDTO mockCategoryWithSize(long categoryId, long size) {
        return mockCategoryWithSize(categoryId, size, 0.0f);
    }

    private CategorySizeInfoDTO mockCategoryWithSize(long categoryId, long size, boolean isLarge) {
        CategorySizeInfoDTO categorySizeInfoDTO = new CategorySizeInfoDTO();
        categorySizeInfoDTO.setCategoryId(categoryId);
        categorySizeInfoDTO.setCategorySize(size);
        categorySizeInfoDTO.setIsLargeUntil(Instant.now().plus(1, ChronoUnit.DAYS));
        return categorySizeInfoDTO;
    }

    private void markWithFailedPingService(String serviceName) {
        ShardSetState commonState = metaStateDAO.getCommonState();
        ShardInfo shardKeyShardInfoMap = commonState.getState()
                .get(serviceName).values().iterator().next();
        ShardInfo newShard = new ShardInfo(
                shardKeyShardInfoMap.getInstancePart(), serviceName,
                shardKeyShardInfoMap.getLastHeartbeatCallMillis(), false, null
        );
        metaStateDAO.setCommonState(commonState.toBuilder().updateShard(serviceName, newShard).build());
    }
}

