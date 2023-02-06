# coding: utf-8
import os
import pytest
from hamcrest import assert_that, equal_to, not_none

from market.idx.datacamp.routines.lib.config import DatacampRoutinesConfig
from market.idx.datacamp.yatf.resources.tokens import YtTokenStub, TvmSecretStub

import yatest.common


COMMON = os.path.join(
    yatest.common.source_path(),
    'market', 'idx', 'datacamp', 'routines', 'etc', 'common.ini'
)
TESTING_WHITE = os.path.join(
    yatest.common.source_path(),
    'market', 'idx', 'datacamp', 'routines', 'etc', 'conf-available', 'testing.white.ini'
)
PROD_WHITE = os.path.join(
    yatest.common.source_path(),
    'market', 'idx', 'datacamp', 'routines', 'etc', 'conf-available', 'production.white.ini'
)


@pytest.mark.parametrize(
    'env, token_path, tvm_path',
    [
        (
            TESTING_WHITE,
            'yt-market-indexer',
            'tvm-token-datacamp-white-testing',
        ),
        (
            PROD_WHITE,
            'yt-market-indexer',
            'tvm-token-datacamp-white-stable',
        ),
    ],
    ids=[
        'TESTING_WHITE',
        'PROD_WHITE',
    ]
)
def test_parse_config(env, token_path, tvm_path):
    """
    Проверяем, что конфиги routines парсится без ошибок
    """
    YtTokenStub(yatest.common.work_path('properties.d/{}'.format(token_path)))
    TvmSecretStub(yatest.common.work_path('properties.d/{}'.format(tvm_path)))

    config = DatacampRoutinesConfig([COMMON, env], 'sas')
    assert_that(config, not_none())


@pytest.mark.parametrize(
    'env, token_path, tvm_path, expected_yt_pool',
    [
        (
            TESTING_WHITE,
            'yt-market-indexer',
            'tvm-token-datacamp-white-testing',
            'market-testing-datacamp-batch',
        ),
        (
            PROD_WHITE,
            'yt-market-indexer',
            'tvm-token-datacamp-white-stable',
            'market-production-datacamp-batch',
        ),
    ],
    ids=[
        'TESTING_WHITE',
        'PROD_WHITE',
    ]
)
def test_yt_pool_in_config(env, token_path, tvm_path, expected_yt_pool):
    """
    Проверяем пробрасывание yt_pool в конфигах
    """
    YtTokenStub(yatest.common.work_path('properties.d/{}'.format(token_path)))
    TvmSecretStub(yatest.common.work_path('properties.d/{}'.format(tvm_path)))

    config = DatacampRoutinesConfig([COMMON, env], 'sas')
    assert_that(config.yt_pool, equal_to(expected_yt_pool))
