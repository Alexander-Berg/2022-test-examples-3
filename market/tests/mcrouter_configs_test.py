# coding: utf-8


import re
import pytest
import yatest.common
import yaml

from market.sre.library.python.maaslib.mcrouter_configschema import MCROUTER_YAML_SCHEMA
from market.sre.library.python.maaslib.utils import get_config_pathes


REGEXP = re.compile(r'^(\d+)-[\w\d_-]+?\.([\w_-]+)\.yaml$')


@pytest.fixture(scope='module')
def fixtures_dir():
    return yatest.common.source_path('market/sre/conf/market-cache-config/etc/yandex/mcrouter/values-available')


def test_validate_memcached_configs(fixtures_dir):
    pathes = get_config_pathes(fixtures_dir, '*.yaml')

    for path in pathes:

        config_name = path.split('/')[-1]

        if not REGEXP.match(config_name):
            assert False, 'The config {} has wrong format of filename.'.format(config_name) +\
                'Please, use such format: <port>-<component>.<cond_group>.yaml'

        with open(path, 'r') as yaml_config:
            config = yaml.safe_load(yaml_config)
            assert MCROUTER_YAML_SCHEMA.validate(config)
