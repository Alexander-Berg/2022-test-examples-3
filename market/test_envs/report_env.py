# coding: utf-8

from itertools import chain

import os

import socket
from time import sleep

import yatest.common
from yatest.common import network
from market.idx.yatf.common import ignore_errors

from market.idx.yatf.resources.report.httpsearch_cfg import HttpSearchConf
from market.idx.yatf.resources.resource import FileResource, FileGeneratorResource
from market.idx.yatf.resources.tovar_tree_pb import MboCategory, TovarTreePb
from market.idx.yatf.test_envs.base_env import BaseEnv


def _STUBS_DIR():
    return os.path.join(yatest.common.source_path(), 'market', 'idx', 'yatf',
                          'resources', 'report', 'stubs')


def _STUB_MODEL_INDEX_DIR():
    return os.path.join(_STUBS_DIR(), 'index', 'model')


def _STUB_OFFER_INDEX_DIR():
    return os.path.join(_STUBS_DIR(), 'index', 'shop')


def _STUB_REPORT_DATA_DIR():
    return os.path.join(_STUBS_DIR(), 'report-data')


def _STUB_TVMTOOL_DIR():
    return os.path.join(_STUBS_DIR(), 'tvmtool')


def _STUB_DYNAMIC_MODELS_DIR():
    return os.path.join(_STUBS_DIR(), 'dynamic_models')


def make_categories():
    return [
        MboCategory(hid=90401, tovar_id=0,
                    unique_name="Все товары", name="Все товары",
                    aliases=["Все товары"], no_search=True,
                    output_type=MboCategory.SIMPLE),

        MboCategory(hid=90764, tovar_id=6, parent_hid=90401,
                    unique_name="Детские товары", name="Детские товары",
                    aliases=["Дитячі товари"],
                    output_type=MboCategory.VISUAL),

        MboCategory(hid=90795, tovar_id=741, parent_hid=90764,
                    unique_name="Товары     для мам и малышей 123",
                    name="Товары для    мам и малышей         ",
                    aliases=["Товари для мам та малюків"],
                    output_type=MboCategory.GURULIGHT),

        MboCategory(hid=90541, tovar_id=1160, parent_hid=90795,
                    unique_name="Товары для беременных и кормящих женщин",
                    name="Товары для мам",
                    aliases=["Товари для мам"],
                    output_type=MboCategory.GURU),

        MboCategory(hid=280280, tovar_id=1, parent_hid=90764,
                    unique_name="Товары     для мам и малышей 123",
                    name="Товары для    мам и малышей         ",
                    aliases=["Товари для мам та малюків"],
                    output_type=MboCategory.GURU),

        MboCategory(hid=12385944, tovar_id=2, parent_hid=90795,
                    unique_name="Товары для беременных и кормящих женщин",
                    name="Товары для мам",
                    aliases=["Товари для мам"],
                    output_type=MboCategory.GURULIGHT),

        MboCategory(hid=100, tovar_id=100, parent_hid=90401,
                    unique_name="Какая-то категория 1",
                    name="Какая-то категория 1",
                    aliases=["Какая-то категория 1"],
                    output_type=MboCategory.UNDEFINED,
                    published=False),

        MboCategory(hid=101, tovar_id=100, parent_hid=100,
                    unique_name="Какая-то категория 2",
                    name="Какая-то категория 2",
                    aliases=["Какая-то категория 2"],
                    output_type=MboCategory.UNDEFINED,
                    published=True),
    ]


class ReportTestEnv(BaseEnv):

    def init_stubs(self):
        self._MODEL_INDEX_STUBS = {
            name: FileResource(os.path.join(_STUB_MODEL_INDEX_DIR(), filename))
            for name, filename in list({
                'model_index_dssm_values_binary': 'dssm.values.binary',
                'model_index_gl_models_mmap': 'gl_models.mmap',
                'model_index_hard2_dssm_values_binary': 'hard2_dssm.values.binary',
                'model_index_hyper_ts_c2n': 'hyper_ts.c2n',
                'model_index_indexaa': 'indexaa',
                'model_index_indexarc': 'indexarc',
                'model_index_indexdir': 'indexdir',
                'model_index_indexfh': 'indexfh',
                'model_index_indexinv': 'indexinv',
                'model_index_indexkey': 'indexkey',
                'model_index_maliases_c2n': 'maliases.c2n',
                'model_index_reformulation_dssm_values_binary': 'reformulation_dssm.values.binary',
            }.items())
        }

        self._OFFER_INDEX_STUBS = {
            name: FileResource(os.path.join(_STUB_OFFER_INDEX_DIR(), filename))
            for name, filename in list({
                'offer_index_base_docs_props_meta': 'base_docs_props.meta',
                'offer_index_base_offer_props': 'base-offer-props.fb',
                'offer_index_base_offer_props_ext': 'base-offer-props-ext.fb64',
                'offer_index_bids_meta_binary': 'bids.meta.binary',
                'offer_index_book_now_offer.mmap': 'book_now_offer.mmap',
                'offer_index_cmagic_id_c2n': 'cmagic_id.c2n',
                'offer_index_content-offer_tsv': 'content-offer.tsv',
                'offer_index_dssm_values_binary': 'dssm.values.binary',
                'offer_index_forbidden_regions_c2n': 'forbidden_regions.c2n',
                'offer_index_generation_name': 'generation_name',
                'offer_index_geo_c2p': 'geo.c2p',
                'offer_index_gl_sc_mmap': 'gl_sc.mmap',
                'offer_index_hard2_dssm_values_binary': 'hard2_dssm.values.binary',
                'offer_index_hyper_ts_c2n': 'hyper_ts.c2n',
                'offer_index_indexaa': 'indexaa',
                'offer_index_indexarc': 'indexarc',
                'offer_index_indexdir': 'indexdir',
                'offer_index_indexfh': 'indexfh',
                'offer_index_indexinv': 'indexinv',
                'offer_index_indexkey': 'indexkey',
                'offer_index_indexsent': 'indexsent',
                'offer_index_local_delivery_yml_mmap': 'local_delivery_yml.mmap',
                'offer_index_offer_delivery_buckets_mmap': 'offer_delivery_buckets.mmap',
                'offer_index_offer_promo_mmap': 'offer_promo.mmap',
                'offer_index_reformulation_dssm_values_binary': 'reformulation_dssm.values.binary',
                'offer_index_vat_props_values_binary': 'vat_props.values.binary',
                'offer_index_vendor_values_binary': 'vendor.values.binary',
                'offer_index_ware_md5_values_binary': 'ware_md5.values.binary',
                'offer_index_bids_timestamps_fb': 'bids-timestamps.fb',
            }.items())
        }

        self._REPORT_DATA_STUBS = {
            name: FileResource(os.path.join(_STUB_REPORT_DATA_DIR(), filename))
            for name, filename in list({
                'base_docs_props_meta': 'base_docs_props.meta',
                'blue_categories_vendors_db': 'blue_categories_vendors.db',
                'book_now_model_mmap': 'book_now_model.mmap',
                'categ_query_ctr_db': 'categ-query-ctr.db',
                'categories_vendors_db': 'categories_vendors.db',
                'category_region_stats_csv': 'category_region_stats.csv',
                'blue_category_region_stats_csv': 'blue_category_region_stats.csv',
                'blue_buybox_category_region_stats_csv': 'blue_buybox_category_region_stats.csv',
                'bnpl_conditions.json' : 'bnpl_conditions.json',
                'business_logos.fb' : 'business_logos.fb',
                'category_regional_shops_csv': 'category_regional_shops.csv',
                'category_restrictions_pb': 'category-restrictions.pb',
                'cataloger.navigation.all.xml': 'cataloger.navigation.all.xml',
                'clickdaemon_keys': 'clickdaemon.keys',
                'cluster_pictures_mmap': 'cluster_pictures.mmap',
                'cluster_pictures_pbuf_sn': 'cluster_pictures.pbuf.sn',
                'cpa_categories_xml': 'cpa-categories.xml',
                'credit_plans.json': 'credit_plans.json',
                'ctr_categ_db': 'ctr-categ.db',
                'ctr_vendor_db': 'ctr-vendor.db',
                'ctr_geo_db': 'ctr-geo.db',
                'ctr_model_db': 'ctr-model.db',
                'ctr_shop_db': 'ctr-shop.db',
                'ctr_ts_db': 'ctr-ts.db',
                'ctr_zero_db': 'ctr-zero.db',
                'currency_rates_xml': 'currency_rates.xml',
                'delivery_holidays_xml': 'delivery_holidays.xml',
                'delivery_service_flags_json': 'delivery_service_flags.json',
                'family_stop_queries_dat': 'family-stop-queries.dat',
                'geo2_c2p': 'geo2.c2p',
                'geobase_xml': 'geobase.xml',
                'gl_mbo_pbuf_sn': 'gl_mbo.pbuf.sn',
                'gl_models_pbuf_sn': 'gl_models.pbuf.sn',
                'group_region_stats_mmap': 'group_region_stats.mmap',
                'guru_light_region_stats_mmap': 'guru_light_region_stats.mmap',
                'hidden-warehouses.json' : 'hidden-warehouses.json',
                'model_geo_stats_mmap': 'model_geo_stats.mmap',
                'model_group_csv': 'model_group.csv',
                'model_group_for_beru_msku_card_csv': 'model_group_for_beru_msku_card.csv',
                'model_local_offers_geo_stats_mmap': 'model_local_offers_geo_stats.mmap',
                'model_offline_offers_geo_stats_mmap': 'model_offline_offers_geo_stats.mmap',
                'model_opinions_db': 'model-opinions.db',
                'model_warnings_mmap': 'model_warnings.mmap',
                'msearch_categ_normalized_by_dnorm_query_ctr_db': 'msearch-categ-normalized_by_dnorm_query-ctr.db',
                'msearch_categ_normalized_by_synnorm_query_ctr_db': 'msearch-categ-normalized_by_synnorm_query-ctr.db',
                'msearch_categ_normalized_to_lower_and_sorted_query_ctr_db': 'msearch-categ-normalized_to_lower_and_sorted_query-ctr.db',
                'msearch_categ_normalized_to_lower_query_ctr_db': 'msearch-categ-normalized_to_lower_query-ctr.db',
                'msearch_categ_query_ctr_db': 'msearch-categ-query-ctr.db',
                'msearch_vendor_normalized_by_dnorm_query_ctr_db': 'msearch-vendor-normalized_by_dnorm_query-ctr.db',
                'msearch_vendor_normalized_by_synnorm_query_ctr_db': 'msearch-vendor-normalized_by_synnorm_query-ctr.db',
                'msearch_vendor_normalized_to_lower_and_sorted_query_ctr_db': 'msearch-vendor-normalized_to_lower_and_sorted_query-ctr.db',
                'msearch_vendor_normalized_to_lower_query_ctr_db': 'msearch-vendor-normalized_to_lower_query-ctr.db',
                'msearch_vendor_query_ctr_db': 'msearch-vendor-query-ctr.db',
                'market_modelbids_banner.tsv': 'market_modelbids_banner.tsv',
                'msearch_model_normalized_by_dnorm_query_ctr_db': 'msearch-model-normalized_by_dnorm_query-ctr.db',
                'msearch_model_normalized_by_synnorm_query_ctr_db': 'msearch-model-normalized_by_synnorm_query-ctr.db',
                'msearch_model_normalized_to_lower_and_sorted_query_ctr_db': 'msearch-model-normalized_to_lower_and_sorted_query-ctr.db',
                'msearch_model_normalized_to_lower_query_ctr_db': 'msearch-model-normalized_to_lower_query-ctr.db',
                'msearch_model_query_ctr_db': 'msearch-model-query-ctr.db',
                'msearch_normalized_by_dnorm_query_ctr_db': 'msearch-normalized_by_dnorm_query-ctr.db',
                'msearch_normalized_by_synnorm_query_ctr_db': 'msearch-normalized_by_synnorm_query-ctr.db',
                'msearch_normalized_to_lower_and_sorted_query_ctr_db': 'msearch-normalized_to_lower_and_sorted_query-ctr.db',
                'msearch_normalized_to_lower_query_ctr_db': 'msearch-normalized_to_lower_query-ctr.db',
                'msearch_query_ctr_db': 'msearch-query-ctr.db',
                'msearch_ts_normalized_by_dnorm_query_ctr_db': 'msearch-ts-normalized_by_dnorm_query-ctr.db',
                'msearch_ts_normalized_by_synnorm_query_ctr_db': 'msearch-ts-normalized_by_synnorm_query-ctr.db',
                'msearch_ts_normalized_to_lower_and_sorted_query_ctr_db': 'msearch-ts-normalized_to_lower_and_sorted_query-ctr.db',
                'msearch_ts_normalized_to_lower_query_ctr_db': 'msearch-ts-normalized_to_lower_query-ctr.db',
                'msearch_ts_query_ctr_db': 'msearch-ts-query-ctr.db',
                'msearch_wmd5_normalized_by_dnorm_query_ctr_db': 'msearch-wmd5-normalized_by_dnorm_query-ctr.db',
                'msearch_wmd5_normalized_by_synnorm_query_ctr_db': 'msearch-wmd5-normalized_by_synnorm_query-ctr.db',
                'msearch_wmd5_normalized_to_lower_and_sorted_query_ctr_db': 'msearch-wmd5-normalized_to_lower_and_sorted_query-ctr.db',
                'msearch_wmd5_normalized_to_lower_query_ctr_db': 'msearch-wmd5-normalized_to_lower_query-ctr.db',
                'msearch_wmd5_query_ctr_db': 'msearch-wmd5-query-ctr.db',
                'msearch_zero_ctr_db': 'msearch-zero-ctr.db',
                'navigation_info_xml': 'navigation_info.xml',
                'parallel_stop_queries_dat': 'parallel-stop-queries.dat',
                'pers_best_grades_csv': 'pers-best-model-grades.csv',
                'picrobot_thumbs_meta': 'picrobot_thumbs.meta',
                'premium_categ_query_ctr_db': 'premium-categ-query-ctr.db',
                'premium_query_ctr_db': 'premium-query-ctr.db',
                'premium_ts_query_ctr_db': 'premium-ts-query-ctr.db',
                'premium_zero_ctr_db': 'premium-zero-ctr.db',
                'preorder_dates.json': 'preorder_dates.json',
                'promo_by_cart_tsv': 'promo-by-cart.tsv',
                'promo_by_cart_filter_by_hid_tsv': 'promo-by-cart-filter-by-hid.tsv',
                'promo_by_cart_filter_by_msku_tsv': 'promo-by-cart-filter-by-msku.tsv',
                'user_split_binary': 'user_split.binary',
                'sponsored_msku_in_category_tsv': 'sponsored-msku-in-category.tsv',
                'low_ue_msku_tsv': 'low-ue-msku.tsv',
                'beru_region_service_delay_tsv': 'beru-region-service-delay.tsv',
                'query_ctr_db': 'query-ctr.db',
                'redirect_stop_categories_db': 'redirect-stop-categories.db',
                'redirect_stop_vendors_db': 'redirect-stop-vendors.db',
                'redirect_white_list_db': 'redirect-white-list.db',
                'regional_delivery_mmap': 'regional_delivery.gz',
                'report_templates_xml': 'report-templates.xml',
                's_c2n': 's.c2n',
                'shop_best_review_txt': 'shop-best-review.txt',
                'shop_names_csv': 'shop_names.csv',
                'shop_rating_direct_txt': 'shop-rating-direct.txt',
                'shops_dat': 'shops.dat',
                'shops_outlet_xml': 'shops_outlet.xml',
                'shops_outlet_mmap': 'shops_outlet_v5.mmap',
                'supplier-category-fees.xml': 'supplier-category-fees.xml',
                'ts_query_ctr_db': 'ts-query-ctr.db',
                'vcluster_wizard_stats_pbuf_sn': 'vcluster_wizard_stats.pbuf.sn',
                'yamarec_conf': 'yamarec.conf',
                'yamarec_data': os.path.join('yamarec_data', '2762'),
                'zero_ctr_db': 'zero-ctr.db',
                'lms_pbuf_sn': 'lms.pbuf.sn',
                'fast_data_outlets_pbuf_sn': 'fast_data_outlets.pbuf.sn',
                'operational-rating.csv': 'operational-rating.csv',
                'warehouses_express_categories.csv' : 'warehouses_express_categories.csv',
            }.items())
        }

        # Full tovar-tree.pb is too large to put it to the Arcadia, so we have to generate even stub
        self._REPORT_DATA_STUBS['tovar_tree_pb'] = TovarTreePb(make_categories())

        self._TVMTOOL_STUBS = {
            name: FileResource(os.path.join(_STUB_TVMTOOL_DIR(), filename))
            for name, filename in list({
                'token': 'token',
            }.items())
        }

        self._DYNAMIC_MODELS_STUBS = {
            name: FileResource(os.path.join(_STUB_DYNAMIC_MODELS_DIR(), filename))
            for name, filename in list({
                'market_click_query_dssm': os.path.join('search', 'market_click_query.dssm'),
                'hard2_query_dssm': os.path.join('search', 'hard2_query.dssm'),
                'reformulation_query_dssm': os.path.join('search', 'reformulation_query.dssm'),
            }.items())
        }

        self._STUBS = dict(
            chain.from_iterable(
                list(d.items())
                for d in (
                    self._REPORT_DATA_STUBS,
                    self._OFFER_INDEX_STUBS,
                    self._MODEL_INDEX_STUBS,
                    self._TVMTOOL_STUBS,
                    self._DYNAMIC_MODELS_STUBS,
                )
            )
        )

    def __init__(self, **resources):
        self.init_stubs()
        super(ReportTestEnv, self).__init__(**resources)
        self.resources['httpsearch_cfg'] = resources.get(
            'httpsearch_cfg',
            HttpSearchConf()
        )

    @property
    def description(self):
        return 'report'

    @property
    def all_index_path(self):
        return os.path.join(self.input_dir, 'index')

    @property
    def model_index_path(self):
        return os.path.join(self.input_dir, 'index', 'model')

    @property
    def offer_index_path(self):
        return os.path.join(self.input_dir, 'index', 'shop')

    @property
    def report_data_path(self):
        return os.path.join(self.input_dir, 'report-data')

    @property
    def tvmtool_token_path(self):
        return os.path.join(self.input_dir, 'tvmtool_token')

    @property
    def dynamic_models_path(self):
        return os.path.join(self.input_dir, 'dynamic_models')

    @property
    def meta_search_url(self):
        host = socket.getfqdn()
        port = self.resources['httpsearch_cfg'].server['options']['Port']
        resource = self.resources['httpsearch_cfg'].meta_search['yandsearch']['collection_id']

        return 'http://{host}:{port}/{resource}'.format(
            host=host,
            port=port,
            resource=resource
        )

    @property
    def port_manager(self):
        return self._pm

    def __enter__(self):
        self._pm = network.PortManager()
        ignore_errors(os.makedirs, OSError)(self.input_dir)
        ignore_errors(os.makedirs, OSError)(self.output_dir)

        ignore_errors(os.makedirs, OSError)(self.all_index_path)
        ignore_errors(os.makedirs, OSError)(self.model_index_path)
        ignore_errors(os.makedirs, OSError)(self.offer_index_path)
        ignore_errors(os.makedirs, OSError)(self.report_data_path)
        ignore_errors(os.makedirs, OSError)(self.tvmtool_token_path)
        ignore_errors(os.makedirs, OSError)(self.dynamic_models_path)
        ignore_errors(os.makedirs, OSError)(os.path.join(self.dynamic_models_path, 'search'))

        for name, resource in list(self.resources.items()):
            path = self.input_dir
            filename = resource.filename
            if name in self._MODEL_INDEX_STUBS:
                path = self.model_index_path
                filename = self._MODEL_INDEX_STUBS[name].filename
            elif name in self._OFFER_INDEX_STUBS:
                path = self.offer_index_path
                filename = self._OFFER_INDEX_STUBS[name].filename
            elif name in self._REPORT_DATA_STUBS:
                path = self.report_data_path
                filename = self._REPORT_DATA_STUBS[name].filename
            elif name in self._TVMTOOL_STUBS:
                path = self.tvmtool_token_path
                filename = self._TVMTOOL_STUBS[name].filename
            elif name in self._DYNAMIC_MODELS_STUBS:
                path = os.path.join(self.dynamic_models_path, 'search')
                filename = self._DYNAMIC_MODELS_STUBS[name].filename

            if isinstance(resource, FileResource):
                resource.symlink(path=os.path.join(path, filename))
            elif isinstance(resource, FileGeneratorResource):
                resource.dump(path=os.path.join(path, filename))
            else:
                raise TypeError('Unsupported recourse type for report')

        self.resources['httpsearch_cfg'].init(self)
        return self

    def __exit__(self, *args):
        super(ReportTestEnv, self).__exit__()
        if self.exec_result and self.exec_result.running:
            self.exec_result.kill()
        if self._pm:
            self._pm.release()

    def _wait_for_start(self):
        settings = self.resources['httpsearch_cfg'].server['options']
        started = False
        while not started:
            sleep(0.1)
            if not self.exec_result or not self.exec_result.running:
                raise Exception("Report died on start!")

            if os.path.isfile(settings['ServerLog']):
                with open(settings['ServerLog'], 'r') as f:
                    server_log = f.read()
                    started = 'Web server started' in server_log

    def execute(self, path=None):
        if path is None:
            relative_path = os.path.join('market', 'report', 'report_base', 'report_bin')
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        httpsearch_config = os.path.join(
            self.input_dir,
            self.resources['httpsearch_cfg'].filename
        )

        cmd = [
            path,
            '-d', httpsearch_config
        ]
        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False,
            wait=False
        )
        self._wait_for_start()
