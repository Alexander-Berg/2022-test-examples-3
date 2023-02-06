package ru.yandex.market.wms.common.spring.dao;

import ru.yandex.market.wms.common.model.enums.ShelfLifeIndicatorType;
import ru.yandex.market.wms.common.model.enums.ShelfLifeTemplate;

public class AssertSkuExpectedParamOne {
    private final String expectedSku;
    private final String expectedManufacturerSku;
    private final String expectedDescription;
    private final ShelfLifeIndicatorType expectedIndicator;
    private final ShelfLifeTemplate expectedTemplate;
    private final Integer toExpireDays;
    private final Integer shelfLifeOnReceivingDays;

    public AssertSkuExpectedParamOne(String expectedSku, String expectedManufacturerSku, String expectedDescription,
                                     ShelfLifeIndicatorType expectedIndicator, ShelfLifeTemplate expectedTemplate,
                                     Integer toExpireDays, Integer shelfLifeOnReceivingDays) {
        this.expectedSku = expectedSku;
        this.expectedManufacturerSku = expectedManufacturerSku;
        this.expectedDescription = expectedDescription;
        this.expectedIndicator = expectedIndicator;
        this.expectedTemplate = expectedTemplate;
        this.toExpireDays = toExpireDays;
        this.shelfLifeOnReceivingDays = shelfLifeOnReceivingDays;
    }

    public String getExpectedSku() {
        return expectedSku;
    }

    public String getExpectedManufacturerSku() {
        return expectedManufacturerSku;
    }

    public String getExpectedDescription() {
        return expectedDescription;
    }

    public ShelfLifeIndicatorType getExpectedIndicator() {
        return expectedIndicator;
    }

    public ShelfLifeTemplate getExpectedTemplate() {
        return expectedTemplate;
    }

    public Integer getToExpireDays() {
        return toExpireDays;
    }

    public Integer getShelfLifeOnReceivingDays() {
        return shelfLifeOnReceivingDays;
    }
}
