package ru.yandex.market.stat.dicts.loaders;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.yandex.commune.bazinga.impl.BazingaIdUtils;

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.NotImplementedException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.stat.dicts.common.Dictionary;
import ru.yandex.market.stat.dicts.integration.help.SpringDataProviderRunner;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

@Slf4j
@ActiveProfiles("integration-tests")
@RunWith(SpringDataProviderRunner.class)
public class LoadersITest extends BaseLoadTest {

    private static final List<String> BIG_DICTIONARIES = Arrays.asList( // more than 5 min
            "shop_datasources_in_testing", // 15 min
            "cpa_open_cutoff", // 39 min
            "open_cutoff", // ??? min
            "shop_param_value", // ??? min
            "order_item", // 30 min
            "indexer_hidden_offers", // ??? min
            "offer_cutoff",
            "oebs_market_plan_fact", // >10 min, на текующий момент (2019-06-30) последний данные за 2019-04-21
            "all_vendors",
            "msku_availability_matrix",
            "wms_internal_transactions",
            "wms_order_status_history",
            "sku_logistics_params",
            "wh_logistics_params",
            "msku_group",
            "grouped_msku",
            "axapta_sales_orders"
    );

    private static final List<String> MEDIUM_DICTIONARIES = Arrays.asList( // more than 1 min
            "regions", // 1.5 min
            "shop_datasource", // 3 min
            "domains", // depends on shop_datasource
            "shop_delivery_options", // 3 min
            "shop_delivery_option_groups", // 3 min
            "premod_problem", // 1 m
            "premod_ticket", // 2.5 min
            "premod_item", // 2.5 min
            "core_offer", // 3.5 min
            "conversations", // 1 min
            "outlet_info",
            "campaign_info",
            "shop_crm",
            "shop_crm_contacts",
            "delivery_services",
            "region_groups_services", // 5 min
            "shop_bid_source",
            "oebs_balances_meta", //2 min
            "supplier_to_market_sku_snapshot",
            "shop_ratings_pers",
            "sku_transitions",
            "model_transitions",
            "mboc_supplier",
            "anaplan__pl_to_yt_csv-1d",
            "anaplan__budget_module_to_yt_csv-1d",
            "msku_status",
            "axapta_inventtrans_open",
            "wms_order_detail",
            "wms_orders",
            "wms_pick_detail",
            "axapta_invent_locations",
            "clab_good",
            "clab_cart",
            "clab_movement",
            "clab_requested_good",
            "clab_requested_movement",
            "clab_raw_photo",
            "clab_edited_photo",
            "clab_user"
    );

    private static final List<String> SMALL_DICTIONARIES = Arrays.asList(
            "pl_bids_shop",
            "pl_bids_shop_feed",
            "pl_bids_broker_strategy",
            "abt_experiments",
            "model_factors",
            "messages",
            "shop_papi_hidden_offer",
            "arbitrage_info",
            "arbitrage_refund",
            "clch_shop_favourite",
            "clch_shop_set",
            "core_problem",
            "core_problem_approve",
            "core_problem_class",
            "core_problem_status",
            "core_problem_type",
            "core_ticket",
            "core_ticket_status",
            "internal_complaint",
            "internal_complaint_offer",
            "internal_complaint_status",
            "internal_complaint_type",
            "no_placement_record",
            "premod_problem_type",
            "recheck_problem",
            "recheck_problem_class",
            "recheck_problem_type",
            "recheck_ticket",
            "goods_status",
            "call_transcription",
            "premod_item_type",
            "hypothesis",
            "ml_shop_data",
            "shop_url",
            "problems_for_agency_bonus",
            "hypothesis_generator",
            "shop_agency_campaigns",
            "shop_agency_history",
            "crossborder_order_billed_amounts",
            "crossborder_partner_contracts",
            "shop_activity",
            "cutoff_types",
            "shop_param_type",
            "shop_user_roles",
            "industrial_manager_client",
            "distribution_region_groups",
            "goods_ad_budget",
            "distribution_clids",
            "shop_ratings",
            "shop_audit",
            "suppliers",
            "v_partner_app_business",
            "extended_cpa_yam_request_history",
            "shop_schedule",
            "shop_vat",
            "manager_channel",
            "channel",
            "order_item_billed_amounts",
            "order_billed_amounts",
            "order_item_billed_amounts_corr",
            "cpa_orders",
            "delivery_region_groups",
            "delivery_track",
            "axapta_category_active",
            "axapta_product_category",
            "axapta_category_hierarchy",
            "axapta_inventory_trans_purchasess",
            "fulfillment_shop_request",
            "fulfillment_shop_request-1h",
            "fulfillment_shop_request-1d",
            "fulfillment_request_item",
            "fulfillment_request_item-1h",
            "fulfillment_request_item-1d",
            "fulfillment_request_status_history",
            "shop_bid_client_id",
            "organization_info",
            "b2b_crm_onlinestores",
            "b2b_crm_missions",
            "b2b_crm_missiontemplates",
            "b2b_crm_businessprocesses",
            "b2b_crm_statusesmissiontemplates",
            "b2b_crm_statusesmissiontemplates_missiontemplates",
            "pp",
            "ocrm_task_history",
            "ocrm_issues",
            "ocrm_calls",
            "ocrm_task_types",
            "ocrm_task_statuses",
            "ocrm_task_history_item_type",
            "ocrm_users",
            "ocrm_topics_and_subtopics",
            "axapta_price_rules_log",
            "axapta_cust_trans_prepayment",
            "axapta_price_list_log",
            "axapta_unapproved_price_rules_log",
            "replenishment_assortment_out",
            "unpublished_offers_report",
            "unpublished_offers_ignore_stocks",
            "shop_real_supplier",
            "axapta_suppliers", // 5.19 minutes
            "axapta_warehouse_movements",
            "white_blue_ds_mapping",
            "axapta_supplier_prices",
            "shop_logo",
            "partner_types",
            "shop_channel_manager",
            "supplier_manager",
            "red_cart",
            "red_bonus",
            "red_address",
            "red_outlet",
            "red_user_device",
            "cargo_type",
            "marschroute_transport_info",
            "complete_reference_information-1d",
            "purchase_amount",
            "address",
            "calendar",
            "calendar_day",
            "contact",
            "databasechangelog",
            "delivery_distributor_params",
            "delivery_interval",
            "dynamic_fault",
            "dynamic_log",
            "job_monitoring_config",
            "location_calendar",
            "logistic_edges",
            "logistic_segments",
            "logistic_segments_services",
            "logistic_segments_services_cargo_types",
            "logistics_point",
            "logistics_point_gate",
            "partner",
            "partner_capacity",
            "partner_capacity_day_off",
            "partner_cargo_type",
            "partner_courier_schedule",
            "partner_customer_info",
            "partner_external_param_type",
            "partner_external_param_value",
            "partner_handling_time",
            "partner_location_tariff_cargo_type",
            "partner_market_id_status",
            "partner_route",
            "partner_relation",
            "partner_relation_cutoff",
            "partner_relation_product_rating",
            "partner_shop",
            "partner_tariff",
            "partner_subtype",
            "phone",
            "platform_client",
            "platform_client_partners",
            "platform_partners_shipment_settings",
            "point_services",
            "possible_order_change",
            "put_reference_warehouse_in_delivery_status",
            "schedule",
            "schedule_day",
            "service_capacity",
            "service_capacity_value",
            "service_code",
            "settings_method",
            "support",
            "oebs_market_downloads_requests",
            "view_accounts_comments",
            "view_emails_2",
            "view_emails_tags",
            "ocrm_service", // <1m
            "ocrm_ticket", // <1m
            "ocrm_ticket_version", // <1m
            "ocrm_comment", // <1m
            "ocrm_employee", // <1m
            "ocrm_ou",// <1m
            "ocrm_employee_distr_status_version", // <1m
            "ocrm_employee_status",
            "ocrm_ticket_beru_category",
            "ocrm_beru_category",
            "ocrm_ticket_market_category",
            "ocrm_market_category",
            "ocrm_ticket_bringly_category",
            "ocrm_bringly_category",
            "lifetime_information-1d",
            "mbi_storage_billing",
            "mbi_supply_billing_correction",
            "mbi_withdraw_item_billed_amounts",
            "mbi_billing_service_type",
            "mbi_supplier_category_fees",
            "mbi_supplier_fee_periods",
            "mbi_fulfillment_tariffs",
            "mbi_fulfillment_tariff_periods",
            "mbi_distribution_share",
            "mbi_overdraft_control",
            "mbi_bank_order",
            "mbi_bank_order_item",
            "mbi_crossborder_order_billed_amounts_corrections",
            "mbi_crossborder_track_code_billed_amount",
            "mbi_balance_firm_migration_log",
            "mbi_currency_exchange_rate",
            "mbi_suppliers_type",
            "mbi_suppliers",
            "mbi_supplier_emails",
            "mbi_feature_open_cutoffs",
            "mbi_feature_closed_cutoffs",
            "mbi_order_item_billed_amount",
            "mbi_order_item_billed_amount_corr",
            "mbi_order_billed_amount",
            "mbi_order_billed_amount_corr",
            "mbi_ruspost_contracts",
            "mbi_sorting_billed_orders_amount",
            "mbi_sorting_billed_daily_amount",
            "mbi_sorting_billed_daily_amount_corr",
            "mbi_hidden_suppliers",
            "ocrm_ticket_fmcg_category",
            "ocrm_fmcg_category",
            "size_measure",
            "exported_demand",
            "exported_demand_msku",
            "legal_info",
            "shipment",
            "ocrm_operator_window",
            "ocrm_orders",
            "gaap_audit",
            "gaap_jobs_new",
            "gaap_jobs_historical",
            "gaap_members_ex",
            "fpa_audit",
            "fpa_jobs_new",
            "fpa_jobs_historical",
            "gaap_exported_all",
            "fpa_exported_all",
            "fpa_members_ex",
            "wms_location_x_lot",
            "wms_sku",
            "wms_lotattribute",
            "wms_receipt",
            "resupply",
            "resupply_package",
            "resupply_request",
            "resupply_item",
            "resupply_item_attribute",
            "resupply_registry",
            "resupply_registry_item",
            "ocrm_call_ended_by",
            "ocrm_voximplant",
            "ocrm_order_bonus",
            "ocrm_bonus_reason",
            "ocrm_additional_subtopics",
            "operational_rating",
            "partner_rating",
            "partner_rating_part",
            "check_order",
            "shop_aliases",
            "serp_shop",
            "shop_banned",
            "partner_rating_exclusion",
            "late_orders",
            "partner_quality_index"
    );

    private static final List<String> MARKET_DATA_GETTER_DICTIONARIES = Arrays.asList(
            "shops",
            "vendors",
            "categories",
            "currency_rates",
            "model_ratings",
            "cataloger",
            "recommendation_rules",
            "cms_pages",
            "sku",
            "models",
            "parameters",
            "shop_price_labs",
            "shops_outlet",
            "shops_outlet_self_delivery_rule",
            "shops_outlet_working_time",
            "cataloger_hid_nid",
            "compatibilities"
    );

    private static final List<String> DISABLED_DICTIONARIES = Arrays.asList(
            "urlchecker_price", // testing hasn't data (loaded records = 0)
            "clch_cluster", // testing hasn't data (loaded records = 0)
            "distribution", // need specific credentials
            "distribution_partners" // need specific credentials

    );

    private static final List<String> DICTIONARIES = ImmutableList.<String>builder()
            .addAll(SMALL_DICTIONARIES)
            .addAll(MEDIUM_DICTIONARIES)
            .addAll(BIG_DICTIONARIES)
            .addAll(MARKET_DATA_GETTER_DICTIONARIES)
            .addAll(DISABLED_DICTIONARIES)
            .build();

    private static final List<String> KNOWN_MILTISCALE_DICTS = Arrays.asList("fulfillment_shop_request", "fulfillment_request_item", "fulfillment_booked_time_slots");


    @DataProvider
    public static Object[][] smallDictionaries() {
        return makeTestData(SMALL_DICTIONARIES);
    }

    @DataProvider
    public static Object[][] mediumDictionaries() {
        return makeTestData(MEDIUM_DICTIONARIES);
    }

    @DataProvider
    public static Object[][] bigDictionaries() {
        return makeTestData(BIG_DICTIONARIES);
    }

    @DataProvider
    public static Object[][] getterDictionaries() {
        return makeTestData(MARKET_DATA_GETTER_DICTIONARIES);
    }

    @Ignore("Access required")
    @Test
    @UseDataProvider("smallDictionaries")
    public void testSmallDictionaries(String dictionary) throws Exception {
        loadDictionary(dictionary);
    }

    @Ignore("Too many times")
    @Test
    @UseDataProvider("mediumDictionaries")
    public void testMediumDictionaries(String dictionary) throws Exception {
        loadDictionary(dictionary);
    }


    @Ignore("Too many times")
    @Test
    @UseDataProvider("bigDictionaries")
    public void testBigDictionaries(String dictionary) throws Exception {
        loadDictionary(dictionary);
    }

    @Ignore("Required getter dir")
    @Test
    @UseDataProvider("getterDictionaries")
    public void testGetterDictionaries(String dictionary) throws Exception {
        loadDictionary(dictionary);
    }

    @Test
    public void testGetSystemSourceForAll() throws Exception {
        for (DictionaryLoader loader : loaders) {
            try {
                if (loader.getSystemSource() ==  null) {
                    throw new Exception(loader.getDictionary() + " doesn't have systemSource");
                }
            } catch (NotImplementedException e) {
                throw new NotImplementedException(loader.getDictionary() + " doesn't have systemSource");
            }
        }
    }

    @Test
    public void testNormalLoaderName() {
        for (DictionaryLoader loader : loaders) {
            BazingaIdUtils.validate(loader.getDictionary().nameForLoader());
        }
    }


    @Ignore
    @Test
    public void testWithMultithreading() throws Exception {
        String dict1 = "replenishment_assortment_out";
        String dict2 = "axapta_supplier_prices";
        String dict3 = "axapta_warehouse_movements";
        String dict4 = "axapta_suppliers";
        String dict5 = "axapta_category_hierarchy";

        Thread load1 = loadDictInSeparateThread(dict1, 3);
        Thread load2 = loadDictInSeparateThread(dict2, 1);
        Thread load3 = loadDictInSeparateThread(dict3, 2);
        LocalDateTime start = LocalDateTime.now();
        load1.start();
        load2.start();
        load3.start();
        loadDictionary(dict4);
        Thread load5 = loadDictInSeparateThread(dict5, 2);
        load5.start();

        load1.join();
        load2.join();
        load3.join();
        load5.join();
        LocalDateTime end = LocalDateTime.now();

        log.info("Total load took " + (Duration.between(start, end).toMillis() * 1.0 / 60000) + " minutes");
    }

    private Thread loadDictInSeparateThread(String dict, Integer times) {
        return new Thread(() -> {
            try {
                LocalDateTime start = LocalDateTime.now();
                long records = 0;
                for (int i = 0; i < times; i++) {
                    records = loadDictionary(dict);
                }
                LocalDateTime end = LocalDateTime.now();

                log.info("Load took " + (Duration.between(start, end).toMillis() * 1.0 / 60000) + " minutes for " + records + " records, times:" + times);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, dict);
    }

    @Test
    @Ignore("Ok, let's admit this is a very stupid test")
    public void allLoadersHasTest() {
        Set<String> loadersDictionaries =
                loaders.stream().map(DictionaryLoader::getDictionary).map(d -> d.nameForLoader().toLowerCase()).collect(toSet());

        Set<String> testDictionaries = new HashSet<>(DICTIONARIES);

        if (loadersDictionaries.size() != loaders.size()) {
            List<String> dictList =
                    loaders.stream().map(DictionaryLoader::getDictionary).map(d -> d.getName().toLowerCase()).collect(toList());
            throw new AssertionError("Multiple loaders load the same dict: " + findDuplicates(dictList));
        }

        assertThat("Multiple loaders load the same dict", loadersDictionaries.size(), equalTo(loaders.size()));

        if (testDictionaries.size() != DICTIONARIES.size()) {
            throw new AssertionError("Duplicate dict in test data: " + findDuplicates(DICTIONARIES));
        }

        Set<String> loadersMinusDicts = new HashSet<>(loadersDictionaries);
        loadersMinusDicts.removeAll(testDictionaries);
        // not require this kind of testing
        loadersMinusDicts.remove("master_data");
        loadersMinusDicts.remove("axapta_purchases");
        // позор тем, кто не добавил тесты на свои словари! нет времени разбираться
        loadersMinusDicts.removeAll(Arrays.asList("market_shop", "anaplan__ue_budget_from_anaplan_csv-1d",
                "market_outlet", "mboc_logistics_params", "market_category", "anaplan__promo_data_to_yt_csv-1d",
                "axapta_inventtrans_overall_hist",
                "korobytes_from_warehouses_information-1d"));

        assertThat("Some loaders hasn't test. Please add", loadersMinusDicts, equalTo(Collections.emptySet()));

        Set<String> dictMinusLoaders = new HashSet<>(testDictionaries);
        dictMinusLoaders.removeAll(loadersDictionaries);

        assertThat("Test contains unknown dict. Please remove", dictMinusLoaders, equalTo(Collections.emptySet()));
    }


    @Test
    public void checkLoadersInitialization() {

        Set<String> loadersDictionaries =
                loaders.stream().map(DictionaryLoader::getDictionary).map(d -> d.nameForLoader().toLowerCase()).collect(toSet());
        // Нет дублей по словарям-скейлам
        assertThat("Multiple loaders load the same dict", loadersDictionaries.size(), equalTo(loaders.size()));


        Map<String, List<LoaderScale>> uniqueDictionaries =
                loaders.stream().map(DictionaryLoader::getDictionary).collect(groupingBy(
                        Dictionary::getName, mapping(Dictionary::getScale, toList())));
        // Вообще приличное кол-во загрузчиков
        assertThat("Not enough dictionaries found!", uniqueDictionaries.keySet().size(), greaterThan(100));

        Map<String, List<LoaderScale>> moreThan2Scales =
                uniqueDictionaries.entrySet().stream().filter(r -> r.getValue().size() > 2).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

        // Не должно быть больше 2х скейлов (1h + 1d)
        assertThat("Found dictionaries with more than 2 scales! " + moreThan2Scales, moreThan2Scales.keySet(), equalTo(Collections.emptySet()));


        Set<String> multiscaleDicts = uniqueDictionaries.entrySet().stream().
                filter(r -> r.getValue().size() > 1 && r.getValue().contains(LoaderScale.DEFAULT) && r.getValue().contains(LoaderScale.DAYLY)).map(Map.Entry::getKey)
                .collect(toSet());
        // Не должно быть одновременно default и 1d scale (тк это одно и то же в разные подпапки)
        assertThat("Unknown dictionary has several scales! ", multiscaleDicts, equalTo(Collections.emptySet()));
    }

    private static Set<String> findDuplicates(Collection<String> collection) {
        Set<String> visited = new HashSet<>();
        Set<String> duplicates = new HashSet<>();
        for (String name : collection) {
            if (visited.contains(name)) {
                duplicates.add(name);
            } else {
                visited.add(name);
            }
        }
        return duplicates;
    }

}
