package ru.yandex.direct.bsexport.snapshot.holders;

import ru.yandex.direct.core.entity.campaign.model.BillingAggregateCampaign;
import ru.yandex.direct.core.entity.product.model.ProductType;

public class TestBillingAggregatesHolder extends BillingAggregatesHolder {
    public TestBillingAggregatesHolder() {
        //noinspection ConstantConditions
        super(null, null, null);
    }

    @Override
    protected void checkInitialized() {
    }

    public void put(ProductType productType, BillingAggregateCampaign billingAggregateCampaign) {
        var billingAggregateRequest = new BillingAggregateRequest(billingAggregateCampaign.getWalletId(), productType);
        put(billingAggregateRequest, billingAggregateCampaign);
    }
}
