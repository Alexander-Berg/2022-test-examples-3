# coding=utf-8
"""
Test is used for checking result of prepare_web_streams --merge-web call.
prepare_web_streams --merge-web merges filtered web stream features & market docs
(blue/white offers or models) either by path or url """

import pytest

from hamcrest import assert_that, equal_to, has_items, has_length

from yt.wrapper import ypath_join
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.streams.yatf.test_env import YtPrepareWebStreamsMergeTestEnv
from market.idx.streams.yatf.resources.yt_prepare_web_streams_data import YtPrepareWebStreamsMergeData
from market.idx.streams.yatf.resources.yt_prepare_web_streams_output import YtPrepareWebStreamsMergeOutput


@pytest.fixture(scope='module')
def working_dir():
    return ypath_join(get_yt_prefix(), 'tmp')


@pytest.fixture(scope='module')
def web_filtered_normalized(yt_stuff, working_dir):
    data = [
        {'url': "market.yandex.ru/product/10?sku=1", 'ann_data': '1'},
        {'url': "market.yandex.ru/product/10?sku=2", 'ann_data': '2'},

        {'url': "white_url.ru/offer1", 'ann_data': 'white1'},
        {'url': "white_url.ru/offer2", 'ann_data': 'white2'},
    ]
    path = ypath_join(working_dir, 'web_filtered_normalized')
    return YtTableResource(yt_stuff, path, data)


@pytest.fixture(scope='module')
def market_joined_normalized(yt_stuff, working_dir):
    data = [
        {'ware_md5': 'offer1', 'url': "white_url.ru/offer1", 'url_not_normalized': "white_url.ru/offer1", 'part': 0, 'msku': 1, 'is_fake_msku_offer': False},
        {'ware_md5': 'offer2', 'url': "white_url.ru/offer2", 'url_not_normalized': "white_url.ru/offer2", 'part': 0, 'msku': 0, 'is_fake_msku_offer': False},
    ]

    attributes = dict(
        schema=[
            dict(name="ware_md5", type="string"),
            dict(name="url", type="string"),
            dict(name="url_not_normalized", type="string"),
            dict(name="part", type="uint64"),
            dict(name="msku", type="int64"),
            dict(name="is_fake_msku_offer", type="boolean"),
        ]
    )
    path = ypath_join(working_dir, 'market_joined_normalized')
    return YtTableResource(yt_stuff, path, data=data, attributes=attributes)


@pytest.fixture(scope='module')
def blue_offers_urls(yt_stuff, working_dir):
    data = [
        {'url': "market.yandex.ru/product/10?sku=1", 'url_not_normalized': "market.yandex.ru/product/10?sku=1", 'path': '/product/10?sku=1',
         'part': 0, 'msku': 1, 'is_fake_msku_offer': False, 'ware_md5': 'blue_offer1'},
        {'url': "market.yandex.ru/product/10?sku=2", 'url_not_normalized': "market.yandex.ru/product/10?sku=2",
         'path': '/product/10?sku=2', 'part': 0, 'msku': 2, 'is_fake_msku_offer': False, 'ware_md5': 'blue_offer2'},
        {'url': "market.yandex.ru/product/10?sku=1", 'url_not_normalized': "market.yandex.ru/product/10?sku=1",
         'path': '/product/10?sku=1', 'part': 0, 'msku': 1, 'is_fake_msku_offer': True, 'ware_md5': 'msku1'},
    ]

    attributes = dict(
        schema=[
            dict(name="ware_md5", type="string"),
            dict(name="url", type="string"),
            dict(name="url_not_normalized", type="string"),
            dict(name="path", type="string"),
            dict(name="part", type="uint64"),
            dict(name="msku", type="int64"),
            dict(name="is_fake_msku_offer", type="boolean"),
        ]
    )
    path = ypath_join(working_dir, 'sharded_blue_urls_as_in_white')
    return YtTableResource(yt_stuff, path, data=data, attributes=attributes)


@pytest.fixture(scope='module')
def input(working_dir, web_filtered_normalized, market_joined_normalized, blue_offers_urls):
    return YtPrepareWebStreamsMergeData(working_dir, web_filtered_normalized, blue_offers_urls, market_joined_normalized)


# output

@pytest.fixture(scope='module')
def output(working_dir):
    results_path = ypath_join(get_yt_prefix(), 'ann_data')
    return YtPrepareWebStreamsMergeOutput(working_dir, results_path)


# execution

@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input, output):
    resources = {
        "input": input,
        "output": output
    }

    with YtPrepareWebStreamsMergeTestEnv(yt_stuff, **resources) as env:
        env.execute(yt_stuff)
        env.verify()
        yield env


# tests

def test_result_table_count(workflow, input):
    assert_that(len(workflow.result_tables), equal_to(input.parts_count), "output number shards != input number of shards")


def test_result_table_data(workflow):
    """Проверяеем результат мержа веб-фичей с офферами по урлу
    Так же проверяем заполннеие полей msku и is_fake_msku_offer, необходимых для джойна стримов для мску
    """
    assert_that(workflow.result_tables[0].data, has_length(5), "Records count is correct")

    assert_that(
        workflow.result_tables[0].data,
        has_items(
            {'url': "white_url.ru/offer1", 'url_not_normalized': "white_url.ru/offer1", 'ware_md5': 'offer1',
             'ann_data': 'white1', 'part': 0, 'table_index': 1, 'msku': 1, 'is_fake_msku_offer': False},
            {'url': "white_url.ru/offer2", 'url_not_normalized': "white_url.ru/offer2", 'ware_md5': 'offer2',
             'ann_data': 'white2', 'part': 0, 'table_index': 1, 'msku': 0, 'is_fake_msku_offer': False},

            {'url': "market.yandex.ru/product/10?sku=1", 'url_not_normalized': "market.yandex.ru/product/10?sku=1", 'path': '/product/10?sku=1',
             'ann_data': '1', 'part': 0, 'table_index': 2, 'msku': 1, 'is_fake_msku_offer': False, 'ware_md5': 'blue_offer1'},
            {'url': "market.yandex.ru/product/10?sku=2", 'url_not_normalized': "market.yandex.ru/product/10?sku=2", 'path': '/product/10?sku=2',
             'ann_data': '2', 'part': 0, 'table_index': 2, 'msku': 2, 'is_fake_msku_offer': False, 'ware_md5': 'blue_offer2'},
            {'url': "market.yandex.ru/product/10?sku=1", 'url_not_normalized': "market.yandex.ru/product/10?sku=1", 'path': '/product/10?sku=1',
             'ann_data': '1', 'part': 0, 'table_index': 2, 'msku': 1, 'is_fake_msku_offer': True, 'ware_md5': 'msku1'},
        )
    )
