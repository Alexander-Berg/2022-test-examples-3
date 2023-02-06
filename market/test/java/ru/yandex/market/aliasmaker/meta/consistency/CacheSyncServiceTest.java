package ru.yandex.market.aliasmaker.meta.consistency;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.data.Percentage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.aliasmaker.AliasMakerService;
import ru.yandex.market.aliasmaker.meta.be.CategoryCacheInfo;
import ru.yandex.market.aliasmaker.meta.be.CurrentStateResponse;
import ru.yandex.market.aliasmaker.meta.be.ShardInfo;
import ru.yandex.market.aliasmaker.meta.be.ShardSetState;
import ru.yandex.market.aliasmaker.meta.be.ShardStateInfo;
import ru.yandex.market.aliasmaker.meta.client.ClientCache;
import ru.yandex.market.aliasmaker.meta.heartbeat.RealsAlivenessWorker;
import ru.yandex.market.aliasmaker.meta.picker.SwitchPickerService;
import ru.yandex.market.aliasmaker.meta.repository.CategorySizeRepository;
import ru.yandex.market.aliasmaker.meta.repository.dto.CategorySizeInfoDTO;
import ru.yandex.market.mbo.storage.StorageKeyValueService;

/**
 * @author apluhin
 * @created 4/25/22
 */
public class CacheSyncServiceTest {

    private CategorySizeRepository categorySizeRepository;
    private StorageKeyValueService storageKeyValueService;
    private RealsAlivenessWorker realsAlivenessWorker;
    private ClientCache clientCache;

    private CacheSyncService cacheSyncService;

    private SwitchPickerService switchPickerService;

    private AliasMakerService aliasMakerService;

    @Before
    public void setUp() throws Exception {
        categorySizeRepository = Mockito.mock(CategorySizeRepository.class);
        realsAlivenessWorker = Mockito.mock(RealsAlivenessWorker.class);
        clientCache = Mockito.mock(ClientCache.class);
        storageKeyValueService = Mockito.mock(StorageKeyValueService.class);
        aliasMakerService = Mockito.mock(AliasMakerService.class);
        switchPickerService = Mockito.mock(SwitchPickerService.class);
        cacheSyncService = new CacheSyncService(
                switchPickerService,
                realsAlivenessWorker,
                categorySizeRepository,
                clientCache,
                storageKeyValueService);
    }

    @Test
    public void testSyncCache() {
        Mockito.when(switchPickerService.isEnableSizeSharding()).thenReturn(true);
        Mockito.when(storageKeyValueService.getCachedBool(Mockito.any(), Mockito.anyBoolean())).thenReturn(true);
        Mockito.when(categorySizeRepository.findAllocatedCategory(Mockito.anyBoolean())).thenReturn(
                List.of(
                        CategorySizeInfoDTO.builder().categoryId(1L).allocatedTo("1").build(),
                        CategorySizeInfoDTO.builder().categoryId(2L).reserveAllocatedTo("2").build(),
                        CategorySizeInfoDTO.builder().categoryId(3L).allocatedTo("3").reserveAllocatedTo("3").build(),
                        CategorySizeInfoDTO.builder().categoryId(4L).allocatedTo("1").build()
                )
        );
        mockShard(Map.of(
                "1", List.of(1L, 5L),
                "3", List.of(3L),
                "2", List.of(2L)
        ));
        var sourcesState = realsAlivenessWorker.getCurrentInMemoryStateState();
        cacheSyncService.syncCaches();

        ArgumentCaptor<AliasMaker.InvalidateCategoryRequest> captor =
                ArgumentCaptor.forClass(AliasMaker.InvalidateCategoryRequest.class);
        Mockito.verify(aliasMakerService, Mockito.times(1)).invalidateCategory(captor.capture());
        Assertions.assertThat(captor.getValue().getCategoryIdList()).containsExactlyInAnyOrder(5);
        Assertions.assertThat(sourcesState).isEqualTo(realsAlivenessWorker.getCurrentInMemoryStateState());

        ArgumentCaptor<Map> skuthInfoCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(categorySizeRepository, Mockito.times(1))
                .updateSkuctherFactor(skuthInfoCaptor.capture());
        Map<Long, Float> skutherInfoForSave = skuthInfoCaptor.getValue();
        Assertions.assertThat(skutherInfoForSave.get(1L)).isCloseTo(0.1f, Percentage.withPercentage(5d));
    }

    private void mockShard(Map<String, List<Long>> categoryMap) {
        ShardSetState.Builder shardStateBuilder = new ShardSetState.Builder();
        categoryMap.entrySet().forEach(it -> {
            var collect =
                    it.getValue().stream().map(c -> new CategoryCacheInfo(c, 10L, 1L))
                            .collect(Collectors.toMap(CategoryCacheInfo::getCategoryId, category -> category));
            shardStateBuilder.updateShard(
                    it.getKey() + "_service",
                    new ShardInfo(
                            new CurrentStateResponse.InstancePart(it.getKey(), null, null, null, null, 0),
                            null, 0L,
                            false,
                            new ShardStateInfo(0L, 0L, collect))
            );
        });
        ShardSetState build = shardStateBuilder.build();
        Mockito.when(realsAlivenessWorker.getCurrentInMemoryStateState()).thenReturn(build);
        Mockito.when(clientCache.getServiceByShardO(Mockito.any())).thenReturn(Optional.of(aliasMakerService));
    }
}
