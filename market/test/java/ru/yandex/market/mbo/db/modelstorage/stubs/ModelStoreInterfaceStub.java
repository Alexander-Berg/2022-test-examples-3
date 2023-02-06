package ru.yandex.market.mbo.db.modelstorage.stubs;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joda.time.DateTime;
import ru.yandex.common.util.db.MultiIdGenerator;
import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.modelstorage.AbstractModelStore;
import ru.yandex.market.mbo.db.modelstorage.ModelProtoTransition;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.health.OperationStats;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.health.SaveStats;
import ru.yandex.market.mbo.db.modelstorage.store.ModelStoreCallbackResult;
import ru.yandex.market.mbo.db.modelstorage.store.ModelStoreResult;
import ru.yandex.market.mbo.db.modelstorage.store.ModelStoreResultBuilder;
import ru.yandex.market.mbo.db.modelstorage.store.ModelStoreSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.utils.ModelStoreUtils;
import ru.yandex.market.mbo.gwt.models.modelstorage.CategoryModelId;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.Model;
import ru.yandex.market.mbo.http.ModelStorage.Model.Builder;
import ru.yandex.market.mbo.http.ModelStorage.OperationStatusType;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.yt.ytclient.rpc.RpcError;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author york
 * @since 10.10.2017
 */
public class ModelStoreInterfaceStub extends AbstractModelStore implements ModelStoreInterface {
    private ModelStorageService modelStorageService;
    private GroupStorageUpdatesStub groupStorageUpdatesStub;
    private AutoUser autoUser;
    private static final Long AUTO_UID = 28027378L;

    public ModelStoreInterfaceStub(ModelStorageServiceStub modelStorageService) {
        this(modelStorageService, modelStorageService.getIdGenerator(), modelStorageService.getIdGenerator());
    }

    public ModelStoreInterfaceStub(ModelStorageService modelStorageService,
                                   MultiIdGenerator idSequence, MultiIdGenerator generatedIdSequence) {
        this(modelStorageService, idSequence, generatedIdSequence, new AutoUser(AUTO_UID));
    }

    public ModelStoreInterfaceStub(ModelStorageService modelStorageService,
                                   MultiIdGenerator idSequence,
                                   MultiIdGenerator generatedIdSequence,
                                   AutoUser autoUser) {
        this.modelStorageService = modelStorageService;
        setIdSequence(idSequence);
        setGeneratedIdSequence(generatedIdSequence);
        this.autoUser = autoUser;
    }

    public ModelStoreInterfaceStub(GroupStorageUpdatesStub groupStorageUpdatesStub,
                                   MultiIdGenerator idSequence, MultiIdGenerator generatedIdSequence) {
        this.groupStorageUpdatesStub = groupStorageUpdatesStub;
        setIdSequence(idSequence);
        setGeneratedIdSequence(generatedIdSequence);
    }

    public ModelStoreInterfaceStub(ModelStorageServiceStub modelStorageService, AutoUser autoUser) {
        this(modelStorageService,
            modelStorageService.getIdGenerator(),
            modelStorageService.getIdGenerator(),
            autoUser);
    }

    public List<CommonModel> getModelById(Collection<Long> modelIds) {
        if (groupStorageUpdatesStub != null) {
            Set<Long> ids = new HashSet<>(modelIds);
            return groupStorageUpdatesStub.getAllModels().stream()
                .filter(m -> ids.contains(m.getId()))
                .map(CommonModel::new)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public List<CommonModel> getModelByParentId(Collection<Long> parentIds) {
        if (groupStorageUpdatesStub != null) {
            Set<Long> ids = new HashSet<>(parentIds);
            return groupStorageUpdatesStub.getAllModels().stream()
                .filter(m -> ids.contains(m.getParentModelId()))
                .map(CommonModel::new)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public Model getModelById(long categoryId, long modelId, ReadStats readStats) throws ModelStoreException {
        Optional<CommonModel> res = Optional.empty();
        if (modelStorageService != null) {
            res = modelStorageService.getModel(categoryId, modelId);
        }
        if (groupStorageUpdatesStub != null) {
            res = groupStorageUpdatesStub.getModel(categoryId, modelId, new ReadStats());
        }
        return res.isPresent() ? ModelProtoConverter.convert(res.get()) : null;
    }

    @Override
    public List<Model> getModels(long categoryId, Collection<Long> modelIds, ReadStats readStats)
        throws ModelStoreException {
        List<CategoryModelId> idsToGet = modelIds.stream()
            .map(id -> new CategoryModelId(categoryId, id))
            .collect(Collectors.toList());
        return getModels(idsToGet, readStats);
    }

    @Override
    public List<Model> getModels(Collection<CategoryModelId> modelIds, ReadStats stats)
        throws ModelStoreException {
        List<Model> result = new ArrayList<>();
        for (CategoryModelId id : modelIds) {
            Model modelById = getModelById(id.getCategoryId(), id.getModelId());
            if (modelById != null) {
                result.add(modelById);
            }
        }
        return result;
    }

    @Override
    public List<Long> saveClusters(Collection<Model> clusters, SaveStats stats) throws ModelStoreException {
        ModelStoreUtils.checkAllClusters(clusters);
        return saveModels(clusters);

    }

    @Override
    public MonitoringResult monitoring() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processCategoryModels(long categoryId, CommonModel.Source type, Boolean deleted, Integer maxRows,
                                      Consumer<Model> processor, ReadStats stats)
        throws ModelStoreException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processCategoryModelsFilters(long categoryId, Set<CommonModel.Source> type, Boolean deleted,
                                             Integer maxRows, Consumer<Model> processor, ReadStats stats) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean checkAndUpdate(Model oldModel, Model model, long date, OperationStats operationStats)
        throws ModelStoreException {
        if (oldModel.equals(model)) {
            // No need to update the modified timestamp if the model hasn't actually changed
            return true;
        }

        Model checkedModel = checkAndUpdateModifyTs(oldModel, model, date);
        if (checkedModel == null) {
            return false;
        }

        OperationStatus os = null;
        if (modelStorageService != null) {
            os = modelStorageService.saveModel(ModelProtoConverter.convert(checkedModel), autoUser.getId())
                .getSingleModelStatus();
        }
        if (groupStorageUpdatesStub != null) {
            long deletionTs = date - 1;
            os = groupStorageUpdatesStub
                .putToStorage(
                    Collections.singletonList(
                        ModelProtoConverter.convert(
                            oldModel
                                .toBuilder()
                                .setDeleted(true)
                                .setModifiedTs(deletionTs)
                                .build()
                        )
                    )
                )
                .getSingleModelStatus();
            if (!os.isOk()) {
                throw new ModelStoreException("Failed to save " + oldModel.getId());
            }
            os = groupStorageUpdatesStub
                .putToStorage(Collections.singletonList(ModelProtoConverter.convert(checkedModel)))
                .getSingleModelStatus();
        }
        if (!os.isOk()) {
            throw new ModelStoreException("Failed to save " + model.getId());
        }
        return true;
    }

    @Override
    public boolean checkAndUpdate(Model model, long date, OperationStats operationStats)
        throws ModelStoreException {
        Model oldModel = getModelById(model.getCategoryId(), model.getId());
        if (oldModel == null) {
            throw new ModelStoreException("Model " + model.getId() + " has no older versions.");
        }

        return checkAndUpdate(oldModel, model, date);
    }

    @Override
    public Map<Long, Boolean> checkAndUpdate(Map<Long, Model> oldModels, List<Model> models, long date,
                                             OperationStats operationStats)
        throws ModelStoreException {
        Map<Long, Boolean> res = new HashMap<>();
        for (Model model : models) {
            res.put(model.getId(), checkAndUpdate(oldModels.get(model.getId()), model, date, operationStats));
        }
        return res;
    }

    @Override
    public Map<Long, Boolean> checkAndUpdate(List<Model> models, long date, OperationStats operationStats)
        throws ModelStoreException {
        Map<Long, Boolean> res = new HashMap<>();
        for (Model model : models) {
            res.put(model.getId(), checkAndUpdate(model, date, operationStats));
        }
        return res;
    }

    protected Model checkAndUpdateModifyTs(Model oldModel, Model model, long date) {
        if (oldModel.getModifiedTs() > model.getModifiedTs()) {
            return null;
        }
        return model.toBuilder()
            .setModifiedTs(date)
            .build();
    }

    @Override
    @SuppressWarnings("checkstyle:LineLength")
    public ModelStoreResult saveModels(ModelStoreSaveGroup modelStoreSaveGroup,
                                       Function<List<Model.Builder>, ModelStoreCallbackResult> beforeModelsSaveCallback,
                                       Function<ModelStoreResultBuilder, ModelStoreCallbackResult> afterModelSaveCallback,
                                       long date, OperationStats operationStats) throws ModelStoreException {
        ModelStoreResultBuilder resultBuilder = ModelStoreResultBuilder.newBuilder();
        Map<Long, Set<Long>> beforeModelIdsByCategoryId = modelStoreSaveGroup.getBeforeModelIdsByCategoryId();

        Map<Long, Model> storageModelsByIds = getModelsInternal(beforeModelIdsByCategoryId);

        for (Model model : modelStoreSaveGroup.getModelsToSave()) {
            long beforeId = modelStoreSaveGroup.getBeforeId(model);
            Model oldModel = storageModelsByIds.get(beforeId);

            try {
                if (oldModel == null) {
                    saveModels(Collections.singletonList(model));
                    resultBuilder.addUpdatedModelId(model.getId());
                    resultBuilder.addModelIdToAudit(model.getId());
                } else if (!checkAndUpdate(oldModel, model, date)) {
                    resultBuilder
                        .setStatus(ModelStorage.OperationStatusType.MODEL_MODIFIED)
                        .addStatusModel(oldModel);
                    resultBuilder.addIgnoredModelId(model.getId());
                    return resultBuilder.build();
                } else if (!oldModel.equals(model)) {
                    resultBuilder.addUpdatedModelId(model.getId());
                    resultBuilder.addModelIdToAudit(model.getId());

                    if (oldModel.getId() != model.getId()) {
                        resultBuilder.addModelTransition(
                            new ModelProtoTransition(oldModel, model, System.currentTimeMillis()));
                    }
                } else {
                    resultBuilder.addIgnoredModelId(model.getId());
                }
            } catch (ModelStoreException e) {
                resultBuilder
                    .setStatus(ModelStorage.OperationStatusType.MODEL_MODIFIED)
                    .addStatusModel(oldModel);
                return resultBuilder.build();
            }
        }
        afterModelSaveCallback.apply(resultBuilder);
        return resultBuilder.build();

    }

    @Override
    public ModelStoreResult saveModels(ModelStoreSaveGroup modelStoreSaveGroup,
                                       Function<List<Model.Builder>, ModelStoreCallbackResult> beforeModelsSaveCallback,
                                       long date, OperationStats operationStats) throws ModelStoreException {
        return saveModels(modelStoreSaveGroup, beforeModelsSaveCallback,
            msrb -> ModelStoreCallbackResult.success(), date, operationStats);
    }

    private List<Long> saveModels(Collection<Model> models) throws ModelStoreException {
        // Find models without id
        List<Model> modelsWithoutIds = new ArrayList<>();
        for (Model model : models) {
            if (ModelStoreUtils.isNewIdRequired(model)) {
                modelsWithoutIds.add(model);
            }
        }

        List<Long> ids = generateIds(modelsWithoutIds);
        List<Model> modelsToSave = ModelStoreUtils.updateModelIds(models, ids);

        List<Long> result = new ArrayList<>();
        for (Model model : modelsToSave) {
            OperationStatus ps = null;
            if (modelStorageService != null) {
                ps = modelStorageService.saveModel(ModelProtoConverter.convert(model), AUTO_UID)
                    .getSingleModelStatus();
            }
            if (groupStorageUpdatesStub != null) {
                ps = groupStorageUpdatesStub
                    .putToStorage(Collections.singletonList(ModelProtoConverter.convert(model)))
                    .getSingleModelStatus();
            }
            if (ps == null || !ps.isOk()) {
                throw new ModelStoreException("Failed to save " + model.getId());
            }
            result.add(ps.getModelId());
        }
        return result;
    }

    @Override
    public ModelStoreResult archiveModels(ModelStoreSaveGroup modelStoreSaveGroup,
                                          Function<List<Builder>, ModelStoreCallbackResult>
                                              beforeModelsArchCallback, long date,
                                          OperationStats operationStats)
        throws ModelStoreException {
        ModelStoreResultBuilder resultBuilder = ModelStoreResultBuilder.newBuilder();
        List<Model.Builder> modelsToSave = modelStoreSaveGroup.getModelsToSave().stream()
            .map(Model::toBuilder)
            .collect(Collectors.toList());
        Map<Long, Set<Long>> beforeModelIds = modelStoreSaveGroup.getBeforeModelIdsByCategoryId();

        try {
            Map<Long, Model.Builder> storageModelsByIds =  getModelsInternal(beforeModelIds)
                .values()
                .stream()
                .collect(Collectors.toMap(Model::getId, Model::toBuilder));

            List<Model.Builder> modelsToArchive = new ArrayList<>();
            for (Model.Builder model : modelsToSave) {
                // Checks for correctness before archiving
                if (ModelStoreUtils.isNewIdRequired(model)) {
//                            log.error("Tried to archive model without existing id");
                    resultBuilder.setStatus(OperationStatusType.RULES_ERROR).addStatusModelId(model.getId());
                    break;
                } else if (!model.hasArchived() || !model.getArchived()) {
//                            log.error("All models must be set as archived before calling this method");
                    resultBuilder.setStatus(OperationStatusType.RULES_ERROR).addStatusModelId(model.getId());
                    break;
                }

                long modelId = model.getId();
                Model.Builder existingModel = storageModelsByIds.get(modelId);
                if (existingModel == null) {
//                            log.error("Not found model with id " + modelId);
                    resultBuilder
                        .setStatus(OperationStatusType.MODEL_NOT_FOUND)
                        .addStatusModelId(modelId);
                    break;
                }  else if (existingModel.hasArchived() && existingModel.getArchived()) {
//                            log.error("Model with id " + modelId + " is already archived. Skipping.");
                    resultBuilder
                        .setStatus(OperationStatusType.RULES_ERROR)
                        .addStatusModelId(modelId);
                } else if (!existingModel.getDeleted()) {
//                            log.error("Tried to archive not deleted model with id " + modelId);
                    resultBuilder
                        .setStatus(OperationStatusType.RULES_ERROR)
                        .addStatusModelId(modelId);
                    break;
                } else if (existingModel.getCurrentType().equals("CLUSTER") &&
                    // It might be better to put it in global constants
                    existingModel.getDeletedDate() > DateTime.now().minusMonths(1).getMillis()
                    || existingModel.getDeletedDate() > DateTime.now().minusYears(1).getMillis()) {
//                            log.error("Tried to archive model " + modelId + " that was deleted too recently");
                    resultBuilder
                        .setStatus(OperationStatusType.RULES_ERROR)
                        .addStatusModelId(modelId);
                    break;
                }
                modelsToArchive.add(model);
            }

            if (resultBuilder.getStatus() == OperationStatusType.OK) {
                List<Model.Builder> allModels = new ArrayList<>(modelsToArchive);
                saveModels(allModels.stream().map(Builder::build).collect(Collectors.toList()));
            } else {
                resultBuilder
                        .setStatus(OperationStatusType.INTERNAL_ERROR);
            }
        } catch (RpcError e) {
                resultBuilder
                    .setStatus(OperationStatusType.INTERNAL_ERROR)
                    .setStatusMessage(ExceptionUtils.getMessage(e));

                resultBuilder
                    .clearUpdatedModelIds()
                    .clearModelIdsToAudit()
                    .clearModelTransitions()
                    .clearBeforeModelsToAudit()
                    .clearIgnoredModelIds();
        }
        return resultBuilder.build();
    }

    private Map<Long, Model> getModelsInternal(Map<Long, Set<Long>> beforeModelIdsByCategoryId) {
        Map<Long, Model> storageModelsByIds = new HashMap<>();
        beforeModelIdsByCategoryId.forEach((category, ids) ->
            ids.forEach(id -> {
                try {
                    Model beforeModel = getModelById(category, id);
                    if (beforeModel != null) {
                        storageModelsByIds.put(beforeModel.getId(), beforeModel);
                    }
                } catch (ModelStoreException e) {
                    throw new RuntimeException();
                }
            }));
        return storageModelsByIds;
    }

    public void setGroupStorageUpdatesStub(GroupStorageUpdatesStub groupStorageUpdatesStub) {
        this.groupStorageUpdatesStub = groupStorageUpdatesStub;
    }
}
