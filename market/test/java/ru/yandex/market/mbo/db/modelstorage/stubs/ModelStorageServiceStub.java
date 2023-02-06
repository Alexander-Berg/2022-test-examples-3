package ru.yandex.market.mbo.db.modelstorage.stubs;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import org.springframework.core.convert.converter.Converter;

import ru.yandex.common.util.db.MultiIdGenerator;
import ru.yandex.market.mbo.core.modelstorage.util.ModelProtoConverter;
import ru.yandex.market.mbo.db.modelstorage.IndexedModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageServiceUtil;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface.ModelStoreException;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusConverter;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.db.modelstorage.health.SaveStats;
import ru.yandex.market.mbo.db.modelstorage.index.GenericField;
import ru.yandex.market.mbo.db.modelstorage.index.MboIndexesFilter;
import ru.yandex.market.mbo.db.modelstorage.merge.ModelMergeService;
import ru.yandex.market.mbo.db.modelstorage.store.ModelStoreResult;
import ru.yandex.market.mbo.db.modelstorage.store.ModelStoreSaveGroup;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelRelation;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.utils.XslNames;
import ru.yandex.market.mbo.http.ModelStorage;

/**
 * Created by anmalysh on 23.03.2017.
 */
public class ModelStorageServiceStub extends IndexedModelStorageService {

    public static final int RANDOM_SEED = 100500;
    public static final LocalDateTime LAST_MODIFIED_START = LocalDateTime.of(2019, 1, 1, 0, 0);
    public static final Date LAST_MODIFIED_START_DATE =
        new Date(LAST_MODIFIED_START.toInstant(ZoneOffset.UTC).toEpochMilli());
    protected final AtomicInteger modelCounter = new AtomicInteger(1);
    protected final AtomicBoolean initialized = new AtomicBoolean(false);
    protected final AtomicLong lastModifiedCounter = new AtomicLong(1);
    protected Map<Long, CommonModel> modelsMap;
    protected EnhancedRandom idGenerator = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
        .seed(RANDOM_SEED)
        .build();

    protected ModelMergeService modelMergeService;

    public ModelStorageServiceStub() {
        super(null, null, null);
        modelsMap = new LinkedHashMap<>();
    }

    public ModelStorageServiceStub(CommonModel... models) {
        this(Arrays.asList(models));
    }

    public ModelStorageServiceStub(Collection<CommonModel> models) {
        super(null, null, null);
        this.modelsMap = new LinkedHashMap<>(models.size());
        initializeWithModels(models);
    }

    MultiIdGenerator getIdGenerator() {
        return new MultiIdGenerator() {
            @Override
            public List<Long> getIds(int count) {
                Integer last = modelCounter.addAndGet(count);
                List<Long> result = new ArrayList<>();
                long start = last - count + 1;
                for (int i = 0; i < count; i++) {
                    result.add(start + i);
                }
                return result;
            }

            @Override
            public long getId() {
                return modelCounter.incrementAndGet();
            }
        };
    }

    public void setModelMergeService(ModelMergeService modelMergeService) {
        this.modelMergeService = modelMergeService;
    }

    @Override
    public GroupOperationStatus saveModel(CommonModel model, ModelSaveContext context) {
        return saveModelInternal(model, context, nextModificationDate());
    }

    private GroupOperationStatus saveModelInternal(CommonModel model,
                                                   ModelSaveContext context, Date modificationDate) {
        CommonModel newModel;
        if (model.isNewModel()) {
            model.setId(modelCounter.getAndIncrement());
            newModel = new CommonModel(model);
        } else {
            if (modelMergeService != null) {
                CommonModel currentModel = modelsMap.get(model.getId());
                newModel = modelMergeService.merge(currentModel, model,
                    context.getOperationSource(), context.getMergeType());
            } else {
                newModel = new CommonModel(model);
            }
        }

        newModel.setModificationDate(modificationDate);
        if (context.getUid() != 0L) {
            newModel.setModifiedUserId(context.getUid());
        }

        if (context.shouldWriteChanges()) {
            modelsMap.put(newModel.getId(), newModel);
        }
        OperationType type = model.isNewModel() ? OperationType.CREATE :
            (model.isDeleted() ? OperationType.REMOVE : OperationType.CHANGE);
        OperationStatus status = new OperationStatus(OperationStatusType.OK, type, newModel.getId());
        status.setModel(newModel);
        GroupOperationStatus groupOperationStatus = new GroupOperationStatus(status);
        return groupOperationStatus;
    }

    private Date nextModificationDate() {
        return new Date(LAST_MODIFIED_START.plusSeconds(lastModifiedCounter.getAndIncrement())
            .toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    @Override
    public GroupOperationStatus deleteModel(CommonModel model, ModelSaveContext context) {
        CommonModel removed = modelsMap.remove(model.getId());
        return new GroupOperationStatus(
            new OperationStatus(removed != null ? OperationStatusType.OK : OperationStatusType.NO_OP,
                OperationType.REMOVE, model.getId())
        );
    }

    @Override
    public GroupOperationStatus deleteModelById(long categoryId, long modelId, ModelSaveContext context) {
        CommonModel removed = modelsMap.remove(modelId);
        return new GroupOperationStatus(
            new OperationStatus(removed != null ? OperationStatusType.OK : OperationStatusType.NO_OP,
                OperationType.REMOVE, modelId)
        );
    }

    @Override
    public List<GroupOperationStatus> deleteModels(Collection<CommonModel> models, ModelSaveContext context) {
        List<GroupOperationStatus> statuses = new ArrayList<>(models.size());
        models.forEach(model -> statuses.add(deleteModel(model, context)));
        return statuses;
    }

    @Override
    public GroupOperationStatus archiveModels(Collection<CommonModel> models, ModelSaveContext context) {
        Date currentDate = new Date();
        List<ModelStorage.Model> modelsToArchive = new ArrayList<>(models.size());
        for (CommonModel model : models) {
            if (model.getId() <= CommonModel.NO_ID) {
                throw new RuntimeException("Model id is not positive");
            } else {
                model.setArchived(true);
                model.setArchivedDate(currentDate);
                modelsToArchive.add(ModelProtoConverter.convert(model));
            }
        }
        Map<Long, Set<Long>> modelIdsByCategoryId = modelsToArchive.stream()
            .collect(Collectors.groupingBy(ModelStorage.Model::getCategoryId,
                Collectors.mapping(ModelStorage.Model::getId, Collectors.toSet())));
        ModelStoreSaveGroup modelStoreSaveGroup = new ModelStoreSaveGroup(
            modelsToArchive,
            // There must be no new models
            Collections.emptyList(),
            Collections.emptyMap(),
            modelIdsByCategoryId
        );
        ModelStoreInterfaceStub modelStore = new ModelStoreInterfaceStub(this);
        try {
            ModelStoreResult result =
                modelStore.archiveModels(modelStoreSaveGroup, currentDate.getTime(), context.getStats());
            OperationStatusType statusType = OperationStatusConverter.convert(result.getModelStoreStatus().getStatus());
            if (statusType != OperationStatusType.OK) {
                throw new RuntimeException();
            }
        } catch (ModelStoreException e) {
            throw new RuntimeException();
        }
        return null;
    }

    @Override
    public boolean linkModels(Long categoryId, Long sourceModelId, Long targetModelId, ModelSaveContext context) {
        Optional<CommonModel> sourceModel = getModel(categoryId, sourceModelId);
        Optional<CommonModel> targetModel = getModel(categoryId, targetModelId);
        if (!sourceModel.isPresent() || !targetModel.isPresent()) {
            return false;
        }
        // Add relations to source model.
        replaceRelationsOfType(targetModel.get(), ModelRelation.RelationType.SYNC_SOURCE,
            sourceModel.get().getId(), sourceModel.get().getCategoryId());
        // Add relations to target model.
        replaceRelationsOfType(sourceModel.get(), ModelRelation.RelationType.SYNC_TARGET,
            targetModel.get().getId(), targetModel.get().getCategoryId());
        modelsMap.put(sourceModel.get().getId(), sourceModel.get());
        modelsMap.put(targetModel.get().getId(), targetModel.get());
        return true;
    }

    @Override
    public Optional<CommonModel> getModel(long categoryId, Long id) {
        CommonModel model = modelsMap.get(id);
        if (model != null && model.getCategoryId() == categoryId) {
            return Optional.of(new CommonModel(model));
        }
        return Optional.empty();
    }

    @Override
    public List<CommonModel> getModels(long categoryId, Collection<Long> ids) {
        List<CommonModel> result = new ArrayList<>();
        for (Long id : ids) {
            CommonModel model = modelsMap.get(id);
            if (model != null && model.getCategoryId() == categoryId) {
                result.add(new CommonModel(model));
            }
        }
        return result;
    }

    @Override
    public void processAllCategoryModels(long categoryId, CommonModel.Source type, Consumer<CommonModel> callback) {
        for (CommonModel model : modelsMap.values()) {
            if (model.getCategoryId() == categoryId && model.getCurrentType().equals(type)) {
                callback.accept(new CommonModel(model));
            }
        }
    }

    @Override
    public void processAllCategoryModels(long categoryId, CommonModel.Source type, Boolean deleted, Integer maxRows,
                                         Consumer<CommonModel> processor) {
        int rows = 0;
        for (CommonModel model : modelsMap.values()) {
            if (model.getCategoryId() == categoryId &&
                model.getCurrentType().equals(type) &&
                deleted.equals(model.isDeleted()) &&
                rows++ < maxRows) {
                processor.accept(new CommonModel(model));
            }
        }
    }

    @Override
    public void processCategoryModels(long categoryId, Collection<Long> modelIds, Consumer<CommonModel> callback,
                                      Consumer<Long> missingModelCallback) {
        for (Long modelId : modelIds) {
            CommonModel model = modelsMap.get(modelId);
            if (model == null || model.getCategoryId() != categoryId) {
                missingModelCallback.accept(modelId);
            } else {
                callback.accept(new CommonModel(model));
            }
        }
    }

    @Override
    public CommonModel searchById(long id) {
        CommonModel model = modelsMap.get(id);
        if (model != null) {
            model = new CommonModel(model);
        }
        return model;
    }

    @Override
    public List<CommonModel> searchByIds(Collection<Long> ids) {
        List<CommonModel> result = new ArrayList<>();
        for (Long id : ids) {
            CommonModel model = modelsMap.get(id);
            if (model != null) {
                result.add(new CommonModel(model));
            }
        }
        return result;
    }

    @Override
    public List<CommonModel> searchByGroupIds(Collection<Long> ids) {
        Set<Long> groupIds = new HashSet<>(ids);
        return modelsMap.values().stream().filter(model -> groupIds.contains(model.getGroupId()))
            .collect(Collectors.toList());
    }

    @Override
    public List<Long> getModelIdsPage(MboIndexesFilter filter) {
        if (filter.getForceUseFor() != null
            || filter.getGroupModelIds() != null
            || filter.getParentIds() != null
            || filter.getParentIdExists() != null
            || filter.getCategoryIds() != null
            || filter.getVendorIds() != null
            || filter.getSku() != null
            || filter.getPublished() != null
            || filter.getOperatorSign() != null
            || filter.getQualities() != null
            || filter.getChecked() != null
            || filter.getClusterizerOfferIds() != null
            || filter.getClusterizerOfferCount() != null) {
            throw new UnsupportedOperationException();
        }

        Set<Long> filterModelIds = filter.getModelIds() == null ? null : new HashSet<>(filter.getModelIds());
        Set<CommonModel.Source> filterCurrentTypes = filter.getCurrentTypes() == null ?
            null : new HashSet<>(filter.getCurrentTypes());
        Set<CommonModel.Source> filterSourceTypes = filter.getSourceTypes() == null ?
            null : new HashSet<>(filter.getSourceTypes());

        return modelsMap.values().stream()
            .filter(m -> filterModelIds == null || filterModelIds.contains(m.getId()))
            .filter(m -> filter.getDeleted() == null || filter.getDeleted().equals(m.isDeleted()))
            .filter(m -> filterCurrentTypes == null || filterCurrentTypes.contains(m.getCurrentType()))
            .filter(m -> filterSourceTypes == null || filterSourceTypes.contains(m.getSource()))
            .map(CommonModel::getId)
            .collect(Collectors.toList());
    }

    @Override
    public Long getCategoryId(long id) {
        CommonModel model = modelsMap.get(id);
        return model == null ? null : model.getCategoryId();
    }

    @Override
    public List<CommonModel> getModelsByBarcodes(List<String> barcodes, List<CommonModel.Source> processingTypes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CommonModel> getModelsByVendorCodes(long categoryId, long vendorId, List<String> vendorCodes,
                                                    List<CommonModel.Source> processingTypes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CommonModel> getModelsByAliases(Collection<String> aliases, Set<Long> categoryIds,
                                                Set<Long> vendorIds, Set<Long> excludeModelIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CommonModel> getModelsPage(MboIndexesFilter filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> Set<T> getFieldValues(
        GenericField genericField, MboIndexesFilter filter, Converter<String, T> converter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processQueryModels(long categoryId, MboIndexesFilter filter, Consumer<CommonModel> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processQueryFullModels(long categoryId, MboIndexesFilter filter,
                                       Consumer<ModelStorage.Model> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processQueryModels(MboIndexesFilter filter, Consumer<CommonModel> processor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processQueryModels(MboIndexesFilter filter, Function<CommonModel, Boolean> processor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void processQueryIds(MboIndexesFilter filter, Consumer<Long> callback) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(long modelId) {
        return modelsMap.containsKey(modelId);
    }

    public void setModelsMap(Map<Long, CommonModel> modelsMap) {
        this.modelsMap = modelsMap;
    }

    private CommonModel replaceRelationsOfType(CommonModel model,
                                               ModelRelation.RelationType type,
                                               Long modelId,
                                               Long categoryId) {
        model.setRelations(
            model.getRelations().stream()
                .filter(r -> !type.equals(r.getType()))
                .collect(Collectors.toList()));
        model.getRelations().add(new ModelRelation(modelId, categoryId, type));
        return model;
    }

    @Override
    public GroupOperationStatus saveModels(ModelSaveGroup modelSaveGroup, ModelSaveContext modelSaveContext) {
        // must be same for group
        Date date = nextModificationDate();
        updateNamesInSKUs(modelSaveGroup);
        setIdsAndUpdateRelations(modelSaveGroup, modelSaveContext.getStats().getSaveStats());
        modelSaveGroup.getModels().forEach(model -> {
            OperationStatus status = saveModelInternal(model, modelSaveContext, date).getSingleModelStatus();
            modelSaveGroup.setStatus(model, status.getStatus());
        });
        return modelSaveGroup.generateOverallStatus();
    }

    private List<Long> generateIds(Collection<CommonModel> models, SaveStats saveStats) {
        if (models == null || models.isEmpty()) {
            return new ArrayList<>();
        } else {
            return idGenerator.longs().filter(id -> id > 0).limit(models.size()).boxed().collect(Collectors.toList());
        }
    }

    private List<Long> setIdsAndUpdateRelations(ModelSaveGroup modelSaveGroup, SaveStats saveStats) {
        List<CommonModel> newModels = modelSaveGroup.getModels().stream()
            .filter(CommonModel::isNewModel)
            .collect(Collectors.toList());

        List<Long> newIds = generateIds(newModels, saveStats);

        for (int i = 0; i < newIds.size(); i++) {
            Long id = newIds.get(i);
            CommonModel model = newModels.get(i);
            ModelStorageServiceUtil.setIdWithUpdateRelations(id, model, modelSaveGroup);
        }
        ModelStorageServiceUtil.addOppositeRelations(modelSaveGroup);
        return newIds;
    }

    private void updateNamesInSKUs(ModelSaveGroup modelSaveGroup) {
        Set<CommonModel> updatedModels = new HashSet<>();

        List<ModelChanges> changedModelChanges = modelSaveGroup.getModelChangesOfType(OperationType.CHANGE);

        //Для всех не новых гуру-моделей с изменённым именем.....
        changedModelChanges.stream()
            .filter(change -> change.getBefore() != null)
            .filter(change -> change.getBefore().getCurrentType().equals(CommonModel.Source.GURU))
            .filter(ModelStorageServiceUtil::hasTitleChanged)
            .forEach(modelChanges -> { //......ищем их SKU и обновляем имя
                CommonModel guru = modelChanges.getAfter();
                guru.getRelations(ModelRelation.RelationType.SKU_MODEL).forEach(relation -> {
                    CommonModel sku = relation.getModel();
                    long categoryId = guru.getCategoryId();
                    ParameterValue nameParamValue = guru.getSingleParameterValue(XslNames.NAME);
                    //старые SKU подгружаются из хранилища и relation::model у них равна null, перестрахуемся:
                    if (sku == null) {
                        sku = getModel(categoryId, relation.getId()).orElse(null);
                    }
                    if (sku != null) {
                        sku.setSingleTitle(guru.getTitle()); //На основе гуру-имени создастся тайтл с параметрами
                        sku.getSingleParameterValue(XslNames.NAME) //источник изменения берём с родительской модели
                            .setModificationSource(nameParamValue.getModificationSource());
                        updatedModels.add(sku);
                    }
                });
            });
        modelSaveGroup.addAllIfAbsent(updatedModels);
    }

    public List<CommonModel> getAllModels() {
        return new ArrayList<>(modelsMap.values());
    }

    public void initializeWithModels(Collection<CommonModel> models) {
        if (initialized.getAndSet(true)) {
            throw new IllegalStateException("test stub can be initialized only once");
        }
        modelsMap.clear();
        modelsMap.putAll(
            models.stream()
                .collect(Collectors.toMap(CommonModel::getId, Function.identity()))
        );
    }

    public void initializeWithModels(CommonModel... models) {
        initializeWithModels(Arrays.asList(models));
    }

    @Override
    public List<CommonModel> getModifications(long modelId) {
        return modelsMap.values().stream()
            .filter(m -> m.getParentModelId() == modelId)
            .collect(Collectors.toList());
    }
}
