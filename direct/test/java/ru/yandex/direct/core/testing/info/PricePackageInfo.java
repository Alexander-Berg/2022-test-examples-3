package ru.yandex.direct.core.testing.info;

import ru.yandex.direct.core.entity.pricepackage.model.PricePackage;

public class PricePackageInfo {

    private PricePackage pricePackage;

    public void setPricePackage(PricePackage pricePackage) {
        this.pricePackage = pricePackage;
    }

    public PricePackage getPricePackage() {
        return pricePackage;
    }

    public PricePackageInfo withPricePackage(PricePackage pricePackage) {
        this.pricePackage = pricePackage;
        return this;
    }

    public Long getPricePackageId() {
        return getPricePackage().getId();
    }
}
