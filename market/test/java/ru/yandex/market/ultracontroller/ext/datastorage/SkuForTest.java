package ru.yandex.market.ultracontroller.ext.datastorage;

import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.mbo.http.SkuBDApi;

public final class SkuForTest {
    private long marketSkuId;
    private String marketSkuName;
    private boolean published;
    private boolean publishedOnMarket;
    private boolean publishedOnBlueMarket;
    private FormalizerParam.FormalizedParamPosition formalizedParamPosition;
    private int modelId;
    private SkuBDApi.SkuOffer.SkuType skuType;

    public long getMarketSkuId() {
        return marketSkuId;
    }

    public void setMarketSkuId(long marketSkuId) {
        this.marketSkuId = marketSkuId;
    }

    public String getMarketSkuName() {
        return marketSkuName;
    }

    public void setMarketSkuName(String marketSkuName) {
        this.marketSkuName = marketSkuName;
    }

    public boolean isPublished() {
        return published;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }

    public boolean isPublishedOnMarket() {
        return publishedOnMarket;
    }

    public void setPublishedOnMarket(boolean publishedOnMarket) {
        this.publishedOnMarket = publishedOnMarket;
    }

    public boolean isPublishedOnBlueMarket() {
        return publishedOnBlueMarket;
    }

    public void setPublishedOnBlueMarket(boolean publishedOnBlueMarket) {
        this.publishedOnBlueMarket = publishedOnBlueMarket;
    }

    public FormalizerParam.FormalizedParamPosition getFormalizedParamPosition() {
        return formalizedParamPosition;
    }

    public void setFormalizedParamPosition(FormalizerParam.FormalizedParamPosition formalizedParamPosition) {
        this.formalizedParamPosition = formalizedParamPosition;
    }

    public int getModelId() {
        return modelId;
    }

    public void setModelId(int modelId) {
        this.modelId = modelId;
    }

    public SkuBDApi.SkuOffer.SkuType getSkuType() {
        return skuType;
    }

    public void setSkuType(SkuBDApi.SkuOffer.SkuType skuType) {
        this.skuType = skuType;
    }
}
