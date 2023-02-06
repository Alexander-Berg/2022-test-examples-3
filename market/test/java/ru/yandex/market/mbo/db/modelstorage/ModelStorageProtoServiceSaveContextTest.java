package ru.yandex.market.mbo.db.modelstorage;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.health.ModelStorageHealthService;
import ru.yandex.market.mbo.db.modelstorage.image.ModelImageUploadingService;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.http.ModelCardApi.SaveModelsGroupRequest;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.market.mbo.user.TestAutoUser;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 10.08.2018
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelStorageProtoServiceSaveContextTest {

    private ModelStorageProtoService protoService;

    @Captor
    private ArgumentCaptor<ModelSaveContext> saveContextCaptor;

    @Mock
    private StatsModelStorageService statsModelStorageService;

    @Mock
    private ModelImageUploadingService modelImageUploadingService;

    private AutoUser autoUser;
    private OperationStatus okStatus;

    @Before
    public void before() {
        autoUser = TestAutoUser.create();
        protoService = new ModelStorageProtoService();

        protoService.setStorageService(statsModelStorageService);
        protoService.setModelImageUploadingService(modelImageUploadingService);
        protoService.setModelStorageHealthService(mock(ModelStorageHealthService.class));
        protoService.setAutoUser(autoUser);

        okStatus = new OperationStatus(OperationStatusType.OK, OperationType.CHANGE, -1L);

        when(statsModelStorageService.saveModels(any(ModelSaveGroup.class), any(ModelSaveContext.class)))
            .thenReturn(new GroupOperationStatus(okStatus));
    }

    @Test
    public void defaults() {
        long userId = autoUser.getId() + 1;
        SaveModelsGroupRequest request = groupRequest(
            ModelStorage.SaveModelsRequest.newBuilder()
                .setUserId(userId)
                .build()
        );

        protoService.saveModelsGroup(request);

        verify(statsModelStorageService).saveModels(any(ModelSaveGroup.class), saveContextCaptor.capture());

        ModelSaveContext context = saveContextCaptor.getValue();
        assertThat(context.getUid()).isEqualTo(userId);
        assertThat(context.getOperationSource()).isEqualTo(ModificationSource.OPERATOR_FILLED);
        assertThat(context.isUseMerge()).isTrue();
        assertThat(context.getMergeType()).isEqualTo(ModelStorage.MergeType.MERGE_REPLACE);
        assertThat(context.isReplacePictures()).isFalse();
        assertThat(context.isForcedGeneralization()).isFalse();
        assertThat(context.isForcedModifyDeleted()).isFalse();
        assertThat(context.isBilledOperation()).isFalse();
        assertThat(context.getStats()).isNotNull();
        assertThat(context.isSkipFirstPictureValidation()).isFalse();
        assertThat(context.isSkipPsku20MandatoryParamsValidation()).isFalse();
    }

    @Test
    public void autoUserDefaults() {
        SaveModelsGroupRequest request = groupRequest(
            ModelStorage.SaveModelsRequest.newBuilder()
                .setUserId(autoUser.getId())
                .build()
        );

        protoService.saveModelsGroup(request);

        verify(statsModelStorageService).saveModels(any(ModelSaveGroup.class), saveContextCaptor.capture());

        ModelSaveContext context = saveContextCaptor.getValue();
        assertThat(context.getOperationSource()).isEqualTo(ModificationSource.AUTO);
    }

    @Test
    public void fieldsPassed() {
        final int requestsCount = 16;
        EnhancedRandom enhancedRandom = EnhancedRandomBuilder.aNewEnhancedRandom();

        List<Flags> flags = enhancedRandom.objects(Flags.class, requestsCount).collect(Collectors.toList());
        SaveModelsGroupRequest.Builder requestBuilder = SaveModelsGroupRequest.newBuilder();
        flags.stream().map(f -> ModelStorage.SaveModelsRequest.newBuilder()
            .setForceAll(f.forceAll)
            .setForceGeneralization(f.forceGeneralization)
            .setReplacePictures(f.replacePictures)
            .setForceModifyDeleted(f.forceModifyDeleted)
            .setMergeType(f.mergeType)
            .setModificationSource(ModelStorage.ModificationSource.valueOf(f.modificationSource.name()))
            .setBilledOperation(f.billedOperation)
            .setSkipPsku20MandatoryParamsValidation(f.skipPsku20MandatoryParamsValidation)
            .build())
            .forEach(requestBuilder::addModelsRequest);

        protoService.saveModelsGroup(requestBuilder.build());


        verify(statsModelStorageService, times(requestsCount))
            .saveModels(any(ModelSaveGroup.class), saveContextCaptor.capture());


        for (int i = 0; i < flags.size(); i++) {
            Flags expected = flags.get(i);
            ModelSaveContext context = saveContextCaptor.getAllValues().get(i);
            assertThat(context.isForcedGeneralization()).isEqualTo(expected.forceAll || expected.forceGeneralization);
            assertThat(context.isReplacePictures()).isEqualTo(expected.replacePictures);
            assertThat(context.isForcedModifyDeleted()).isEqualTo(expected.forceModifyDeleted);
            assertThat(context.getMergeType()).isEqualTo(expected.mergeType);
            assertThat(context.getOperationSource()).isEqualTo(expected.modificationSource);
            assertThat(context.isBilledOperation()).isEqualTo(expected.billedOperation);
            assertThat(context.isSkipPsku20MandatoryParamsValidation())
                .isEqualTo(expected.skipPsku20MandatoryParamsValidation);
        }
    }

    private static SaveModelsGroupRequest groupRequest(ModelStorage.SaveModelsRequest... requests) {
        return SaveModelsGroupRequest.newBuilder()
            .addAllModelsRequest(Arrays.asList(requests))
            .build();
    }

    private static class Flags {
        boolean forceAll;
        boolean forceGeneralization;
        boolean replacePictures;
        boolean forceModifyDeleted;
        ModelStorage.MergeType mergeType;
        ModificationSource modificationSource;
        boolean billedOperation;
        boolean skipPsku20MandatoryParamsValidation;
    }
}
