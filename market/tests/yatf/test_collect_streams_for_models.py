# coding=utf-8
"""
Test is used for checking result of collect_streams call for models.
As a result, it generates intersection as YT table.

Тест используется для проверки работы вызова collect_streams для моделей
Коротко, collect_streams пересекает модельную табличку и вебовскую табличку с данными о стримах.
В результате, генерится выходная таблица, являющихся пересечением по urls.
"""

import pytest

from hamcrest import assert_that, is_not

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.streams.yatf.test_env import YtStreamsTestEnv
from market.idx.streams.yatf.resources.yt_streams_data import YTStreamsData
from market.idx.streams.yatf.resources.yt_streams_output import YtStreamsOutput

from market.idx.streams.yatf.utils import StreamsMatcher, StreamFactory, NormalizedUrlMatcher, add_fake_streams, ModelFactory


# Input data

@pytest.fixture(scope='module')
def url_with_big_stream():
    return "http://market.yandex.ru/product/7769949?big=true"


@pytest.fixture(scope='module')
def skipped_urls():
    return ["http://www.skipped1.ru", "https://skipped2.com, http://market.yandex.fi/product/77"]


@pytest.fixture(scope='module')
def market_model_urls():
    return [
        "http://market.yandex.ru/product/1",
        "http://market.yandex.by/product/2",
        "http://market.yandex.kz/product/3",
        "http://market.yandex.com/product/4",
        "http://market.yandex.ua/product/5",
    ]


# the same urls as not_normilized_urls but with other params in query
@pytest.fixture(scope='module')
def not_normilized_urls_for_streams():
    urls = [
        ('http://market.yandex.ru/product/7?'
         'id=black_list&'
         'url=www.good.ru&url2=opana&url-shop=shop&'
         'cpc=true&show-uid=uid&fee-show=fee&'
         'url-point-info=a&url-outlet=out&url-mobicard=url&'
         'url-cpa=cpa&url-show-phone=pp&frommarket=yes&ymclid=1&'
         '_openstat=1&r1=1&r2=2&r3=3&r4=4&rs=s&rc=c&from=test&'
         'utm_prefix=yep&referer_pr=gromoi&referrer_pr=gromoi'
         'cvosrc=a&extid=77&wb_tp=tp&src=src&prov=yes&wt_mc=mc&'
         'yandex_market=yes&mrkt=1&roistat=e&partner=yes&'
         'reff=op&ref=mdee&hid=666'),
        "http://market.yandex.ru/product/8?id=mixed&cpc=true&stay=true&hid=666",
        "http://market.yandex.ru/product/9?cpc=true&hid=6666&stay=true&id=mixed2",
        "http://market.yandex.ru/product/10?id=mixed&black_list=false",
        "http://market.yandex.ru/product/11?id=mixed_prefix&utm_pref=yep&black-list-prefix=true&referer_gr=gromoi",
    ]

    return urls


# Expected normilized urls which should be transformed based on not_normilized_urls
@pytest.fixture(scope='module')
def normilized_urls():
    urls = [
        "market.yandex.ru/product/7",
        "market.yandex.ru/product/8",
        "market.yandex.ru/product/9",
        "market.yandex.ru/product/10",
        "market.yandex.ru/product/11",
    ]

    return urls


_BIG_PART = "999999"


# Input YT web table with stream data
@pytest.fixture(scope='module')
def web_streams(yt_stuff, skipped_urls, url_with_big_stream, market_model_urls, not_normilized_urls_for_streams):
    web_streams_path = "//userfeat/exports/web/IndexAnnSourceData"
    rows = []

    add_fake_streams(rows, skipped_urls)
    add_fake_streams(rows, market_model_urls)
    add_fake_streams(rows, not_normilized_urls_for_streams)

    # append streams for common url: small and big
    add_fake_streams(rows, [url_with_big_stream])
    rows.append(StreamFactory.stream(url_with_big_stream, url_with_big_stream + _BIG_PART))

    return YtTableResource(yt_stuff, web_streams_path, data=rows)


@pytest.fixture(scope='module')
def input_path():
    return "//home/indexer/streams/models"


@pytest.fixture(scope='module')
def input_shard(yt_stuff, input_path, market_model_urls, not_normilized_urls_for_streams, url_with_big_stream):
    rows = []

    rows.extend(ModelFactory.models(market_model_urls))
    rows.extend(ModelFactory.models(not_normilized_urls_for_streams))
    rows.extend(ModelFactory.models([url_with_big_stream]))

    shard = YtTableResource(yt_stuff, input_path, data=rows)

    return [shard]


# web table with streams and offers table
@pytest.fixture(scope='module')
def input_data(web_streams, input_shard, input_path):
    res = YTStreamsData(web_streams, input_shard, input_path)

    return res


# output info and YT tables
@pytest.fixture(scope='module')
def stream_output():
    parts_count = 0
    working_path = "//indexer/streams/tmp"
    streams_path = "//indexer/streams/ann_data"
    output_path = streams_path

    return YtStreamsOutput(working_path, streams_path, output_path, parts_count, is_model=True)


# Execution of binary

@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data, stream_output):
    resources = {
        "input": input_data,
        "output": stream_output
    }

    with YtStreamsTestEnv(**resources) as env:
        env.execute(yt_stuff, is_models=True)
        env.verify()
        yield env


# Helper functions

def should_see_streams(res_data, streams):
    assert_that(res_data, StreamsMatcher(streams), "All streams should be in result table")


def should_not_see_streams(res_data, streams):
    assert_that(res_data, is_not(StreamsMatcher(streams)), "These streams shouldn't be in result table")


def should_see_normalized_urls(res_data, urls):
    assert_that(res_data, NormalizedUrlMatcher(urls), "All normilized urls should be presented in ann_data table")


@pytest.fixture(scope='module')
def result_shard_table(workflow):
    return workflow.result_tables[0]


# Tests

def test_result_table_count(workflow):
    assert_that(len(workflow.result_tables), 1, "output number shards != input number of shards")


def test_streams_for_good_urls_are_presented(result_shard_table, market_model_urls):
    should_see_streams(
        result_shard_table.data,
        [
            'market.yandex.ru/product/1',
            'market.yandex.ru/product/10',
            'market.yandex.ru/product/11',
            'market.yandex.ru/product/2',
            'market.yandex.ru/product/3',
            'market.yandex.ru/product/4',
            'market.yandex.ru/product/5',
            'market.yandex.ru/product/7',
            'market.yandex.ru/product/7769949',
            'market.yandex.ru/product/8',
            'market.yandex.ru/product/9'
        ]
    )


def test_streams_for_skipped_urls_are_absent(result_shard_table, skipped_urls):
    should_not_see_streams(result_shard_table.data, skipped_urls)


# Check that if we have several urls in web table which will be normilized to the same url, collect_streams chooses the biggest one.
def test_choose_of_the_biggest_stream(result_shard_table, url_with_big_stream):
    should_see_streams(result_shard_table.data, ['market.yandex.ru/product/7769949'])


def test_url_normalization(result_shard_table, normilized_urls):
    should_see_normalized_urls(result_shard_table.data, normilized_urls)
