package ru.yandex.market.mbo.db.modelstorage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.health.ModelStorageHealthService;
import ru.yandex.market.mbo.db.modelstorage.health.OperationStats;
import ru.yandex.market.mbo.db.modelstorage.image.ModelImageUploadingService;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelCardApi.SaveModelsGroupRequest;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.user.TestAutoUser;

import javax.annotation.Nonnull;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 02.04.2018
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelStorageProtoServiceSaveModelsGroupTest {

    private static final long FIRST_MODEL_ID = 971056934061L;
    private static final long SECOND_MODEL_ID = 8955299015314L;
    private static final long CATEGORY_ID = 538422L;

    @Mock
    private ModelImageUploadingService modelImageUploadingService;

    @Mock
    private ModelStorageHealthService modelStorageHealthService;

    private ModelStorageProtoService protoService;

    private StatsModelStorageServiceStub modelStorageService;
    private OperationStatus okStatus;

    @Before
    public void before() {
        modelStorageService = new StatsModelStorageServiceStub();
        protoService = new ModelStorageProtoService();
        protoService.setModelImageUploadingService(modelImageUploadingService);
        protoService.setAutoUser(TestAutoUser.create());
        protoService.setStorageService(modelStorageService);
        protoService.setModelStorageHealthService(modelStorageHealthService);

        okStatus = new OperationStatus(OperationStatusType.OK, OperationType.CHANGE, -1L);
    }

    @Test
    public void treatExceptionAsInternalError() {
        StatsModelStorageService storageService = mock(StatsModelStorageService.class);

        SaveModelsGroupRequest request = SaveModelsGroupRequest.newBuilder()
            .addModelsRequest(ModelStorage.SaveModelsRequest.newBuilder()
                .addModels(model().setId(FIRST_MODEL_ID))
                .build())
            .addModelsRequest(ModelStorage.SaveModelsRequest.newBuilder()
                .addModels(model().setId(SECOND_MODEL_ID))
                .build())
            .build();

        protoService.setStorageService(storageService);

        Exception exception = new RuntimeException("exception-message");
        when(storageService.saveModels(nullable(ModelSaveGroup.class), any()))
            .thenThrow(exception)
            .thenReturn(new GroupOperationStatus(okStatus));


        ModelCardApi.SaveModelsGroupResponse response = protoService.saveModelsGroup(request);


        assertThat(response).isNotNull();
        assertThat(response.getResponseCount()).isEqualTo(2);
        assertThat(response.getResponse(0).getStatus()).isEqualTo(ModelStorage.OperationStatusType.INTERNAL_ERROR);
        assertThat(response.getResponse(1).getStatus()).isEqualTo(ModelStorage.OperationStatusType.OK);
    }

    @Test
    public void healthCalledIfFail() {
        SaveModelsGroupRequest request = SaveModelsGroupRequest.newBuilder()
            .addModelsRequest(ModelStorage.SaveModelsRequest.newBuilder()
                .addModels(model())
                .build())
            .build();

        protoService.saveModelsGroup(request);

        verify(modelStorageHealthService).appendStats(any(OperationStats.class));
    }

    @Nonnull
    private ModelStorage.Model.Builder model() {
        return ModelStorage.Model.newBuilder()
            .setCategoryId(CATEGORY_ID)
            .setSourceType(CommonModel.Source.GURU.name().toUpperCase());
    }
}
