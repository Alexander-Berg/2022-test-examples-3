package ru.yandex.market.gutgin.tms.manual.config;


import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.gutgin.tms.config.interceptor.CompositeHttpRequestInterceptor;
import ru.yandex.market.gutgin.tms.config.interceptor.TraceStartHttpRequestInterceptor;
import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.http.ServiceClient;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryModelsHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.export.CategoryModelsService;
import ru.yandex.market.mbo.export.CategoryModelsServiceStub;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageService;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;
import ru.yandex.market.request.httpclient.trace.TraceHttpRequestInterceptor;
import ru.yandex.market.request.httpclient.trace.TraceHttpResponseInterceptor;
import ru.yandex.market.request.trace.Module;

@Configuration
public class ManualExternalProductionModelsServices {
    @Value("${ag-mbo.card.api.model.storage.host}")
    String modelStorageUrl;
    @Value("${mbo.http-exporter.url}/categoryParameters/")
    String categoryParametersServiceHost;
    @Value("${mbo.http-exporter.url}/categoryModels/")
    String categoryModelsServiceHost;
    @Value("${user.agent}")
    String defaultUserAgent;

    int modelStorageWithoutRetryConnectionTimeoutMillis = 500;
    int modelStorageReadSocketTimeoutMillis = 500;
    int defaultTriesBeforeFail = 1;
    int defaultSleepBetweenTries = 500;
    int defaultConnectionTimeoutMillis = 500;

    @Bean(name = "model.storage.service")
    ModelStorageService modelStorageService() {
        ModelStorageServiceStub result = new ModelStorageServiceStub();
        result.setHost(modelStorageUrl);
        result.setConnectionTimeoutMillis(modelStorageWithoutRetryConnectionTimeoutMillis);
        result.setSocketTimeoutSeconds(modelStorageReadSocketTimeoutMillis);
        result.setTriesBeforeFail(1);
        initServiceClient(result, Module.MBO_CARD_API);
        ModelStorageService service = new ModelStorageService() {
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
                return result.getModels(getModelsRequest);
            }

            @Override
            public ModelStorage.GetModelIdsByBarcodeResponse getModelIdsByBarcode(ModelStorage.GetModelIdsByBarcodeRequest getModelIdsByBarcodeRequest) {
                return result.getModelIdsByBarcode(getModelIdsByBarcodeRequest);
            }

            @Override
            public ModelStorage.GetModelsResponse findModels(ModelStorage.FindModelsRequest findModelsRequest) {
                return result.findModels(findModelsRequest);
            }

            @Override
            public ModelStorage.GetStateResponse getState(ModelStorage.VoidRequest voidRequest) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ModelStorage.OperationResponse saveModels(ModelStorage.SaveModelsRequest saveModelsRequest) {
                ModelStorage.SaveModelsRequest newRequest = saveModelsRequest.toBuilder()
                    .setWriteChanges(false)
                    .build();
                throw new UnsupportedOperationException();
//                return result.saveModels(newRequest);
            }

            @Override
            public ModelCardApi.SaveModelsGroupResponse saveModelsGroup(ModelCardApi.SaveModelsGroupRequest saveModelsGroupRequest) {
                ModelCardApi.SaveModelsGroupRequest.Builder newRequest = saveModelsGroupRequest.toBuilder()
                    .clearModelsRequest();
                saveModelsGroupRequest.toBuilder()
                    .getModelsRequestBuilderList()
                    .stream()
                    .map(builder -> builder.setWriteChanges(false))
                    .map(ModelStorage.SaveModelsRequest.Builder::build)
                    .forEach(newRequest::addModelsRequest);
//                throw new UnsupportedOperationException();
                ModelCardApi.SaveModelsGroupResponse saveModelsGroupResponse = result.saveModelsGroup(newRequest.build());
                return saveModelsGroupResponse;
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
            public ModelStorage.OperationResponse createSkuFromGSku(ModelCardApi.CreateSkuFromGSkuRequest createSkuFromGSkuRequest) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ModelStorage.OperationResponse uploadImages(ModelStorage.UploadImageRequest uploadImageRequest) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ModelStorage.UploadDetachedImagesResponse uploadDetachedImages(ModelStorage.UploadDetachedImagesRequest uploadDetachedImagesRequest) {
                return result.uploadDetachedImages(uploadDetachedImagesRequest);
            }

            @Override
            public ModelStorage.ValidateImagesResponse validateImages(ModelStorage.ValidateImagesRequest validateImagesRequest) {
                throw new UnsupportedOperationException();
            }

            @Override
            public ModelStorage.OperationResponse removeModelWithTransitions(ModelCardApi.RemoveModelWithTransitionsRequest removeModelWithTransitionsRequest) {
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
        };
        return service;
    }

    @Bean
    CategoryModelsService categoryModelsService() {
        CategoryModelsServiceStub categoryModelsService = new CategoryModelsServiceStub();
        categoryModelsService.setHost(categoryModelsServiceHost);
        categoryModelsService.setTriesBeforeFail(defaultTriesBeforeFail);
        categoryModelsService.setSleepBetweenTries(defaultSleepBetweenTries);
        categoryModelsService.setConnectionTimeoutMillis(defaultConnectionTimeoutMillis);
        initServiceClient(categoryModelsService, Module.MBO_HTTP_EXPORTER);

        return categoryModelsService;
    }

    @Bean
    ModelStorageHelper modelStorageHelper(
        @Qualifier("model.storage.service") ModelStorageService modelStorageService
    ) {
        return new ModelStorageHelper(
            modelStorageService,
            modelStorageService
        );
    }

    @Bean
    CategoryModelsHelper categoryModelsHelper(
            CategoryModelsService categoryModelsService
    ) {
        return new CategoryModelsHelper(
                categoryModelsService
        );
    }

    private void initServiceClient(ServiceClient serviceClient, Module traceModule) {
        serviceClient.setUserAgent(defaultUserAgent);
        if (traceModule != null) {
            serviceClient.setHttpRequestInterceptor(CompositeHttpRequestInterceptor.of(
                    new TraceStartHttpRequestInterceptor(),
                    new TraceHttpRequestInterceptor(traceModule))
            );
            serviceClient.setHttpResponseInterceptor(new TraceHttpResponseInterceptor());
        }
    }
}
