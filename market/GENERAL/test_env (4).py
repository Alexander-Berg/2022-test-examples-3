# coding: utf-8

import logging
import os
import requests
import uuid
import yatest

from market.idx.yatf.common import ignore_errors, get_binary_path
from market.library.common_proxy.yatf.test_envs.test_env import ShinyProxyTestEnv
from market.idx.datacamp.controllers.scanner.yatf.resources.shiny_config import ScannerShinyConfig
from market.idx.datacamp.controllers.scanner.yatf.resources.common_proxy_config import ScannerCommonProxyConfig


logger = logging.getLogger()


class ScannerTestEnv(ShinyProxyTestEnv):
    def __init__(self, environment_variables, **resources):
        scanner_bin = get_binary_path(
            os.path.join('market', 'idx', 'datacamp', 'controllers', 'scanner', 'bin', 'scanner'))
        super(ScannerTestEnv, self).__init__('scanner', scanner_bin, environment_variables, **resources)

    def after_environment_init(self):
        ignore_errors(os.makedirs, OSError)(os.path.join(self.output_dir, 'app', 'log', 'scanner'))
        ignore_errors(os.makedirs, OSError)(os.path.join(self.output_dir, 'app', 'log', 'trace'))
        self._environment_variables.update({
            'YT_TOKEN_PATH': self.resources['yt_token'].path,
        })

        super(ScannerTestEnv, self).after_environment_init()

    def _get_processed_count(self, processor):
        url = 'http://localhost:{port}?command=get_info_server'.format(port=self.controller_port)
        result = requests.get(url).json()
        try:
            return int(result['result']['processors'][processor]['offers_count_count'])
        except KeyError as e:
            print(e)
            return 0

    @property
    def description(self):
        return 'scaner_env'

    @property
    def united_offers_processed(self):
        return self._get_processed_count('UnitedOffersUpdater')

    @property
    def mskus_processed(self):
        return self._get_processed_count('MskuUpdater')

    @property
    def basic_offers_table(self):
        self.resources['basic_offers_table'].load()
        return self.resources['basic_offers_table']

    @property
    def service_offers_table(self):
        self.resources['service_offers_table'].load()
        return self.resources['service_offers_table']

    @property
    def actual_service_offers_table(self):
        self.resources['actual_service_offers_table'].load()
        return self.resources['actual_service_offers_table']

    @staticmethod
    def make_environment(yt_server, color, log_broker_stuff, local_saas, subscription_service_topic, resources):
        environment = {
            'LOGS_DIR': 'app/log/scanner',
            'COLOR': color,
            'LOG_LEVEL': 7,
            'PROXY': yt_server.get_server(),
            'BASIC_OFFERS_TABLE': resources['basic_offers_table'].get_path(),
            'SERVICE_OFFERS_TABLE': resources['service_offers_table'].get_path(),
            'ACTUAL_SERVICE_OFFERS_TABLE': resources['actual_service_offers_table'].get_path(),
            'CHANGE_BACKUP_DIR': '',
            'BLOG_TABLE': '',
            'CATEGORIES_TABLE': '',
            'BUSINESS_STATUS_TABLE': '',
            'MAX_UNITED_OFFERS_INFLIGHT': 50,
            'ENABLE_ESTIMATED_READ_SET_SIZE_ACCOUNTING': True,
            'DATACAMP_MSKU_TABLE': resources['datacamp_msku_table'].get_path(),
            'PROMO_TABLE': resources['promo_table'].get_path(),
            'ENABLE_VERTICAL_APPROVED': True,
            'VERTICAL_APPROVED_TABLE': resources['vertical_approved_table'].get_path(),
            'VERTICAL_APPROVED_STATE_TABLE': '//home/vertical_approved_state' + str(uuid.uuid4()),
            'VERTICAL_APPROVED_TABLE_READER_BATCH_SIZE': 200,
            'VERTICAL_APPROVED_TABLE_READER_NUM_SHARDS': 4,
            'ENABLE_OFFERS_DIFF_READER': True,
            'OFFERS_DIFF_TABLE': resources['offers_diff_table'].get_path(),
            'OFFERS_DIFF_STATE_PATH': '//home/offers_diff_state' + str(uuid.uuid4()),
            'PIPER_QUEUE_DUMP_TABLE': resources['piper_queue_dump_table'].get_path(),
            'PIPER_QUEUE_DUMP_STATE_PATH': '//home/piper_queue_dump_state' + str(uuid.uuid4()),
            'PROMO_STATE_PATH': '//home/promo_state' + str(uuid.uuid4()),
            'PROMO_WHITE_TABLE': resources['promo_table'].get_path(),
            'PROMO_WHITE_STATE_PATH': '//home/promo_state' + str(uuid.uuid4()),
            'ENABLE_ABO_HIDINGS_READER': True,
            'ABO_HIDING_TABLE': resources['abo_hiding_table'].get_path(),
            'ABO_HIDING_STATE_PATH': '//home/abo_hiding_state' + str(uuid.uuid4()),
            'DATACAMP_MSKU_TABLE_LOCK': '//home/msku_state_lock' + str(uuid.uuid4()),
            'MBO_MSKU_TABLE': resources['mbo_msku_table'].get_path(),
            'MBO_HIDING_TABLE': resources['mbo_hiding_table'].get_path(),
            'MBO_HIDING_STATE_PATH': '//home/mbo_hiding_state' + str(uuid.uuid4()),
            'STOCK_SKU_TABLE': resources['stock_sku_table'].get_path(),
            'STOCK_SKU_STATE_PATH': '//home/stock_sku_state' + str(uuid.uuid4()),
            'STOCK_TIMESTAMP_FROM_TABLE_NAME': 'false',
            'STOCK_CHECK_ALL_PROXIES': 'true',
            'ENABLE_DELIVERY_DIFF_READER': True,
            'DELIVERY_DIFF_TABLE': resources['delivery_diff_table'].get_path(),
            'DELIVERY_DIFF_LOCK_PATH': '//home/delivery_diff_lock' + str(uuid.uuid4()),
            'YT_TABLET_CELL_BUNDLE': 'default',
            'YT_MR_TABLET_CELL_BUNDLE': 'default',
            'DATACAMP_TVM_SECRET': '',
            'DATACAMP_TVM_CLIENT_ID': 0,
            'YT_META_PROXY': yt_server.get_server(),
            'YT_PRIMARY_PROXY': yt_server.get_server(),
            'YT_RESERVE_PROXY': yt_server.get_server(),
            'YT_PROXY': yt_server.get_server(),
            'YT_PROXY_MAN': '',
            'YT_PROXY_SAS': '',
            'YT_PROXY_VLA': '',
            'YT_PROMO_PROXY': yt_server.get_server(),
            'YT_PROMO_WHITE_PROXY': yt_server.get_server(),
            'YT_TOKEN_PATH': '',
            'LOG_BROKER_OUTPUT_REPORT_TOPIC_WRITERS_COUNT': 1,
            'REPORT_WHITE_OUTPUT_TOPIC_PREFIX': 'rty-white',
            'REPORT_WHITE_ALLOWED_COLORS': 'WHITE;BLUE;LAVKA;EDA',
            'REPORT_WHITE_ALLOWED_SOURCES': '',
            'REPORT_TURBO_OUTPUT_TOPIC_PREFIX': 'rty-turbo',
            'REPORT_TURBO_ALLOWED_COLORS': 'WHITE;BLUE;DIRECT;DIRECT_SITE_PREVIEW;DIRECT_STANDBY;DIRECT_GOODS_ADS;DIRECT_SEARCH_SNIPPET_GALLERY;VERTICAL_GOODS_ADS',
            'REPORT_TURBO_ALLOWED_SOURCES': '',
            'VERTICAL_APPROVED_THRESHOLD_HOURS': "72h",
            'LOG_BROKER_SOURCE_ID_PREFIX': '',
            'LOG_BROKER_HOST': log_broker_stuff.host,
            'LOG_BROKER_PORT': log_broker_stuff.port,
            'DC': 'sas',
            'CURRENCY_RATES_FILEPATH': os.path.join(yatest.common.source_path(), 'market', 'idx', 'yatf', 'resources', 'stubs', 'getter', 'mbi', 'currency_rates.xml'),
            'REFRESH_CURRENCY_RATES_FILE_IN_MINUTES': 1,
            'PARTNER_STATS_TABLE': resources['partners_table'].get_path(),
            'OFFER_STATUS_TABLE': resources['offer_status_table'].get_path(),
            'OFFER_STATUS_BATCHED_TABLE': resources['offer_status_table_batched'].get_path(),
            'OFFER_STATUS_STATE_PATH': '//home/offer_status_state' + str(uuid.uuid4()),
            'OFFER_STATUS_BATCHED_STATE_PATH': '//home/offer_status_batched_state' + str(uuid.uuid4()),
            'ENABLE_OFFER_STATUS_READER': 'true',
            'ENABLE_OFFER_STATUS_BATCHED_READER': 'true',
            'FRESH_OFFER_STATUS_BATCHED_STATE_PATH': '//home/fresh_offer_status_batched_state' + str(uuid.uuid4()),
            'FRESH_OFFER_STATUS_BATCHED_TABLE': resources['fresh_offer_status_table_batched'].get_path(),
            'ENABLE_FRESH_OFFER_STATUS_BATCHED_READER': 'true',
            'SAAS_DIFF_TABLE': resources['saas_diff_table'].get_path(),
            'SAAS_DIFF_STATE_PATH': '//home/saas_diff_state' + str(uuid.uuid4()),
            'ENABLE_SAAS_DIFF_READER': 'true',
            'RESOLVED_REDIRECT_TABLE': resources['resolved_redirect_table'].get_path(),
            'RESOLVED_REDIRECT_STATE_PATH': '//home/resolved_redirect_state' + str(uuid.uuid4()),
            'ENABLE_RESOLVED_REDIRECT_READER': 'true',
            'TABLE_READER_WAIT_BEFORE_EXECUTE': 'true',
            'ENABLE_MBO_MSKU_TABLE_READER': True,
            'ENABLE_MSKU_CHANGES_SEND_TO_IRIS_INTERNAL_TOPIC': True,
            'IRIS_SUBSCRIPTION_CONTROLLERS_TO_MINER_TOPIC': resources['iris_subscription_internal_topic'].topic,
            'IRIS_SUBSCRIPTION_CONTROLLERS_TO_MINER_TOPIC_WRITERS_COUNT': 1,
            'ENABLE_CARGO_TYPE_TO_DATACAMP_SUBSCRIPTION': True,
            'ENABLE_PARAM_GLOBALISATION_OFFERS': True,
            'PARAM_GLOBALISATION_OFFERS_TABLE': '//home/mbo_params_globalization',
            'PARAM_GLOBALISATION_OFFERS_LOCKPATH': '//home/mbo_params_globalization_lock',
            'FRESH_WHITE_OFFERS_TABLE': '//home/fresh/white',
            'FRESH_BLUE_OFFERS_TABLE': '//home/fresh/blue',
            'FRESH_OFFERS_ACCEPTED_SOURCES': '',
            'FRESH_OFFERS_NOT_ACCEPTED_SOURCES': '',
            'ENABLE_LAVKA_OFFERS': True,
            'LAVKA_BASIC_OFFERS_TABLE': resources['lavka_basic_offers_table'].get_path(),
            'LAVKA_BASIC_OFFERS_STATE': '//home/lavka_basic_offers_state' + str(uuid.uuid4()),
            'LAVKA_SERVICE_OFFERS_TABLE': resources['lavka_service_offers_table'].get_path(),
            'LAVKA_SERVICE_OFFERS_STATE': '//home/lavka_service_offers_state' + str(uuid.uuid4()),
            'ENABLE_EDA_OFFERS': True,
            'EDA_BASIC_OFFERS_TABLE': resources['eda_basic_offers_table'].get_path(),
            'EDA_BASIC_OFFERS_STATE': '//home/eda_basic_offers_state' + str(uuid.uuid4()),
            'EDA_SERVICE_OFFERS_TABLE': resources['eda_service_offers_table'].get_path(),
            'EDA_SERVICE_OFFERS_STATE': '//home/eda_service_offers_state' + str(uuid.uuid4()),
            'PICROBOT_NAMESPACE': 'marketpic',
            'PICROBOT_VIDEO_NAMESPACE': 'direct',
            'PICROBOT_DIRECT_BANNERLAND_NAMESPACE': 'mrkt_idx_direct_test',
            'MBOC_SEND_CONSISTENT_OFFERS_ONLY': 'true',
            'BASIC_SEARCH_OFFERS_TABLE': '',
            'SERVICE_SEARCH_OFFERS_TABLE': '',
            'ACTUAL_SERVICE_SEARCH_OFFERS_TABLE': '',
            'ENABLE_SORTDC_UPDATES': True,
            'SORTDC_UPDATES_TABLE': resources['sortdc_updates_table'].get_path(),
            'SORTDC_UPDATES_LOCKPATH': '//home/sortdc_updates_table' + str(uuid.uuid4()),
            'SORTDC_UPDATES_TABLE_READER_BATCH_SIZE': 200,
            'SORTDC_UPDATES_TABLE_READER_NUM_SHARDS': 4,
            'SAAS_FOR_SHOPS_SEND_TYPE': 'proto_debug',
            'SAAS_FOR_SHOPS_SERVICE_NAME': 'market_datacamp_test',
            'SAAS_FOR_SHOPS_SERVICE_CTYPE': 'testing',
            'SAAS_FOR_SHOPS_LOGBROKER_SERVER': log_broker_stuff.host,
            'SAAS_FOR_SHOPS_LOGBROKER_TVM_CLIENT_ID': 0,
            'SAAS_FOR_SHOPS_TOPICS_PREFIX': '',
            'UNITED_SAAS_LOGBROKER_SERVER': log_broker_stuff.host,
            'UNITED_SAAS_LOGBROKER_TVM_CLIENT_ID': 0,
            'UNITED_SAAS_SEND_TYPE': 'proto_debug',
            'UNITED_SAAS_SERVICE_NAME': 'market_datacamp',
            'UNITED_SAAS_SERVICE_CTYPE': 'testing',
            'UNITED_SAAS_TOPICS_PREFIX': '',
            'QUOTER_PORT': 80,
            'DEGRADATION_RECOVERY_INTERVAL': '10m',
            'CONSUMING_SYSTEM': '',
            'REPLICA_MONITOR_REFRESH_PERIOD': '1m'
        }

        if subscription_service_topic:
            environment.update({
                'ENABLE_SUBSCRIPTION_SERVICE_SENDER': 'true',
                'SUBSCRIPTION_SERVICE_TOPIC': subscription_service_topic.topic,
            })

        if local_saas:
            environment.update({
                'SAAS_FOR_SHOPS_SEND_TYPE': 'json_ref',
                'SAAS_FOR_SHOPS_SERVICE_HASH': local_saas.get_service_hash('json_ref'),
                'SAAS_INDEXERPROXY_HOST': 'localhost',
                'SAAS_INDEXERPROXY_PORT': local_saas.indexer_port,
                'UNITED_SAAS_SEND_TYPE': 'json_ref',
                'UNITED_SAAS_SERVICE_HASH': local_saas.get_service_hash('json_ref'),
                'UNITED_SAAS_INDEXERPROXY_HOST': 'localhost',
                'UNITED_SAAS_INDEXERPROXY_PORT': local_saas.indexer_port,
                'SAAS_SEARCH_ENABLED': 'true',
                'SAAS_SEARCHPROXY_HOST': 'localhost',
                'SAAS_SEARCHPROXY_PORT': local_saas.search_port,
                'SAAS_SEARCH_SERVICE': local_saas.service_name,
                'SAAS_SEARCH_USE_TVM_AUTH': 'false',
            })

        return environment


def make_scanner(
    yt_server,
    log_broker_stuff,
    color,
    local_saas=None,
    shopsdat_cacher=None,
    test_configs=None,
    subscription_service_topic=None,
    **kwargs
):
    custom_configs = []
    if not shopsdat_cacher:
        custom_configs.extend(['shopsdat_cacher.cfg'])

    resources = {
        'scanner_config': ScannerCommonProxyConfig(
            custom_configs=custom_configs,
            test_configs=test_configs,
        ),
        'scanner_shiny_config': ScannerShinyConfig(),
    }
    resources.update(kwargs)

    return ScannerTestEnv(
        ScannerTestEnv.make_environment(
            yt_server,
            color,
            log_broker_stuff,
            local_saas,
            subscription_service_topic,
            resources
        ),
        **resources
    )
