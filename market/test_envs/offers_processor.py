# coding: utf-8

import os
import yatest.common
import base64

from yt.wrapper.ypath import ypath_join

from market.proto.indexer import GenerationLog_pb2 as Record_pb
from market.proto.indexer.FeedLog_pb2 import Feed
from market.proto.common.common_pb2 import ApiData, TCategParamsEntryGenlog
from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_token_resource import YtTokenResource
from market.idx.yatf.test_envs.base_env import BaseEnv
from subprocess import Popen, PIPE

from google.protobuf.json_format import ParseDict
from google.protobuf.json_format import MessageToDict


def _IDX_STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'offers', 'yatf',
        'resources',
        'offers_indexer',
        'stubs',
    )


def _STUBS_DIR():
    return os.path.join(
        yatest.common.source_path(),
        'market', 'idx', 'yatf',
        'resources',
        'stubs',
    )


def _MBI_STUBS_DIR():
    return os.path.join(
        _STUBS_DIR(),
        'getter',
        'mbi',
    )


def _MARKETDYNAMIC_STUBS_DIR():
    return os.path.join(
        _STUBS_DIR(),
        'getter',
        'marketdynamic'
    )


def _DELIVERY_STUBS_DIR():
    return os.path.join(
        _STUBS_DIR(),
        'delivery'
    )


def get_stub(stub_dir, stub_name):
    return FileResource(os.path.join(stub_dir, stub_name))


class OffersProcessorTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def __init__(
        self,
        yt_stuff,
        generation='20181129_1422',
        mapper_memory_mb=4096,
        use_bids=False,
        input_table_paths=None,
        drop_offers_with_no_sizes=False,
        stderr_log_yt_tabe_path=None,
        dropped_offers_table_path=None,
        do_collapse_offers=False,
        collapse_use_express=False,
        collapse_use_dsbs=False,
        enable_recipes_logic=False,
        enable_completed_offers_tracing=False,
        reduce_regions_instead_set_earth=False,
        drop_direct_hidden_by_market_idx=False,
        add_regions_from_shops_dat_or_external_table=False,
        add_earth_if_empty_regions=False,
        ignore_zero_region_in_buckets=False,
        prefer_reduced_regions=False,
        use_genlog_scheme=False,
        genlog_integrity_detect_empty_strings=False,
        **resourses
    ):
        self._STUBS = {
            name: get_stub(_IDX_STUBS_DIR(), filename)
            for name, filename in {
                'bucket_indices_csv': 'bucket_indices.csv',
                'categories_availability_tsv': 'categoriesAvailability.tsv',
                'category_restrictions_pb': 'category-restrictions.pb',
                'cms_report_promo_pbsn': 'cms_report_promo.pbsn',
                'cpa_categories_xml': 'cpa-categories.xml',
                'currency_rates_xml': 'currency_rates.xml',
                'delivery_service_flags_json': 'delivery_service_flags.json',
                'feedssessions_offers': 'feedssessions.offers',
                'forbidden_category_regions_xml': 'forbidden_category_regions.xml',
                'geo_c2p': 'geo.c2p',
                'geobase_xml': 'geobase.xml',
                'gl_mbo_pbuf_sn': 'gl_mbo.pbuf.sn',
                'global_vendors_xml': 'global.vendors.xml',
                'model_ids': 'model_ids.gz',
                'model_klp': 'model_klp.gz',
                'model_sale_dates': 'model_sale_dates.gz',
                'model_quantities': 'model_quantities.gz',
                'model_vidal': 'model_vidal.gz',
                'model_medical_flags': 'model_medical_flags.gz',
                'model_medicine_form': 'model_medicine_form.gz',
                'sku_sample': 'sku_sample.gz',
                'regional_delivery_mmap.gz': 'regional_delivery.gz',
                'rules_to_hide_offers_json': 'rules-to-hide-offers.json',
                'stop_word_hiding_rules_json': 'stop-word-hiding-rules.json.gz',
                'shops_outlet_v5_mmap': 'shops_outlet.gz',
                'tovar_tree_pb': 'tovar-tree.pb.gz',
                'cataloger.navigation.xml': 'cataloger.navigation.xml',
                'cataloger.navigation.all.xml': 'cataloger.navigation.all.xml',
                'ungrouping_model_params_gz': 'ungrouping_model_params.gz',
                'ungrouping_models_gz': 'ungrouping_models.gz',
                'yt_promo_details_mmap': 'yt_promo_details.mmap',
                'vendor_bids_csv': 'vendor_bids.csv',
                'vendor_recommended_business_csv': 'vendor_recommended_business.csv',
                'region_cache.pb.zstd': 'region_cache.pb.zstd',
                'model_hypes': 'model_hypes.gz',
            }.items()
        }
        self._STUBS['shops_utf8_dat'] = get_stub(
            _MBI_STUBS_DIR(), 'shops-utf8.dat'
        )
        if 'direct_dat' in resourses:
            self._STUBS['direct_dat'] = get_stub(
                _MBI_STUBS_DIR(), 'direct.dat'
            )
        self._STUBS['pickup_bucket_indices_csv'] = get_stub(
            _DELIVERY_STUBS_DIR(),
            'pickup_bucket_indices.csv'
        )
        self._STUBS['offer_filter_db'] = get_stub(
            _MARKETDYNAMIC_STUBS_DIR(), 'offer-filter.db'
        )
        self._STUBS['market_sku_filters_pbuf'] = get_stub(
            _MARKETDYNAMIC_STUBS_DIR(), 'market-sku-filters.pbuf'
        )
        self._STUBS['shop_cpc_filter_db'] = get_stub(
            _MARKETDYNAMIC_STUBS_DIR(), 'shop-cpc-filter.db'
        )
        self._STUBS['shop_cpa_filter_db'] = get_stub(
            _MARKETDYNAMIC_STUBS_DIR(), 'shop-cpa-filter.db'
        )
        self._STUBS['supplier_filter_db'] = get_stub(
            _MARKETDYNAMIC_STUBS_DIR(), 'supplier-filter.db'
        )
        self._STUBS['supplier_crossdock_filter_db'] = get_stub(
            _MARKETDYNAMIC_STUBS_DIR(), 'supplier-crossdock-filter.db'
        )
        super(OffersProcessorTestEnv, self).__init__(**resourses)

        self.yt_stuff = yt_stuff
        self.yt_client = yt_stuff.get_yt_client()

        self.generation = generation
        self.mapper_memory_mb = mapper_memory_mb
        self.use_bids = use_bids
        self.drop_offers_with_no_sizes = drop_offers_with_no_sizes
        self.do_collapse_offers = do_collapse_offers
        self.collapse_use_express = collapse_use_express
        self.collapse_use_dsbs = collapse_use_dsbs
        self.enable_recipes_logic = enable_recipes_logic
        self.enable_completed_offers_tracing = enable_completed_offers_tracing
        self.reduce_regions_instead_set_earth = reduce_regions_instead_set_earth
        self.drop_direct_hidden_by_market_idx = drop_direct_hidden_by_market_idx
        self.add_regions_from_shops_dat_or_external_table = add_regions_from_shops_dat_or_external_table
        self.add_earth_if_empty_regions = add_earth_if_empty_regions
        self.ignore_zero_region_in_buckets = ignore_zero_region_in_buckets
        self.prefer_reduced_regions = prefer_reduced_regions
        self.use_genlog_scheme = use_genlog_scheme
        self.genlog_integrity_detect_empty_strings = genlog_integrity_detect_empty_strings

        self.input_table_paths = []
        if input_table_paths is not None:
            self.input_table_paths.extend(input_table_paths)
        feed = self.resources.get('feed')
        if feed is not None:
            self.input_table_paths.append(feed._table_path)

        self.yt_test_folder = ypath_join(get_yt_prefix(), 'genlog')
        self.yt_test_collapse_offers_folder = ypath_join(get_yt_prefix(), 'genlog_collapse_offers')
        self.yt_test_filtered_offers_folder = ypath_join(get_yt_prefix(), 'genlog_filtered_offers')
        self.yt_table_process_log = ypath_join(get_yt_prefix(), 'process_log', 'process_log')
        self.yt_table_completed_offers_for_tracer = ypath_join(get_yt_prefix(), 'completed_offers_for_tracer', 'completed_offers_for_tracer')

        self.stderr_log_yt_tabe_path = ypath_join(get_yt_prefix(), stderr_log_yt_tabe_path) if stderr_log_yt_tabe_path else None
        self.dropped_offers_table_path = dropped_offers_table_path

        resources_stubs = {
            'yt_token': YtTokenResource(),
        }
        for name, val in resources_stubs.items():
            if name not in self.resources:
                self.resources[name] = val

    @property
    def description(self):
        return 'offers_processor'

    def execute(self, path=None, use_pokupki_domain=False):
        relative_filter_path = os.path.join(
            'market', 'idx', 'generation',
            'allowed-param-filter',
            'allowed-param-filter',
        )
        absolute_filter_path = yatest.common.binary_path(relative_filter_path)
        cmd = [
            absolute_filter_path,
            os.path.join(self.input_dir, 'gl_mbo.pbuf.sn'),
            os.path.join(self.input_dir, 'allowed_params'),
            os.path.join(self.input_dir, 'gl_params.gz'),
            self.input_dir
        ]
        if not os.path.exists(self.input_dir + "/shops_outlet.gz"):
            cmd.extend([self.input_dir + "/"])
        self.try_execute_under_gdb(cmd)

        if path is None:
            relative_path = os.path.join(
                'market', 'idx', 'offers', 'bin',
                'offers-processor',
                'offers-processor',
            )
            absolute_path = yatest.common.binary_path(relative_path)
            path = absolute_path

        token_path = self.resources['yt_token'].path
        yt_log_path = os.path.join(self.output_dir, 'yt_client.log')

        cmd = [
            path,
            '--yt-proxy', self.yt_stuff.get_server(),
            '--yt-token-path', token_path,
            '--yt-output-dir', self.output_table,
            '--yt-mapper-memory-mb', str(self.mapper_memory_mb),
            '--yt-client-log-path', yt_log_path,
            '--input-dir', self.input_dir,
            '--generation', self.generation,
            '--process-picture-crc'
        ]

        for input_table_path in self.input_table_paths:
            cmd += ['--yt-input-table', input_table_path]

        if self.use_bids:
            cmd += ['--yt-small-bids-table', self.resources['small_bids_table'].table_path]
        else:
            cmd += ['--no-bids']

        if self.drop_offers_with_no_sizes:
            cmd += ['--drop-offers-with-no-sizes']

        if self.stderr_log_yt_tabe_path:
            cmd += ['--std-err-yt-path', self.stderr_log_yt_tabe_path]

        if self.dropped_offers_table_path:
            cmd += ['--yt-dropped-offers-table', self.dropped_offers_table_path]

        if use_pokupki_domain:
            cmd += ['--use-pokupki-domain']

        if self.enable_recipes_logic:
            cmd += ['--enable-recipes-logic']

        if self.enable_completed_offers_tracing:
            cmd += ['--enable-completed-offers-tracing']

        if self.reduce_regions_instead_set_earth:
            cmd += ['--reduce-regions-instead-set-earth']

        if self.do_collapse_offers:
            cmd += ['--yt-output-dir-collapse-offers', self.yt_test_collapse_offers_folder]
            cmd += ['--yt-output-dir-filtered-offers', self.yt_test_filtered_offers_folder]

        if self.collapse_use_express:
            cmd += ['--collapse-use-express']

        if self.collapse_use_dsbs:
            cmd += ['--collapse-use-dsbs']

        if self.drop_direct_hidden_by_market_idx:
            cmd += ['--drop-direct-hidden-by-market-idx']

        if self.add_regions_from_shops_dat_or_external_table:
            cmd += ['--add-regions-from-shops-dat-or-external-table']

        if self.add_earth_if_empty_regions:
            cmd += ['--add-earth-if-empty-regions']

        if self.ignore_zero_region_in_buckets:
            cmd += ['--ignore-zero-region-in-buckets']

        if self.prefer_reduced_regions:
            cmd += ['--prefer-reduced-regions']

        if self.use_genlog_scheme:
            cmd += ['--use-genlog']

        if 'parameters_mapping_pb' in self.resources:
            cmd += ['--use-parameters-mapping']

        if self.genlog_integrity_detect_empty_strings:
            cmd += ['--genlog-integrity-detect-empty-strings']
        self.exec_result = self.try_execute_under_gdb(cmd)

    @property
    def output_table(self):  # TODO: should be folder, not table
        return ypath_join(self.yt_test_folder)

    def _get_all_tables(self, folder):
        tables = list()
        if self.yt_client.exists(folder):
            tables = list(self.yt_client.list(ypath_join(folder)))
        return tables

    @property
    def output_tables_list(self):
        return self._get_all_tables(self.output_table)  # thats folder actually

    def _read_offers_from_each_table(self, folder, tables_list):
        data = []
        for table in tables_list:
            table_path = ypath_join(folder, table)
            if self.yt_client.exists(table_path):
                data.append(list(self.yt_client.read_table(table_path)))
        return data

    @property
    def output_tables_data(self):
        return self._read_offers_from_each_table(self.output_table, self.output_tables_list)

    def _collect_offer_from_row(self, row):
        record = Record_pb.Record()
        for pic in row['pictures']:
            if pic['md5'] is not None:
                pic['md5'] = base64.b64encode(pic['md5'])
        if row['binary_ware_md5'] is not None:
            row['binary_ware_md5'] = base64.b64encode(row['binary_ware_md5'])
        if row['amore_data'] is not None:
            row['amore_data'] = base64.b64encode(row['amore_data'])
        if row['amore_beru_supplier_data'] is not None:
            row['amore_beru_supplier_data'] = base64.b64encode(row['amore_beru_supplier_data'])
        if row['amore_beru_vendor_data'] is not None:
            row['amore_beru_vendor_data'] = base64.b64encode(row['amore_beru_vendor_data'])
        if row['api_data'] is not None:
            api_data = ApiData()
            api_data.ParseFromString(row['api_data'])
            row['api_data'] = MessageToDict(api_data, preserving_proto_field_name=True, use_integers_for_enums=True)
            print('api_data {}'.format(row['api_data']))
        del row['bids']

        if row['params_entry'] is not None:
            params_entry = TCategParamsEntryGenlog()
            params_entry.ParseFromString(row['params_entry'])
            row['params_entry'] = MessageToDict(params_entry)
        ParseDict(row, record, ignore_unknown_fields=True)
        if row['api_data'] is not None:
            print('parsed offer_id,api_data {} {}'.format(record.offer_id, record.api_data))
        return record

    def _all_offers_from_folder(self, tables_data):
        offers_list = []
        for table in tables_data:
            for row in table:
                offers_list.append(self._collect_offer_from_row(row))
        return offers_list

    @property
    def genlog(self):
        tables = []
        for table in self.output_tables_data:
            if table != 'buybox':
                tables.append(table)
        return self._all_offers_from_folder(tables)

    @property
    def genlog_with_row_index(self):
        offers_list = []
        for table in self.output_tables_data:
            row_index = 0
            for row in table:
                offer = (row_index, self._collect_offer_from_row(row))
                offers_list.append(offer)
                row_index += 1
        return offers_list

    @property
    def genlog_dicts(self):
        offers_list = []
        for table in self.output_tables_data:
            if table != 'buybox':
                for row in table:
                    offers_list.append(row)
        return offers_list

    @property
    def genlog_buybox(self):
        buybox_data = []
        for table in self.output_tables_data:
            if table == 'buybox':
                for row in table:
                    buybox_data.append(row)
        return buybox_data

    @property
    def genlog_all(self):
        return self.genlog + self.genlog_buybox

    @property
    def has_std_err_table(self):
        return self.yt_client.exists(self.stderr_log_yt_tabe_path) if self.stderr_log_yt_tabe_path else False

    @property
    def std_err_logs(self):
        return list(self.yt_client.read_table(self.stderr_log_yt_tabe_path))

    @property
    def genlog_stat(self):
        offers_list = []
        for table in self.output_tables_data:
            if table != 'buybox':
                for row in table:
                    if 'stat' in row:
                        record = Record_pb.StatRecord()
                        record.ParseFromString(row['stat'])
                        offers_list.append(record)
        return offers_list

    @property
    def feedlog(self):
        feedlog = []
        for row in self.yt_client.read_table(ypath_join(get_yt_prefix(), 'applied_feedlog/feedlog')):
            feed = Feed()
            feed.ParseFromString(row['feed'])
            feedlog.append(feed)
        return feedlog

    @property
    def process_log_table(self):
        process_log = []
        for row in self.yt_client.read_table(self.yt_table_process_log):
            process_log.append(row)
        return process_log


class OffersProcessorModelDumperTestEnv(OffersProcessorTestEnv):
    def execute(self, path=None):
        super(OffersProcessorModelDumperTestEnv, self).execute(path)

        model_ids_table = ypath_join(get_yt_prefix(), 'models', 'model_ids')
        model_dumper_path = os.path.join(
            'market', 'idx', 'models', 'bin',
            'model-dumper',
            'model-dumper',
        )
        model_dumper_path = yatest.common.binary_path(model_dumper_path)
        token_path = self.resources['yt_token'].path
        cmd = [
            model_dumper_path,
            '--yt-proxy', self.yt_stuff.get_server(),
            '--yt-token', token_path,
            '--yt-input', model_ids_table,
            '--output', '/dev/stdout',
        ]
        self.model_dumper_out, self.model_dumper_err = Popen(cmd, stdout=PIPE).communicate()


class OffersProcessorGenlogIntegrityTestEnv(OffersProcessorTestEnv):
    def __init__(self, yt_stuff, integrity_log_path='integrity.log', integrity_stats_path='integrity.tsv', **kwargs):
        super(OffersProcessorGenlogIntegrityTestEnv, self).__init__(yt_stuff, **kwargs)
        self.integrity_log_path = integrity_log_path
        self.integrity_stats_path = integrity_stats_path

    def execute(self, path=None):
        super(OffersProcessorGenlogIntegrityTestEnv, self).execute(path)

        relative_path = os.path.join(
            'market', 'idx', 'offers', 'bin',
            'genlog-integrity',
            'genlog-integrity',
        )
        absolute_path = yatest.common.binary_path(relative_path)
        path = absolute_path

        token_path = self.resources['yt_token'].path
        yt_log_path = os.path.join(self.output_dir, 'yt_client.log')

        cmd = [
            path,
            '--yt-proxy', self.yt_stuff.get_server(),
            '--yt-token-path', token_path,
            '--yt-output-dir', self.output_table,
            '--yt-client-log-path', yt_log_path,
            '--generation', self.generation,
            '--integrity-log-path', self.integrity_log_path,
            '--integrity-stats-path', self.integrity_stats_path,
        ]

        self.try_execute_under_gdb(cmd)

    @property
    def _integrity_stats_path(self):
        return ypath_join(get_yt_prefix(), 'genlog_integrity_stats_reduced')

    @property
    def integrity_stats(self):
        stats = []
        for row in self.yt_client.read_table(self._integrity_stats_path):
            stats.append((
                row['field_name'],
                row['null_count'],
                row['non_null_count'],
                row['null_ratio'],
                row['non_null_ratio']
            ))
        return stats

    @property
    def has_integrity_stats(self):
        return self.yt_client.exists(self._integrity_stats_path)


class OffersProcessorBusinessOfferTestEnv(OffersProcessorTestEnv):
    @property
    def output_collapse_offers_folder(self):
        return ypath_join(self.yt_test_collapse_offers_folder)

    @property
    def output_filtered_offers_folder(self):
        return ypath_join(self.yt_test_filtered_offers_folder)

    @property
    def output_collapse_offers_tables_list(self):
        return self._get_all_tables(self.output_collapse_offers_folder)

    @property
    def output_filtered_offers_tables_list(self):
        return self._get_all_tables(self.output_filtered_offers_folder)

    @property
    def output_collapse_offers_tables_data(self):
        return self._read_offers_from_each_table(self.output_collapse_offers_folder,
                                                 self.output_collapse_offers_tables_list)

    @property
    def output_filtered_offers_tables_data(self):
        return self._read_offers_from_each_table(self.output_filtered_offers_folder,
                                                 self.output_filtered_offers_tables_list)

    @property
    def collapse_offers(self):
        return self._all_offers_from_folder(self.output_collapse_offers_tables_data)

    @property
    def filtered_offers(self):
        return self._all_offers_from_folder(self.output_filtered_offers_tables_data)
