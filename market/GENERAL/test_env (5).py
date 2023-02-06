# coding: utf-8

import logging
import os
import requests

from market.idx.yatf.common import ignore_errors, get_binary_path
from market.library.common_proxy.yatf.test_envs.test_env import ShinyProxyTestEnv, ServiceConnector
from market.idx.datacamp.controllers.stroller.yatf.resources.shiny_config import StrollerShinyConfig
from market.idx.datacamp.controllers.stroller.yatf.resources.common_proxy_config import StrollerCommonProxyConfig
from market.idx.yatf.resources.mbo_mapping_http_data import MbocHTTPData
from market.idx.yatf.resources.datacamp.datacamp_tables import (
    DataCampPartnersTable,
    DataCampBasicOffersTable,
    DataCampServiceOffersTable,
    DataCampCategoriesTable,
    DataCampPromoTable
)
from market.idx.yatf.resources.lbk_topic import LbkTopic


logger = logging.getLogger()


class StrollerTestEnv(ShinyProxyTestEnv):
    def __init__(self, environment_variables, **resources):
        super(StrollerTestEnv, self).__init__('stroller', StrollerTestEnv.stroller_bin(), environment_variables, **resources)

    @staticmethod
    def stroller_bin():
        return get_binary_path(os.path.join('market', 'idx', 'datacamp', 'controllers', 'stroller', 'bin', 'sync', 'stroller'))

    @property
    def description(self):
        return 'stroller_env'

    def after_environment_init(self):
        ignore_errors(os.makedirs, OSError)(os.path.join(self.output_dir, 'app', 'log', 'stroller'))
        ignore_errors(os.makedirs, OSError)(os.path.join(self.output_dir, 'app', 'log', 'trace'))
        self._environment_variables.update({
            'YT_TOKEN_PATH': self.resources['yt_token'].path,
            'MBOC_HOST': self.resources['mboc'].host,
            'MBOC_PORT': self.resources['mboc'].port,
            'PICROBOT_API_URL': self.resources['picrobot'].url if 'picrobot' in self.resources else '',
        })

        super(StrollerTestEnv, self).after_environment_init()

    @staticmethod
    def make_environment(config, yt_server, log_broker_stuff, local_saas):
        environment = {
            'COLOR': config.color_name,
            'LOG_LEVEL': 7,
            'LOGS_DIR': 'app/log/stroller',
            'SELECT_ROWS_LIMIT': config.select_rows_limit,
            'BASIC_OFFERS_TABLE': config.yt_basic_offers_tablepath,
            'SERVICE_OFFERS_TABLE': config.yt_service_offers_tablepath,
            'ACTUAL_SERVICE_OFFERS_TABLE': config.yt_actual_service_offers_tablepath,
            'PARTNER_STATS_TABLE': config.yt_partners_tablepath,
            'BUSINESS_STATUS_TABLE': config.yt_business_status_tablepath,
            'CATEGORIES_TABLE': config.yt_categories_tablepath,
            'PROMO_DESCRIPTION_TABLE': config.yt_promo_tablepath,
            'CHANGE_BACKUP_DIR': config.yt_backupdir,
            'BLOG_TABLE': '',
            'ENABLE_ESTIMATED_READ_SET_SIZE_ACCOUNTING': True,
            'REPORT_WHITE_OUTPUT_TOPIC_PREFIX': config.report_topic,
            'REPORT_WHITE_ALLOWED_COLORS': 'WHITE;BLUE;LAVKA;EDA',
            'REPORT_WHITE_ALLOWED_SOURCES': '',
            'REPORT_TURBO_ALLOWED_COLORS': 'WHITE;BLUE;DIRECT;DIRECT_SITE_PREVIEW;DIRECT_STANDBY;DIRECT_GOODS_ADS;DIRECT_SEARCH_SNIPPET_GALLERY;VERTICAL_GOODS_ADS',
            'REPORT_TURBO_ALLOWED_SOURCES': '',
            'REPORT_TURBO_OUTPUT_TOPIC_PREFIX': config.report_topic,
            'VERTICAL_APPROVED_THRESHOLD_HOURS': "72h",
            'GOODS_USE_GOODS_SM_MAPPING': False,
            'PROMO_LOYALTY_OUTPUT_TOPIC': config.loyalty_promos_topic,
            'SPECIAL_PARTNERS': '',
            'YT_TABLET_CELL_BUNDLE': '',
            'MAX_UNITED_OFFERS_INFLIGHT': 50,
            'YT_META_PROXY': yt_server.get_server(),
            'LOG_BROKER_HOST': log_broker_stuff.host,
            'LOG_BROKER_PORT': log_broker_stuff.port,
            'LOG_BROKER_SOURCE_ID_PREFIX': '',
            'LOG_BROKER_OUTPUT_OFFERS_TOPIC_WRITERS_COUNT': 1,
            'LOG_BROKER_OUTPUT_REPORT_TOPIC_WRITERS_COUNT': 1,
            'LOG_BROKER_OUTPUT_PROMO_LOYALTY_SENDER_WRITERS_COUNT': 1,
            'ROUTINES_INPUT_TOPIC': config.united_offers_topic,
            'DATACAMP_TVM_SECRET': '',
            'DATACAMP_TVM_CLIENT_ID': config.tvm_client_id,
            'SCHEMA_SWITCHING_MAX_HOURS': config.schema_switching_max_hours,
            'PICROBOT_NAMESPACE': config.picrobot_namespace,
            'PICROBOT_VIDEO_NAMESPACE': config.picrobot_video_namespace,
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
            'CURRENCY_RATES_FILEPATH': config.currency_rates_file_path,
            'REFRESH_CURRENCY_RATES_FILE_IN_MINUTES': config.refresh_file_minutes,
            'MBOC_SEND_CONSISTENT_OFFERS_ONLY': 'true',
            'DC': 'sas',
            'YT_PRIMARY_PROXY': yt_server.get_server(),
            'YT_RESERVE_PROXY': yt_server.get_server(),
            'FORCE_RELAXED': config.force_relaxed,
            'BASIC_SEARCH_OFFERS_TABLE': config.yt_basic_search_offers_tablepath,
            'SERVICE_SEARCH_OFFERS_TABLE': config.yt_service_search_offers_tablepath,
            'ACTUAL_SERVICE_SEARCH_OFFERS_TABLE': config.yt_actual_service_search_offers_tablepath,
            'ENABLE_SEARCH_TABLES_SELECT': config.enable_search_tables_select,
            'ENABLE_PICTURE_VERDICTS': True,
            'QUOTER_PORT': 80,
            'ENABLE_SUBSCRIPTION_SERVICE_SENDER': config.enable_subscription_service_sender,
            'SUBSCRIPTION_SERVICE_TOPIC': config.subscription_service_topic,
            'SUBSCRIPTION_SERVICE_SEND_AFTER_COMMIT': True,
            'SEND_TO_SAAS_VIA_DISPATCHER': config.enable_subscription_dispatcher,
            'ENABLE_DISPATCHER_IN_STROLLER_FOR_TEST': config.enable_subscription_dispatcher,
            'DEGRADATION_RECOVERY_INTERVAL': '10m',
            'ENABLE_QUARANTINE_FOR_PRICE': True,
            'CONSUMING_SYSTEM': '',
            'YDB_END_POINT': '',
            'YDB_PATH': '',
            'YDB_COORDINATION_NODE_PATH': '',
            'YDB_TOKEN_PATH': '',
            'BLOCKING_SEMAPHORE_NAME': '',
            'PUBLISHING_SEMAPHORE_NAME': '',
            'PUBLISH_YT_DOWNTIME_STATUS_IN_YDB': 'false',
            'USE_SEPARATE_THREAD_TO_KEEP_SESSION_ALIVE': 'false',
            'REPLICA_MONITOR_REFRESH_PERIOD': '1m'
        }

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

        if hasattr(config, 'migrator_tvm_id'):
            environment.update({
                'TVM_CLIENT_ID_MIGRATOR': config.migrator_tvm_id,
            })

        return environment

    def _get_processed_count(self, processor):
        url = 'http://localhost:{port}?command=get_info_server'.format(port=self.controller_port)
        result = requests.get(url).json()
        try:
            return int(result['result']['processors'][processor]['offers_count_count'])
        except KeyError as e:
            print(e)
            return 0

    @property
    def united_offers_processed(self):
        return self._get_processed_count('UnitedOffersUpdater')

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

    @property
    def categories_table(self):
        self.resources['categories_table'].load()
        return self.resources['categories_table']

    @property
    def promo_table(self):
        self.resources['promo_table'].load()
        return self.resources['promo_table']

    @property
    def business_status_table(self):
        self.resources['business_status_table'].load()
        return self.resources['business_status_table']


def produce_connector(host="localhost", port=80, stroller=None):
    if stroller is not None:
        return stroller.connector
    else:
        return ServiceConnector(host, port)


def make_stroller(
    config,
    yt_server,
    log_broker_stuff,
    local_saas=None,
    shopsdat_cacher=False,
    **kwargs
):
    custom_configs = []
    if 'report_topic' not in kwargs:
        custom_configs.extend([
            'united_report_sender_processors.cfg',
            'united_report_sender_links.cfg'
        ])
    if 'loyalty_promos_topic' not in kwargs:
        custom_configs.extend([
            'promo_sender.cfg',
            'promo_sender_links.cfg'
        ])

    if not shopsdat_cacher:
        custom_configs.extend(['shopsdat_cacher.cfg'])

    resources = {
        'config': config,
        'partners_table': kwargs.get('partners_table', DataCampPartnersTable(
            yt_server,
            config.yt_partners_tablepath
        )),
        'basic_offers_table': kwargs.get('basic_offers_table', DataCampBasicOffersTable(
            yt_server,
            config.yt_basic_offers_tablepath
        )),
        'service_offers_table': kwargs.get('service_offers_table', DataCampServiceOffersTable(
            yt_server,
            config.yt_service_offers_tablepath
        )),
        'actual_service_offers_table': kwargs.get('actual_service_offers_table', DataCampServiceOffersTable(
            yt_server,
            config.yt_actual_service_offers_tablepath
        )),
        'promo_table': kwargs.get('promo_table', DataCampPromoTable(
            yt_server,
            config.yt_promo_tablepath
        )),
        'categories_table': DataCampCategoriesTable(yt_server, config.yt_categories_tablepath),
        'united_offers_topic': kwargs.get(
            'united_offers_topic',
            LbkTopic(log_broker_stuff, topic=config.united_offers_topic)
        ),
        'stroller_config': StrollerCommonProxyConfig(custom_configs=custom_configs),
        'stroller_shiny_config': kwargs.get('shiny_config', StrollerShinyConfig()),
        'mboc': MbocHTTPData(None),
    }
    resources.update(kwargs)

    environment = StrollerTestEnv.make_environment(
        config,
        yt_server,
        log_broker_stuff,
        local_saas,
    )
    return StrollerTestEnv(environment, **resources)
