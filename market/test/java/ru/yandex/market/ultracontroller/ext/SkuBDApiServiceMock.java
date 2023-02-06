package ru.yandex.market.ultracontroller.ext;

import java.util.Map;

import org.springframework.beans.factory.InitializingBean;

import ru.yandex.market.http.MonitoringResult;
import ru.yandex.market.mbo.http.SkuBDApi;
import ru.yandex.market.mbo.http.SkuBDApiService;
import ru.yandex.market.ultracontroller.ext.datastorage.DataStorage;
import ru.yandex.market.ultracontroller.ext.datastorage.ModelForTest;
import ru.yandex.market.ultracontroller.ext.datastorage.SkuForTest;

public class SkuBDApiServiceMock implements SkuBDApiService, InitializingBean {
    private DataStorage dataStorage;
    private Map<Integer, ModelForTest> modelIdToModelForTestsMap;
    private Map<Long, SkuForTest> skuIdToSkuForTestMap;
    private Map<Integer, SkuForTest> modelIdToRejectedSkuTestMap;


    @Override
    public void afterPropertiesSet() {
        modelIdToModelForTestsMap = dataStorage.getModelIdToModelForTestMap();
        skuIdToSkuForTestMap = dataStorage.getSkuIdToSkuForTestMap();
        modelIdToRejectedSkuTestMap = dataStorage.getModelIdToRejectedSkuTestMap();
    }

    private SkuBDApi.SkuOffer buildSkuOffer(ModelForTest model, SkuForTest sku, SkuBDApi.Status status) {
        final SkuBDApi.SkuOffer.Builder builder = SkuBDApi.SkuOffer.newBuilder();

        builder.setStatus(status);
        if (status == SkuBDApi.Status.OK || status == SkuBDApi.Status.REJECT_BY_PARAMETERS) {
            builder.setMarketSkuId(sku.getMarketSkuId())
                    .setModelId(sku.getModelId())
                    .setCategoryId(model.getCategotyId())
                    .setPublished(sku.isPublished())
                    .setPublishedOnMarket(sku.isPublishedOnMarket())
                    .setMarketSkuName(sku.getMarketSkuName())
                    .setPublishedOnBlueMarket(sku.isPublishedOnBlueMarket())
                    .addFormalizedParam(sku.getFormalizedParamPosition())
                    .setSkuType(sku.getSkuType());
        }

        return builder.build();
    }

    @Override
    public SkuBDApi.GetSkuResponse getSku(SkuBDApi.GetSkuRequest getSkuRequest) {
        SkuBDApi.GetSkuResponse.Builder responseBuilder = SkuBDApi.GetSkuResponse.newBuilder();
        getSkuRequest.getOfferList().forEach(offerInfo -> {
            int modelId = (int) offerInfo.getModelId();
            if (modelIdToRejectedSkuTestMap.containsKey(modelId)) {
                var sku = modelIdToRejectedSkuTestMap.get(modelId);
                var model = modelIdToModelForTestsMap.get(modelId);
                responseBuilder.addSkuOffer(buildSkuOffer(model, sku, SkuBDApi.Status.REJECT_BY_PARAMETERS));
                return;
            }
            long mSku = offerInfo.getMarketSkuId();
            if (!skuIdToSkuForTestMap.containsKey(mSku)) {
                responseBuilder.addSkuOffer(buildSkuOffer(null, null, SkuBDApi.Status.NO_SKU));
            } else {
                SkuForTest sku = skuIdToSkuForTestMap.get(mSku);
                ModelForTest model = modelIdToModelForTestsMap.get(sku.getModelId());
                responseBuilder.addSkuOffer(buildSkuOffer(model, sku, SkuBDApi.Status.OK));
            }
        });
        return responseBuilder.build();
    }

    @Override
    public SkuBDApi.ReloadResponse reload(SkuBDApi.CategoryRequest categoryRequest) {
        throw new RuntimeException("Don't support this method.");
    }

    @Override
    public MonitoringResult ping() {
        throw new RuntimeException("Don't support this method.");
    }

    @Override
    public MonitoringResult monitoring() {
        throw new RuntimeException("Don't support this method.");
    }

    public void setDataStorage(DataStorage dataStorage) {
        this.dataStorage = dataStorage;
    }
}
