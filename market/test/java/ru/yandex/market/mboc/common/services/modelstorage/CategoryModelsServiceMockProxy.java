package ru.yandex.market.mboc.common.services.modelstorage;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mbo.export.CategoryModelsService;
import ru.yandex.market.mbo.export.MboExport;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageService;

/**
 * @author s-ermakov
 */
public class CategoryModelsServiceMockProxy implements CategoryModelsService {

    private final ModelStorageService modelStorageService;

    public CategoryModelsServiceMockProxy(ModelStorageService modelStorageService) {
        this.modelStorageService = modelStorageService;
    }

    @Override
    public MboExport.GetCategoryModelsResponse getModels(MboExport.GetCategoryModelsRequest request) {

        ModelStorage.GetModelsResponse response = modelStorageService.getModels(
            ModelStorage.GetModelsRequest.newBuilder()
                .setCategoryId(request.getCategoryId())
                .addAllModelIds(request.getModelIdList())
                .build());

        return MboExport.GetCategoryModelsResponse.newBuilder()
            .addAllModels(response.getModelsList())
            .build();
    }

    @Override
    public MboExport.GetCategoryModelsResponse getDeletedModels(MboExport.GetCategoryModelsRequest request) {
        return null;
    }

    @Override
    public MboExport.GetCategoryModelsResponse getSkus(MboExport.GetCategoryModelsRequest request) {
        return null;
    }

    @Override
    public MonitoringResult ping() {
        return modelStorageService.ping();
    }

    @Override
    public MonitoringResult monitoring() {
        return modelStorageService.monitoring();
    }
}
