# coding: utf-8

import os
import datetime


import yatest.common
from yt.wrapper import ypath_join
from yatest.common import network

from market.idx.feeds.feedparser.yatf.resources.checker_cfg import CheckerConf
from market.idx.feeds.feedparser.yatf.resources.feed_cfg import FeedConf, YtFeedConf
from market.idx.feeds.feedparser.yatf.resources.pb_result import (
    Offers, Categories, FeedMetaData, Recs, OffersPromos, OffersDetails, UcData,
    SuggestOutput, CommitOutput, QOfferOutput
)
from market.idx.feeds.feedparser.yatf.resources.ucdata_pbs import UcHTTPData, UcFileData

from market.idx.yatf.common import get_binary_path, get_source_path

from market.idx.yatf.matchers.env_matchers import HasExitCode
from market.idx.yatf.resources.category_restrictions_pb import CategoryRestrictions
from market.idx.feeds.feedparser.yatf.resources.pb_result import CheckerOutput
from market.idx.yatf.resources.pbsn import ExplanationLogOutput
from market.idx.yatf.resources.resource import FileResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.yt_table_resource import YtDynTableResource
from market.idx.yatf.resources.yt_token_resource import YtTokenResource
from market.idx.yatf.test_envs.base_env import BaseEnv

from market.proto.delivery.delivery_yt.indexer_part_pb2 import CommonDeliveryOptionsBucket, DeliveryOptionsGroupsForBucket
from market.proto.feedparser.OffersData_pb2 import Category, OfferRecsWrapper
from market.proto.feedparser.deprecated.OffersData_pb2 import Offer
from market.proto.feedparser.Promo_pb2 import OfferPromo, PromoDetails
from market.proto.ir.UltraController_pb2 import EnrichedOffer
from market.proto.content.mbo.Restrictions_pb2 import RestrictionsData
from market.proto.SessionMetadata_pb2 import Feedparser


def _STUBS_DIR():
    return os.path.join(
        get_source_path(),
        'market', 'idx', 'feeds', 'feedparser', 'yatf', 'resources', 'stubs'
    )


def _DATA_DIR():
    return os.path.join(
        get_source_path(),
        'market', 'idx', 'feeds', 'feedparser', 'test', 'data'
    )


def _GEOBASE_DIR():
    return os.path.join(
        get_source_path(),
        'market', 'idx', 'yatf', 'resources', 'stubs', 'getter', 'geobase'
    )


class FeedParserTestEnv(BaseEnv):
    _MATCHERS = [
        HasExitCode(0)
    ]

    def init_stubs(self):
        self._STUBS = BaseEnv.merge_stubs(
            {
                name: FileResource(os.path.join(_DATA_DIR(), filename))
                for name, filename in list({
                    'additional_countries_utf8_c2n': 'additional_countries_utf8.c2n',
                    'caps_exceptions_txt': 'caps-exceptions.txt',
                    'caps_mustfix_txt': 'caps-must-fix.txt',
                    'category_restrictions': 'category-restrictions.pb',
                    'countries_utf8_c2n': 'countries_utf8.c2n',
                    'currency_rates_xml': 'currency_rates.xml',
                    'feed_parsing_task_filepath': 'feed-parsing-task.pb',
                    'shops_outlet_mmap': 'shops_outlet.mmap',
                    'tovar_tree_pb': 'tovar-tree.pb',
                }.items())
            },
            {
                name: FileResource(os.path.join(_STUBS_DIR(), filename))
                for name, filename in list({
                    'cutprice_shops_path': 'cutprice_shops.txt',
                    'fetched_xml': 'fetched.xml',
                }.items())
            },
            {
                name: FileResource(os.path.join(_GEOBASE_DIR(), filename))
                for name, filename in list({
                    'geo_c2p': 'geo.c2p',
                    'geobase_xml': 'geobase.xml',
                }.items())
            },
            {
                'dict_dict': FileResource(os.path.join(yatest.common.work_path(), 'dict.dict'))
            }
        )

    def __init__(self, yt_stuff=None, env=None, yt_test_folder=None, **resources):
        self.init_stubs()
        super(FeedParserTestEnv, self).__init__(**resources)
        self._pm = None
        self._yt_stuff = yt_stuff
        self.yt_client = yt_stuff.get_yt_client() if yt_stuff else None
        self._env = env
        self.yt_test_folder = yt_test_folder or get_yt_prefix()
        resources_stubs = {
            'category_restrictions': CategoryRestrictions(RestrictionsData()),
            'feed_cfg': FeedConf() if yt_stuff is None else YtFeedConf(yt_stuff),
            'ucdata_pbs': UcHTTPData(None),
            'yt_token': YtTokenResource(),
        }
        for name, val in list(resources_stubs.items()):
            if name not in self.resources:
                self.resources[name] = val

    @property
    def resource_dependencies(self):
        return {
            'feed_cfg': [
                'additional_countries_utf8_c2n'
                'caps_exceptions_txt',
                'caps_mustfix_txt',
                'category_restrictions',
                'countries_utf8_c2n',
                'credits_info',
                'currency_rates_xml',
                'cutprice_shops_path',
                'delivery_calc',
                'dict_dict',
                'feed_parsing_task_filepath',
                'fetched_xml',
                'geo_c2p',
                'geobase_xml',
                'mboc',
                'vmid_server',
                'parameter_names_pbuf_sn',
                'shops_outlet_mmap',
                'tovar_tree_pb',
                'ucdata_pbs',
                'yt_token',
            ]
        }

    def __enter__(self):
        self._pm = network.PortManager()
        super(FeedParserTestEnv, self).__enter__()
        self.feed_cfg.init(self)
        return self

    def __exit__(self, *args):
        if self._pm:
            self._pm.release()

    @property
    def yt_root_path(self):
        return self.yt_test_folder

    @property
    def yt_or3_path(self):
        return ypath_join(
            self.yt_test_folder,
            'or3',  # для совместимости интеграционных тестов. в коде захоркожены пути
        )

    @property
    def yt_stuff(self):
        return self._yt_stuff

    @property
    def port_manager(self):
        return self._pm

    @property
    def feed_cfg(self):
        return self.resources['feed_cfg']

    @property
    def description(self):
        return 'feed_parser'

    @property
    def executable_path(self):
        return os.path.join(
            'market', 'idx', 'feeds', 'feedparser',
            'bin', 'market-feedparser', 'market-feedparser'
        )

    def execute(self, path=None):
        self.do_execute(path=path)
        self.do_update_outputs()

    def do_execute(self, path=None):
        if path is None:
            relative_path = self.executable_path
            absolute_path = get_binary_path(relative_path)
            path = absolute_path

        feed_parser_config = os.path.join(
            self.input_dir,
            self.feed_cfg.filename
        )

        cmd = [path, '--config', feed_parser_config]
        if 'description_stop_words' in self.resources:
            cmd.extend([
                '--general.description_stop_words_path',
                self.resources['description_stop_words'].path
            ])
        if 'allowed_price_from_categories' in self.resources:
            cmd.extend([
                '--general.price_from_categories_path',
                self.resources['allowed_price_from_categories'].path
            ]),
        if 'cutprice_shops_path' in self.resources:
            cmd.extend([
                '--general.cutprice_shops_path',
                self.resources['cutprice_shops_path'].path
            ])
        if 'categories_dimensions' in self.resources:
            cmd.extend([
                '--general.categories_dimensions',
                self.resources['categories_dimensions'].path
            ])

        if 'certificates_path' in self.resources:
            cmd.extend([
                '--general.certificates_path',
                self.resources['certificates_path'].path
            ])

        if 'flags' in self.resources:
            cmd.extend(self.resources['flags'].flags)

        if 'ucdata_pbs' in self.resources:
            if isinstance(self.resources['ucdata_pbs'], UcFileData):
                cmd.extend(['--ultrac.data_filepath', self.resources['ucdata_pbs'].path])

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            env=self._env,
            cwd=self.output_dir,
            check_exit_code=False
        )

    def do_update_outputs(self):
        self.outputs.update({
            'categories': Categories(os.path.join(self.output_dir, 'categories.pbs')),
            'checker': CheckerOutput(os.path.join(self.output_dir, 'check-result.pbuf.sn')),
            'commit': CommitOutput(os.path.join(self.output_dir, 'commit-result.pbuf.sn')),
            'feed_metadata': FeedMetaData(os.path.join(self.output_dir, 'feed-metadata.pb')),
            'offer_promos': OffersPromos(os.path.join(self.output_dir, 'offer-promos.pbs')),
            'offers': Offers(os.path.join(self.output_dir, 'offers.pbs')),
            'promo_details': OffersDetails(os.path.join(self.output_dir, 'promo-details.pbs')),
            'qoffers': QOfferOutput(os.path.join(self.output_dir, 'qoffer.pbuf.sn')),
            'recs': Recs(os.path.join(self.output_dir, 'recs.pbs')),
            'suggest': SuggestOutput(os.path.join(self.output_dir, 'suggest-result.pbuf.sn')),
            'uc_data': UcData(os.path.join(self.output_dir, 'enriched-offers.pbs')),
        })

        if self.resources['feed_cfg'].get('general', 'enable_explanation_proto_log') == 'true':
            self.outputs.update({
                'explanation_log': ExplanationLogOutput(os.path.join(
                    self.output_dir,
                    self.resources['feed_cfg'].get('general', 'feed_errors_filepath', 'fatal_errors_file.pbuf.sn')
                )),
            })

    def _get_yt_last_table(self, table_path):
        yt = self.yt_stuff.get_yt_client()
        if not yt.exists(table_path):
            return []
        all_table_names = yt.list(table_path)

        time_template = '%Y%m%d_%H%M'
        last_table_name_raw = max(
            [datetime.datetime.strptime(table_name, time_template)
             for table_name in all_table_names]
        )
        last_table_name = str(last_table_name_raw.strftime(time_template))

        last_table_path = ypath_join(table_path, last_table_name)

        if not yt.exists(last_table_path):
            return []
        return yt.read_table(last_table_path)

    def _get_yt_table_results(self, table_path, result_key, fabric):
        return [
            fabric.FromString(raw_result[result_key])
            for raw_result in self._get_yt_last_table(table_path)
            if raw_result[result_key]
        ]

    def _get_pbs_results(self, result_key):
        self.outputs[result_key].load()
        return self.outputs[result_key].proto_results

    def get_all_sessions_table(self, path):
        yt = self.yt_stuff.get_yt_client()
        if not yt.exists(path):
            return []
        all_table_names = yt.list(path)
        if not all_table_names:
            return []
        result = []
        for table_name in all_table_names:
            result.append(ypath_join(path, table_name))
        return result

    @property
    def offers(self):
        if self.resources['feed_cfg'].get('general', 'use_dump_mode') == 'true':
            return self._get_pbs_results('offers')
        table_path = self.resources['feed_cfg'].options['yt']['offers_home_dir']
        return self._get_yt_table_results(table_path=table_path, result_key='offer', fabric=Offer)

    @property
    def blue_offers(self):
        if self.resources['feed_cfg'].get('general', 'use_dump_mode') == 'true':
            return self._get_pbs_results('offers')
        table_path = self.resources['feed_cfg'].options['yt']['blue_offers_home_dir']
        return self._get_yt_table_results(table_path=table_path, result_key='offer', fabric=Offer)

    @property
    def red_offers(self):
        if self.resources['feed_cfg'].get('general', 'use_dump_mode') == 'true':
            return self._get_pbs_results('offers')
        table_path = self.resources['feed_cfg'].options['yt']['red_offers_home_dir']
        return self._get_yt_table_results(table_path=table_path, result_key='offer', fabric=Offer)

    @property
    def categories(self):
        if self.resources['feed_cfg'].get('general', 'use_dump_mode') == 'true':
            return self._get_pbs_results('categories')
        table_path = self.resources['feed_cfg'].options['yt']['categories_home_dir']
        return self._get_yt_table_results(table_path=table_path, result_key='cat', fabric=Category)

    @property
    def recommendations(self):
        if self.resources['feed_cfg'].get('general', 'use_dump_mode') == 'true':
            return self._get_pbs_results('rect')
        table_path = self.resources['feed_cfg'].options['yt']['offers_home_dir']
        return self._get_yt_table_results(table_path=table_path, result_key='recs', fabric=OfferRecsWrapper)

    @property
    def offer_promos(self):
        if self.resources['feed_cfg'].get('general', 'use_dump_mode') == 'true':
            return self._get_pbs_results('offer_promos')
        table_path = self.resources['feed_cfg'].options['yt']['offers_home_dir']
        return self._get_yt_table_results(table_path=table_path, result_key='promo', fabric=OfferPromo)

    @property
    def promo_details(self):
        if self.resources['feed_cfg'].get('general', 'use_dump_mode') == 'true':
            return self._get_pbs_results('promo_details')
        table_path = self.resources['feed_cfg'].options['yt']['promos_home_dir']
        return self._get_yt_table_results(table_path=table_path, result_key='promo', fabric=PromoDetails)

    @property
    def explanation_log(self):
        if self.resources['feed_cfg'].get('general', 'enable_explanation_proto_log') == 'true':
            return self._get_pbs_results('explanation_log')
        return None

    @property
    def gifts(self):
        table_path = self.resources['feed_cfg'].options['yt']['gifts_home_dir']
        return self._get_yt_table_results(table_path=table_path, result_key='offer', fabric=Offer)

    @property
    def uc_data(self):
        if self.resources['feed_cfg'].get('general', 'use_dump_mode') == 'true':
            return self._get_pbs_results('uc_data')
        table_path = self.resources['feed_cfg'].options['yt']['offers_home_dir']
        return self._get_yt_table_results(table_path=table_path, result_key='uc', fabric=EnrichedOffer)

    @property
    def red_uc_data(self):
        table_path = self.resources['feed_cfg'].options['yt']['red_offers_home_dir']
        return self._get_yt_table_results(table_path=table_path, result_key='uc', fabric=EnrichedOffer)

    @property
    def feed_metadata(self):
        path = self.resources['feed_cfg'].options['general']['fp_metadata']
        if os.path.exists(path):
            with open(path, 'rb') as f:
                return Feedparser.FromString(f.read())
        else:
            return None

    @property
    def feed_quick_metadata(self):
        path = self.resources['feed_cfg'].options['general']['fp_quick_metadata']
        if path and os.path.exists(path):
            with open(path, 'rb') as f:
                return Feedparser.FromString(f.read())
        else:
            return None

    @property
    def process_log_table(self):
        table_path = self.resources['feed_cfg'].options['general']['process_log_dyn_path']
        table = YtDynTableResource(yt_stuff=self.yt_stuff, path=table_path, load=True)
        return table.data

    @property
    def delivery_calc_table(self):
        table_path = self.resources['feed_cfg'].options['yt']['deliverycalc_table']
        table = YtDynTableResource(yt_stuff=self.yt_stuff, path=table_path, load=True)

        result = {
            raw_result['key']: {
                'data:b':
                    CommonDeliveryOptionsBucket.FromString(raw_result['data:b']) if raw_result['data:b'] else None,
                'data:ag':
                    DeliveryOptionsGroupsForBucket.FromString(raw_result['data:ag']) if raw_result['data:ag'] else None
            }
            for raw_result in table.data
        }

        return result

    @property
    def delivery_options_table(self):
        table_path = self.resources['feed_cfg'].options['yt']['delivery_options_table']
        yt = self.yt_stuff.get_yt_client()
        if not yt.exists(table_path):
            return None

        table = YtDynTableResource(yt_stuff=self.yt_stuff, path=table_path, load=True)
        return table.data

    @property
    def qidx_snapshot_table(self):
        table_path = self.resources['feed_cfg'].options['yt']['qidx_snapshot_table']
        table = YtDynTableResource(yt_stuff=self.yt_stuff, path=table_path, load=True)
        return table

    @property
    def suggests(self):
        if not self.outputs['suggest'].proto_results:
            self.outputs['suggest'].load()
        return self.outputs['suggest'].proto_results

    @property
    def commits(self):
        if not self.outputs['commit'].proto_results:
            self.outputs['commit'].load()
        return self.outputs['commit'].proto_results

    @property
    def checker(self):
        if not self.outputs['checker'].proto_results:
            self.outputs['checker'].load()
        return self.outputs['checker'].proto_results

    @property
    def qoffers(self):
        if not self.outputs['qoffers'].proto_results:
            self.outputs['qoffers'].load()
        return self.outputs['qoffers'].proto_results

    @property
    def delivery_calculator(self):
        return self.resources['delivery_calc']

    @property
    def offer_trace_log(self):
        def parse_tskv(tskv_line):
            fields = tskv_line.split('\t')
            if len(fields) < 1 or fields[0] != 'tskv':
                return None
            return dict([f.split('=', 1) for f in fields[1:] if '=' in f])

        offer_trace_options = self.resources['feed_cfg'].options['trace_offers']
        if (not offer_trace_options.get('enable_tracing_for_blue_1p_offers', False) and
                not offer_trace_options.get('enable_tracing_for_blue_offers', False)):
            return None

        if 'offer_trace_directory' in offer_trace_options:
            log_path = os.path.join(offer_trace_options['offer_trace_directory'], 'offers-trace.log')
            if os.path.exists(log_path):
                with open(log_path, 'r') as log_file:
                    return [parse_tskv(line) for line in log_file]

        return None

    @property
    def uc_sample_table(self):
        table_path = self.resources['feed_cfg'].options['yt']['uc_samples_table']
        yt = self.yt_stuff.get_yt_client()
        if not yt.exists(table_path):
            return None

        return YtDynTableResource(yt_stuff=self.yt_stuff, path=table_path, load=True)

    @property
    def yt_offers_table(self):
        if self.resources['feed_cfg'].get('general', 'use_dump_mode') == 'true':
            raise RuntimeError('no yt tables in dump mode')
        return self._get_yt_last_table(self.resources['feed_cfg'].options['yt']['offers_home_dir'])


class CheckFeedTestEnv(BaseEnv):
    _PBSN_FILENAME = 'check-result.pbuf.sn'

    def init_stubs(self):
        self._STUBS = BaseEnv.merge_stubs(
            {
                name: FileResource(os.path.join(_DATA_DIR(), filename))
                for name, filename in list({
                    'additional_countries_utf8_c2n': 'additional_countries_utf8.c2n',
                    'categories_availability_tsv': 'categoriesAvailability.tsv',
                    'category_restrictions': 'category-restrictions.pb',
                    'countries_utf8_c2n': 'countries_utf8.c2n',
                    'currency_rates_xml': 'currency_rates.xml',
                    'tovar_tree_pb': 'tovar-tree.pb',
                }.items())
            },
            {
                name: FileResource(os.path.join(_GEOBASE_DIR(), filename))
                for name, filename in list({
                    'geo_c2p': 'geo.c2p',
                    'geobase_xml': 'geobase.xml',
                }.items())
            },
            {
                'dict_dict': FileResource(os.path.join(yatest.common.work_path(), 'dict.dict'))
            }
        )

    def __init__(self, bin_path=None, **resources):
        self.init_stubs()
        super(CheckFeedTestEnv, self).__init__(**resources)
        if bin_path is None:
            relative_path = os.path.join('market', 'idx', 'feeds', 'feedparser', 'scripts', 'market_checkfeed', 'market-checkfeed')
            bin_path = get_binary_path(relative_path)
        self.bin_path = bin_path

        relative_path = os.path.join('market', 'idx', 'feeds', 'feedparser', 'bin', 'market-feedparser', 'market-feedparser')
        path = get_binary_path(relative_path)

        resources_stubs = {
            'checker_cfg': CheckerConf(
                feedchecker={'feedparser': path}
            ),
        }
        for name, val in list(resources_stubs.items()):
            if name not in self.resources:
                self.resources[name] = val

    @property
    def description(self):
        return 'market_checkfeed'

    def execute(self, url, **kwargs):
        checker_config = os.path.join(
            self.input_dir,
            self.checker_cfg.filename
        )

        cmd = [
            self.bin_path,
            '--config', checker_config,
            '--copy-result-to', os.path.join(self.output_dir, self._PBSN_FILENAME),
            url
        ]
        if 'flags' in self.resources:
            cmd.extend(self.resources['flags'].flags)

        self.exec_result = self.try_execute_under_gdb(
            cmd,
            cwd=self.output_dir,
            check_exit_code=False
        )
        self.do_update_outputs()

    def do_update_outputs(self):
        self.outputs.update({
            'checker': CheckerOutput(os.path.join(self.output_dir, self._PBSN_FILENAME)),
        })

    @property
    def checker_cfg(self):
        return self.resources['checker_cfg']

    @property
    def checker(self):
        if not self.outputs['checker'].proto_results:
            self.outputs['checker'].load()
        return self.outputs['checker'].proto_results
