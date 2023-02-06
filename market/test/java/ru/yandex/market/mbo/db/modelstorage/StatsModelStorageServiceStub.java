package ru.yandex.market.mbo.db.modelstorage;

import org.springframework.core.convert.converter.Converter;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.index.GenericField;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.index.util.CursorAwareResponse;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.HasModelIndexPayload;
import ru.yandex.market.mbo.db.modelstorage.yt.indexes.payload.ModelIndexPayload;
import ru.yandex.market.mbo.gwt.models.modelstorage.CategoryModelId;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by anmalysh on 23.03.2017.
 */
public class StatsModelStorageServiceStub extends ModelStorageServiceStub
    implements StatsModelStorageService {

    @Override
    public ModelStoreInterface getModelStore() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CommonModel> getModels(long categoryId, Collection<Long> ids, ReadStats readStats) {
        return getModels(categoryId, ids);
    }

    @Override
    public List<CommonModel> getModels(Collection<CategoryModelId> ids, ReadStats readStats) {
        return searchByIds(ids.stream().map(CategoryModelId::getModelId).collect(Collectors.toList()));
    }

    @Override
    public List<CommonModel> getModelsWithParent(long categoryId, Collection<Long> ids, ReadStats readStats) {
        return getModels(categoryId, ids);
    }

    @Override
    public void processAllCategoryModels(long categoryId, CommonModel.Source type,
                                         Boolean deleted, Integer maxRows,
                                         Consumer<CommonModel> processor, ReadStats readStats) {
        processAllCategoryModels(categoryId, type, deleted, maxRows, processor);
    }

    @Override
    public void processCategoryModels(long categoryId, Collection<Long> modelIds,
                                      Consumer<CommonModel> callback,
                                      Consumer<Long> missingModelCallback, ReadStats readStats) {
        processCategoryModels(categoryId, modelIds, callback, missingModelCallback);
    }

    @Override
    public List<OperationStatus> processModelsOfType(Long categoryId,
                                                     CommonModel.Source type,
                                                     OperationType operationType,
                                                     Collection<Long> modelIds,
                                                     ModelSaveContext context,
                                                     BiFunction<CommonModel, ModelSaveContext, GroupOperationStatus>
                                                         modelCallback) {
        List<OperationStatus> result = new ArrayList<>(modelIds.size());

        Consumer<CommonModel> processModel = model -> {
            if (!type.equals(model.getCurrentType())) {
                result.addAll(
                    ModelStorageServiceUtil.createWrongModelStatus(model, operationType, type).getAllModelStatuses()
                );
            } else {
                result.addAll(
                    modelCallback.apply(model, context).getAllModelStatuses()
                );
            }
        };
        Consumer<Long> processModelMissing = id -> result.add(
            ModelStorageServiceUtil.createOperationStatus(id, operationType, OperationStatusType.MODEL_NOT_FOUND)
        );
        processCategoryModels(categoryId, modelIds,
            processModel,
            processModelMissing,
            context.getStats().getReadStats()
        );
        return result;
    }

    @Override
    public List<OperationStatus> processModelsOfType(Long categoryId,
                                                     CommonModel.Source type,
                                                     OperationType operationType,
                                                     ModelSaveContext context,
                                                     BiFunction<CommonModel, ModelSaveContext, GroupOperationStatus>
                                                         modelCallback) {
        return new ArrayList<>(modelsMap.values())
            .stream()
            .filter(model -> model.getCurrentType() == type)
            .flatMap(model -> modelCallback.apply(model, context).getAllModelStatuses().stream())
            .collect(Collectors.toList());
    }

    @Override
    public CommonModel searchById(long id, ReadStats readStats) {
        return searchById(id);
    }

    @Override
    public List<CommonModel> searchByIds(Collection<Long> ids, ReadStats stats) {
        return searchByIds(ids);
    }

    @Override
    public List<CommonModel> searchByGroupIds(Collection<Long> ids, ReadStats stats) {
        return null;
    }

    @Override
    public List<Long> getModelIdsPage(MboIndexesFilter filter, ReadStats stats) {
        return getModelIdsPage(filter);
    }

    @Override
    public List<CommonModel> getModelsPage(MboIndexesFilter filter, ReadStats stats) {
        return getModelsPage(filter);
    }

    @Override
    public Long getCategoryId(long id, ReadStats readStats) {
        return getCategoryId(id);
    }

    @Override
    public List<Long> getCategoryIds(Collection<Long> ids, ReadStats readStats) {
        return getCategoryIds(ids);
    }

    @Override
    public List<CommonModel> getModelsByBarcodes(List<String> barcodes, List<CommonModel.Source> processingTypes,
                                                 ReadStats readStats) {
        return getModelsByBarcodes(barcodes, processingTypes);
    }

    @Override
    public List<ModelIndexPayload> getModelsIndexByBarcodes(List<String> barcodes,
                                                            List<CommonModel.Source> processingTypes,
                                                            ReadStats readStats) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CommonModel> getModelsByVendorCodes(long categoryId, long vendorId, List<String> vendorCodes,
                                                    List<CommonModel.Source> processingTypes, ReadStats readStats) {
        return getModelsByVendorCodes(categoryId, vendorId, vendorCodes, processingTypes);
    }

    @Override
    public List<CommonModel> getModelsByAliases(Collection<String> aliases, Set<Long> categoryIds,
                                                Set<Long> vendorIds, Set<Long> excludeModelIds, ReadStats readStats) {
        return getModelsByAliases(aliases, categoryIds, vendorIds, excludeModelIds);
    }

    @Override
    public List<CommonModel> getValidModifications(Collection<Long> modelIds, ReadStats stats) {
        return modelsMap.values().stream()
            .filter(model -> model.isModification() && modelIds.contains(model.getParentModelId()))
            .filter(model -> !model.isDeleted())
            .map(CommonModel::new)
            .collect(Collectors.toList());
    }

    @Override
    public List<ModelStorage.Model> getFullModelsPage(MboIndexesFilter filter, ReadStats stats) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Set<T> getFieldValues(GenericField genericField,
                                     MboIndexesFilter filter,
                                     Converter<String, T> converter,
                                     ReadStats stats) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Set<T> getFieldValues(MboIndexesFilter filter,
                                     Converter<HasModelIndexPayload, T> converterFromIndex,
                                     ReadStats stats) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CursorAwareResponse<ModelStorage.Model> getFullModelsCursor(MboIndexesFilter filter,
                                                                       String cursorMark,
                                                                       ReadStats stats) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processQueryModels(long categoryId, MboIndexesFilter filter,
                                   Consumer<CommonModel> callback, ReadStats stats) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processQueryModels(MboIndexesFilter filter,
                                   Function<CommonModel, Boolean> processor,
                                   ReadStats stats) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processQueryFullModels(long categoryId, MboIndexesFilter filter,
                                       Consumer<ModelStorage.Model> callback, ReadStats stats) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processQueryFullModels(MboIndexesFilter filter,
                                       Consumer<ModelStorage.Model> callback,
                                       ReadStats stats) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processQueryIndexModel(MboIndexesFilter filter, Consumer<ModelIndexPayload> callback, ReadStats stats) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processQueryIds(MboIndexesFilter filter, Consumer<Long> callback, ReadStats readStats) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long count(MboIndexesFilter query, ReadStats stats) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(long modelId, ReadStats stats) {
        return exists(modelId);
    }

    public int getModelsCount() {
        return modelsMap.size();
    }
}
