package ru.yandex.market.mbo.db.modelstorage.stubs;

import ru.yandex.common.util.db.MultiIdGenerator;
import ru.yandex.market.mbo.db.modelstorage.AbstractModelStore;
import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface;
import ru.yandex.market.mbo.db.modelstorage.StatsIndexedModelQueryService;
import ru.yandex.market.mbo.db.modelstorage.StatsIndexedModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.StorageStatisticService;
import ru.yandex.market.mbo.db.modelstorage.audit.ModelAuditContextProvider;
import ru.yandex.market.mbo.db.modelstorage.audit.ModelAuditService;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatus;
import ru.yandex.market.mbo.db.modelstorage.data.OperationStatusType;
import ru.yandex.market.mbo.db.modelstorage.data.OperationType;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.db.modelstorage.group.engine.MultiIdGeneratorStub;
import ru.yandex.market.mbo.db.modelstorage.health.ReadStats;
import ru.yandex.market.mbo.db.modelstorage.preprocessing.ModelSavePreprocessingService;
import ru.yandex.market.mbo.db.modelstorage.transitions.ClusterTransitionsService;
import ru.yandex.market.mbo.db.modelstorage.transitions.ModelTransitionsService;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationService;
import ru.yandex.market.mbo.db.modelstorage.validation.processing.ValidationErrorProcessingService;
import ru.yandex.market.mbo.db.repo.ModelTransitionsRepository;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Metrics;

/**
 * @author s-ermakov
 */
public class GroupStorageUpdatesStub extends StatsIndexedModelStorageService
    implements AllModelsStorage {

    private static final int USER_ID = 1993;
    private final AtomicLong atomicLong = new AtomicLong(1);
    private final Map<ModelIndexKey, CommonModel> modelsInYtIndex = new HashMap<>();

    public GroupStorageUpdatesStub() {
        super(null, null, null,
            null, null, null,
            null, null, null, null,
            null);
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public GroupStorageUpdatesStub(ModelStoreInterface modelStore,
                                   StatsIndexedModelQueryService modelQueryService,
                                   ClusterTransitionsService clusterTransitionsService,
                                   ModelTransitionsService modelTransitionsService,
                                   ModelSavePreprocessingService modelSavePreprocessingService,
                                   ModelValidationService modelValidationService,
                                   ModelAuditContextProvider modelAuditContextProvider,
                                   ValidationErrorProcessingService validationErrorProcessingService,
                                   ModelAuditService modelAuditService,
                                   ModelTransitionsRepository modelTransitionsRepository) {
        super(modelStore, modelQueryService, clusterTransitionsService, modelTransitionsService,
            modelSavePreprocessingService, modelValidationService, modelAuditContextProvider,
            validationErrorProcessingService, modelAuditService, new StorageStatisticService(Metrics.globalRegistry),
            modelTransitionsRepository);
    }

    @Override
    public Map<ModelIndexKey, CommonModel> getAllModelsMap() {
        return modelsInYtIndex;
    }

    @Override
    public Optional<CommonModel> getModel(long categoryId, long modelId, ReadStats readStats) {
        return copy(Optional.ofNullable(modelsInYtIndex.get(ModelIndexKey.from(modelId, categoryId))));
    }

    @Override
    public List<CommonModel> getValidModifications(Collection<Long> modelIds, ReadStats readStats) {
        return modelsInYtIndex.values().stream()
            .filter(model -> model.isModification() && modelIds.contains(model.getParentModelId()))
            .filter(model -> !model.isDeleted())
            .map(GroupStorageUpdatesStub::copy)
            .collect(Collectors.toList());
    }

    public GroupOperationStatus putToStorage(CommonModel... models) {
        return putToStorage(Arrays.stream(models).collect(Collectors.toList()));
    }

    public GroupOperationStatus putToStorage(Collection<CommonModel> models) {
        List<OperationStatus> statuses = models.stream()
            .map(model -> {
                if (model.isNewModel()) {
                    long id = atomicLong.getAndIncrement();
                    model.setId(id);
                } else {
                    updateNextModelId(model.getId());
                }

                CommonModel copy = new CommonModel(model);
                modelsInYtIndex.put(ModelIndexKey.from(copy.getId(), copy.getCategoryId()), copy);
                return copy;
            })
            .map(m -> {
                OperationStatus status = new OperationStatus(OperationStatusType.OK, OperationType.CREATE, m.getId());
                status.setModel(m);
                return status;
            })
            .collect(Collectors.toList());
        return new GroupOperationStatus(statuses);
    }

    private void updateNextModelId(long currentId) {
        long maxIdValue = Math.max(currentId + 1, atomicLong.get());
        atomicLong.set(maxIdValue);

        ModelStoreInterface modelStore = getModelStore();
        if (modelStore instanceof AbstractModelStore) {
            AbstractModelStore abstractModelStore = (AbstractModelStore) modelStore;

            MultiIdGenerator idSequence = abstractModelStore.getIdSequence();
            if (currentId < ModelStoreInterface.GENERATED_ID_MIN_VALUE && idSequence instanceof MultiIdGeneratorStub) {
                MultiIdGeneratorStub atomicLongIdSequence = (MultiIdGeneratorStub) idSequence;
                long maxIdSequenceValue = Math.max(currentId, atomicLongIdSequence.getLastId());
                atomicLongIdSequence.setStartId(maxIdSequenceValue);
            }

            MultiIdGenerator generatedIdSequence = abstractModelStore.getGeneratedIdSequence();
            if (generatedIdSequence instanceof MultiIdGeneratorStub) {
                MultiIdGeneratorStub atomicLongIdSequence = (MultiIdGeneratorStub) generatedIdSequence;
                long maxIdSequenceValue = Math.max(currentId, atomicLongIdSequence.getLastId());
                atomicLongIdSequence.setStartId(maxIdSequenceValue);
            }
        }
    }

    public void deleteModels(Collection<CommonModel> models) {
        deleteModels(models, new ModelSaveContext(USER_ID));
    }

    @Override
    public List<GroupOperationStatus> deleteModels(Collection<CommonModel> models, ModelSaveContext context) {
        return models.stream()
            .map(CommonModel::new)
            .peek(m -> m.setDeleted(true))
            .map(m -> saveModel(m, context))
            .collect(Collectors.toList());
    }

    @Override
    public List<CommonModel> getModels(long categoryId, Collection<Long> ids, ReadStats stats) {
        return ids.stream()
            .map(id -> ModelIndexKey.from(id, categoryId))
            .map(key -> copy(Optional.ofNullable(modelsInYtIndex.get(key))))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }


    @Override
    public List<CommonModel> getModelsWithParent(long categoryId, Collection<Long> ids, ReadStats stats) {
        return getModels(categoryId, ids, stats);
    }

    private static Optional<CommonModel> copy(Optional<CommonModel> modelOpt) {
        return modelOpt.map(GroupStorageUpdatesStub::copy);
    }

    private static CommonModel copy(CommonModel model) {
        return new CommonModel(model);
    }

    private static List<CommonModel> copy(Collection<CommonModel> models) {
        return models.stream()
            .map(CommonModel::new)
            .collect(Collectors.toList());
    }
}
