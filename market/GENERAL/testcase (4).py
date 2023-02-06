# -*- coding: utf-8 -*-
import argparse
import inspect
import json
import logging
import os
import signal
import socket
import six
import sys
import unittest
import shutil
import re
import ast

from . import dynamic
from . import report
from .access_agent import MarketAccessAgent
from .access_server import MarketAccessServer
from .panther import ExternalPantherSuite
from .types.autogen import Auto, get_seed, get_port, set_seed, Autoscope
from .types.delivery import default_start_date
from .adv_machine import AdvMachine
from .bigb import BigB
from .bk import BK
from .carter import Carter
from .clickdaemon import ClickDaemon, ClickUrlObserver
from .click_n_collect import ClickNCollect
from .crypta import Crypta
from .datasync import DataSync
from .delivery_calc import DeliveryCalc
from .emergency_flags import EmergencyFlags, ExperimentFlags
from .formalizer import Formalizer
from .frozen import Frozen
from .gencfg import GenCfgService
from .ichwill import IchWill
from .loyalty import Loyalty
from .quoter import Quoter
from .recommender import Recommender
from .media_adv import MediaAdvertising
from .report import NoRequestObservers
from .index import Index, IndexContext
from .types.hypercategory import CategoryStreamsStorage
from .types import MarketAccessSingleFbResource
from .logs import (
    CommonLogFrontend,
    AccessLogFrontend,
    BaseAccessLogFrontend,
    ShadeRequestServiceLogFrontend,
    TskvShowLogFrontend,
    TskvPromoLogFrontend,
    ClickLogFrontend,
    TestDataLogFrontend,
    FeatureLogFrontend,
    AmmoLogFrontend,
    ErrorLogFrontend,
    ExternalHttpServiceLogFrontend,
    ExternalServicesTraceLogFrontend,
    ExternalServicesLogFrontend,
    RtyStatsLogFrontend,
    CandidateLogFrontend,
    LogBrokerProtoTopicBackend,
)
from .matrixnet import Matrixnet
from .calc_service import CalcService
from .model_service import ModelService
from .bert_service import BertService
from .tvmtool import TvmTool
from .tvmtool_service import TvmToolService
from .parametrizator import Parametrizator
from .paths import MetaReportPaths, BaseReportPaths, FreshBaseReportPaths, BUILDROOT, SRCROOT
from .reqwizard import ReqWizard
from .response import NullResponse
from .speller import Speller
from .suggester import Suggester
from .snippet import Snippets
from .chats import ChatsService
from .request_categories_classificator import RequestCategoriesClassificator
from .dj import Dj, FastDj, TextQueryDj
from .turbo import Turbo
from .saashub import SaasHub
from .saas_ugc import SaasUGC
from .blackbox import Blackbox
from .combinator import Combinator, CombinatorExpress, RouteStorage
from .memcached import Memcached
from .content_storage import ContentStorage
from .fast_mappings_service import FastMappings
from .unified_agent import UnifiedAgent
from . import indexerproxy
from . import rty
from . import logbroker
from .matcher import ustr
from .execution_stats import ExecutionStats
from market.pylibrary.lite.color import Color, draw, colorstr
from market.pylibrary.lite.dump import Dumper
from market.shade.lite.shade import Shade
from market.pylibrary.lite.test_loader import TestLoader as LiteTestLoader
from market.pylibrary.lite.process import run_external_tool
import market.pylibrary.lite.test_platform as test_platform

from .report import get_port_manager
import yatest.common

__author__ = 'yuraaka'
_PORT = None
_WAIT = False
_BREAKPOINT = False

logger = logging.getLogger(__name__)


def safe_lambda_exec(exceptions, func):
    try:
        func()
    except Exception as e:
        exceptions.append(e)


def _make_symlink_or_copy_dir(src_dir, dst_dir, log_path, copy_dirs=None):
    if not os.path.exists(dst_dir):
        os.makedirs(dst_dir)
    for dir_name in os.listdir(src_dir):
        src = os.path.join(src_dir, dir_name)
        dst = os.path.join(dst_dir, dir_name)
        if copy_dirs and dir_name in copy_dirs:
            run_external_tool(['cp', '-r', src, dst], log_path)
        else:
            run_external_tool(['ln', '-s', src, dst], log_path)


def _delete_unused_index_files_on_base_report(report_data_path, log_path):
    unused_files = {
        "best_grades_for_koldunshik.mmap",
        "bid-correction-data.mmap",
        "contex_msku.mmap",
        "demand-prediction-one-p-input.mmap",
        "demand-prediction-sales.mmap",
        "gifts.mmap",
        "guru_light_region_stats.mmap",
        "model-transitions.mmap",
        "model2msku.mmap",
        "model_geo_stats.mmap",
        "msku-transitions.mmap",
        "recipes.mmap",
        "shop_delivery_options.mmap",
    }

    for file_name in unused_files:
        file_path = os.path.join(report_data_path, file_name)
        if os.path.exists(file_path):
            run_external_tool(['rm', '-rf', file_path], log_path)


class YaRuntime(object):
    __is_active = None

    @classmethod
    def __check_runtime(cls):
        if cls.__is_active is not None:
            return
        try:
            yatest.common.get_param('')
            cls.__is_active = True
        except NotImplementedError:
            cls.__is_active = False

    @classmethod
    def is_active(cls):
        cls.__check_runtime()
        return cls.__is_active

    @classmethod
    def get_param(cls, name, default=None):
        cls.__check_runtime()
        return yatest.common.get_param(name, default) if cls.__is_active else None

    @classmethod
    def get_param_bool(cls, name, default=None):
        value = cls.get_param(name, default)
        if value is not None:
            if isinstance(value, str if sys.version_info.major >= 3 else basestring):  # checks for P2 and P3+
                value = bool(int(value)) if value.isdigit() else value.lower() in ('true', '1', 'da', 'yes')
            else:
                value = bool(value)
        return value


class Multiplexer(object):
    def __init__(self, *args):
        self.__objects = args

    def __getattr__(self, item):
        def __do_call(*args, **kwargs):
            results = [getattr(o, item)(*args, **kwargs) for o in self.__objects]
            return Multiplexer(*results)

        return __do_call


class MarketAccessSettings(Frozen):
    def __init__(self):
        self.enabled = False
        self.download_market_dynamic = False
        self.use_market_dynamic_from_access_for_search = False
        self.use_access_delivery_fb_files = False
        self.download_order_services_by_access = False
        self.download_svn_data = False
        self.use_svn_data = False
        self.download_vendors_datasource_ids_for_search = False
        self.use_access_vendors_datasource_ids_for_search = False
        self.download_prism_stats = False
        self.use_prism_stats = False
        self.download_catstreams = False
        self.download_regional_prices_by_access = False
        self.download_cms_incuts_from_access = False
        self.use_unified_hide_rules_from_access = False
        self.download_age_categ_ecom_stats = False
        self.use_age_categ_ecom_stats = False
        self.download_age_categ_vendor_ecom_stats = False
        self.use_age_categ_vendor_ecom_stats = False
        self.download_age_categ_stream_ecom_stats = False
        self.use_age_categ_stream_ecom_stats = False
        self.download_age_vendor_ecom_stats = False
        self.use_age_vendor_ecom_stats = False
        self.download_categ_ecom_stats = False
        self.use_categ_ecom_stats = False
        self.download_categ_vendor_ecom_stats = False
        self.use_categ_vendor_ecom_stats = False
        self.download_categ_stream_ecom_stats = False
        self.use_categ_stream_ecom_stats = False
        self.download_gender_ecom_stats = False
        self.use_gender_ecom_stats = False
        self.download_gender_categ_ecom_stats = False
        self.use_gender_categ_ecom_stats = False
        self.download_gender_categ_vendor_ecom_stats = False
        self.use_gender_categ_vendor_ecom_stats = False
        self.download_gender_categ_stream_ecom_stats = False
        self.use_gender_categ_stream_ecom_stats = False
        self.download_gender_vendor_ecom_stats = False
        self.use_gender_vendor_ecom_stats = False
        self.download_model_ecom_stats = False
        self.use_model_ecom_stats = False
        self.download_pop_categ_ecom_stats = False
        self.use_pop_categ_ecom_stats = False
        self.download_pop_categ_vendor_ecom_stats = False
        self.use_pop_categ_vendor_ecom_stats = False
        self.download_pop_categ_stream_ecom_stats = False
        self.use_pop_categ_stream_ecom_stats = False
        self.download_pop_vendor_ecom_stats = False
        self.use_pop_vendor_ecom_stats = False
        self.download_pr_categ_ecom_stats = False
        self.use_pr_categ_ecom_stats = False
        self.download_pr_categ_vendor_ecom_stats = False
        self.use_pr_categ_vendor_ecom_stats = False
        self.download_pr_categ_stream_ecom_stats = False
        self.use_pr_categ_stream_ecom_stats = False
        self.download_pr_vendor_ecom_stats = False
        self.use_pr_vendor_ecom_stats = False
        self.download_vendor_ecom_stats = False
        self.use_vendor_ecom_stats = False
        self.download_age_ecom_stats = False
        self.use_age_ecom_stats = False
        self.download_pr_ecom_stats = False
        self.use_pr_ecom_stats = False
        self.download_pop_ecom_stats = False
        self.use_pop_ecom_stats = False
        self.download_model_fashionability = False
        self.use_model_fashionability = False


class Settings(Frozen):
    def __init__(self):
        # index settings
        self.is_multi_signatures = False
        self.is_archive_new_format = True
        self.enable_experimental_panther = False
        self.enable_experimental_panther2 = False
        self.enable_panther = True
        self.enable_panther_external = False
        self.enable_category_panther = False
        self.enable_knn = False
        self.enable_cluster_knn = False
        self.enable_promo = True
        self.generate_old_erf = False
        self.dont_put_sku_to_blue_shard = False

        # report settings
        self.enable_exec_stats_log = False
        self.enable_exec_stats_for_global_init = False
        self.enable_event_log = False
        self.enable_load_log = False
        self.cpc_sandbox_is_real = False
        self.downgrade_cpa_offers_by_shop_settings = True
        self.disable_long_requests_to_req_wizard = False
        self.enable_dssm = True
        self.formulas_path = ''
        self.enable_testing_features = True
        self.omm_requests_enabled_on_prime = False
        self.report_subrole = 'market'
        self.cloud_service = None
        self.logbroker_enabled = False
        self.direct_sending_consolidate_log_to_logbroker = False
        self.logbroker_topics = [
            logbroker.ERRORBOOSTER_TOPIC,
            logbroker.FEATURE_TOPIC,
            logbroker.CONSOLIDATE_TOPIC,
            logbroker.HEALTH_TOPIC,
            logbroker.COMBINATOR_DELIVERY_TOPIC,
            logbroker.COMBINATOR_COURIER_TOPIC,
            logbroker.COMBINATOR_PICKUP_TOPIC,
            logbroker.CANDIDATE_TOPIC,
        ]
        self.logbroker_port = None
        self.rty_qpipe = False
        self.wait_rty_started = True
        self.use_delivery_statistics = False
        self.use_factorann = False
        self.use_fresh_base_report = False
        self.use_apphost = False
        self.init_threads = 16
        self.lms_autogenerate = True
        self.nordstream_autogenerate = True
        self.nordstream_types = [0, 1, 2]  # all types courier, pickup, post
        self.delivery_calendar_start_date = default_start_date()
        self.loyalty_enabled = False
        self.disable_random = None
        self.microseconds_for_disabled_random = None
        self.detect_leaks = True
        self.use_external_snippets = os.getenv('USE_EXT_SNIPPETS', '1') == '1'
        self.use_no_snippet_arc = True
        self.set_default_reqid = True
        self.use_saashub_delivery = False
        self.use_multiconnect_http_client = False
        self.hide_partner_documents = False
        self.emergency_flags_path = None
        self.experiment_flags_path = None
        self.memcache_enabled = False
        self.force_archive_mode = None
        self.disable_snippet_request = False
        self.snippet_saas_throw_on_saas_error = False
        self.gencfg_enabled = True
        self.enable_remote_storage = False
        self.enable_access_delivery_for_search = False
        self.rty_use_service_offers = False
        self.relevance_tread_count_percentage = 0

        # https://st.yandex-team.ru/MARKETOUT-41386

        self.docid_cache_size = 0
        self.docid_cache_docs_limit = 0
        self.docid_cache_chunks_count = 10

        # logs settings
        self.ignore_qtree_decoding_failed_in_error_log = False
        self.check_combinator_errors = False
        self.init_combinator_topics = False
        self.unified_agent_enabled = False

        self.blue_market_free_delivery_threshold = 0
        self.blue_market_prime_free_delivery_threshold = 0
        self.blue_market_yandex_plus_free_delivery_threshold = 0
        self.blue_delivery_price_enabled = True

        # default search experiment flags
        self.default_search_experiment_flags = []
        self.ignore_search_experiment_flags = []

        # https://st.yandex-team.ru/MARKETOUT-19497
        self.create_blue_shard = True
        self.rty_backup_period = '0'
        self.rty_merger_policy = 'TIME'
        self.index_sort = ''
        self.rty_delivery = False
        self.allow_status_new_for_alco_licence = False
        self.disable_url_encryption = True
        self.rty_write_erf = False
        self.rty_write_fullarc_mode = 'sparse'
        self.rty_read_mode = 'fullarc_sparse'

        # https://st.yandex-team.ru/MARKETOUT-35188
        self.put_white_cpa_offer_to_the_blue_shard = True

        # https://st.yandex-team.ru/MARKETOUT-34127
        self.quoter_enabled = False

        self.rgb_blue_is_cpa = False

        self.market_access_settings = MarketAccessSettings()
        self.meta_pinger_use_internal_base_consistency_checker = False

        # https://st.yandex-team.ru/MARKETOUT-47092
        self.test_candidate_log_tear_down = False


class LogsStorage(object):
    def __init__(self, prefix):
        self.__logs_prefix = prefix
        self.__error_log_checked = False

        self.common_log = CommonLogFrontend()
        self.error_log = ErrorLogFrontend()
        self.access_log = AccessLogFrontend()
        self.subplace_access_log = AccessLogFrontend()
        self.base_access_log = BaseAccessLogFrontend()
        self.ammo_log = AmmoLogFrontend()
        self.bigb_log = ExternalHttpServiceLogFrontend("{}-bigb".format(prefix))
        self.calc_log = ExternalHttpServiceLogFrontend("{}-calc".format(prefix))
        self.crypta_log = ExternalHttpServiceLogFrontend("{}-crypta".format(prefix))
        self.delivery_calc_log = ExternalHttpServiceLogFrontend("{}-delivery_calc".format(prefix))
        self.dj_log = ExternalHttpServiceLogFrontend("{}-dj".format(prefix))
        self.ichwill_log = ExternalHttpServiceLogFrontend("{}-ichwill".format(prefix))
        self.recommender_log = ExternalHttpServiceLogFrontend("{}-recommender".format(prefix))
        self.media_adv_log = ExternalHttpServiceLogFrontend("{}-media_adv".format(prefix))
        self.reqwizard_log = ShadeRequestServiceLogFrontend()
        self.show_log_tskv = TskvShowLogFrontend()
        self.show_log = self.show_log_tskv
        self.promo_log_tskv = TskvPromoLogFrontend()
        self.click_log = ClickLogFrontend()
        self.test_data_log = TestDataLogFrontend()
        self.feature_log = FeatureLogFrontend()
        self.external_services_trace_log = ExternalServicesTraceLogFrontend("{}-external-services-trace".format(prefix))
        self.external_services_log = ExternalServicesLogFrontend()
        self.rty_stats_log = RtyStatsLogFrontend()
        self.candidate_log = CandidateLogFrontend("candidate-log")

    def bind(self, report_server, settings, clickdaemon):
        self.common_log.bind(report_server.common_log)
        self.error_log.bind(report_server.error_log)
        self.subplace_access_log.bind(report_server.subplace_access_log)
        self.access_log.bind(report_server.access_log)
        self.base_access_log.bind(report_server.base_access_log)
        self.show_log_tskv.bind(report_server.show_log_tskv)
        self.promo_log_tskv.bind(report_server.promo_log_tskv)
        self.test_data_log.bind(report_server.test_data_log)
        self.click_log.bind(clickdaemon.click_log)
        self.feature_log.bind(report_server.feature_log)
        self.ammo_log.bind(report_server.ammo_log)
        self.bigb_log.bind(report_server.bigb_log)
        self.calc_log.bind(report_server.calc_log)
        self.crypta_log.bind(report_server.crypta_log)
        self.delivery_calc_log.bind(report_server.delivery_calc_log)
        self.dj_log.bind(report_server.dj_log)
        self.ichwill_log.bind(report_server.ichwill_log)
        self.recommender_log.bind(report_server.recommender_log)
        # self.media_adv_log.bind(report_server.media_avd_log)
        self.reqwizard_log.bind(report_server.reqwizard_log)
        self.external_services_trace_log.bind(report_server.external_services_trace_log)
        self.external_services_log.bind(report_server.external_services_log)
        self.error_log.apply_settings(settings)
        self.__error_log_checked = False

    def bind_rty(self, report_server):
        self.rty_stats_log.bind(report_server.rty_stats_log)

    def bind_broker(self, log_broker):
        self.candidate_log.bind(
            LogBrokerProtoTopicBackend(log_broker, logbroker.CANDIDATE_TOPIC, logbroker.LogBrokerClient.CODEC_GZIP, 10)
        )

    def check_error_log(self, validator, all_requests):
        # break infinite loop
        if self.__error_log_checked is True:
            return
        self.__error_log_checked = True
        self.error_log.check(validator, all_requests)

    def check(self, validator, all_requests):
        self.common_log.check(validator, all_requests)
        self.check_error_log(validator, all_requests)
        self.access_log.check(validator, all_requests)
        self.subplace_access_log.check(validator, all_requests)
        self.show_log.check(validator, all_requests)
        self.show_log_tskv.check(validator, all_requests)
        self.promo_log_tskv.check(validator, all_requests)
        self.test_data_log.check(validator, all_requests)
        self.click_log.check(validator, all_requests)
        self.feature_log.check(validator, all_requests)
        self.ammo_log.check(validator, all_requests)
        self.bigb_log.check(validator, all_requests)
        self.calc_log.check(validator, all_requests)
        self.crypta_log.check(validator, all_requests)
        self.delivery_calc_log.check(validator, all_requests)
        self.dj_log.check(validator, all_requests)
        self.ichwill_log.check(validator, all_requests)
        self.recommender_log.check(validator, all_requests)
        self.media_adv_log.check(validator, all_requests)
        self.reqwizard_log.check(validator, all_requests)
        self.external_services_trace_log.check(validator, all_requests)
        self.external_services_log.check(validator, all_requests)

    def check_rty(self, validator, all_requests):
        self.rty_stats_log.check(validator, all_requests)

    def check_broker(self, validator, all_requests):
        self.candidate_log.check(validator, all_requests)


class TestCase(unittest.TestCase, Frozen):
    __meta_search_server = None
    __shade = None
    access_agent = None
    access_server = None
    quoter = None
    index = None
    base_index = None
    fresh_base_index = None
    dynamic = None
    base_dynamic = None
    rty = None
    speller = None
    suggester = None
    request_categories_classificator = None
    formalizer = None
    parametrizator = None
    reqwizard = None
    settings = Settings()
    matrixnet = None
    chats = None
    crypta = None
    datasync = None
    delivery_calc = None
    ichwill = None
    loyalty = None
    recommender = None
    media_advertising = None
    book_now_ctr = None
    adv = None
    bigb = None
    bk = None
    click_n_collect = None
    calc_service = None
    tvmtool = None
    snippets = None
    saas_discovery_service = None
    dj = None
    fast_dj = None
    model_service = None
    bert_service = None
    text_query_dj = None
    turbo = None
    saashub = None
    blackbox = None
    combinator = None
    combinatorExpress = None
    routeStorage = None
    carter = None
    saas_ugc = None
    memcached = None
    unified_agent = None
    fast_mappings = None
    content_storage = None
    panther = None
    emergency_flags = None
    experiment_flags = None
    external_services = None
    report_log_level = None
    __external_services_for_activate = None
    __clickdaemon = None
    __indexerproxy_client = None
    __on_break = []
    __name__ = None
    __testcase_name = None
    __tests_run_exec_stats = None
    __dump = None
    __check_empty_output = True
    __logbroker_server = None
    __base_search_port = None
    __base_search_server = None
    __fresh_base_search_port = None
    __fresh_base_search_server = None
    __default_search_experiment_flags = []
    __crash_only = False
    __sanitizer_type = None
    __report_dead = []
    __portman = None

    __meta_debug = False
    __base_debug = False
    __meta_gdbserver = None
    __base_gdbserver = None
    __meta_bin_path = None
    __base_bin_path = None
    __fresh_base_bin_path = None

    __paths_testroot = None
    __reuse_index = None
    __paths_env = None
    meta_paths = MetaReportPaths()
    base_paths = BaseReportPaths()
    fresh_base_paths = FreshBaseReportPaths()

    _UnitTestCase__pytest_class_setup = None
    _UnitTestCase__pytest_method_setup = None

    @property
    def error_log(self):
        return self.meta_logs_storage.error_log

    @property
    def base_error_log(self):
        return self.base_logs_storage.error_log

    @property
    def common_log(self):
        return self.meta_logs_storage.common_log

    @property
    def access_log(self):
        return self.meta_logs_storage.access_log

    @property
    def subplace_access_log(self):
        return self.meta_logs_storage.subplace_access_log

    @property
    def base_access_log(self):
        return self.base_logs_storage.base_access_log

    @property
    def base_common_log(self):
        return self.base_logs_storage.common_log

    @property
    def show_log(self):
        return self.meta_logs_storage.show_log

    @property
    def show_log_tskv(self):
        return self.meta_logs_storage.show_log_tskv

    @property
    def promo_log_tskv(self):
        return self.meta_logs_storage.promo_log_tskv

    @property
    def test_data_log(self):
        return self.meta_logs_storage.test_data_log

    @property
    def click_log(self):
        return self.meta_logs_storage.click_log

    @property
    def feature_log(self):
        return self.meta_logs_storage.feature_log

    @property
    def ammo_log(self):
        return self.meta_logs_storage.ammo_log

    @property
    def bigb_log(self):
        return self.meta_logs_storage.bigb_log

    @property
    def calc_log(self):
        return self.meta_logs_storage.calc_log

    @property
    def crypta_log(self):
        return self.meta_logs_storage.crypta_log

    @property
    def delivery_calc_log(self):
        return self.meta_logs_storage.delivery_calc_log

    @property
    def dj_log(self):
        return self.meta_logs_storage.dj_log

    @property
    def ichwill_log(self):
        return self.meta_logs_storage.ichwill_log

    @property
    def recommender_log(self):
        return self.meta_logs_storage.recommender_log

    @property
    def media_adv_log(self):
        return self.meta_logs_storage.media_adv_log

    @property
    def reqwizard_log(self):
        return self.meta_logs_storage.reqwizard_log

    @property
    def external_services_trace_log(self):
        return self.meta_logs_storage.external_services_trace_log

    @property
    def external_services_log(self):
        return self.meta_logs_storage.external_services_log

    @property
    def rty_stats_log(self):
        return self.base_logs_storage.rty_stats_log

    @property
    def candidate_log(self):
        return self.base_logs_storage.candidate_log

    @classmethod
    def enable_dump(cls, path, seed=None):
        cls.__dump = path
        if path is not None:
            if seed is None:
                set_seed(0)

    @classmethod
    def disable_check_empty_output(cls):
        cls.__check_empty_output = False

    @classmethod
    def set_default_search_flags(cls, flags):
        cls.__default_search_experiment_flags = flags

    @classmethod
    def disable_randx_randomize(cls):
        Auto.set_randomized_randx(enable_randomizing=False)

    @classmethod
    def setup_index(cls, seed=None):
        if seed is not None and get_seed() is None:
            set_seed(seed)
        cls._init_environment()
        cls._setup_environment(prepare_index_only=True)

    @classmethod
    def enable_crash_only_mode(cls):
        cls.__crash_only = True

    @classmethod
    def set_sanitizer_type(cls, sanitizer_type):
        cls.__sanitizer_type = sanitizer_type

    @classmethod
    def _update_params_from_ya_make(cls):
        if YaRuntime.is_active():
            cls.__crash_only = YaRuntime.get_param_bool('crash_only', default=cls.__crash_only)
            flags = YaRuntime.get_param('with_rearr')
            if flags is not None:
                cls.__default_search_experiment_flags = flags.split(';')
            cls.__meta_bin_path = YaRuntime.get_param('meta_bin', default=cls.__meta_bin_path)
            cls.__base_bin_path = YaRuntime.get_param('base_bin', default=cls.__base_bin_path)
            cls.__fresh_base_bin_path = YaRuntime.get_param('fresh_offers_bin', default=cls.__fresh_base_bin_path)

    @classmethod
    def __exec_stats(cls, name):
        return ExecutionStats(name, cls.meta_paths)

    # called after __init__ of object
    @classmethod
    def setUpClass(cls):
        cls._test_unique_functions()
        try:
            cls._init_testcase_name()
            cls._update_params_from_ya_make()
            cls._setup_paths()
            with cls.__exec_stats("init environment"):
                cls._init_environment()
            with cls.__exec_stats("environment setup"):
                cls._setup_environment()
            with cls.__exec_stats("binaries start"):
                cls._start_binaries()

            cls.__tests_run_exec_stats = cls.__exec_stats("run tests")
        except unittest.SkipTest:
            raise
        except Exception as err:
            import traceback

            tb = traceback.format_exc()
            raise RuntimeError(str(err) + '\nOriginal traceback = {}\nSeed = {}'.format(tb, get_seed()))

    @classmethod
    def _test_unique_functions(cls):
        node = ast.parse(inspect.getsource(cls))
        classes = [n for n in node.body if isinstance(n, ast.ClassDef)]
        for class_node in classes:
            function_names = set()
            functions = [n for n in class_node.body if isinstance(n, ast.FunctionDef)]
            for function_node in functions:
                function_name = function_node.name
                if function_name in function_names:
                    file_name = os.path.basename(inspect.getsourcefile(cls))
                    raise RuntimeError('Not unique function: {} in file: {}'.format(function_name, file_name))
                else:
                    function_names.add(function_name)

    @classmethod
    def _enable_crash_only_mode(cls):
        def wrap_crash_only(test):
            def wrapper(self):
                try:
                    test(self)
                except report.CrashError:
                    raise
                except:
                    pass

            return wrapper

        for name in dir(cls):
            if name.startswith('test') and name != 'test_data_log':
                test = getattr(cls, name)
                setattr(cls, name, wrap_crash_only(test))

    @classmethod
    def _setup_paths(cls, clear_testroot=True, clear_logs=True):
        if cls.__paths_env:
            cls.meta_paths.setup_env(cls.__paths_env)
            cls.base_paths.setup_env(cls.__paths_env)
            cls.fresh_base_paths.setup_env(cls.__paths_env)
        cls.meta_paths.setup(
            cls.__testcase_name,
            forced_testroot=cls.__paths_testroot,
            clear_logs=clear_logs,
            clear_testroot=(clear_testroot and not cls.__reuse_index),
            report_bin_path=cls.__meta_bin_path,
        )
        cls.base_paths.setup(
            cls.__testcase_name,
            forced_testroot=cls.__paths_testroot,
            clear_logs=clear_logs,
            clear_testroot=False,  # Already done in cls.meta_paths.setup(...)
            report_bin_path=cls.__base_bin_path,
        )
        cls.fresh_base_paths.setup(
            cls.__testcase_name,
            forced_testroot=cls.__paths_testroot,
            clear_logs=clear_logs,
            clear_testroot=False,  # Already done in cls.meta_paths.setup(...)
            report_bin_path=cls.__fresh_base_bin_path,
        )

    @classmethod
    def _init_testcase_name(cls):
        if not cls.__testcase_name:
            cls.__testcase_name = os.path.splitext(os.path.basename(inspect.getfile(cls)))[0]

    @classmethod
    def set_testcase_name(cls, testcase_name=None):
        cls.__testcase_name = testcase_name

    @classmethod
    def set_report_bin_path(cls, meta_bin_path=None, base_bin_path=None, fresh_base_bin_path=None):
        cls.__meta_bin_path = meta_bin_path
        cls.__base_bin_path = base_bin_path
        cls.__fresh_base_bin_path = fresh_base_bin_path

    @classmethod
    def set_paths_params(cls, test_root_path=None, reuse_index=None, env=None):
        cls.__paths_testroot = test_root_path
        cls.__reuse_index = reuse_index
        cls.__paths_env = env

    @classmethod
    def __prepare_debug_settings(cls):
        if cls.__meta_debug and cls.__base_debug:
            raise RuntimeError("Can't debug both reports at the same time")

        if (cls.__base_debug and cls.__base_gdbserver) or (cls.__meta_debug and cls.__meta_gdbserver):
            raise RuntimeError("Can't run processes under gdb and gdbserver at the same time")

        if cls.__meta_debug or cls.__meta_gdbserver:
            draw(
                "Only meta process will be runned under debugger, consider using --base-debug or --base-gdbserver",
                Color.RED,
            )
            draw("Report will not be stopped at base search, be careful", Color.RED)

    @classmethod
    def set_debug_settings(cls, meta_debug, base_debug, meta_gdbserver, base_gdbserver):
        cls.__meta_debug = meta_debug
        cls.__base_debug = base_debug
        cls.__meta_gdbserver = meta_gdbserver
        cls.__base_gdbserver = base_gdbserver

    @classmethod
    def _init_environment(cls):
        # set up general python logging
        logging.basicConfig(filename=os.path.join(cls.meta_paths.logs, 'testenv.log'), level=logging.DEBUG)

        cls.__portman = get_port_manager(cls.meta_paths.port_sync_dir)

        # External services
        cls.speller = Speller(cls.meta_paths.logs)
        cls.suggester = Suggester()
        cls.request_categories_classificator = RequestCategoriesClassificator(cls.meta_paths.logs)
        cls.formalizer = Formalizer(cls.meta_paths.logs)
        cls.parametrizator = Parametrizator(cls.meta_paths.logs)
        cls.matrixnet = Matrixnet()
        cls.crypta = Crypta(cls.meta_paths.logs)
        cls.datasync = DataSync(cls.meta_paths.logs)
        cls.delivery_calc = DeliveryCalc(cls.meta_paths)
        cls.ichwill = IchWill(cls.meta_paths.logs)
        cls.loyalty = Loyalty()
        cls.recommender = Recommender(cls.meta_paths.logs)
        cls.media_advertising = MediaAdvertising(cls.meta_paths.logs)
        cls.reqwizard = ReqWizard()
        cls.adv = AdvMachine(cls.meta_paths.logs)
        cls.bigb = BigB(cls.meta_paths.logs)
        cls.emergency_flags = EmergencyFlags(cls.meta_paths.configroot)
        cls.experiment_flags = ExperimentFlags(cls.meta_paths.configroot)
        cls.bk = BK(cls.meta_paths.logs)
        cls.click_n_collect = ClickNCollect(cls.meta_paths.logs)
        cls.calc_service = CalcService(cls.meta_paths.logs)
        cls.chats = ChatsService()
        cls.tvmtool = TvmToolService(cls.meta_paths.logs)
        cls.snippets = Snippets(cls.__reuse_index)
        cls.dj = Dj(cls.meta_paths.logs)
        cls.fast_dj = FastDj(cls.meta_paths.logs)
        cls.model_service = ModelService(lambda: cls.index.regiontree)
        cls.bert_service = BertService(lambda: cls.index.regiontree)
        cls.text_query_dj = TextQueryDj(cls.meta_paths.logs)
        cls.turbo = Turbo()
        cls.saashub = SaasHub()
        cls.saas_ugc = SaasUGC()
        cls.saas_discovery_service = GenCfgService(cls.settings, cls.snippets, cls.saas_ugc, cls.meta_paths.logs)
        cls.blackbox = Blackbox(cls.meta_paths.logs)
        cls.combinator = Combinator()
        cls.combinatorExpress = CombinatorExpress()
        cls.routeStorage = RouteStorage()
        cls.carter = Carter(cls.meta_paths.logs)
        cls.unified_agent = UnifiedAgent(
            settings=cls.settings,
            build_root=BUILDROOT,
            work_dir=cls.meta_paths.testroot,
            portman=cls.__portman,
            log_path=os.path.join(cls.meta_paths.logs, 'feature.log'),
            topic=logbroker.FEATURE_TOPIC,
            ua_log_path=os.path.join(cls.meta_paths.logs, 'unified_agent.log'),
            ua_err_path=os.path.join(cls.meta_paths.logs, 'unified_agent.err'),
        )
        cls.memcached = Memcached(
            settings=cls.settings,
            build_root=BUILDROOT,
            work_dir=cls.meta_paths.testroot,
            portman=cls.__portman,
        )
        cls.content_storage = ContentStorage()
        cls.fast_mappings = FastMappings()
        cls.panther = ExternalPantherSuite(cls.settings, cls.__portman, work_dir=cls.meta_paths.testroot)
        cls.quoter = Quoter(
            settings=cls.settings,
            svc_name='svc_name_quoter',
            runtime_name='runtime_name_quoter',
            portman=cls.__portman,
            work_dir=os.path.join(cls.meta_paths.testroot, 'quoter'),
        )
        cls.access_server = MarketAccessServer(
            settings=cls.settings,
            svc_name='svc_name_access',
            runtime_name='runtime_name_access',
            portman=cls.__portman,
            work_dir=os.path.join(cls.meta_paths.testroot, 'access'),
        )

        cls.access_agent = MarketAccessAgent(
            settings=cls.settings,
            server_port=cls.access_server.grpc_port,
            svc_name='svc_name_access_agent',
            runtime_name='runtime_name_access_agent',
            portman=cls.__portman,
            work_dir=os.path.join(cls.meta_paths.testroot, 'access_agent'),
        )
        Auto.set_randomized_randx(True)
        cls.external_services = (
            cls.formalizer,
            cls.parametrizator,
            cls.speller,
            cls.suggester,
            cls.request_categories_classificator,
            cls.matrixnet,
            cls.crypta,
            cls.datasync,
            cls.delivery_calc,
            cls.ichwill,
            cls.loyalty,
            cls.reqwizard,
            cls.adv,
            cls.bigb,
            cls.bk,
            cls.calc_service,
            cls.carter,
            cls.tvmtool,
            cls.snippets,
            cls.saas_discovery_service,
            cls.recommender,
            cls.media_advertising,
            cls.chats,
            cls.dj,
            cls.fast_dj,
            cls.model_service,
            cls.bert_service,
            cls.text_query_dj,
            cls.turbo,
            cls.saashub,
            cls.blackbox,
            cls.click_n_collect,
            cls.combinator,
            cls.combinatorExpress,
            cls.routeStorage,
            cls.saas_ugc,
            cls.memcached,
            cls.unified_agent,
            cls.panther,
            cls.quoter,
            cls.access_server,
            cls.content_storage,
            cls.fast_mappings,
            cls.access_agent,
        )

        cls.settings.default_search_experiment_flags = cls.__default_search_experiment_flags
        cls.settings.emergency_flags_path = cls.emergency_flags.flags_path
        cls.settings.experiment_flags_path = cls.experiment_flags.flags_path

        meta_index_ctx = IndexContext(
            settings=cls.settings,
            snippets=cls.snippets,
            saashub=cls.saashub,
            chats=cls.chats,
            paths=cls.meta_paths,
        )

        base_index_ctx = IndexContext(
            settings=cls.settings,
            snippets=cls.snippets,
            saashub=cls.saashub,
            chats=cls.chats,
            paths=cls.base_paths,
        )

        fresh_base_index_ctx = IndexContext(
            settings=cls.settings,
            snippets=cls.snippets,
            saashub=cls.saashub,
            chats=cls.chats,
            paths=cls.fresh_base_paths,
        )

        cls.beforePrepare()
        cls.dynamic = dynamic.ReportDynamicData(cls.settings, cls.meta_paths)
        cls.index = Index(meta_index_ctx)
        cls.base_index = Index(base_index_ctx)
        cls.fresh_base_index = Index(fresh_base_index_ctx)
        cls.base_dynamic = dynamic.ReportDynamicData(cls.settings, cls.base_paths)

    @classmethod
    def _setup_environment(cls, prepare_index_only=False):
        cls.prepare()

        if cls.settings.microseconds_for_disabled_random is not None:
            cls.combinator.set_start_date(cls.settings.microseconds_for_disabled_random)
            cls.combinatorExpress.set_start_date(cls.settings.microseconds_for_disabled_random)
            cls.routeStorage.set_start_date(cls.settings.microseconds_for_disabled_random)
        for name in dir(cls):
            if name.startswith('prepare_'):
                prepare_method = getattr(cls, name)
                prepare_method()
        cls.afterPrepare()

        cls.__prepare_debug_settings()

        if cls.settings.enable_panther and cls.settings.enable_panther_external:
            if len(cls.index.models) > 0:
                cls.panther.add("model", cls.meta_paths.modelindex)
            if len(cls.index.offers) > 0:
                cls.panther.add("offer", cls.meta_paths.shopindex)

        if prepare_index_only:
            return

        use_separate_base_index = not cls.base_index.is_empty
        if not cls.__reuse_index:
            cls.index.commit()
            if use_separate_base_index:
                cls.base_index.commit()
        else:
            cls.index.set_out_collections()
            if use_separate_base_index:
                cls.base_index.set_out_collections()

        TvmTool.commit_token(cls.meta_paths.secrets)
        if use_separate_base_index:
            TvmTool.commit_token(cls.base_paths.secrets)
        TvmTool.init_tvmtool(cls.tvmtool)

        if cls.settings.lms_autogenerate:
            with Autoscope():
                cls.dynamic.lms.generate(cls.index)
                if use_separate_base_index:
                    cls.base_dynamic.lms.generate(cls.base_index)

        if cls.settings.nordstream_autogenerate:
            cls.dynamic.nordstream.generate(cls.index, cls.settings.nordstream_types)
            if use_separate_base_index:
                cls.base_dynamic.nordstream.generate(cls.base_index, cls.settings.nordstream_types)

        is_fresh_base_report_active = cls.settings.use_fresh_base_report and not cls.fresh_base_index.is_empty
        if is_fresh_base_report_active:
            cls.fresh_base_index.commit()

        if cls.settings.logbroker_enabled:
            cls.__logbroker_server = logbroker.LogBroker(
                cls.settings.logbroker_topics, cls.meta_paths, cls.settings.logbroker_port
            )
            cls.settings.logbroker_port = cls.__logbroker_server.port
            cls.unified_agent.link_lb_port(cls.__logbroker_server.port)

        cls.dynamic._freeze()
        cls.dynamic._save_files()
        if use_separate_base_index:
            cls.base_dynamic._freeze()
            cls.base_dynamic._save_files()

        cls.emergency_flags.save()
        cls.experiment_flags.save()

        cls.__external_services_for_activate = [
            svc for svc in cls.external_services if svc.need_activation and not svc.shade_support
        ]

        cls.__meta_search_server = report.Server(
            portman=cls.__portman,
            collections=cls.index.ctx.out_collections,
            port=_PORT,
            external_services=cls.external_services,
            report_settings=cls.settings,
            check_empty_output=cls.__check_empty_output,
            force_archive_mode=cls.settings.force_archive_mode,
            server_type=report.ServerType.META_SEARCH,
            paths=cls.meta_paths,
            sanitizer_type=cls.__sanitizer_type,
            log_level=cls.report_log_level,
        )

        testenv_log = os.path.join(cls.meta_paths.logs, 'testenv.log')
        if not use_separate_base_index:
            run_external_tool(["rm", "-rf", cls.base_paths.search_root], testenv_log)
            _make_symlink_or_copy_dir(
                cls.meta_paths.search_root, cls.base_paths.search_root, testenv_log, copy_dirs=['report_data']
            )
        _delete_unused_index_files_on_base_report(cls.base_paths.reportdata, testenv_log)

        cls.__base_search_port = get_port()
        base_index = cls.base_index if use_separate_base_index else cls.index
        cls.__base_search_server = report.Server(
            portman=cls.__portman,
            collections=base_index.ctx.out_collections,
            port=cls.__base_search_port,
            external_services=cls.external_services,
            report_settings=cls.settings,
            check_empty_output=cls.__check_empty_output,
            server_type=report.ServerType.BASE_SEARCH,
            paths=cls.base_paths,
            sanitizer_type=cls.__sanitizer_type,
            log_level=cls.report_log_level,
        )

        if is_fresh_base_report_active:
            cls.__fresh_base_search_port = get_port()
            cls.__fresh_base_search_server = report.Server(
                portman=cls.__portman,
                collections=cls.fresh_base_index.ctx.out_collections,
                port=cls.__fresh_base_search_port,
                external_services=cls.external_services,
                report_settings=cls.settings,
                server_type=report.ServerType.FRESH_BASE_SEARCH,
                paths=cls.fresh_base_paths,
                sanitizer_type=cls.__sanitizer_type,
                log_level=cls.report_log_level,
            )

        cls.__clickdaemon = ClickDaemon(cls.meta_paths.logs)
        enable_shade_grpc = (
            cls.combinator.has_data()
            or cls.combinatorExpress.has_data()
            or cls.content_storage.has_data()
            or cls.fast_mappings.has_data()
            or cls.routeStorage.has_data()
        )
        cls.__shade = Shade(
            cls.meta_paths.testroot,
            BUILDROOT,
            SRCROOT,
            cls.meta_paths.shade_bin,
            cls.__portman,
            enable_shade_grpc,
        )

        if cls.__reuse_index:
            cls.__shade.load_existing_config()

        for svc in cls.external_services:
            if svc.shade_support:
                cls.__shade.register(svc)

        if cls.__crash_only:
            cls._enable_crash_only_mode()

    @classmethod
    def _start_binaries(cls):
        base_search_service_info = ''
        if cls.__base_search_server:
            base_search_service_info += ' (base search service will use port: {})'.format(cls.__base_search_port)
        if cls.__fresh_base_search_server:
            base_search_service_info += ' (fresh offers search service will use port: {})'.format(
                cls.__fresh_base_search_port
            )
        cls.breakpoint('before start' + base_search_service_info)

        logger.info('Crash only mode: {}'.format(cls.__crash_only))
        logger.info('Default search experiment flags: {}'.format(cls.__default_search_experiment_flags))

        old_sigint_handler = signal.signal(signal.SIGINT, cls._on_interrupt)
        try:
            with cls.__exec_stats("shade start"):
                cls.__shade.start()
            with cls.__exec_stats("external services start"):
                for service in cls.__external_services_for_activate:
                    service.start()
            with cls.__exec_stats("setup market access"):
                cls._setup_market_access()

            with cls.__exec_stats("base report start"):
                cls.__base_search_server.start(debug=cls.__base_debug, gdbserver=cls.__base_gdbserver)
                cls.__meta_search_server.set_base_search_port(cls.__base_search_server.port)

            if cls.settings.use_fresh_base_report and not cls.fresh_base_index.is_empty:
                with cls.__exec_stats("fresh base report start"):
                    cls.__fresh_base_search_server.start()  # TODO: add debug
                    cls.__meta_search_server.set_fresh_base_search_port(cls.__fresh_base_search_server.port)

            with cls.__exec_stats("meta report start"):
                cls.__meta_search_server.start(debug=cls.__meta_debug, gdbserver=cls.__meta_gdbserver)
        except Exception as e:
            cls._on_error(e)
            raise

        signal.signal(signal.SIGINT, old_sigint_handler)

        if cls.settings.rty_qpipe:
            cls.__indexerproxy_client = indexerproxy.Client(cls.meta_paths, cls.__base_search_server.indexer_port)
            cls.rty = rty.Pipeline(cls.__indexerproxy_client)

        if _WAIT or _BREAKPOINT:
            cls._print_info()
            sys.stdout.write('Report process pid:      ')
            draw(str(cls.__meta_search_server.pid), Color.GREEN)
            if _WAIT:
                if six.PY2:
                    raw_input('Press ENTER to run tests...')
                else:
                    input('Press ENTER to run tests...')

    @classmethod
    def _setup_market_access_catstreams(cls, access_server, shade_host_port):
        if len(cls.index.category_streams) > 0:
            catstream_resource = MarketAccessSingleFbResource(
                access_server=access_server,
                shade_host_port=shade_host_port,
                meta_paths=cls.meta_paths,
                resource_name="report_catstreams",
                publisher_name="report",
            )
            catstream_resource.create_version(CategoryStreamsStorage(cls.index.category_streams, cls.meta_paths))

    @classmethod
    def _setup_market_access(cls):
        if cls.settings.market_access_settings.enabled:
            shade_host_port = 'localhost:{}'.format(cls.__shade.port)
            cls.setup_market_access_resources(cls.access_server.get_client(), shade_host_port)
            cls._setup_market_access_catstreams(cls.access_server.get_client(), shade_host_port)

    @classmethod
    def _stop_binaries(cls):
        exceptions = []
        safe_lambda_exec(exceptions, lambda: cls.__meta_search_server.stop())
        safe_lambda_exec(exceptions, lambda: cls.__base_search_server.stop())
        if cls.settings.use_fresh_base_report and not cls.fresh_base_index.is_empty:
            safe_lambda_exec(exceptions, lambda: cls.__fresh_base_search_server.stop())
        for service in cls.__external_services_for_activate:
            safe_lambda_exec(exceptions, lambda: service.stop())
        if cls.__shade:
            safe_lambda_exec(exceptions, lambda: cls.__shade.stop())
        safe_lambda_exec(exceptions, lambda: cls.__clickdaemon.stop())

        if cls.settings.logbroker_enabled:
            safe_lambda_exec(exceptions, lambda: cls.__logbroker_server.stop())

        for e in exceptions:
            if e:
                raise e

    @classmethod
    def setup_market_access_resources(cls, access_server, shade_host_port):
        pass

    @classmethod
    def tearDownClass(cls):
        cls.__tests_run_exec_stats.flush()

        with cls.__exec_stats("stop binaries"):
            cls._stop_binaries()
        with cls.__exec_stats("release portman"):
            cls.__portman.release()

    @classmethod
    def beforePrepare(cls):
        """Hook method for setting up custom test settings to overwrite usual test behaviour"""

    @classmethod
    def afterPrepare(cls):
        """Hook method for setting up custom test settings to overwrite usual test behaviour"""

    @classmethod
    def prepare(cls):
        """Hook method for setting up class fixture before running tests in the class."""

    @classmethod
    def wait(cls):
        cls._print_info()
        if cls.__meta_debug:
            cls.__meta_search_server.wait()

        if cls.__base_debug:
            cls.__base_search_server.wait()

        else:
            if six.PY2:
                raw_input('Press ENTER to exit...')
            else:
                input('Press ENTER to exit...')

    @classmethod
    def _print_info(cls):
        sys.stdout.write('Index has been built:    ')
        draw(cls.meta_paths.testroot, Color.GREEN)
        sys.stdout.write('Report has been started: ')
        request_str = 'http://{host}:{port}/yandsearch?'.format(
            host=socket.getfqdn(), port=cls.__meta_search_server.port
        )
        draw(request_str, Color.GREEN)

    @classmethod
    def stop_report(cls):
        cls.__meta_search_server.stop_report_binary()
        cls.__base_search_server.stop_report_binary()

    @classmethod
    def restart_report(cls):
        cls.__meta_search_server.restart(debug=cls.__meta_debug)
        cls.__base_search_server.restart(debug=cls.__base_debug)

    def __init__(self, name='runTest'):
        unittest.TestCase.__init__(self, name)

        def alive_stub():
            pass

        self.report = report.Client(
            alive_stub, self.settings, self.meta_paths
        )  # intellisense only, will be overridden on setUp
        self.__base_search_client = None
        self.__click_observer = None
        self.meta_logs_storage = LogsStorage("meta")
        self.base_logs_storage = LogsStorage("base")

    def fail(self, msg=None):
        unittest.TestCase.fail(self, msg)

    @property
    def rty_controller(self):
        return self.__base_search_server.rty_controller

    @property
    def server(self):
        return self.__meta_search_server

    @property
    def base_search_server(self):
        return self.__base_search_server

    @property
    def base_search_client(self):
        return self.__base_search_client

    def __breakpoint_observer(self, tail, **kwargs):
        with NoRequestObservers():
            self.breakpoint('before request "{}"'.format(tail))

    @property
    def logbroker(self):
        return self.__logbroker_server

    @staticmethod
    def replace_random(text):
        replace_map = {
            'localhost:\d+': 'localhost:###',
            ':\d+/': ':###/',
            '"searchTimeMs":\d+': '"searchTimeMs":###',
            '"startTime":".+"': '"startTime":"###"',
            '"duration":".+"': '"duration":"###"',
            'duration=".+"': 'duration="###"',
            '"finishTime":".+"': '"finishTime":"###"',
            '"tscRes":\d+': '"tscRes": ###',
            '"firstStartTime":".+"': '"firstStartTime":"###"',
            '"lastFinishTime":".+"': '"lastFinishTime":"###"',
            '"found":[1-9]\d*': '"found":1',
            '"initialFound":[1-9]\d*': '"initialFound":1',
            '\d+ ms': '### ms',
            '\d+ wareMd5': '### wareMd5',
            '<search-time-ms>\d+</search-time-ms>': '<search-time-ms>###</search-time-ms>',
            'localhost%3A\d+': 'localhost%3A###',
            '0 1 \d+': '0 1 ###',
            '"cpc":".+"': '"cpc":"###"',
            '/uid=\d+/': '/uid=###/',
            '/link_id=\d+/': '/link_id=###/',
            '/show_block_id=\d+/': '/show_block_id=###/',
            '/show_time=\d+/': '/show_time=###/',
            '"feeShow":".+"': '"feeShow":"###"',
            '\d+ debug-xml': '### debug-xml',
            '&ts=\d+': '&ts=###',
            'reqwizard-history=\w+&': 'reqwizard-history=###&',
            'Error:request failed\(HTTP/1.1 400 Bad Request\)': 'Error:Connection refused',  # bad test from recom
            'meta_request_url_hash: .+"': 'meta_request_url_hash: "###"',
            'start_time=".+"': 'start_time="###"',
            'finish_time=".+"': 'finish_time="###"',
            'tsc_res="\d+"': 'tsc_res="###"',
            'first_start_time=".+"': 'first_start_time="###"',
            'last_finish_time=".+"': 'last_finish_time="###"',
            'meta_request_url_hash: &quot;.+&quot;': 'meta_request_url_hash: &quot;###&quot;',
            '&reqwizard-history=.+&': '&reqwizard-history=###&',
            '"timestamp":\d+': '"timestamp":###',
        }

        for place in ['main', 'parallel', 'cards', 'model_statistics', 'trivial_inorder']:
            replace_map['{}%3A.+&'.format(place)] = '{}%3A###&'.format(place)
            replace_map['{}:.+"'.format(place)] = '{}:###'.format(place)

        tskv_fields = ['unixtime', 'event_time', 'url_hash', 'fetch_time', 'unixtime_ms']

        for k, v in replace_map.items():
            text = re.sub(k, v, text)

        for field in tskv_fields:
            text = re.sub(r'{}=.+\t'.format(field), r'{}=###\t'.format(field), text)
            text = re.sub(r'{}=.+\\t'.format(field), r'{}=###\\t'.format(field), text)

        return text

    def setUp(self):
        if self.__report_dead:
            self.skipTest("Report is dead, so skip futher tests")

        if self.settings.use_multiconnect_http_client:
            self.report = report.MultiConnectClient(self.__meta_search_server.alive, self.settings, self.meta_paths)
        else:
            self.report = report.Client(self.__meta_search_server.alive, self.settings, self.meta_paths)
        self.report.connect(
            port=self.__meta_search_server.port,
            apphost_port=self.__meta_search_server.apphost_port,
            debug=self.__meta_debug,
        )
        self.report.check_alive()
        self.assertFragmentIn(
            self.report.request_xml('admin_action=unistat-reset', store_request=False), '<status>ok</status>'
        )

        self.report.observe_call(self.__breakpoint_observer)
        self.__click_observer = ClickUrlObserver(self.__clickdaemon)
        self.report.observe_output(self.__click_observer)
        if self.__dump:
            dumper = Dumper(
                self.__testcase_name,
                self._testMethodName,
                self.__dump,
                lambda request: 'place' in request,
                TestCase.replace_random,
            )
            self.report.observe_output(dumper)

        self.__base_search_client = report.Client(
            self.__base_search_server.alive,
            self.settings,
            self.base_paths,
            server_type=report.ServerType.BASE_SEARCH,
        )
        self.__base_search_client.connect(
            port=self.__base_search_server.port,
            apphost_port=self.__base_search_server.apphost_port,
            debug=self.__base_debug,
        )
        self.__base_search_client.check_alive()

        self.meta_logs_storage.bind(self.__meta_search_server, self.settings, self.__clickdaemon)
        self.base_logs_storage.bind(self.__base_search_server, self.settings, self.__clickdaemon)

        if self.rty:
            self.rty.setup(self.base_search_client, self.__on_break)
            self.base_logs_storage.bind_rty(self.__base_search_server)

        if self.logbroker:
            self.base_logs_storage.bind_broker(self.__logbroker_server)

        self.dynamic.setup(self.report, self.__base_search_client, self.__on_break)

    @classmethod
    def breakpoint(cls, hint=None):
        if _BREAKPOINT:
            for on_break in cls.__on_break:
                on_break()
            text = '{} [ENTER]'.format(hint) if hint else ' [ENTER] '
            draw(text, Color.YELLOW, newline=False)
            if six.PY2:
                raw_input()
            else:
                input()

    @classmethod
    def _on_error(cls, error):
        exceptions = []
        safe_lambda_exec(exceptions, lambda: cls.__meta_search_server.stop(error))
        safe_lambda_exec(exceptions, lambda: cls.__base_search_server.stop(error))
        for service in cls.__external_services_for_activate:
            safe_lambda_exec(exceptions, lambda: service.stop())
        if cls.__shade:
            safe_lambda_exec(exceptions, lambda: cls.__shade.stop())

        for e in exceptions:
            if e:
                raise e

    @classmethod
    def _on_interrupt(cls, *_):
        cls._on_error('SIGINT')
        if cls.__clickdaemon:
            cls.__clickdaemon.stop()
        raise KeyboardInterrupt

    def tearDown(self):
        self.breakpoint('before tearDown')
        self.report.observe_call(self.__breakpoint_observer, False)
        try:
            self.report.check_alive()
            self.base_search_client.check_alive()
        except:
            # megahack: to pass state from one testcase instance to another (value-types are copied and not suitable here)
            self.__report_dead.append(None)
            raise

        all_requests = self.report.all_requests
        with open(os.path.join(self.meta_paths.logs, 'all_requests.txt'), 'wt') as f:
            for request in all_requests:
                f.write('{}\n'.format(request))

        if self.__crash_only is False:
            self.report.observe_output(self.__click_observer, False)
            self.__on_break = []
            self.assertFragmentIn(
                self.report.request_xml('admin_action=flushlogs', store_request=False),
                '<status>Logs flushed ok</status>',
            )

            self.meta_logs_storage.check(self, all_requests)

            self.assertFragmentIn(
                self.base_search_client.request_xml('admin_action=flushlogs', store_request=False),
                '<status>Logs flushed ok</status>',
            )

            all_base_requests = self.base_search_client.all_requests
            with open(os.path.join(self.base_paths.logs, 'all_requests.txt'), 'wt') as f:
                for request in all_base_requests:
                    f.write('{}\n'.format(request))
            self.base_logs_storage.check(self, all_base_requests)

            if self.rty:
                self.rty_stats_log.check(self, all_requests)

            if self.logbroker and self.settings.test_candidate_log_tear_down:
                self.base_logs_storage.check_broker(self, all_requests)

        self.report.stop()
        self.base_search_client.stop()

    def __log_response(self, response):
        fr_path = os.path.join(str(self.meta_paths.logs), '{}.fail'.format(self._testMethodName))
        with open(fr_path, 'w+') as fr:
            fr.write(str(response))
        return fr_path

    def __format_assert_tail(self, preserve_order, fr_path):
        return 'Strict order: {}\nOriginal response: {}'.format(preserve_order, fr_path)

    def assertFragmentIn(
        self, response, fragment, __barrier=0xDEADBEAF, preserve_order=False, allow_different_len=True, use_regex=False
    ):
        # need to prevent fragment leakage to assert arguments due to wrong json formatting
        if __barrier != 0xDEADBEAF:
            print(__barrier)
            raise RuntimeError('Fragment formatting error')

        if isinstance(response, NullResponse):
            self.fail_verbose('Report has crashed', [], response.request)
        success, reasons = response.contains(fragment, preserve_order, allow_different_len, use_regex)
        if success is False:
            fr_path = self.__log_response(response)
            if isinstance(fragment, (list, dict)):
                fragment = json.dumps(fragment, indent=2, ensure_ascii=False, default=str)
            tail = self.__format_assert_tail(preserve_order, fr_path)
            msg = 'Response does not contain {}\n{}'.format(ustr(fragment), tail)
            self.fail_verbose(msg, reasons, response.request)

    def assertFragmentNotIn(self, response, fragment, __barrier=0xDEADBEAF, preserve_order=False, use_regex=False):
        # need to prevent fragment leakage to assert arguments due to wrong json formatting
        if __barrier != 0xDEADBEAF:
            raise RuntimeError('Fragment formatting error')

        if isinstance(response, NullResponse):
            self.fail_verbose('Report has crashed', [], response.request)
        exist, _ = response.contains(fragment, preserve_order, use_regex=use_regex)
        if exist:
            if isinstance(fragment, (list, dict)):
                fragment = json.dumps(fragment, indent=2, ensure_ascii=False, default=ustr)
            fr_path = self.__log_response(response)
            self.fail_verbose(
                'Response contains unexpected {}\nStrict order: {}\nOriginal response: {}'.format(
                    ustr(fragment), preserve_order, fr_path
                ),
                [],
                response.request,
            )

    def assertEqual(self, first, second):
        msg = '{} != {}'.format(first, second)
        unittest.TestCase.assertEqual(
            self,
            first,
            second,
            format_assert(
                msg, self.report.last_request, self.__testcase_name, self._testMethodName, str(self.meta_paths.testroot)
            ),
        )

    def assertNotEqual(self, first, second):
        msg = '{} == {}'.format(first, second)
        unittest.TestCase.assertNotEqual(
            self,
            first,
            second,
            format_assert(
                msg, self.report.last_request, self.__testcase_name, self._testMethodName, str(self.meta_paths.testroot)
            ),
        )

    def assertTrue(self, expr):
        msg = '{} is not True'.format(expr)
        unittest.TestCase.assertTrue(
            self,
            expr,
            format_assert(
                msg, self.report.last_request, self.__testcase_name, self._testMethodName, str(self.meta_paths.testroot)
            ),
        )

    def assertFalse(self, expr):
        msg = '{} is not False'.format(expr)
        unittest.TestCase.assertFalse(
            self,
            expr,
            format_assert(
                msg, self.report.last_request, self.__testcase_name, self._testMethodName, str(self.meta_paths.testroot)
            ),
        )

    def assertIn(self, elem, lst):
        msg = '{} not in {}'.format(elem, lst)
        unittest.TestCase.assertIn(
            self,
            elem,
            lst,
            format_assert(
                msg, self.report.last_request, self.__testcase_name, self._testMethodName, str(self.meta_paths.testroot)
            ),
        )

    def assertNotIn(self, elem, lst):
        msg = '{} in {}'.format(elem, lst)
        unittest.TestCase.assertNotIn(
            self,
            elem,
            lst,
            format_assert(
                msg, self.report.last_request, self.__testcase_name, self._testMethodName, str(self.meta_paths.testroot)
            ),
        )

    def __assertEqualResponses(
        self,
        get_response_func,
        offer_template,
        request1,
        request2,
        count_offers,
        ignore_click_urls,
        ignore_incuts=False,
    ):
        response1 = get_response_func(request1)
        response2 = get_response_func(request2)
        if ignore_click_urls:

            def iter_list(data):
                for value in data:
                    iter_json(value)

            def iter_dict(data):
                if 'urls' in data:
                    del data['urls']
                for key, value in data.items():
                    iter_json(value)

            def iter_json(data):
                if isinstance(data, list):
                    iter_list(data)
                elif isinstance(data, dict):
                    iter_dict(data)

            iter_json(response1.root)
            iter_json(response2.root)

        if ignore_incuts:

            def delete_incuts_block(data):
                if "incuts" in data:
                    del data["incuts"]

            delete_incuts_block(response1.root)
            delete_incuts_block(response2.root)

        is_equal, reasons = response1.equal_to(response2)
        if not is_equal:
            self.fail_verbose(
                'Response on request\n{}\nis not equal to response on request\n{}'.format(
                    response1.request, response2.request
                ),
                reasons,
                response1.request,
            )
        if count_offers is not None:
            self.assertEqual(response1.count(offer_template), count_offers)

    def assertEqualJsonResponses(
        self, request1, request2, count_offers=None, ignore_click_urls=False, ignore_incuts=False
    ):
        return self.__assertEqualResponses(
            self.report.request_json,
            {"entity": "offer"},
            request1,
            request2,
            count_offers,
            ignore_click_urls,
            ignore_incuts,
        )

    def assertEqualXmlResponses(self, request1, request2, count_offers=None, ignore_click_urls=False):
        return self.__assertEqualResponses(
            self.report.request_xml, '</offer>', request1, request2, count_offers, ignore_click_urls
        )

    def assertEqualBsResponses(self, request1, request2):
        return self.__assertEqualResponses(self.report.request_bs, None, request1, request2, None, False)

    def fail_verbose(self, msg, reasons, requests):
        self.meta_logs_storage.check_error_log(
            self, self.report.all_requests
        )  # sometimes gives better diag then error message

        self.base_logs_storage.check_error_log(self, self.base_search_client.all_requests)

        request = requests if isinstance(requests, str) else '\n'.join(requests)
        text = format_assert(
            msg, request, self.__testcase_name, self._testMethodName, str(self.meta_paths.testroot), reasons
        )
        self.fail(text)

    def shortDescription(self):
        return None


def format_assert(msg, request, case, test, testroot, reasons=None):
    error = colorstr(msg, Color.RED)
    where = colorstr('Data: {}'.format(testroot), Color.GREEN)
    how = colorstr('Query: {}'.format(request), Color.GREEN)
    reproduce = colorstr('Exec: ./{}.py -t {} --seed {}'.format(case, test, get_seed()), Color.GREEN)
    reasons = set(reasons) if reasons else []
    reasons_fmt = '\n'.join(reasons)
    why = colorstr(reasons_fmt, Color.YELLOW)

    if reasons:
        return '\n{}\n\n{}\n\n{}\n{}\n{}'.format(error, why, how, where, reproduce)
    return '\n{}\n\n{}\n{}\n{}'.format(error, how, where, reproduce)


def reflect_testcase():
    module = __import__('__main__')
    hierarchy = []
    for name in dir(module):
        obj = getattr(module, name)
        if isinstance(obj, type) and issubclass(obj, TestCase) and obj.__module__ == module.__name__:
            hierarchy.append(obj)

    if not hierarchy:
        return None

    def class_cmp(x, y):
        xy = issubclass(x, y)
        yx = issubclass(y, x)
        if xy == yx:
            return 0
        return -1 if xy else 1

    hierarchy = sorted(hierarchy, cmp=class_cmp)
    return hierarchy[0]


def server_mode(name=None, wait=True):
    if name:
        TestCase.set_testcase_name(name)

    testcase = reflect_testcase()
    if testcase is None:
        raise RuntimeError('Cannot find test case to run.')

    testcase.disable_check_empty_output()
    testcase.setUpClass()
    if wait:
        testcase.wait()
    testcase.tearDownClass()


def make_common_parser():
    """
    Arguments that are suitable for both lite with tests and lite binary
    """
    parser = argparse.ArgumentParser(formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('-p', '--port', help="port to run report on", metavar='NUM')
    parser.add_argument('-r', '--root', help="test root path for all data: configs, index, logs, etc.", metavar='PATH')
    parser.add_argument('-d', '--meta-debug', action="store_true", help="run tests under debugger", default=False)
    parser.add_argument('--base-debug', action="store_true", help="run tests under debugger", default=False)
    parser.add_argument('--meta-gdbserver', help="run meta report under gdbserver")
    parser.add_argument('--base-gdbserver', help="run base report under gdbserver")
    parser.add_argument('--with-rearr', help="add default flags for each test", action='append', metavar='FLAG')
    parser.add_argument('--reuse', action="store_true", help="reuse index built in past")
    parser.add_argument('--seed', help="seed for randomizer", metavar='NUM')
    parser.add_argument('--no-color', help="disable colorization of output", action="store_true")
    parser.add_argument('--crash-only', help="consider test fail on service crash only", action="store_true")
    return parser


def parse_args():
    parser = make_common_parser()
    parser.add_argument('-s', '--server', action="store_true", help="build index and run report")
    parser.add_argument('-t', '--test', help="name of test to execute", metavar='NAME')
    parser.add_argument('-w', '--wait', action="store_true", help="keypress wait after server starts")
    parser.add_argument('-v', '--verbose', action="store_true", help="verbose execution")
    parser.add_argument('-f', '--failfast', action="store_true", help="stop after first fail")
    parser.add_argument('-b', '--breakpoint', action="store_true", help="pause before each test run")
    parser.add_argument('-l', '--list', action="store_true", help="list all tests in suite")
    parser.add_argument('-e', '--env', help="path to custom environment config", metavar='PATH')
    parser.add_argument(
        '--sanitize', choices=['address', 'memory', 'thread', 'undefined'], help='set report binary sanitizer type'
    )

    parser.add_argument('--meta-bin', help='path to custom meta report', metavar='PATH')
    parser.add_argument('--base-bin', help='path to custom base report', metavar='PATH')
    parser.add_argument('--fresh-base-bin', help='path to custom fresh base report', metavar='PATH')

    parser.add_argument(
        '--desc', action="store_true", help="prints test's name and description (works if nose installed)"
    )
    parser.add_argument('--dump', help="dumps all output and logs got from report to passed directory", metavar='PATH')

    args = parser.parse_args()
    return args


def setup_common_args(args):
    global _PORT

    TestCase.set_debug_settings(args.meta_debug, args.base_debug, args.meta_gdbserver, args.base_gdbserver)

    if args.port is not None:
        _PORT = args.port

    if args.no_color:
        Color.enabled = False

    TestCase.set_report_bin_path(
        meta_bin_path=args.meta_bin, base_bin_path=args.base_bin, fresh_base_bin_path=args.fresh_base_bin
    )
    TestCase.set_paths_params(
        test_root_path=os.path.realpath(os.path.expanduser(args.root)) if args.root else None,
        reuse_index=args.reuse,
        env=args.env,
    )

    if args.seed is not None:
        set_seed(int(args.seed))


def list_tests():
    testcase = reflect_testcase()
    if testcase is None:
        print('Testcase has not been found')
        return

    for name in dir(testcase):
        if name.startswith('test_'):
            print(name)


def main():
    global _WAIT
    global _BREAKPOINT
    global _PORT

    # https://st.yandex-team.ru/MSI-898
    # This removes interpretation of external compiled libraries
    os.environ.pop('Y_PYTHON_SOURCE_ROOT', None)
    os.environ.pop('Y_PYTHON_ENTRY_POINT', None)

    args = parse_args()
    setup_common_args(args)
    if args.list:
        list_tests()
        return

    if args.wait is True:
        _WAIT = True

    if args.dump and os.path.exists(args.dump):
        shutil.rmtree(args.dump)

    TestCase.enable_dump(args.dump, args.seed)

    if args.with_rearr is not None:
        TestCase.set_default_search_flags(args.with_rearr)

    if args.crash_only:
        TestCase.enable_crash_only_mode()
        args.failfast = True

    if args.sanitize is not None:
        TestCase.set_sanitizer_type(args.sanitize)

    if args.port:
        _PORT = int(_PORT)
    if _PORT is None and (args.server or args.breakpoint or args.wait):
        _PORT = report.get_user_port()
    if _PORT:
        os.environ["NO_RANDOM_PORTS"] = "1"

    if args.server is True:
        server_mode()
        return

    verbosity = 2 if args.verbose else 1
    if args.breakpoint is True:
        _BREAKPOINT = True
        verbosity = 2

    loader = LiteTestLoader()
    default_test = 'T.{0}'.format(args.test) if args.test else 'T'
    unittest.TestProgram(
        argv=sys.argv[:1],
        defaultTest=default_test,
        verbosity=verbosity,
        failfast=args.failfast,
        testLoader=loader,
    )
