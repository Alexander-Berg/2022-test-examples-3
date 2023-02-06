package ru.yandex.market.clab.common.test.stubs;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author anmalysh
 * @since 3/21/2019
 */
public class ModelStorageServiceStub implements ModelStorageService {

    private Map<Long, ModelStorage.Model> models = new HashMap<>();

    public void addModel(ModelStorage.Model model) {
        models.put(model.getId(), model);
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
    public ModelStorage.GetModelsResponse getModels(ModelStorage.GetModelsRequest getModelsRequest) {
        List<ModelStorage.Model> foundModels = getModelsRequest.getModelIdsList().stream()
            .map(models::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        return ModelStorage.GetModelsResponse.newBuilder()
            .addAllModels(foundModels)
            .build();
    }

    @Override
    public ModelStorage.GetModelIdsByBarcodeResponse getModelIdsByBarcode(
        ModelStorage.GetModelIdsByBarcodeRequest getModelIdsByBarcodeRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelStorage.GetModelsResponse findModels(ModelStorage.FindModelsRequest findModelsRequest) {
        List<ModelStorage.Model> foundModels = findModelsRequest.getModelIdsList().stream()
            .map(models::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        return ModelStorage.GetModelsResponse.newBuilder()
            .addAllModels(foundModels)
            .build();
    }

    @Override
    public ModelStorage.GetStateResponse getState(ModelStorage.VoidRequest voidRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelStorage.OperationResponse saveModels(ModelStorage.SaveModelsRequest saveModelsRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelCardApi.SaveModelsGroupResponse saveModelsGroup(
        ModelCardApi.SaveModelsGroupRequest saveModelsGroupRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelStorage.OperationResponse removeModels(ModelStorage.RemoveModelsRequest removeModelsRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelStorage.OperationResponse createGuruModels(ModelCardApi.SyncGuruModelsRequest syncGuruModelsRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelStorage.OperationResponse updateGuruModels(ModelCardApi.SyncGuruModelsRequest syncGuruModelsRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelStorage.OperationResponse createSkuFromGSku(
        ModelCardApi.CreateSkuFromGSkuRequest createSkuFromGSkuRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelStorage.OperationResponse uploadImages(ModelStorage.UploadImageRequest uploadImageRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelStorage.UploadDetachedImagesResponse uploadDetachedImages(
        ModelStorage.UploadDetachedImagesRequest uploadDetachedImagesRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelStorage.ValidateImagesResponse validateImages(
        ModelStorage.ValidateImagesRequest validateImagesRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelStorage.OperationResponse removeModelWithTransitions(
        ModelCardApi.RemoveModelWithTransitionsRequest removeModelWithTransitionsRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelStorage.OperationResponse restoreDeletedModel(ModelCardApi.RestoreDeletedModelRequest request) {
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
