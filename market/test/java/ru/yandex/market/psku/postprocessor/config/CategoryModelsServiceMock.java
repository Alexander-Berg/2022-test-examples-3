package ru.yandex.market.psku.postprocessor.config;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mbo.export.CategoryModelsService;
import ru.yandex.market.mbo.export.MboExport;

public class CategoryModelsServiceMock implements CategoryModelsService {
    @Override
    public MboExport.GetCategoryModelsResponse getModels(MboExport.GetCategoryModelsRequest getCategoryModelsRequest) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboExport.GetCategoryModelsResponse getDeletedModels(MboExport.GetCategoryModelsRequest request) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MboExport.GetCategoryModelsResponse getSkus(MboExport.GetCategoryModelsRequest getCategoryModelsRequest) {
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
}
