package ru.yandex.market.mbo.db.modelstorage;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.yandex.market.http.ServiceException;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusConverter;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.health.OperationStats;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.payload.ModelIndexPayload;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.http.ModelStorage;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 03.09.2018
 */
public class ModelStorageProtoServiceRemoveModelsTest extends ModelStorageProtoServiceTestBase {

    private static final int MODELS_COUNT = 15;

    @Override
    public void before() {
        super.before();
        when(storageService.getModel(anyLong(), anyLong(), any()))
            .thenAnswer(invocation -> {
                long categoryId = invocation.getArgument(0);
                long modelId = invocation.getArgument(1);
                if (categoryId > 0 && modelId > 0L) {
                    return Optional.of(CommonModelBuilder.newBuilder(modelId, categoryId).getModel());
                } else {
                    return Optional.empty();
                }
            });
        when(storageService.deleteModels(any(), any(), anyList()))
            .thenReturn(Collections.singletonList(new GroupOperationStatus(okStatus)));
        when(storageService.deleteModelById(anyLong(), anyLong(), any(), anyList()))
            .thenCallRealMethod();
    }

    @Test
    public void removeCalled() {
        when(storageService.deleteModel(any(), any(), anyList())).thenCallRealMethod();

        List<ModelStorage.Model> models = generateModels(MODELS_COUNT);
        ModelStorage.RemoveModelsRequest request = ModelStorage.RemoveModelsRequest.newBuilder()
            .addAllModels(models)
            .build();

        ModelStorage.OperationResponse response = protoService.removeModels(request);

        assertThat(response.getStatusesCount()).isEqualTo(MODELS_COUNT);
        verify(storageService, times(MODELS_COUNT))
            .deleteModel(any(CommonModel.class), any(ModelSaveContext.class), anyList());
    }

    @Test
    public void removeByIdCalled() {
        // assume
        List<ModelStorage.Model> models = generateModels(MODELS_COUNT);
        List<ModelStorage.Model> idAndCategoryWrapper = models.stream()
            .map(model -> ModelStorage.Model.newBuilder()
                .setId(model.getId())
                .build())
            .collect(Collectors.toList());
        List<Long> modelIds = models.stream().map(ModelStorage.Model::getId).collect(Collectors.toList());

        // mock service
        Mockito.doAnswer(invocation -> {
            Consumer<ModelIndexPayload> callback = invocation.getArgument(1);
            for (Long modelId : modelIds) {
                callback.accept(new ModelIndexPayload(modelId, CATEGORY_ID, null));
            }
            return null;
        }).when(storageService).processQueryIndexModel(any(MboIndexesFilter.class), any(), any());

        // act
        ModelStorage.OperationResponse response = protoService.removeModels(
            ModelStorage.RemoveModelsRequest.newBuilder()
                .addAllModels(idAndCategoryWrapper)
                .setById(true)
                .build()
        );

        // assert
        assertThat(response.getStatusesCount()).isEqualTo(MODELS_COUNT);
        List<Long> forceDeletedModelIds = getForceDeletedModels(CATEGORY_ID);
        assertThat(forceDeletedModelIds).containsExactlyElementsOf(modelIds);
    }

    @Test
    public void checkRemoveReturnsOverallStatusCorrectly() {
        final Long invalidModel1Id = 2L;
        final Long invalidModel2Id = 3L;
        // preparing a model to be removed
        ModelStorage.Model model = ModelStorage.Model.newBuilder().setId(1).build();

        // preparing models for statuses
        CommonModel failedGroupModel1 = CommonModelBuilder.newBuilder(1, CATEGORY_ID).endModel();
        CommonModel invalidModel1 = CommonModelBuilder.newBuilder(invalidModel1Id, CATEGORY_ID).endModel();
        CommonModel invalidModel2 = CommonModelBuilder.newBuilder(invalidModel2Id, CATEGORY_ID).endModel();

        // preparing statuses
        ModelSaveGroup group = ModelSaveGroup.fromModels(failedGroupModel1, invalidModel1);
        group.addIfAbsent(invalidModel2);
        group.setStatus(failedGroupModel1, OperationStatusType.FAILED_MODEL_IN_GROUP);
        group.setStatus(invalidModel1, OperationStatusType.VALIDATION_ERROR);
        group.setStatus(invalidModel2, OperationStatusType.VALIDATION_ERROR);

        ModelValidationError validationError1 = new ModelValidationError(invalidModel1Id,
            ModelValidationError.ErrorType.TRANSITION_ERROR);
        ModelValidationError validationError2 = new ModelValidationError(invalidModel2Id,
            ModelValidationError.ErrorType.EMPTY_NAME);

        group.addValidationErrors(invalidModel1, Collections.singletonList(validationError1));
        group.addValidationErrors(invalidModel2, Collections.singletonList(validationError2));

        GroupOperationStatus overallStatus = group.generateOverallStatus();

        when(storageService.deleteModelById(anyLong(), anyLong(), any(), any())).thenReturn(overallStatus);

        // act
        ModelStorage.OperationResponse response = protoService.removeModels(
            ModelStorage.RemoveModelsRequest.newBuilder()
                .addModels(model)
                .setById(true)
                .build()
        );

        // assert
        assertThat(response.getStatusesList())
            .extracting(ModelStorage.OperationStatus::getStatus)
            .containsExactly(OperationStatusConverter.convert(OperationStatusType.VALIDATION_ERROR));
        assertThat(response.getStatusesList())
            .flatExtracting(ModelStorage.OperationStatus::getValidationErrorList)
            .containsExactlyInAnyOrder(
                OperationStatusConverter.convert(validationError1),
                OperationStatusConverter.convert(validationError2)
            );
    }

    @Test
    public void removeByIdWithNotPositiveIds() {
        // assume
        List<ModelStorage.Model> models = Arrays.asList(
            ModelStorage.Model.newBuilder().setId(0L).build(),
            ModelStorage.Model.newBuilder().setId(-1L).build()
        );

        // act
        ModelStorage.OperationResponse response = protoService.removeModels(
            ModelStorage.RemoveModelsRequest.newBuilder()
                .addAllModels(models)
                .setById(true)
                .build()
        );

        // assert
        assertThat(response.getStatusesCount()).isEqualTo(2);
        assertThat(response.getStatuses(0).getLocalizedMessage(0).getValue()).contains("Id модели 0 не положителен");
        assertThat(response.getStatuses(1).getLocalizedMessage(0).getValue()).contains("Id модели -1 не положителен");
    }

    @Test
    public void removeByIdNotExistingModel() {
        // assume
        List<ModelStorage.Model> models = Collections.singletonList(
            ModelStorage.Model.newBuilder().setId(2L).build()
        );

        // act
        ModelStorage.OperationResponse response = protoService.removeModels(
            ModelStorage.RemoveModelsRequest.newBuilder()
                .addAllModels(models)
                .setById(true)
                .build()
        );

        // assert
        assertThat(response.getStatusesCount()).isEqualTo(1);
        assertThat(response.getStatuses(0).getLocalizedMessage(0).getValue())
            .contains("Модель 2 уже удалена или не существует");
    }

    @Test
    public void removeByIdWithPassedCategoryIdsWontGetCategoryIds() {
        // assume
        List<ModelStorage.Model> models = generateModels(MODELS_COUNT);
        List<ModelStorage.Model> idAndCategoryWrapper = models.stream()
            .map(model -> ModelStorage.Model.newBuilder()
                .setId(model.getId())
                .setCategoryId(model.getCategoryId())
                .build())
            .collect(Collectors.toList());
        List<Long> modelIds = models.stream().map(ModelStorage.Model::getId).collect(Collectors.toList());

        // act
        ModelStorage.OperationResponse response = protoService.removeModels(
            ModelStorage.RemoveModelsRequest.newBuilder()
                .addAllModels(idAndCategoryWrapper)
                .setById(true)
                .build()
        );

        // assert
        assertThat(response.getStatusesCount()).isEqualTo(MODELS_COUNT);
        List<Long> forceDeletedModelIds = getForceDeletedModels(CATEGORY_ID);
        assertThat(forceDeletedModelIds).containsExactlyElementsOf(modelIds);

        // assert no calls to solr
        Mockito.verify(storageService, never())
            .processQueryIndexModel(any(MboIndexesFilter.class), any(Consumer.class), any());
    }

    @Test
    public void statsWrittenIfFailed() {
        List<ModelStorage.Model> models = Collections.singletonList(model().build());

        Exception exception = new RuntimeException("exception-message");
        when(storageService.deleteModel(any(), any(ModelSaveContext.class), anyList())).thenThrow(exception);

        assertThatThrownBy(() ->
            protoService.removeModels(ModelStorage.RemoveModelsRequest.newBuilder().addAllModels(models).build())
        ).isInstanceOf(ServiceException.class)
            .hasMessage("Failed to removeModels with error: RuntimeException: exception-message")
            .hasCause(exception);


        ArgumentCaptor<OperationStats> healthStatsCaptor = ArgumentCaptor.forClass(OperationStats.class);
        verify(modelStorageHealthService).appendStats(healthStatsCaptor.capture());

        OperationStats healthStats = healthStatsCaptor.getValue();
        assertThat(healthStats.getService()).isEqualTo("ModelStorageService");
        assertThat(healthStats.getMethod()).isEqualTo("RemoveModels");
    }

    private List<Long> getForceDeletedModels(long categoryId) {
        ArgumentCaptor<Long> modelIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(storageService, Mockito.atLeastOnce())
            .deleteModelById(eq(categoryId), modelIdCaptor.capture(), any(ModelSaveContext.class), anyList());
        return modelIdCaptor.getAllValues();
    }

    @Nonnull
    protected List<ModelStorage.Model> generateModels(int modelsCount) {
        return Stream.generate(this::model)
            .limit(modelsCount)
            .map(ModelStorage.Model.Builder::build)
            .collect(Collectors.toList());
    }
}
