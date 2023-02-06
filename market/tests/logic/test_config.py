# coding: utf-8

import os
import pytest
from hamcrest import assert_that, not_none

import yatest.common

from market.idx.datacamp.parser.lib.config import PushParserConfig

from market.idx.datacamp.yatf.resources.tokens import TvmSecretStub, YtTokenStub

COMMON = os.path.join(yatest.common.source_path(), 'market', 'idx', 'datacamp', 'parser', 'etc', 'common-deploy.ini')
TESTING = os.path.join(yatest.common.source_path(), 'market', 'idx', 'datacamp', 'parser', 'etc', 'conf-available', 'testing.parser.ini')
TESTING_CHECKFEED = os.path.join(yatest.common.source_path(), 'market', 'idx', 'datacamp', 'parser', 'etc', 'conf-available', 'testing.checkfeed.ini')
PROD = os.path.join(yatest.common.source_path(), 'market', 'idx', 'datacamp', 'parser', 'etc', 'conf-available', 'production.parser.ini')
PROD_CHECKFEED = os.path.join(yatest.common.source_path(), 'market', 'idx', 'datacamp', 'parser', 'etc', 'conf-available', 'production.checkfeed.ini')


@pytest.mark.parametrize('env_file', [
    TESTING,
    TESTING_CHECKFEED,
    PROD,
    PROD_CHECKFEED,
], ids=[
    'testing.parser',
    'testing.checkfeed',
    'production.parser',
    'production.checkfeed'
])
def test_parse_config(env_file):
    """
    Проверяем, что конфиг парсера парсится без ошибок
    """
    YtTokenStub(yatest.common.work_path('app/secrets/yt-market-fp'))
    TvmSecretStub(yatest.common.work_path('app/secrets/tvm'))

    config = PushParserConfig([COMMON, env_file], 'sas')
    assert_that(config, not_none())

    # проверяем, что основная и резервная yt-прокси правильно проинициализировались
    assert_that(config.has_option('yt', 'primary_proxy'))
    assert_that(config.get('yt', 'primary_proxy') == 'hahn.yt.yandex.net')
    assert_that(config.has_option('yt', 'reserve_proxy'))
    assert_that(config.get('yt', 'reserve_proxy') == 'arnold.yt.yandex.net')
