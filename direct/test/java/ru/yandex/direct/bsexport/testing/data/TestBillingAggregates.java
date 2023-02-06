package ru.yandex.direct.bsexport.testing.data;

import ru.yandex.direct.bsexport.model.BillingAggregateRule;
import ru.yandex.direct.bsexport.model.BillingAggregates;
import ru.yandex.direct.bsexport.model.ProductType;

public class TestBillingAggregates {
    private TestBillingAggregates() {
    }

    public static final BillingAggregateRule cpmVideoRule = BillingAggregateRule.newBuilder()
            .addProductTypes(ProductType.VideoCreativeReach)
            .setResult(41603887)
            .build();

    public static final BillingAggregateRule cpmOutdoorRule = BillingAggregateRule.newBuilder()
            .addProductTypes(ProductType.VideoCreativeReachOutdoor)
            .setResult(41603894)
            .build();

    public static final BillingAggregateRule cpmIndoorRule = BillingAggregateRule.newBuilder()
            .addProductTypes(ProductType.VideoCreativeReachIndoor)
            .setResult(43921364)
            .build();

    public static final BillingAggregateRule cpmAudioRule = BillingAggregateRule.newBuilder()
            .addProductTypes(ProductType.AudioCreativeReach)
            .setResult(44798409)
            .build();

    public static final BillingAggregates billingAggregates1 = BillingAggregates.newBuilder()
            .setDefault(41603880)
            .addRules(cpmVideoRule)
            .addRules(cpmOutdoorRule)
            .addRules(cpmIndoorRule)
            .addRules(cpmAudioRule)
            .build();
}
