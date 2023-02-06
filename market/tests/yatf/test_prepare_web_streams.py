# coding=utf-8
"""
Test is used for checking result of prepare_web_streams call.
prepare_web_streams filters web stream features by domain leaving only beru.ru/market.yandex.ru docs
"""

import pytest

from hamcrest import assert_that, has_item, has_length, has_entries, all_of

from yt.wrapper import ypath_join
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.streams.yatf.test_env import YtPrepareWebStreamsTestEnv
from market.idx.streams.yatf.resources.yt_prepare_web_streams_data import YtPrepareWebStreamsData
from market.idx.streams.yatf.resources.yt_prepare_web_streams_output import YtPrepareWebStreamsOutput


WHITE_CPC_HOST = 'white_cpc.ru'
WHITE_CPC_PATH = '/product/5'
WHITE_CPC_URL_NORMALIZED = WHITE_CPC_HOST + WHITE_CPC_PATH
WHITE_CPC_URL = 'http://www.' + WHITE_CPC_URL_NORMALIZED

WHITE_CPA_HOST = 'white_cpa.ru'
WHITE_CPA_PATH = '/product/6'
WHITE_CPA_URL_NORMALIZED = WHITE_CPA_HOST + WHITE_CPA_PATH
WHITE_CPA_URL = 'http://www.' + WHITE_CPA_URL_NORMALIZED


# input

@pytest.fixture(scope='module', params=[True, False],
                ids=["merge_blue_features_with_white", "no_merge_blue_features_with_white"])
def merge_blue_features_with_white(request):
    return request.param


@pytest.fixture(scope='module')
def offers_dir():
    return ypath_join(get_yt_prefix(), 'offers')


@pytest.fixture(scope='module')
def working_dir():
    return ypath_join(get_yt_prefix(), 'tmp')


@pytest.fixture(scope='module')
def web_streams_table(yt_stuff):
    data = [
        {'key': "http://market.yandex.ru/product/10?sku=1", 'value': '1'},
        {'key': "https://market.yandex.ru/product/10?sku=2&cpa=1", 'value': '2'},
        {'key': "https://market.yandex.ru/product/3", 'value': '3'},
        {'key': "http://market.yandex.ru/product/4", 'value': '4'},
        {'key': WHITE_CPC_URL, 'value': '5'},
        {'key': WHITE_CPA_URL, 'value': '6'},
    ]
    path = ypath_join(get_yt_prefix(), 'web_streams_table')
    return YtTableResource(yt_stuff, path, data)


@pytest.fixture(scope='module')
def offers_table(yt_stuff, offers_dir):
    data = []
    path = ypath_join(offers_dir, '0000')
    return YtTableResource(yt_stuff, path, data)


@pytest.fixture(scope='module')
def hosts_table(yt_stuff, working_dir):
    data = [
        {'host': WHITE_CPC_HOST, 'count': 1, 'is_white_cpa': False},
        {'host': WHITE_CPA_HOST, 'count': 1, 'is_white_cpa': True},
    ]
    path = ypath_join(working_dir, 'hosts')
    return YtTableResource(yt_stuff, path, data)


@pytest.fixture(scope='module')
def input(web_streams_table, offers_table, hosts_table):
    return YtPrepareWebStreamsData(web_streams_table, offers_table, hosts_table, parts_count=1)


# output

@pytest.fixture(scope='module')
def output(working_dir):
    return YtPrepareWebStreamsOutput(working_dir)


# execution

@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input, output, merge_blue_features_with_white):
    resources = {
        "input": input,
        "output": output
    }

    with YtPrepareWebStreamsTestEnv(yt_stuff, **resources) as env:
        env.execute(yt_stuff, merge_blue_features_with_white=merge_blue_features_with_white)
        env.verify()
        yield env


# tests

def test_blue_table_data(workflow):
    expected = [
        {
            'url': 'market.yandex.ru/product/10?sku=1', 'ann_data': '1'
        },
        {
            'url': 'market.yandex.ru/product/10?sku=2', 'ann_data': '2'   # normalized
        },
        {
            'url': WHITE_CPA_URL_NORMALIZED, 'ann_data': '6'
        }
    ]

    assert_that(workflow.beru_filtered_normalized.data, has_length(len(expected)), "Records count is correct")
    assert_that(
        workflow.beru_filtered_normalized.data,
        all_of(*(
            has_item(has_entries(feature))
            for feature in expected
        )),
        "Blue features lost"
    )
    pass


def test_white_table_data(workflow, merge_blue_features_with_white):
    expected = [
        {
            'url': WHITE_CPC_URL_NORMALIZED, 'ann_data': '5'
        },
        {
            'url': WHITE_CPA_URL_NORMALIZED, 'ann_data': '6'
        }
    ]
    if merge_blue_features_with_white:
        expected.extend([
            {
                'url': 'market.yandex.ru/product/10?sku=1', 'ann_data': '1'
            },
            {
                'url': 'market.yandex.ru/product/10?sku=2', 'ann_data': '2'   # normalized
            },
        ])

    assert_that(workflow.web_filtered_normalized.data, has_length(len(expected)), "Records count is correct")
    assert_that(
        workflow.web_filtered_normalized.data,
        all_of(*(
            has_item(has_entries(feature))
            for feature in expected
        )),
        "White features lost"
    )


def test_model_table_data(workflow):
    """Пока что на всякий случай синие фичи для ску останутся и в модельном,
    куда они попадали по ошибке с тех пор, как синие оффера на выдаче получили маркетные урлы
    """
    expected = [
        {
            'url': 'market.yandex.ru/product/10',
            'ann_data': '1'
        },
        {
            'url': 'market.yandex.ru/product/10',
            'ann_data': '2'
        },
        {
            'url': 'market.yandex.ru/product/3',
            'ann_data': '3'
        },
        {
            'url': 'market.yandex.ru/product/4',
            'ann_data': '4'
        }
    ]

    assert_that(workflow.model_filtered_normalized.data, has_length(len(expected)), "Records count is correct")
    assert_that(
        workflow.model_filtered_normalized.data,
        all_of(*(
            has_item(has_entries(feature))
            for feature in expected
        )),
        "Model features lost"
    )
