package ru.yandex.market.ir.autogeneration_api.http.service;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.exception.ExceptionUtils;
import ru.yandex.common.util.collections.CollectionUtils;
import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.ir.autogeneration.common.util.MapUtils;
import ru.yandex.market.ir.autogeneration.common.util.ModelProtoUtils;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelCardApi.RemoveModelWithTransitionsRequest;
import ru.yandex.market.mbo.http.ModelCardApi.SaveModelsGroupOperationResponse;
import ru.yandex.market.mbo.http.ModelCardApi.SaveModelsGroupRequest;
import ru.yandex.market.mbo.http.ModelCardApi.SaveModelsGroupResponse;
import ru.yandex.market.mbo.http.ModelCardApi.SyncGuruModelsRequest;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.FindModelsRequest;
import ru.yandex.market.mbo.http.ModelStorage.GetModelsRequest;
import ru.yandex.market.mbo.http.ModelStorage.GetModelsResponse;
import ru.yandex.market.mbo.http.ModelStorage.Model;
import ru.yandex.market.mbo.http.ModelStorage.ModelOrBuilder;
import ru.yandex.market.mbo.http.ModelStorage.OperationResponse;
import ru.yandex.market.mbo.http.ModelStorage.OperationStatus;
import ru.yandex.market.mbo.http.ModelStorage.OperationStatusType;
import ru.yandex.market.mbo.http.ModelStorage.Relation;
import ru.yandex.market.mbo.http.ModelStorage.RelationType;
import ru.yandex.market.mbo.http.ModelStorage.RemoveModelsRequest;
import ru.yandex.market.mbo.http.ModelStorage.SaveModelsRequest;
import ru.yandex.market.mbo.http.ModelStorage.UploadDetachedImagesRequest;
import ru.yandex.market.mbo.http.ModelStorage.UploadDetachedImagesResponse;
import ru.yandex.market.mbo.http.ModelStorage.UploadImageRequest;
import ru.yandex.market.mbo.http.ModelStorage.ValidateImagesRequest;
import ru.yandex.market.mbo.http.ModelStorage.ValidateImagesResponse;
import ru.yandex.market.mbo.http.ModelStorage.VoidRequest;
import ru.yandex.market.mbo.http.ModelStorageService;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ayratgdl
 * @date 05.06.18
 */
public class ModelStorageServiceMock implements ModelStorageService {
    private static final Map<RelationType, RelationType> RELATIONS =
        ImmutableMap.<RelationType, RelationType>builder()
            .put(ModelStorage.RelationType.SYNC_SOURCE, ModelStorage.RelationType.SYNC_TARGET)
            .put(ModelStorage.RelationType.SYNC_TARGET, ModelStorage.RelationType.SYNC_SOURCE)
            .put(ModelStorage.RelationType.EXPERIMENTAL_MODEL, ModelStorage.RelationType.EXPERIMENTAL_BASE_MODEL)
            .put(ModelStorage.RelationType.EXPERIMENTAL_BASE_MODEL, ModelStorage.RelationType.EXPERIMENTAL_MODEL)
            .put(ModelStorage.RelationType.SKU_MODEL, ModelStorage.RelationType.SKU_PARENT_MODEL)
            .put(ModelStorage.RelationType.SKU_PARENT_MODEL, ModelStorage.RelationType.SKU_MODEL)
            .build();

    private final Map<Long, Model> models = new HashMap<>();
    private final Multimap<String, Long> barcodeIndex = HashMultimap.create();
    private final Map<String, PictureStatus> pictureStatuses = new HashMap<>();
    private final Set<Long> indestructibleModelsIds = new HashSet<>();
    private final Map<Long, ModelStorage.ModelTransition> transitions = new HashMap<>();
    private long nextModelId = 1;

    private Function<String, PictureStatus> missingPictureStatus = url -> new PictureStatus(
            null,
            OperationStatus.newBuilder()
                    .setStatus(OperationStatusType.INTERNAL_ERROR)
                    .setType(ModelStorage.OperationType.UPLOAD_IMAGE)
                    .build()
    );


    public ModelStorageServiceMock putModel(ModelOrBuilder model) {
        models.put(model.getId(), model instanceof Model ? ((Model) model) : ((Model.Builder) model).build());
        nextModelId = Math.max(nextModelId, model.getId() + 1);
        model.getParameterValuesList().stream()
            .filter(parameterValue -> parameterValue.getParamId() == ParameterValueComposer.BARCODE_ID)
            .map(ModelStorage.ParameterValue::getStrValueList)
            .flatMap(Collection::stream)
            .map(ModelStorage.LocalizedString::getValue)
            .forEach(barcode -> barcodeIndex.put(barcode, model.getId()));
        return this;
    }

    public ModelStorageServiceMock putModels(ModelOrBuilder... values) {
        return putModels(Arrays.asList(values));
    }

    public ModelStorageServiceMock putModels(Collection<? extends ModelOrBuilder> values) {
        for (ModelOrBuilder value : values) {
            putModel(value);
        }
        return this;
    }

    public void makeModelIndestructible(Long modelId) {
        indestructibleModelsIds.add(modelId);
    }

    public ModelStorageServiceMock putPicture(String picUrl, ModelStorage.Picture picture, OperationStatus status) {
        pictureStatuses.put(picUrl, new PictureStatus(picture, status));
        return this;
    }

    public ModelStorageServiceMock putTransition(ModelStorage.ModelTransition transition) {
        if (transition != null) {
            transitions.put(transition.getOldEntityId(), transition);
        }
        return this;
    }

    public Map<Long, ModelStorage.ModelTransition> getTransitionsMap() {
        return ImmutableMap.copyOf(transitions);
    }

    public Map<Long, Model> getModelsMap() {
        return ImmutableMap.copyOf(models);
    }

    public void clearModels() {
        models.clear();
    }

    public Model extractModel(long id) {
        return models.get(id);
    }

    public Model findModel(long modelId) {
        return models.get(modelId);
    }

    private Model getModel(long categoryId, long modelId) {
        return getModels(categoryId, Collections.singletonList(modelId)).stream().findFirst().orElse(null);
    }

    public List<Model> getModels(long categoryId, List<Long> modelIds) {
        return getModels(GetModelsRequest.newBuilder()
            .setCategoryId(categoryId)
            .addAllModelIds(modelIds).build()).getModelsList();
    }

    public void setMissingPictureStatus(Function<String, PictureStatus> missingPictureStatus) {
        this.missingPictureStatus = missingPictureStatus;
    }

    @Override
    public GetModelsResponse getModels(GetModelsRequest request) {
        GetModelsResponse.Builder response = GetModelsResponse.newBuilder();
        models.values().stream()
            .filter(m -> m.getCategoryId() == request.getCategoryId())
            .filter(m -> request.getModelIdsList().contains(m.getId()))
            .forEach(response::addModels);
        return response.build();
    }

    @Override
    public ModelStorage.GetModelIdsByBarcodeResponse getModelIdsByBarcode(
        ModelStorage.GetModelIdsByBarcodeRequest getModelIdsByBarcodeRequest
    ) {
        ModelStorage.GetModelIdsByBarcodeResponse.Builder builder = ModelStorage.GetModelIdsByBarcodeResponse
            .newBuilder();
        for (String barcode : getModelIdsByBarcodeRequest.getBarcodeList()) {
            Collection<Long> modelIds = barcodeIndex.get(barcode);
            if (CollectionUtils.isNonEmpty(modelIds)) {
                builder.addBarcodeResponse(ModelStorage.BarcodeToModelIds.newBuilder()
                    .setBarcode(barcode)
                    .addAllModelIds(modelIds)
                    .build()
                );
            }
        }
        return builder.build();
    }

    @Override
    public ModelStorage.AllocateSkuIdsResponse allocateSkuIds(ModelStorage.AllocateSkuIdsRequest request) {
        throw new UnsupportedOperationException("Not implement");
    }

    @Override
    public ModelStorage.AllocateModelIdsResponse allocateModelIds(ModelStorage.AllocateModelIdsRequest request) {
        throw new UnsupportedOperationException("Not implement");
    }

    @Override
    public GetModelsResponse findModels(FindModelsRequest request) {
        long offset = request.hasOffset() ? request.getOffset() : 0;
        long limit = request.hasLimit() ? request.getLimit() : Long.MAX_VALUE;
        List<Model> foundModels = models.values().stream()
            .filter(m -> match(m, request))
            .skip(offset)
            .limit(limit)
            .collect(Collectors.toList());
        return GetModelsResponse.newBuilder()
            .addAllModels(foundModels)
            .build();
    }

    @Override
    public ModelStorage.GetStateResponse getState(VoidRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OperationResponse saveModels(SaveModelsRequest request) {
        OperationResponse.Builder response = OperationResponse.newBuilder();
        Map<Long, Model> requestedModels = request.getModelsList().stream()
            .filter(m -> m.getId() != 0)
            .collect(Collectors.toMap(Model::getId, Function.identity(), (f, s) -> f));
        for (Model model : request.getModelsList()) {
            model = requestedModels.getOrDefault(model.getId(), model);
            OperationStatus.Builder statusBuilder = OperationStatus.newBuilder()
                .setStatus(OperationStatusType.OK);
            long oldModelId = model.getId();

            // if parent model relations changed, remove opposite relations from old parents
            Model beforeModel = models.get(model.getId());
            if (beforeModel != null) {
                Set<Long> oldParentIds = beforeModel.getRelationsList().stream()
                        .filter(relation -> relation.getType() == RelationType.SKU_PARENT_MODEL)
                        .map(Relation::getId)
                        .collect(Collectors.toSet());
                Set<Long> newParentIds = model.getRelationsList().stream()
                        .map(Relation::getId)
                        .collect(Collectors.toSet());

                oldParentIds.removeAll(newParentIds);
                // if parent relations changed, oldParentIds is not empty
                oldParentIds.forEach(oldParentId -> {
                    Model oldParentModel = requestedModels.getOrDefault(oldParentId, findModel(oldParentId));
                    List<Relation> cleanedRelations = oldParentModel.getRelationsList().stream()
                            .filter(relation -> relation.getType() != RelationType.SKU_MODEL
                                    || relation.getId() != oldModelId)
                            .collect(Collectors.toList());
                    oldParentModel = oldParentModel.toBuilder()
                            .clearRelations()
                            .addAllRelations(cleanedRelations)
                            .build();
                    if (requestedModels.containsKey(oldParentId)) {
                        requestedModels.put(oldParentId, oldParentModel);
                    } else {
                        putModel(oldParentModel);
                    }
                });
            }

            if (!model.hasId() || model.getId() <= 0L) {
                model = model.toBuilder().setId(nextModelId++).build();
                putModel(model);
                statusBuilder.setType(ModelStorage.OperationType.CREATE)
                    .setModelId(oldModelId <= 0 ? model.getId() : oldModelId)
                    .setModel(model);
                if (oldModelId != model.getId()) {
                    statusBuilder
                        .setNewModelId(model.getId())
                        .setModel(model);
                }
            } else {
                putModel(model);
                statusBuilder.setType(ModelStorage.OperationType.CHANGE)
                    .setModelId(model.getId())
                    .setModel(model);
            }
            response.addStatuses(statusBuilder);

            // resave models in relations
            for (Relation relation : model.getRelationsList()) {
                if (ModelStorage.ModelType.VENDOR.name().equals(model.getCurrentType())
                    && relation.getType() == RelationType.SYNC_TARGET) {
                    // guru models do not contain link to vendor models, so that skip setting relative relation
                    break;
                }

                Relation oldOppositeRelation = Relation.newBuilder()
                    .setCategoryId(model.getCategoryId())
                    .setId(oldModelId <= 0L ? oldModelId : model.getId())
                    .setType(RELATIONS.get(relation.getType()))
                    .build();
                Model relatedModel = requestedModels.getOrDefault(relation.getId(), findModel(relation.getId()));
                int indexOfOldOppositeRelation = -1;
                List<Relation> relatedRelations = new ArrayList<>(relatedModel.getRelationsList());
                for (int i = 0; i < relatedRelations.size(); i++) {
                    Relation relatedRelation = relatedRelations.get(i);
                    if (relatedRelation.equals(oldOppositeRelation)) {
                        indexOfOldOppositeRelation = i;
                    }
                }

                if (indexOfOldOppositeRelation >= 0) {
                    relatedRelations.remove(indexOfOldOppositeRelation);
                }
                relatedRelations.add(
                    Relation.newBuilder()
                        .setCategoryId(model.getCategoryId())
                        .setId(model.getId())
                        .setType(RELATIONS.get(relation.getType()))
                        .build()
                );
                relatedModel = relatedModel.toBuilder()
                    .clearRelations()
                    .addAllRelations(relatedRelations)
                    .build();
                if (requestedModels.containsKey(relatedModel.getId())) {
                    requestedModels.put(relatedModel.getId(), relatedModel);
                } else {
                    putModel(relatedModel);
                }
            }

        }
        return response.build();
    }

    @Override
    public SaveModelsGroupResponse saveModelsGroup(SaveModelsGroupRequest request) {
        SaveModelsGroupResponse.Builder response = SaveModelsGroupResponse.newBuilder();
        for (SaveModelsRequest saveModelsRequest : request.getModelsRequestList()) {
            OperationResponse operationResponse = saveModels(saveModelsRequest);
            response.addResponse(
                SaveModelsGroupOperationResponse.newBuilder()
                    .setStatus(operationResponse.getStatuses(0).getStatus())
                    .addAllRequestedModelsStatuses(operationResponse.getStatusesList())
                    .build()
            );
        }
        return response.build();
    }

    @Override
    public OperationResponse removeModels(RemoveModelsRequest request) {
        OperationResponse.Builder response = OperationResponse.newBuilder();
        OperationStatus.Builder operationStatus = OperationStatus.newBuilder()
            .setType(ModelStorage.OperationType.REMOVE);
        Map<Long, ModelStorage.ModelTransition> transitionByModelId =
                MapUtils.toMap(request.getTransitionList(), ModelStorage.ModelTransition::getOldEntityId);
        for (Model model : request.getModelsList()) {
            if (!models.containsKey(model.getId())) {
                response.addStatuses(operationStatus.setStatus(OperationStatusType.MODEL_NOT_FOUND));
                continue;
            }
            if (model.getDeleted()) {
                response.addStatuses(operationStatus.setStatus(OperationStatusType.NO_OP));
                putModel(model);
                continue;
            }
            if (indestructibleModelsIds.contains(model.getId())) {
                response.addStatuses(operationStatus.setStatus(OperationStatusType.INTERNAL_ERROR));
                continue;
            }

            Model deletedModel = models.get(model.getId()).toBuilder().setDeleted(true).build();
            putModel(deletedModel);
            putTransition(transitionByModelId.get(model.getId()));
            response.addStatuses(operationStatus.setStatus(OperationStatusType.OK).setModelId(model.getId()));
        }
        return response.build();
    }

    @Override
    public OperationResponse createGuruModels(SyncGuruModelsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OperationResponse updateGuruModels(SyncGuruModelsRequest request) {
        throw new UnsupportedOperationException();
    }

    /**
     * Mock realization is close to real one.
     */
    @Override
    @SuppressWarnings("checkstyle:methodlength")
    public OperationResponse createSkuFromGSku(
        ModelCardApi.CreateSkuFromGSkuRequest request) {
        long categoryId = request.getCategoryId();
        List<Long> gskuIds = request.getGskuIdsList();

        try {
            if (!request.hasUserId()) {
                ModelStorage.OperationResponse.Builder builder = ModelStorage.OperationResponse.newBuilder();
                gskuIds.forEach(gskuId -> {
                    builder.addStatuses(ModelStorage.OperationStatus.newBuilder()
                        .setModelId(gskuId).setFailureModelId(gskuId)
                        .setType(ModelStorage.OperationType.CREATE)
                        .setStatus(ModelStorage.OperationStatusType.NOT_SUPPORTED)
                        .setStatusMessage("userId is required"));
                });
                return builder.build();
            }
            if (gskuIds.isEmpty()) {
                ModelStorage.OperationResponse.Builder builder = ModelStorage.OperationResponse.newBuilder();
                gskuIds.forEach(gskuId -> {
                    builder.addStatuses(ModelStorage.OperationStatus.newBuilder()
                        .setModelId(gskuId)
                        .setType(ModelStorage.OperationType.CREATE)
                        .setStatus(ModelStorage.OperationStatusType.OK)
                        .setStatusMessage("Empty generated sku list"));
                });
                return builder.build();
            }

            // get skus from storage
            Map<Long, Model> modelsMap = getModels(categoryId, gskuIds).stream()
                .collect(Collectors.toMap(Model::getId, Function.identity()));
            // validate all gskus are found
            if (modelsMap.size() != gskuIds.size()) {
                ModelStorage.OperationResponse.Builder builder = ModelStorage.OperationResponse.newBuilder();
                for (long gskuId : gskuIds) {
                    boolean foundModel = modelsMap.containsKey(gskuId);
                    ModelStorage.OperationStatus.Builder operationStatus = ModelStorage.OperationStatus.newBuilder()
                        .setModelId(gskuId).setFailureModelId(gskuId)
                        .setType(ModelStorage.OperationType.CREATE)
                        .setStatus(!foundModel
                            ? ModelStorage.OperationStatusType.MODEL_NOT_FOUND
                            : ModelStorage.OperationStatusType.INTERNAL_ERROR)
                        .setStatusMessage(!foundModel
                            ? "Failed to find gsku (" + gskuId + ") in category " + categoryId
                            : "Failed to process model due to other errors in save");
                    builder.addStatuses(operationStatus);
                }
                return builder.build();
            }

            // validate all gsku has relation to vendor model and also all of them are from single vendor model
            Map<Optional<Relation>, List<Model>> toVendorRelations = modelsMap.values().stream()
                .collect(Collectors.groupingBy(ModelProtoUtils::relationToVendorModel));
            boolean hasErrors = toVendorRelations.containsKey(Optional.<Relation>empty())
                || toVendorRelations.size() > 1;

            if (hasErrors) {
                ModelStorage.OperationResponse.Builder builder = ModelStorage.OperationResponse.newBuilder();
                toVendorRelations.forEach((relationOpt, gskus) -> {
                    if (!relationOpt.isPresent()) {
                        for (Model gsku : gskus) {
                            builder.addStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setModelId(gsku.getId()).setFailureModelId(gsku.getId())
                                .setType(ModelStorage.OperationType.CREATE)
                                .setStatus(ModelStorage.OperationStatusType.INTERNAL_ERROR)
                                .setStatusMessage("Failed to find parent model in gsku: " + gsku.getId()));
                        }
                    } else {
                        long vendorModelId = relationOpt.get().getId();
                        for (Model gsku : gskus) {
                            builder.addStatuses(ModelStorage.OperationStatus.newBuilder()
                                .setModelId(gsku.getId()).setFailureModelId(gsku.getId())
                                .setType(ModelStorage.OperationType.CREATE)
                                .setStatus(ModelStorage.OperationStatusType.NOT_SUPPORTED)
                                .setStatusMessage("Generated sku (" + gsku.getId() + " " +
                                    "is from different vendor model (" + vendorModelId + ")"));
                        }
                    }
                });
                return builder.build();
            }

            // get vendor model
            long vendorModelId = toVendorRelations.keySet().iterator().next().get().getId();
            Optional<Model> vendorModelOpt = Optional.ofNullable(getModel(categoryId, vendorModelId));
            if (!vendorModelOpt.isPresent()) {
                ModelStorage.OperationResponse.Builder builder = ModelStorage.OperationResponse.newBuilder();
                for (long gskuId : gskuIds) {
                    builder.addStatuses(ModelStorage.OperationStatus.newBuilder()
                        .setModelId(gskuId).setFailureModelId(vendorModelId)
                        .setType(ModelStorage.OperationType.CREATE)
                        .setStatus(ModelStorage.OperationStatusType.INTERNAL_ERROR)
                        .setStatusMessage("Vendor model (id: " + vendorModelId + ") " +
                            "for generated sku (id: " + gskuId + ") not found"));
                }
                return builder.build();
            }

            // get guru model
            Model vendorModel = vendorModelOpt.get();
            Optional<Model> guruModelOpt = ModelProtoUtils.relationToGuruModel(vendorModel)
                .map(relation -> getModel(relation.getCategoryId(), relation.getId()));
            if (!guruModelOpt.isPresent()) {
                ModelStorage.OperationResponse.Builder builder = ModelStorage.OperationResponse.newBuilder();
                for (long gskuId : gskuIds) {
                    builder.addStatuses(ModelStorage.OperationStatus.newBuilder()
                        .setModelId(gskuId).setFailureModelId(vendorModel.getId())
                        .setType(ModelStorage.OperationType.CREATE)
                        .setStatus(ModelStorage.OperationStatusType.INTERNAL_ERROR)
                        .setStatusMessage("Guru model for vendor model (id: " + vendorModel.getId() + ") not found"));
                }
                return builder.build();
            }
            Model guruModel = guruModelOpt.get();

            // create skus from gskus and save
            List<Model> gskus = gskuIds.stream().map(modelsMap::get).collect(Collectors.toList());
            List<Model> skus = gskus.stream()
                .map(gsku -> gsku.toBuilder()
                    .clearId()
                    .setCurrentType(ModelStorage.ModelType.SKU.name())
                    .clearRelations()
                    .addRelations(Relation.newBuilder().setId(gsku.getId()).setCategoryId(
                        gsku.getCategoryId()).setType(RelationType.SYNC_SOURCE))
                    .addRelations(Relation.newBuilder().setId(guruModel.getId()).setCategoryId(
                        guruModel.getCategoryId()).setType(RelationType.SKU_PARENT_MODEL))
                    .build())
                .collect(Collectors.toList());

            SaveModelsGroupResponse resultStatus = saveModelsGroup(SaveModelsGroupRequest.newBuilder()
                .addModelsRequest(SaveModelsRequest.newBuilder()
                    .addAllModels(skus).build()).build());

            // change modelId in operation status in order to get sku by gskuId
            List<OperationStatus> statuses = resultStatus.getResponse(0).getRequestedModelsStatusesList();
            Map<Long, Long> skuToGsku = statuses.stream()
                .filter(status -> status.getModel() != null)
                .map(OperationStatus::getModel)
                .filter(model -> !ModelProtoUtils.isNewModel(model) && ModelProtoUtils.isMarketSku(model))
                .collect(Collectors.toMap(Model::getId, sku -> {
                    return ModelProtoUtils.relationToGskuModel(sku).orElseThrow(
                        () -> new RuntimeException("Expected sku " + sku.getId() + " to contain gsku relation"))
                        .getId();
                }));

            List<ModelStorage.OperationStatus> protoStatuses = statuses.stream()
                .map(OperationStatus::toBuilder)
                .map(status -> {
                    long skuId = status.getModelId();
                    Long gskuId = skuToGsku.get(skuId);
                    if (gskuId != null) {
                        status.setModelId(gskuId);
                    }
                    return status.build();
                })
                .collect(Collectors.toList());
            return ModelStorage.OperationResponse.newBuilder()
                .addAllStatuses(protoStatuses)
                .build();
        } catch (Exception e) {
            String message = "Failed to create sku models from gsku: " + ExceptionUtils.getMessage(e);
            ModelStorage.OperationResponse.Builder builder = ModelStorage.OperationResponse.newBuilder();
            gskuIds.forEach(gskuId -> {
                builder.addStatuses(ModelStorage.OperationStatus.newBuilder()
                    .setModelId(gskuId).setFailureModelId(gskuId)
                    .setType(ModelStorage.OperationType.CREATE)
                    .setStatus(ModelStorage.OperationStatusType.INTERNAL_ERROR)
                    .setStatusMessage(message)
                    .build());
            });
            return builder.build();
        }
    }

    @Override
    public OperationResponse uploadImages(UploadImageRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UploadDetachedImagesResponse uploadDetachedImages(UploadDetachedImagesRequest request) {
        List<ModelStorage.DetachedImageStatus> imageStatuses = request.getImageDataList()
            .stream()
            .map(imageData -> {
                long id = imageData.getId();
                String picUrl = imageData.getUrl();
                PictureStatus status = pictureStatuses.getOrDefault(picUrl, missingPictureStatus.apply(picUrl));
                ModelStorage.DetachedImageStatus.Builder statusBuilder = ModelStorage.DetachedImageStatus.newBuilder()
                    .setId(id)
                    .setStatus(status.getStatus());
                if (status.getPicture() != null) {
                    statusBuilder.setPicture(status.getPicture());
                }
                return statusBuilder.build();
            })
            .collect(Collectors.toList());
        return UploadDetachedImagesResponse.newBuilder()
            .addAllUploadedImage(imageStatuses)
            .build();
    }

    @Override
    public ValidateImagesResponse validateImages(ValidateImagesRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OperationResponse removeModelWithTransitions(RemoveModelWithTransitionsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public OperationResponse restoreDeletedModel(ModelCardApi.RestoreDeletedModelRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelCardApi.UpdateStrictChecksResponse updateStrictChecks(ModelCardApi.UpdateStrictChecksRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelCardApi.ChangeCategoryStatusResponse changeModelsCategory(ModelCardApi.ChangeCategoryRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelCardApi.ChangeCategoryResponse changeModelsCategoryStatus(ModelCardApi.ChangeCategoryStatusRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MonitoringResult ping() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MonitoringResult monitoring() {
        throw new UnsupportedOperationException();
    }

    public static boolean match(Model model, FindModelsRequest request) {
        if (!request.getModelIdsList().isEmpty() && !request.getModelIdsList().contains(model.getId())) {
            return false;
        }
        if (request.hasCategoryId() && request.getCategoryId() != model.getId()) {
            return false;
        }
        if (request.hasModelName() && !request.getModelName().trim().isEmpty()) {
            String modelName = request.getModelName().trim();
            boolean containsName = model.getTitlesList().stream()
                .map(ModelStorage.LocalizedString::getValue)
                .anyMatch(modelName::equalsIgnoreCase);
            if (!containsName) {
                return false;
            }
        }
        if (request.hasModelType() && !request.getModelType().name().equals(model.getCurrentType())) {
            return false;
        }
        if (request.hasVendorId() && request.getVendorId() != model.getVendorId()) {
            return false;
        }
        if (request.hasDeleted() && request.getDeleted() != FindModelsRequest.DeletedState.ALL) {
            boolean deleted = request.getDeleted() == FindModelsRequest.DeletedState.DELETED;
            if (deleted != model.getDeleted()) {
                return false;
            }
        } else if (!request.hasDeleted() && model.getDeleted()) {
            // by default we should match only non-deleted (mbo behaviour)
            return false;
        }
        if (request.hasGuruCardExists()
            || request.hasCursorMark()
            || request.hasFinishCreatedDate()
            || request.hasFinishDeletedDate()
            || request.hasFinishModifiedDate()
            || request.hasStartCreatedDate()
            || request.hasStartDeletedDate()
            || request.hasStartModifiedDate()
            || request.hasOrderBy()
            || request.hasOrderType()
            || request.getParameterFilterCount() > 0) {

            throw new UnsupportedOperationException();
        }

        return true;
    }

    @Override
    public ModelCardApi.GetStrictOrBrokenResponse getStrictOrBroken(ModelCardApi.GetStrictOrBrokenRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelCardApi.ModelsTransferResponse modelsTransfer(ModelCardApi.ModelsTransferRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelCardApi.ModelsTransferStatusResponse modelsTransferStatus(ModelCardApi.ModelsTransferStatusRequest request) {
        throw new UnsupportedOperationException();
    }
}
