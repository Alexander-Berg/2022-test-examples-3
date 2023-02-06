package manual.ru.yandex.market.psku.postprocessor.bazinga.dna.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.http.ServiceClient;
import ru.yandex.market.ir.autogeneration.common.helpers.MboMappingsServiceHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.export.CategoryModelsService;
import ru.yandex.market.mbo.export.CategoryModelsServiceStub;
import ru.yandex.market.mbo.http.ModelCardApi;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorageService;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.MboMappingsServiceStub;
import ru.yandex.market.psku.postprocessor.bazinga.dna.ProcessModelsTask;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.DeletedMappingModelsResultDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.ExternalRequestResponseDao;
import ru.yandex.market.psku.postprocessor.common.db.dao.RemovedNomappingModelsDao;
import ru.yandex.market.psku.postprocessor.service.dna.ModelCleaningService;
import ru.yandex.market.psku.postprocessor.service.dna.ModelSaveService;
import ru.yandex.market.psku.postprocessor.service.dna.RedundantOwnerIdsExtractionService;
import ru.yandex.market.psku.postprocessor.service.dna.RedundantOwnersCleaningService;

@Configuration
public class ManualTestServicesConfiguration {
    @Value("${mboc.mappings.service.uri}")
    String mbocMappingsUrl;
    @Value("${ppp.card-api.url}")
    String modelStorageUrl;
    @Value("${mbo.http-exporter.url}")
    String httpExporterUrl;
    @Value("${user.agent}")
    String userAgent;

    private static final int DEFAULT_CONNECTION_TIMEOUT = 30000;

    @Bean(name = "model.storage.service.with.retry")
    ModelStorageService modelStorageService() {
        ModelStorageServiceStub result = new ModelStorageServiceStub();
        initServiceClient(result, modelStorageUrl);
//        return result;
        return makeReadOnlyProxy(result);
    }

    @Bean(name = "model.storage.service.without.retry")
    ModelStorageService modelStorageWithoutRetryService() {
        ModelStorageServiceStub result = new ModelStorageServiceStub();
        initServiceClient(result, modelStorageUrl);
        result.setTriesBeforeFail(1);
//        return result;
        return makeReadOnlyProxy(result);
    }

    @Bean
    CategoryModelsService categoryModelsService() {
        CategoryModelsServiceStub categoryModelsServiceImpl = new CategoryModelsServiceStub();
        categoryModelsServiceImpl.setHost(httpExporterUrl);
        return categoryModelsServiceImpl;
    }

    @Bean
    ModelStorageHelper modelStorageHelper(
            @Qualifier("model.storage.service.with.retry") ModelStorageService modelStorageServiceWithRetry,
            @Qualifier("model.storage.service.without.retry") ModelStorageService modelStorageServiceWithoutRetry,
            CategoryModelsService categoryModelsService
    ) {
        return new ModelStorageHelper(
                modelStorageServiceWithRetry,
                modelStorageServiceWithoutRetry
        );
    }

    @Bean
    MboMappingsService mboMappingsService() {
        MboMappingsServiceStub result = new MboMappingsServiceStub();
        result.setHost(mbocMappingsUrl);
        result.setUserAgent(userAgent);
        result.setConnectionTimeoutMillis(DEFAULT_CONNECTION_TIMEOUT);
        result.setSocketTimeoutMillis(DEFAULT_CONNECTION_TIMEOUT);
        result.setTriesBeforeFail(1);
        return result;
    }

    @Bean
    MboMappingsServiceHelper mboMappingsServiceHelper(MboMappingsService mboMappingsService) {
        return new MboMappingsServiceHelper(mboMappingsService);
    }

    private void initServiceClient(ServiceClient serviceClient, String serviceHost) {
        serviceClient.setUserAgent(userAgent);
        serviceClient.setTriesBeforeFail(5);
        serviceClient.setSleepBetweenTries(200);
        serviceClient.setConnectionTimeoutMillis(DEFAULT_CONNECTION_TIMEOUT);
        serviceClient.setHost(serviceHost);
    }

    @Bean
    RedundantOwnerIdsExtractionService redundantOwnerIdsExtractionService(
            MboMappingsServiceHelper mboMappingsServiceHelper) {
        return new RedundantOwnerIdsExtractionService(mboMappingsServiceHelper);
    }

    @Bean
    RedundantOwnersCleaningService redundantOwnersCleaningService() {
        return new RedundantOwnersCleaningService();
    }

    @Bean
    ModelSaveService modelSaveService(ModelStorageHelper modelStorageHelper,
                                      ExternalRequestResponseDao externalRequestResponseDao) {
        return new ModelSaveService(modelStorageHelper, externalRequestResponseDao);
    }

    @Bean
    ModelCleaningService modelCleaningService(RedundantOwnerIdsExtractionService ownerIdsExtractionService,
                                              RedundantOwnersCleaningService redundantOwnersCleaningService) {
        return new ModelCleaningService(ownerIdsExtractionService, redundantOwnersCleaningService);
    }

    @Bean
    ProcessModelsTask processModelsTask(DeletedMappingModelsDao deletedMappingModelsDao,
                                        DeletedMappingModelsResultDao deletedMappingModelsResultDao,
                                        ModelStorageHelper modelStorageHelper,
                                        ModelSaveService modelSaveService,
                                        RemovedNomappingModelsDao removedNomappingModelsDao,
                                        ModelCleaningService modelCleaningService) {
        return new ProcessModelsTask(
                deletedMappingModelsDao,
                deletedMappingModelsResultDao,
                modelStorageHelper,
                modelSaveService,
                removedNomappingModelsDao,
                modelCleaningService);
    }

    private ModelStorageService makeReadOnlyProxy(ModelStorageService service) {
        return new ModelStorageService() {
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
                return service.getModels(getModelsRequest);
            }

            @Override
            public ModelStorage.GetModelIdsByBarcodeResponse getModelIdsByBarcode(ModelStorage.GetModelIdsByBarcodeRequest getModelIdsByBarcodeRequest) {
                return service.getModelIdsByBarcode(getModelIdsByBarcodeRequest);
            }

            @Override
            public ModelStorage.GetModelsResponse findModels(ModelStorage.FindModelsRequest findModelsRequest) {
                return service.findModels(findModelsRequest);
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
//                return service.saveModels(newRequest);
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
                throw new UnsupportedOperationException();
//                ModelCardApi.SaveModelsGroupResponse saveModelsGroupResponse = service.saveModelsGroup(newRequest.build());
//                return saveModelsGroupResponse;
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
                return service.uploadDetachedImages(uploadDetachedImagesRequest);
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
    }
}
