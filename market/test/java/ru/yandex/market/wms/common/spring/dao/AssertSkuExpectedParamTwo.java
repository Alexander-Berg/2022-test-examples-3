package ru.yandex.market.wms.common.spring.dao;

public class AssertSkuExpectedParamTwo {
    private final Integer shelfLifeDays;
    private final Integer shelfLifeOnReceivingPercentage;
    private final Integer shelfLifePercentage;
    private final Integer shelfLifeOnReceivingDaysBeforeReceiving;
    private final Integer shelfLifeDaysBeforeReceiving;

    public AssertSkuExpectedParamTwo(Integer shelfLifeDays, Integer shelfLifeOnReceivingPercentage,
                                     Integer shelfLifePercentage, Integer shelfLifeOnReceivingDaysBeforeReceiving,
                                     Integer shelfLifeDaysBeforeReceiving) {
        this.shelfLifeDays = shelfLifeDays;
        this.shelfLifeOnReceivingPercentage = shelfLifeOnReceivingPercentage;
        this.shelfLifePercentage = shelfLifePercentage;
        this.shelfLifeOnReceivingDaysBeforeReceiving = shelfLifeOnReceivingDaysBeforeReceiving;
        this.shelfLifeDaysBeforeReceiving = shelfLifeDaysBeforeReceiving;
    }

    public Integer getShelfLifeDays() {
        return shelfLifeDays;
    }

    public Integer getShelfLifeOnReceivingPercentage() {
        return shelfLifeOnReceivingPercentage;
    }

    public Integer getShelfLifePercentage() {
        return shelfLifePercentage;
    }

    public Integer getShelfLifeOnReceivingDaysBeforeReceiving() {
        return shelfLifeOnReceivingDaysBeforeReceiving;
    }

    public Integer getShelfLifeDaysBeforeReceiving() {
        return shelfLifeDaysBeforeReceiving;
    }
}
