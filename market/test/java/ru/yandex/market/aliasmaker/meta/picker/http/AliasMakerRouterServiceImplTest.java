package ru.yandex.market.aliasmaker.meta.picker.http;

import java.util.Collections;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.aliasmaker.AliasMakerService;
import ru.yandex.market.aliasmaker.meta.be.ShardInfo;
import ru.yandex.market.aliasmaker.meta.be.ShardSetSnapshot;
import ru.yandex.market.aliasmaker.meta.be.ShardSetState;
import ru.yandex.market.aliasmaker.meta.client.ClientCache;
import ru.yandex.market.aliasmaker.meta.heartbeat.RealsAlivenessWorker;
import ru.yandex.market.aliasmaker.meta.http.AliasMakerRouterServiceImpl;
import ru.yandex.market.aliasmaker.meta.peristence.IMetaStateDAO;
import ru.yandex.market.aliasmaker.meta.peristence.MetaStateDAO;
import ru.yandex.market.aliasmaker.meta.picker.HashPickerService;
import ru.yandex.market.aliasmaker.meta.picker.PickerService;
import ru.yandex.market.aliasmaker.meta.picker.SwitchPickerService;
import ru.yandex.market.aliasmaker.meta.repository.dto.CategorySizeInfoDTO;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.YangLogStorageService;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mbo.tree.ExportTovarTree;

public class AliasMakerRouterServiceImplTest {

    private final EnhancedRandom random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder().build();

    private PickerService pickerService;
    private ClientCache clientCache;
    private AliasMakerService client;
    private YangLogStorageService yangLogStorageService;
    private RealsAlivenessWorker realsAlivenessWorker;
    private StorageKeyValueService storageKeyValueService = Mockito.mock(StorageKeyValueService.class);
    private final IMetaStateDAO metaStateDAO = Mockito.mock(MetaStateDAO.class);
    private final CategorySizeInfoDTO categorySizeInfoDTO = new CategorySizeInfoDTO();

    private AliasMakerRouterServiceImpl aliasMakerRouterService;

    @Before
    public void setUp() {
        categorySizeInfoDTO.setCategoryId(1L);
        categorySizeInfoDTO.setCategorySize(1000000L);
        //picker service -> isZeroShardEnabled = true
        Mockito.when(storageKeyValueService.getValue(Mockito.anyString(), Mockito.any())).thenReturn(true);
        pickerService = new HashPickerService(Collections.singleton(1), 1, 100, storageKeyValueService);
        ShardSetState.Builder stateBuilder = new ShardSetState.Builder();
        stateBuilder.updateShard("test-service", random.nextObject(ShardInfo.class));
        ShardSetState state = stateBuilder.build();
        pickerService.updateFrom(new ShardSetSnapshot(state, state.toBuilder().cleanUpFailedHeartbeat().build()));

        client = Mockito.mock(AliasMakerService.class);

        clientCache = Mockito.mock(ClientCache.class);
        Mockito.when(clientCache.getServiceByShard(Mockito.any()))
                .thenReturn(client);

        yangLogStorageService = Mockito.mock(YangLogStorageService.class);
        realsAlivenessWorker = Mockito.mock(RealsAlivenessWorker.class);

        aliasMakerRouterService = new AliasMakerRouterServiceImpl(
                new SwitchPickerService(
                        storageKeyValueService,
                        null,
                        (HashPickerService) pickerService),
                clientCache,
                yangLogStorageService,
                realsAlivenessWorker,
                null, null);
    }

    @Test
    public void testGetTovarTreeOk() {
        ExportTovarTree.GetTovarTreeResponse testTovarTree = ExportTovarTree.GetTovarTreeResponse.newBuilder()
                .addCategories(MboParameters.Category.newBuilder().setHid(1))
                .build();
        Mockito.when(client.getTovarTree(Mockito.any())).thenReturn(testTovarTree);

        ExportTovarTree.GetTovarTreeResponse responseTovarTree =
                aliasMakerRouterService.getTovarTree(AliasMaker.GetTovarTreeRequest.newBuilder().build());

        Assertions.assertThat(responseTovarTree)
                .isEqualTo(testTovarTree);
    }

    @Test
    public void testGetParametersOk() {
        var parametersResponse = AliasMaker.GetCategoryParametersResponse.newBuilder()
                .setResponse(MboParameters.GetCategoryParametersResponse.newBuilder()
                        .setCategoryParameters(MboParameters.Category.newBuilder()
                                .setBookingEnabled(true)
                                .build())
                        .build())
                .build();

        Mockito.when(client.getParameters(Mockito.any())).thenReturn(parametersResponse);

        var actualParametersResponse =
                aliasMakerRouterService.getParameters(MboParameters.GetCategoryParametersRequest.newBuilder()
                        .setCategoryId(1)
                        .build());

        Assertions.assertThat(actualParametersResponse)
                .isEqualTo(parametersResponse);
    }
}
