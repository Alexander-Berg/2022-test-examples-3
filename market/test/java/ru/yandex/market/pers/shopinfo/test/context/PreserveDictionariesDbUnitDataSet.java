package ru.yandex.market.pers.shopinfo.test.context;


import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ru.yandex.market.common.test.db.DbUnitDataSet;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
@DbUnitDataSet(nonTruncatedTables = {
        "SHOPS_WEB.ORGANIZATION_TYPES",
        "SHOPS_WEB.NOTIFICATION_TYPE",
        "SHOPS_WEB.NN_TYPE_TEMPLATE_TRANSPORT",
        "SHOPS_WEB.NOTIFICATION_TEMPLATE",
        "SHOPS_WEB.NOTIFICATION_THEME",
        "SHOPS_WEB.NN_ALIAS",
        "SHOPS_WEB.NN_ADDRESS",
        "SHOPS_WEB.FEED_SITE_TYPE",
        "SHOPS_WEB.RUS_POST_OFFICE",
        "SHOPS_WEB.RUS_POST_REGION",
        "MARKET_BILLING.R_ACTIVE_BILLING_TYPES",
        "READER.V_CURRENCY_RATE",
        "SHOPS_WEB.OPERATION_TYPE",
        "SHOPS_WEB.PARAM_TYPE",
        "SHOPS_WEB.DICTIONARY",
})
public @interface PreserveDictionariesDbUnitDataSet {
}
