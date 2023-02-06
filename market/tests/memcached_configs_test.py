# coding: utf-8


import pytest
import yatest.common
# Проверяем корректность текущих конфигов memcached

from market.sre.library.python.maaslib.memcached import MemcachedConfig
from market.sre.library.python.maaslib.memcached_configschema import MEMCACHED_SCHEMA
from market.sre.library.python.maaslib.utils import get_config_pathes


@pytest.fixture(scope='module')
def fixtures_dir():
    return yatest.common.source_path('market/sre/conf/market-cache-config/etc/yandex/memcached/conf-available')


def test_validate_memcached_configs(fixtures_dir):
    pathes = get_config_pathes(fixtures_dir, '*.conf')
    for path in pathes:
        memcached_config = MemcachedConfig(path)
        assert memcached_config.validate(MEMCACHED_SCHEMA)
