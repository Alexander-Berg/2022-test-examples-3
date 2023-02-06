package ru.yandex.market.core.database;


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
        // lqb
        "public.databasechangelog",
        "public.databasechangeloglock",
        // app
        "market_billing.billing_service_type",
        // TODO remove from csvs and uncomment "market_billing.ff_partner_tariff",
        // TODO remove from csvs and uncomment "market_billing.ff_partner_tariff_rule",
        "market_billing.free_storage_interval_tariff",
        "market_billing.operation_type",
        "market_billing.r_active_billing_types",
        "market_billing.tariff",
        "shops_web.action_type",
        "shops_web.api_custom_resources_limits",
        "shops_web.api_resources",
        "shops_web.api_resources_group",
        "shops_web.calendar_day_types",
        "shops_web.calendar_owner_types",
        "shops_web.calendar_types",
        "shops_web.cutoff_types",
        "shops_web.delivery_types",
        "shops_web.delivery_zone",
        "shops_web.delivery_zone_region",
        "shops_web.dictionary",
        "shops_web.feed_site_type",
        "shops_web.nn_address",
        "shops_web.nn_alias",
        "shops_web.nn_type_template_transport",
        "shops_web.notification_template",
        "shops_web.notification_theme",
        "shops_web.notification_type",
        "shops_web.operation_type",
        "shops_web.organization_types",
        "shops_web.param_type",
        "shops_web.r_place_code",
        "shops_web.region_preferred_lang",
        "shops_web.report_query",
        "shops_web.report_query_param",
        "shops_web.report_query_result",
        "shops_web.rus_post_office",
        "shops_web.rus_post_region",
        "monitor.monitoring"
})
public @interface PreserveDictionariesDbUnitDataSet {
}
