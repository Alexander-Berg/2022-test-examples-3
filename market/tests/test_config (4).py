# -*- coding: utf-8 -*-

import os
from hamcrest import (
    assert_that,
    contains,
)

from utils import (
    reset_config_env_variables,
    set_config_env_variables,
    COMMON_PATH,
    LOCAL_PATH
)
import market.idx.api.backend.config as cfg


def setup():
    reset_config_env_variables()


def test_files_exists():
    paths = [
        COMMON_PATH,
        LOCAL_PATH
    ]
    for p in paths:
        assert os.path.exists(p)


def test_get_config_path_default():
    paths = cfg.get_config_paths()

    assert_that(
        (paths[0], paths[1]),
        contains(
            '/etc/yandex/idxapi/common.ini',
            '/etc/yandex/idxapi/local.ini'
        )
    )


def test_get_config_path_environment():
    set_config_env_variables()

    paths = cfg.get_config_paths()

    assert_that((paths[0], paths[1]), contains(COMMON_PATH, LOCAL_PATH))


def test_plain_config_with_environment_common():
    os.environ['IDXAPI_CONFIG_PATH'] = COMMON_PATH
    os.environ['IDXAPI_LOCAL_CONFIG_PATH'] = COMMON_PATH

    config = cfg.build_config()
    assert config
    assert config.get('hbase.hostname') == '127.0.0.1'
    assert config.get('hbase.port') == '9090'
    assert config.get('hbase.table_feeds') == 'marketindexer-feeds'
    assert config.get('hbase.table_sessions') == 'marketindexer-sessions'
    assert config.get('hbase.table_offers') == 'marketindexer-offers'
    assert config.get('hbase.table_promos') == 'marketindexer-promos'
    assert config.get('hbase.table_qindex') == 'marketindexer-qindex'
    assert config.get('hbase.table_qsessions') == 'marketindexer-qsessions'


def test_plain_config_with_paths_common():
    paths = [
        COMMON_PATH,
        COMMON_PATH
    ]
    config = cfg.build_config(paths=paths)

    assert config
    assert config.get('hbase.hostname') == '127.0.0.1'
    assert config.get('hbase.port') == '9090'
    assert config.get('hbase.table_feeds') == 'marketindexer-feeds'
    assert config.get('hbase.table_sessions') == 'marketindexer-sessions'
    assert config.get('hbase.table_offers') == 'marketindexer-offers'
    assert config.get('hbase.table_promos') == 'marketindexer-promos'
    assert config.get('hbase.table_qindex') == 'marketindexer-qindex'
    assert config.get('hbase.table_qsessions') == 'marketindexer-qsessions'


def test_plain_config_with_environment_local():
    set_config_env_variables()

    config = cfg.build_config()
    assert config
    assert config.get('hbase.hostname') == '127.0.0.1'
    assert config.get('hbase.port') == '9090'
    assert config.get('hbase.table_feeds') == 'marketindexer-ps-feeds'
    assert config.get('hbase.table_sessions') == 'marketindexer-ps-sessions'
    assert config.get('hbase.table_offers') == 'marketindexer-ps-offers'
    assert config.get('hbase.table_promos') == 'marketindexer-ps-promos'
    assert config.get('hbase.table_qindex') == 'marketindexer-ps-qindex'
    assert config.get('hbase.table_qsessions') == 'marketindexer-ps-qsessions'


def test_plain_config_with_paths_local():
    paths = [
        COMMON_PATH,
        LOCAL_PATH
    ]
    config = cfg.build_config(paths=paths)

    assert config
    assert config.get('hbase.hostname') == '127.0.0.1'
    assert config.get('hbase.port') == '9090'
    assert config.get('hbase.table_feeds') == 'marketindexer-ps-feeds'
    assert config.get('hbase.table_sessions') == 'marketindexer-ps-sessions'
    assert config.get('hbase.table_offers') == 'marketindexer-ps-offers'
    assert config.get('hbase.table_promos') == 'marketindexer-ps-promos'
    assert config.get('hbase.table_qindex') == 'marketindexer-ps-qindex'
    assert config.get('hbase.table_qsessions') == 'marketindexer-ps-qsessions'
