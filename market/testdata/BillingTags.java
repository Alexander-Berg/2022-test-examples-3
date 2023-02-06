package ru.yandex.autotests.market.billing.backend.data.testdata;

import ru.yandex.autotests.market.test.data.mongo.MarketMongoKey;

/**
 * @author Sergey Syrisko <a href="mailto:syrisko@yandex-team.ru"/>
 * @date 2/18/15
 */
public enum BillingTags implements MarketMongoKey {
    PLOG_CLICK_FROM_MST_API,
    CLICK_ROW_IDS_FROM_MST_API,
    PLOG_CLICK_FROM_BILLING,
    CLICK_ROW_IDS_FROM_BILLING;

    @Override
    public String getTitle() {
        return this.name();
    }
}
