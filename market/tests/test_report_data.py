# coding: utf-8

import os
import tempfile
import unittest
import context
import json

from market.pylibrary.mi_util import util
from market.pylibrary.yatestwrap.yatestwrap import source_path
from mock import patch
from market.idx.marketindexer.marketindexer import miconfig
from market.idx.pylibrary.mindexer_core.dist_data import reportdata
from market.idx.pylibrary.version import get_package_version


BUILDROOT = os.environ.get('ARCADIA_BUILD_ROOT')

ORIGIN_SEARCH_STATS_MMAP_FILES = [
    'search-stats-mmap/model2msku.mmap',
    'search-stats-mmap/model_geo_stats.mmap',
    'search-stats-mmap/model_local_offers_geo_stats.mmap',
    'search-stats-mmap/model_offline_offers_geo_stats.mmap',
    'search-stats-mmap/model_warnings.mmap',
    'search-stats-mmap/model_stats.mmap',
    'search-stats-mmap/group_region_stats.mmap',
    'search-stats-mmap/blue_group_region_stats.mmap',
    'search-stats-mmap/gl_mbo.mmap',
    'search-stats-mmap/gl_mbo.aho_corasick',
    'search-stats-mmap/book_now_model.mmap',
    'search-stats-mmap/cluster_pictures.mmap',
    'search-stats-mmap/is_global_regional_stats.mmap',
    'search-stats-mmap/guru_light_region_stats.mmap',

    'input/delivery/regional_delivery.mmap',
    'input/getter/pers/best_grades_for_koldunshik.mmap',
    'input/getter/pers/best_grades_for_koldunshik_for_basesearch.mmap',
    'input/contex_experiments.mmap',
    'input/getter/mbi/shops_outlet_v5.mmap',
    'input/getter/yt_money/bid-correction-data.mmap',
    'input/getter/yt_money/demand-prediction-one-p-input.mmap',
    'input/getter/yt_money/demand-prediction-sales.mmap',
    'input/getter/yt_money/min-bids-category-stats.mmap',
    'input/getter/yt_money/min-bids-model-stats.mmap',
    'input/model-transitions.mmap',
    'input/msku-transitions.mmap',
    'input/getter/yt_shop_vendor_promo_clicks_stats/yt_shop_vendor_promo_clicks_stats.mmap',

    'output/click_n_collect_info.mmap',
    'output/gifts.mmap',
    'output/promo_details.mmap',
    'output/shop_delivery_options.mmap',
    'output/yt_promo_details.mmap',
    'output/yt_promo_details_generic_bundle.mmap'
    'output/contex_msku.mmap',
    'output/shop_calendar.mmap',
]

ORIGIN_REPORT_DATA_FILES = [
    'market-svn-data-dir/modelConversion.csv',
    'market-svn-data-dir/no-models-categories-exp.txt',
    'market-svn-data-dir/test-shops.txt',
    'market-svn-data-dir/model_groups.tsv',
    'market-svn-data-dir/vendor_reserve_prices.pb',
    'market-svn-data-dir/filters.txt',
    'market-svn-data-dir/prohibited_blue_offers.json',
    'input/filtered.gl_mbo.pbuf.sn',
    'input/getter/abo/shop-rating.txt',
    'input/getter/currency_rates/currency_rates.xml',
    'input/getter/fast_data_outlets/fast_data_outlets.pbuf.sn',
    'input/getter/geobase/geo2.c2p',
    'input/getter/geobase/geobase.xml',
    'input/getter/mbi/category_min_bids_params.xml',
    'input/getter/mbi/cpa-categories.xml',
    'input/getter/mbi/delivery_holidays.xml',
    'input/getter/mbi/delivery_service_flags.json',
    'input/getter/mbi/model-clicks.txt',
    'input/getter/mbi/shopsDeliveryServiceShipmentCalendar.xml',
    'input/getter/mbi/region-holidays.xml',
    'input/getter/mbi/credit-templates.fb',
    'input/getter/mbi/market_modelbids_banner.tsv',
    'input/getter/yado/delivery-service-end-point-intervals.json',
    'input/getter/lms/lms.pbuf.sn',
    'input/getter/nordstream/nordstream.pb.sn',
    'input/getter/loyalty/loyalty_delivery_discount.pbuf.sn',
    'input/getter/mbo_stuff/category-restrictions.pb',
    'input/getter/mbo_stuff/category_transitions.pb',
    'input/getter/mbo_stuff/parameters_mapping.pb',
    'input/getter/pers/best_grade_for_report.csv',
    'input/getter/pers/shop-best-review.txt',
    'input/getter/pers/shop_rating.txt',
    'input/getter/pers/regional-clone.txt',
    'input/getter/pers/model_rating.txt',
    'input/getter/yandex_market_recom_yt/report',
    'input/getter/yandex_market_recom_yt/yamarec_yt.conf.patched',
    'input/getter/yt_money/min-bids-config.json',
    'input/getter/yt_money/min-bids-price-groups.json',
    'input/mbo/stable/navigation/cataloger.navigation.xml',
    'input/mbo/stable/navigation/cataloger.navigation.all.xml',
    'input/mbo/stable/navigation/navigation-redirects.xml',
    'input/mbo/tovar-tree.pb',
    'input/model-opinions.db',
    'input/model_group.csv',
    'input/model_group_for_beru_msku_card.csv',
    'input/shops-utf8.dat.report.generated',
    'stats/category_stats.csv',
    'stats/warehouses_express_categories.csv',
    'stats/shop_names.csv',
    'stats/total-stats.txt',

    'market-svn-data-dir/shop_incut.tsv',
    'market-svn-data-dir/safety_guarantee_shops.tsv',
    'market-svn-data-dir/uncollapsed_models.tsv',
    'market-svn-data-dir/adult-queries.dat',
    'market-svn-data-dir/direct-minus-words.dat',
    'market-svn-data-dir/family-stop-queries.dat',
    'market-svn-data-dir/parallel-block-queries.dat',
    'market-svn-data-dir/parallel-block-words.dat',
    'market-svn-data-dir/parallel-final-stop-words.dat',
    'market-svn-data-dir/parallel-offers-stop-queries.dat',
    'market-svn-data-dir/parallel-query-cpa-models.db',
    'market-svn-data-dir/parallel-query-cpc-models.db',
    'market-svn-data-dir/alcohol-queries.dat',
    'market-svn-data-dir/stop-words.dat',
    'market-svn-data-dir/wizards-vendor-queries.dat',
    'market-svn-data-dir/market.pure.trie',
    'market-svn-data-dir/parallel_import_warranty.json',
    'market-svn-data-dir/redirect-black-list.db',
    'market-svn-data-dir/resale_gradations.json',
    'market-svn-data-dir/redirect-stop-categories.db',
    'market-svn-data-dir/redirect-stop-vendors.db',
    'market-svn-data-dir/relevance_tweaker_data.dat',
    'market-svn-data-dir/redirect-white-list-low.db',
    'market-svn-data-dir/fast/redirect-white-list.db',
    'market-svn-data-dir/fast/redirect-white-list-app.db',
    'market-svn-data-dir/fast/booster.json',
    'market-svn-data-dir/fast/splitted_category_names.json',
    'market-svn-data-dir/fast/offers-min-price-for-region-delivery.db',
    'market-svn-data-dir/redirect-white-list-blue-low.db',
    'market-svn-data-dir/redirect-white-list-blue.db',
    'market-svn-data-dir/incut-black-list.db',
    'market-svn-data-dir/incut-black-list-hid.db',
    'market-svn-data-dir/fast/nailed-docs-white-list.db',
    'market-svn-data-dir/nailed-docs-white-list-catalog.db',
    'market-svn-data-dir/fast/credit_plans.testing.json',
    'market-svn-data-dir/warehouse_priorities.testing.pbuf.sn',
    'market-svn-data-dir/allowed_regions_for_books.pbuf.sn',
    'market-svn-data-dir/mskus_for_count_restriction.pbuf.sn',
    'market-svn-data-dir/sponsored-msku-in-category.tsv',
    'market-svn-data-dir/low-ue-msku.tsv',
    'market-svn-data-dir/beru-region-service-delay.tsv',
    'market-svn-data-dir/fmcg_parameters.json',
    'market-svn-data-dir/express_partners.json',
    'market-svn-data-dir/parent_promos.json',
    'market-svn-data-dir/categories_compare_params.csv',
    'market-svn-data-dir/boost_fee_groups.tsv',
    'market-svn-data-dir/boost_fee_reserve_prices.tsv',
    'market-svn-data-dir/bnpl_conditions.json',
    'market-svn-data-dir/preorder_dates.testing.json',
    'market-svn-data-dir/hidden-warehouses.json',
    'market-svn-data-dir/fashion_categories.json',
    'market-svn-data-dir/1p_fashion_premium.json',
    'market-svn-data-dir/3p_fashion_premium.json',
    'market-svn-data-dir/express_unit_economy.pbuf.sn',
    'market-svn-data-dir/honest_express_filter_settings.pbuf.sn',
    'market-svn-data-dir/hide_cash_only_conditions.pbuf.sn',
    'market-svn-data-dir/dynamic_delivery_tarrifs_models.pbuf.sn',
    'market-svn-data-dir/free_dsbs_shops.json',
    'input/getter/abo/operational-rating.csv',
    'input/getter/abo/shop-rating-direct.txt',
    'input/getter/mbi/delivery-direction-holidays.json',
    'input/getter/mbi/supplier-category-fees.xml',
    'input/getter/mbi/regional-payment-types.fb',
    'input/getter/mbo_cms/cms_report_promo.pbsn',
    'input/getter/mbo_stuff/vendors-info.xml',
    'input/getter/promo_by_cart/promo-by-cart.tsv',
    'input/getter/promo_by_cart/promo-by-cart-filter-by-hid.tsv',
    'input/getter/promo_by_cart/promo-by-cart-filter-by-msku.tsv',
    'input/getter/promo_by_cart/promo-by-cart-sku-special.tsv',
    'input/getter/user_split/user_split.binary',
    'input/getter/yt_money/bid-correction-config.json',
    'input/getter/yt_money/market_pp.json',
    'input/getter/yt_money/reference-shops-data.json',
    'input/getter/yt_money/return_rate.tsv',
    'input/getter/yt_money/buybox_settings.tsv',
    'input/getter/yt_money/buybox_exceptions.tsv',
    'input/getter/yt_money/card_analogs_settings.tsv',
    'input/mbo/global-vendors.pb',
    'input/mbo/vendor-recommended-shops.xml',
    'input/model_color_glob_vendor.csv',
    'input/picrobot_thumbs.meta',
    'service-offers/service_offers_mapping.fb',
    'service-offers/ware_md5_mapping.fb64',
    'stats/category_region_stats.csv',
    'stats/blue_buybox_category_region_stats.csv',
    'stats/category_regional_shops.csv',
    'stats/shop_regional_categories.csv',
    'stats/vendor_category_stats.pbuf.sn',
    'input/getter/yt_money/transaction_category_fees.tsv',
    'input/getter/yt_money/recommended_bids.tsv',
    'input/getter/yt_money/transaction_fee_periods.tsv',
    'input/getter/yt_money/reserve_price.tsv',
    'input/getter/yt_money/region_warehouse_mapping.tsv',
    'input/getter/yt_money/region_warehouse_mapping.fb',
    'input/getter/yt_money/price_thrs.mmap',
    'input/getter/yt_grade_agitations/model_grade_agitations.csv',
    'cron_stats/vcluster_wizard_stats/recent/vcluster_wizard_stats.pbuf.sn',
    'input/getter/yt_express_warehouses/express_warehouses.pbuf.sn',
    'input/supplier_region_restrictions.fb',
]

ORIGIN_REPORT_CARD_INDEX_FILES = [
    'cards-index/categories_vendors.db',
    'cards-index/blue_categories_vendors.db',
]


def make_filestubs(root_dir, file_list):
    '''
    Создает множество пустых файлов.
    root_dir - корневая директория относительно которой
                создаются файлы
    file_list - list вида:
    {
        "dir_name1/file_name1",
        "dir_name1/file_name2",
        "dir_name2/file_name2",
    }

    '''
    for path in file_list:
        target_path = os.path.join(root_dir, path)
        dir_path = os.path.dirname(target_path)
        util.makedirs(dir_path)
        util.touch(target_path)


def make_file_content(path, content):
    util.makedirs(os.path.dirname(path))
    with open(path, 'w') as file:
        file.write(content)


class TestReportData(unittest.TestCase):

    def setUp(self):
        """
        make "/var/lib/yandex/indexer/market/last_complete/search-stats-mmap/"
        """
        self.maxDiff = None

        self.root_dir = tempfile.mkdtemp()
        self.generation_name = "20190506_1019"
        self.prices_generation_name = "20190506_080010"
        self.generation_dir = os.path.join(self.root_dir, self.generation_name)
        print "generation_dir={}".format(self.generation_dir)

        make_filestubs(self.generation_dir, ORIGIN_SEARCH_STATS_MMAP_FILES)

        self.backends_dir = os.path.join(self.root_dir, 'backends')
        make_filestubs(self.backends_dir, ['somefile'])

        os.environ['PR_CONFIG_PATH'] = source_path('market/idx/mir/etc/picrobot.cfg')
        self.config = miconfig.MiConfig(context.MI_CONFIG_PATH, context.DS_CONFIG_PATH)
        self.config.working_dir = self.root_dir
        self.config.dists_dir = os.path.join(self.root_dir, 'dists')
        self.config.cron_stats_dir = os.path.join(self.generation_dir, "cron_stats")
        self.config.jump_table_dumper_dir = os.path.join(self.root_dir, 'jump_table')

        self.config.log_dir = os.path.join(self.root_dir, 'logs')
        util.makedirs(self.config.log_dir)
        util.makedirs(os.path.join(self.generation_dir, "ctr", "mmap"))

        self.config.svn_data_dir = os.path.join(self.generation_dir, 'market-svn-data-dir')
        self.config.fast_svn_data_dir = os.path.join(self.generation_dir, 'market-svn-data-dir/fast')
        self.config.gl_update_dir = os.path.join(self.generation_dir, 'gl-update-dir')
        self.config.warehouse_priorities_filename = 'warehouse_priorities.testing.pbuf.sn',
        self.config.preorder_dates_filename = 'preorder_dates.testing.json'
        self.config.credit_plans_filename = 'credit_plans.testing.json'
        make_filestubs(self.generation_dir, ORIGIN_REPORT_DATA_FILES)
        make_filestubs(self.generation_dir, ORIGIN_REPORT_CARD_INDEX_FILES)

        util.makedirs(self.config.jump_table_dumper_dir)
        util.touch(os.path.join(self.config.jump_table_dumper_dir, 'jump_table.fb'))

        self.config.make_generation_meta = True
        make_file_content(
            os.path.join(
                self.generation_dir, 'input', 'qpipe_snapshots', 'prices', 'prices.generation'
            ),
            '{}\n'.format(self.prices_generation_name)
        )

        self.config.compression_binary = os.path.join(BUILDROOT, 'tools/uc/uc')
        self.config.dump_shop_by_region_restrictions_enabled = True

    def _get_result_stat_files(self, add_meta, add_base):
        # for meta and base
        files = [
            'blue_group_region_stats.mmap',
            'gl_mbo.mmap',
            'model_warnings.mmap',
            'gl_mbo.aho_corasick',
            'group_region_stats.mmap',
            'cluster_pictures.mmap',
            'book_now_model.mmap',
            'model2msku.mmap',
            'shops_outlet_v5.mmap',
            'min-bids-model-stats.mmap',
            'contex_experiments.mmap',
            'min-bids-category-stats.mmap',
            'regional_delivery.mmap',
            'yt_promo_details.mmap',
            'promo_details.mmap',
            'click_n_collect_info.mmap',
        ]

        # only for meta
        if add_meta:
            files.extend([
                'model_geo_stats.mmap',
                'bid-correction-data.mmap',
                'msku-transitions.mmap',
                'model-transitions.mmap',
                'guru_light_region_stats.mmap',
                'shop_delivery_options.mmap',
                'gifts.mmap',
                'shop_calendar.mmap',
                'model_local_offers_geo_stats.mmap',
                'best_grades_for_koldunshik.mmap',
            ])

        # only for base
        if add_base:
            files.extend([
                'yt_shop_vendor_promo_clicks_stats.mmap',
                'model_offline_offers_geo_stats.mmap',
                'is_global_regional_stats.mmap',
                'model_stats.mmap',
                'best_grades_for_koldunshik_for_basesearch.mmap',
            ])

        return files

    def test_meta_stats_mmap_dist(self):
        with patch('market.idx.pylibrary.mindexer_core.dist_data.torrentdata.delete_torrent'), \
                patch('market.idx.pylibrary.mindexer_core.dist_data.torrentdata.make_torrent'):
            reportdata.make_meta_stats_mmap_dist(self.config, self.generation_name)

        target_arc_dir = os.path.join(self.config.dists_dir, self.generation_name, 'search-meta-stats')
        files = os.listdir(target_arc_dir)
        self.assertItemsEqual(files, ['search-meta-stats.tar.zstd_10'])

        target_files_dir = os.path.join(self.generation_dir, 'search-meta-stats-mmap')
        files = os.listdir(target_files_dir)
        self.assertItemsEqual(files, self._get_result_stat_files(add_meta=True, add_base=False))

    def test_base_stats_mmap_dist(self):
        with patch('market.idx.pylibrary.mindexer_core.dist_data.torrentdata.delete_torrent'), \
                patch('market.idx.pylibrary.mindexer_core.dist_data.torrentdata.make_torrent'):
            reportdata.make_stats_mmap_dist(self.config, self.generation_name)

        target_arc_dir = os.path.join(self.config.dists_dir, self.generation_name, 'search-stats')
        files = os.listdir(target_arc_dir)
        self.assertItemsEqual(files, ['search-stats.tar.zstd_10'])

        target_files_dir = os.path.join(self.generation_dir, 'search-stats-mmap')
        files = os.listdir(target_files_dir)
        self.assertItemsEqual(files, self._get_result_stat_files(add_meta=True, add_base=True))

    def _get_result_report_data_files(self, only_for_meta=False):
        files = [
            'delivery_service_flags.json',
            'currency_rates.xml',
            'lms.pbuf.sn',
            'nordstream.pb.sn',
            'model_group.csv',
            'model_group_for_beru_msku_card.csv',
            'fast_data_outlets.pbuf.sn',
            'geo2.c2p',
            'region_holidays.xml',
            'yamarec.conf',
            'category-restrictions.pb',
            'category_transitions.pb',
            'parameters_mapping.pb',
            'shops.dat',
            'no-models-categories-exp.txt',
            'navigation_info.xml',
            'tovar-tree.pb',
            'geobase.xml',
            'cpa-categories.xml',
            'cataloger.navigation.all.xml',
            'min-bids-price-groups.json',
            'modelConversion.csv',
            'prohibited_blue_offers.json',
            'model-opinions.db',
            'category_stats.csv',
            'warehouses_express_categories.csv',
            'loyalty_delivery_discount.pbuf.sn',
            'pers-best-model-grades.csv',
            'model_grade_agitations.csv',
            'shop-best-review.txt',
            'shop-rating.txt',
            'shops-regional-clone.txt',
            'model_rating.txt',
            'model-clicks.txt',
            'total-stats.txt',
            'delivery_holidays.xml',
            'yamarec_data',
            'shops_delivery_service_calendar.xml',
            'navigation-redirects.xml',
            'category_min_bids_params.xml',
            'min-bids-config.json',
            'test-shops.txt',
            'model_groups.tsv',
            'vendor_reserve_prices.pb',
            'filters.txt',
            'shop_rating_on_verified_buyer.txt',
            'delivery_service_end_point_intervals.json',
            'shop_names.csv',
            's.c2n',
            'mindexer.version',
            'generation.metadata',
            'pictures-config.xml',
            'backends',
            'market_pp.json',
            'return_rate.tsv',
            'buybox_settings.tsv',
            'buybox_exceptions.tsv',
            'card_analogs_settings.tsv',
            'transaction_category_fees.tsv',
            'recommended_bids.tsv',
            'transaction_fee_periods.tsv',
            'reserve_price.tsv',
            'region_warehouse_mapping.tsv',
            'region_warehouse_mapping.fb',
            'price_thrs.mmap',
            'categories_compare_params.csv',
            'boost_fee_groups.tsv',
            'boost_fee_reserve_prices.tsv',
            'vcluster_wizard_stats.pbuf.sn',
            'service_offers_mapping.fb',
            'jump_table.fb',
            'ware_md5_mapping.fb64',
            'bnpl_conditions.json',
            'preorder_dates.json',
            'credit_plans.json',
            'regional-payment-types.fb',
            'hidden-warehouses.json',
            'fashion_categories.json',
            '1p_fashion_premium.json',
            '3p_fashion_premium.json',
            'express_unit_economy.pbuf.sn',
            'honest_express_filter_settings.pbuf.sn',
            'category_region_stats.csv',
            'blue_buybox_category_region_stats.csv',
            'parallel_import_warranty.json',
            'redirect-stop-categories.db',
            'redirect-stop-vendors.db',
            'redirect-white-list.db',
            'redirect-white-list-app.db',
            'relevance_tweaker_data.dat',
            'redirect-white-list-low.db',
            'redirect-white-list-blue.db',
            'redirect-white-list-blue-low.db',
            'redirect-black-list.db',
            'resale_gradations.json',
            'booster.json',
            'splitted_category_names.json',
            'fmcg_parameters.json',
            'offers-min-price-for-region-delivery.db',
            'supplier_region_restrictions.fb',
            'nailed-docs-white-list.db',
            'nailed-docs-white-list-catalog.db',
            'dynamic_delivery_tarrifs_models.pbuf.sn',
            'free_dsbs_shops.json',
            'picrobot_thumbs.meta',
        ]

        if only_for_meta:
            return files

        files.extend([
            'shop_incut.tsv',
            'safety_guarantee_shops.tsv',
            'uncollapsed_models.tsv',
            'vendor-recommended-shops.xml',
            'wizards-vendor-queries.dat',
            'family-stop-queries.dat',
            'vendors-info.xml',
            'parallel-block-queries.dat',
            'parallel-block-words.dat',
            'parallel-final-stop-words.dat',
            'parallel-query-cpa-models.db',
            'parallel-query-cpc-models.db',
            'global-vendors.pb',
            'market.pure.trie',
            'parallel-offers-stop-queries.dat',
            'vendor_category_stats.pbuf.sn',
            'incut-black-list.db',
            'incut-black-list-hid.db',
            'direct-minus-words.dat',
            'delivery_direction_holidays.json',
            'category_regional_shops.csv',
            'model_color_glob_vendor.csv',
            'cms_report_promo.pbsn',
            'bid-correction-config.json',
            'supplier-category-fees.xml',
            'reference-shops-data.json',
            'shop_regional_categories.csv',
            'adult-queries.dat',
            'stop-words.dat',
            'alcohol-queries.dat',
            'shop-rating-direct.txt',
            'warehouse_priorities.pbuf.sn',
            'credit-templates.fb',
            'market_modelbids_banner.tsv',
            'promo-by-cart.tsv',
            'promo-by-cart-filter-by-hid.tsv',
            'promo-by-cart-filter-by-msku.tsv',
            'promo-by-cart-sku-special.tsv',
            'user_split.binary',
            'sponsored-msku-in-category.tsv',
            'low-ue-msku.tsv',
            'beru-region-service-delay.tsv',
            'express_partners.json',
            'parent_promos.json',
            'allowed_regions_for_books.pbuf.sn',
            'mskus_for_count_restriction.pbuf.sn',
            'operational-rating.csv',
            'demand-prediction-one-p-input.mmap',
            'demand-prediction-sales.mmap',
            'hide_cash_only_conditions.pbuf.sn',
            'express_warehouses.pbuf.sn',
        ])

        return files

    def _check_generation_meta(self, target_arc_dir):
        with open(os.path.join(target_arc_dir, 'generation.metadata'), 'r') as fp:
            generation_meta = json.load(fp)
            assert generation_meta['version'] == get_package_version()
            assert generation_meta['generation'] == self.generation_name
            assert generation_meta['prices_generation'] == self.prices_generation_name
            assert generation_meta['rty_fallback_ts'] == util.generation2timestamp(
                self.prices_generation_name, '%Y%m%d_%H%M%S'
            )

    def test_meta_report_data_dist(self):
        with patch('market.idx.pylibrary.mindexer_core.dist_data.torrentdata.delete_torrent'), \
                patch('market.idx.pylibrary.mindexer_core.dist_data.torrentdata.make_torrent'):
            reportdata.make_meta_report_data_dist(self.config, self.generation_name, self.backends_dir)

        target_arc_dir = os.path.join(self.config.dists_dir, self.generation_name, 'search-meta-report-data')
        files = os.listdir(target_arc_dir)
        self.assertItemsEqual(files, ['search-meta-report-data.tar.zstd_10'])

        target_arc_dir = os.path.join(self.generation_dir, 'meta-report-data')
        files = os.listdir(target_arc_dir)
        self.assertItemsEqual(files, self._get_result_report_data_files(True))
        self._check_generation_meta(target_arc_dir)

    def test_report_data_dist(self):
        with patch('market.idx.pylibrary.mindexer_core.dist_data.torrentdata.delete_torrent'), \
                patch('market.idx.pylibrary.mindexer_core.dist_data.torrentdata.make_torrent'):
            reportdata.make_report_data_dist(self.config, self.generation_name, self.backends_dir)

        target_arc_dir = os.path.join(self.config.dists_dir, self.generation_name, 'search-report-data')
        files = os.listdir(target_arc_dir)
        self.assertItemsEqual(files, ['search-report-data.tar.zstd_10'])

        target_arc_dir = os.path.join(self.generation_dir, 'report-data')
        files = os.listdir(target_arc_dir)
        self.assertItemsEqual(files, self._get_result_report_data_files(False))
        self._check_generation_meta(target_arc_dir)

    def test_report_data_dist_on_fresh_test(self):
        """
        В отличие от других окружений, fresh-test должен добавлять файлы categories_vendors.db и
        blue_categories_vendors.db в report-data.
        """

        with patch('market.idx.pylibrary.mindexer_core.dist_data.torrentdata.delete_torrent'), \
                patch('market.idx.pylibrary.mindexer_core.dist_data.torrentdata.make_torrent'):
            self.config.is_fresh = True
            self.config.is_testing = True
            self.config.cards_need_build_in_big_generation = True
            reportdata.make_report_data_dist(self.config, self.generation_name, self.backends_dir)

        target_arc_dir = os.path.join(self.config.dists_dir, self.generation_name, 'search-report-data')
        files = os.listdir(target_arc_dir)
        self.assertItemsEqual(files, ['search-report-data.tar.zstd_10'])

        target_arc_dir = os.path.join(self.generation_dir, 'report-data')
        files = os.listdir(target_arc_dir)
        category_files = ['categories_vendors.db', 'blue_categories_vendors.db']
        self.assertItemsEqual(files, self._get_result_report_data_files(False) + category_files)
        self._check_generation_meta(target_arc_dir)


if __name__ == '__main__':
    unittest.main()
