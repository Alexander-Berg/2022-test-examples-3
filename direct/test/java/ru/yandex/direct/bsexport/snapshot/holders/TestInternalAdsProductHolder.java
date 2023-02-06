package ru.yandex.direct.bsexport.snapshot.holders;

import ru.yandex.direct.core.entity.internalads.model.InternalAdsProduct;

public class TestInternalAdsProductHolder extends InternalAdsProductsHolder {
    public TestInternalAdsProductHolder() {
        //noinspection ConstantConditions
        super(null, null);
    }

    @Override
    protected void checkInitialized() {
    }

    public void put(InternalAdsProduct internalAdsProduct) {
        Long clientId = internalAdsProduct.getClientId().asLong();
        put(clientId, internalAdsProduct);
    }
}
